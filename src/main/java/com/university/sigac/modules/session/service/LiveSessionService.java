package com.university.sigac.modules.session.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.parameter.ParameterService;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.core.util.HtmlSanitizer;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestForm;
import com.university.sigac.modules.requests.dto.SessionDtos.AgendaItemDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.entity.SessionSummon;
import com.university.sigac.modules.requests.enums.DecisionOutcome;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.requests.service.AgendaService;
import com.university.sigac.modules.requests.service.CouncilRequestService;
import com.university.sigac.modules.requests.service.SessionMapper;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendanceDto;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendanceItem;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendanceRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.AttendeeDto;
import com.university.sigac.modules.session.dto.LiveSessionDtos.DecisionRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.MiscItemRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.NoteRequest;
import com.university.sigac.modules.session.dto.LiveSessionDtos.QuorumDto;
import com.university.sigac.modules.session.entity.Attendance;
import com.university.sigac.modules.session.repository.AttendanceRepository;
import com.university.sigac.modules.session.util.QuorumCalculator;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.service.UserLookupService;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Module 4: attendance and quorum, session control, decisions, debate notes and miscellaneous items. */
@Service
@RequiredArgsConstructor
public class LiveSessionService {

    private final AgendaService agenda;
    private final AttendanceRepository attendances;
    private final UserLookupService users;
    private final ParameterService parameters;
    private final CouncilRequestService requestService;
    private final AuditService audit;
    private final AppProperties props;

    // ---------------------------------------------------------------- Attendance and quorum (RF-21)

    @Transactional(readOnly = true)
    public AttendanceDto attendance(Long sessionId) {
        return buildAttendance(agenda.loadWithAccess(sessionId));
    }

    @Transactional
    public AttendanceDto recordAttendance(Long sessionId, AttendanceRequest req) {
        Session s = agenda.loadWithAccess(sessionId);
        if (!Set.of(SessionStatus.CONVENED, SessionStatus.STARTED, SessionStatus.SUSPENDED).contains(s.getStatus())) {
            throw ApiException.conflict("Attendance cannot be recorded with the session in status " + s.getStatus());
        }
        Set<Long> recordable = recordableIds(s);
        for (AttendanceItem item : req.items()) {
            if (!recordable.contains(item.userId())) {
                throw ApiException.badRequest("User " + item.userId() + " is not a voting member of this Council");
            }
            Attendance a = attendances.findBySessionIdAndUserId(s.getId(), item.userId()).orElseGet(() -> {
                Attendance created = new Attendance();
                created.setSession(s);
                created.setUser(users.reference(item.userId()));
                return created;
            });
            a.setPresent(item.present());
            attendances.save(a);
        }
        attendances.flush();
        return buildAttendance(s);
    }

    // ---------------------------------------------------------------- Session control (RF-22)

    @Transactional
    public SessionDto start(Long sessionId) {
        Session s = agenda.loadWithAccess(sessionId);
        requireTransition(s, SessionStatus.STARTED);
        requireQuorum(s);
        s.setStatus(SessionStatus.STARTED);
        s.setActualStart(LocalDateTime.now(ZoneId.of(props.getZone())));
        audit.record("EVENT_SESSION_STARTED", "SESSION", s.getId(), null);
        return SessionMapper.toDto(s);
    }

    @Transactional
    public SessionDto suspend(Long sessionId) {
        Session s = agenda.loadWithAccess(sessionId);
        requireTransition(s, SessionStatus.SUSPENDED);
        s.setStatus(SessionStatus.SUSPENDED);
        audit.record("EVENT_SESSION_SUSPENDED", "SESSION", s.getId(), null);
        return SessionMapper.toDto(s);
    }

    @Transactional
    public SessionDto resume(Long sessionId) {
        Session s = agenda.loadWithAccess(sessionId);
        if (!s.hasStatus(SessionStatus.SUSPENDED)) {
            throw ApiException.conflict("Only a SUSPENDED session can be resumed");
        }
        requireQuorum(s);
        s.setStatus(SessionStatus.STARTED);
        audit.record("EVENT_SESSION_RESUMED", "SESSION", s.getId(), null);
        return SessionMapper.toDto(s);
    }

    /**
     * Formal closing. Points still without a decision are deferred: the request returns to approved
     * so it can be placed on a later agenda, and the minutes can be generated.
     */
    @Transactional
    public SessionDto finish(Long sessionId) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = agenda.loadWithAccess(sessionId);
        requireTransition(s, SessionStatus.FINISHED);
        for (AgendaItem p : s.getItems()) {
            if (p.hasDecision()) {
                continue;
            }
            p.setOutcome(DecisionOutcome.POSTPONED);
            p.setVotesFor(0);
            p.setVotesAgainst(0);
            p.setAbstentions(0);
            p.setDecidedBy(u.name());
            if (p.getRequest() != null && p.getRequest().getStatus() == RequestStatus.ON_AGENDA) {
                requestService.changeStatus(p.getRequest(), RequestStatus.APPROVED,
                        "Deferred at the close of session " + s.getId() + "; it can be scheduled again", u.name());
            }
            audit.record("EVENT_ITEM_DEFERRED", "AGENDA_ITEM", p.getId(), p.getTitle());
        }
        s.setStatus(SessionStatus.FINISHED);
        s.setActualEnd(LocalDateTime.now(ZoneId.of(props.getZone())));
        s.getItems().stream()
                .filter(p -> p.getRequest() != null && p.getRequest().getStatus() == RequestStatus.ON_AGENDA)
                .forEach(p -> requestService.changeStatus(p.getRequest(), RequestStatus.PROCESSED,
                        "Processed in session " + s.getId(), u.name()));
        audit.record("EVENT_SESSION_FINISHED", "SESSION", s.getId(), null);
        return SessionMapper.toDto(s);
    }

    // ---------------------------------------------------------------- Decisions, notes and miscellaneous items

    /** RF-23. RN-12: correcting a recorded decision requires a reason and leaves a trace in the audit log. */
    @Transactional
    public AgendaItemDto recordDecision(Long sessionId, Long itemId, DecisionRequest req) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = agenda.loadWithAccess(sessionId);
        requireStatus(s, SessionStatus.STARTED, "Decisions can only be recorded while the session is STARTED");
        QuorumDto quorum = computeQuorum(s);
        if (!quorum.reached()) {
            throw ApiException.conflict("No deliberative quorum: " + quorum.required()
                    + " members must be present and there are " + quorum.present());
        }
        AgendaItem p = findItem(s, itemId);
        checkVotes(req, quorum.present());
        if (p.hasDecision()) {
            boolean same = p.getOutcome() == req.outcome()
                    && p.getVotesFor() == req.votesFor()
                    && p.getVotesAgainst() == req.votesAgainst()
                    && p.getAbstentions() == req.abstentions();
            if (same) {
                return SessionMapper.toItem(p);
            }
            if (req.changeReason() == null || req.changeReason().isBlank()) {
                throw ApiException.badRequest("Para modificar la decisión indique el motivo del cambio");
            }
            audit.record("EVENT_DECISION_MODIFIED", "AGENDA_ITEM", p.getId(), "Before: " + p.getOutcome() + " ("
                    + p.getVotesFor() + "/" + p.getVotesAgainst() + "/" + p.getAbstentions() + "). After: "
                    + req.outcome() + " (" + req.votesFor() + "/" + req.votesAgainst() + "/" + req.abstentions()
                    + "). Reason: " + req.changeReason().trim());
        } else {
            audit.record("EVENT_DECISION_REGISTERED", "AGENDA_ITEM", p.getId(), req.outcome().name());
        }
        p.setOutcome(req.outcome());
        p.setVotesFor(req.votesFor());
        p.setVotesAgainst(req.votesAgainst());
        p.setAbstentions(req.abstentions());
        p.setDecidedBy(u.name());
        return SessionMapper.toItem(p);
    }

    /** RF-25: appends an intervention (basic, sanitized rich text) to the item's notes. */
    @Transactional
    public AgendaItemDto addNote(Long sessionId, Long itemId, NoteRequest req) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = agenda.loadWithAccess(sessionId);
        requireStatus(s, SessionStatus.STARTED, "Debate notes can only be recorded while the session is STARTED");
        AgendaItem p = findItem(s, itemId);
        String clean = HtmlSanitizer.cleanFragment(req.content());
        if (clean.isBlank()) {
            throw ApiException.badRequest("The note does not contain valid text");
        }
        String note = "<div><strong>" + HtmlSanitizer.escapeXml(u.name()) + ":</strong> " + clean + "</div>";
        p.setDebateNotes((p.getDebateNotes() == null ? "" : p.getDebateNotes()) + note);
        return SessionMapper.toItem(p);
    }

    /** Files the official form and adds it to the agenda of the session in progress. */
    @Transactional
    public SessionDto addMiscellaneousRequest(Long sessionId, CouncilRequestForm form, List<MultipartFile> files,
                                              List<String> documentCodes) {
        return agenda.addMiscellaneousRequest(sessionId, form, files, documentCodes);
    }

    /** RF-26: item not included in the original agenda. */
    @Transactional
    public SessionDto addMiscItem(Long sessionId, MiscItemRequest req) {
        Session s = agenda.loadWithAccess(sessionId);
        requireStatus(s, SessionStatus.STARTED, "Miscellaneous items can only be created while the session is STARTED");
        AgendaItem p = new AgendaItem();
        p.setSession(s);
        p.setPosition(s.getItems().size() + 1);
        p.setTitle(req.title().trim());
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.setMiscellaneous(true);
        s.getItems().add(p);
        audit.record("EVENT_MISC_ITEM_ADDED", "SESSION", s.getId(), p.getTitle());
        return SessionMapper.toDto(s);
    }

    // ---------------------------------------------------------------- Quorum and attendees (also used by the minutes generator)

    /** Deliberative quorum of the people summoned. A virtual council uses the same rule. */
    public QuorumDto computeQuorum(Session s) {
        List<User> members = members(s);
        Set<Long> ids = members.stream().map(User::getId).collect(Collectors.toSet());
        int present = (int) attendances.findBySessionId(s.getId()).stream()
                .filter(a -> a.isPresent() && ids.contains(a.getUser().getId())).count();
        if (!s.kind().requiresQuorum()) {
            return new QuorumDto(members.size(), present, 0, 0, true, false);
        }
        int percentage = parameters.getInt(ParameterService.QUORUM_PERCENTAGE, 50);
        return new QuorumDto(members.size(), present, QuorumCalculator.required(members.size(), percentage),
                percentage, QuorumCalculator.reached(members.size(), present, percentage), true);
    }

    /**
     * Voting members, plus a program head summoned to sit as a counselor.
     * The program head can be marked present; they do not count toward the quorum.
     */
    public List<AttendeeDto> attendees(Session s) {
        Map<Long, Boolean> present = attendances.findBySessionId(s.getId()).stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), Attendance::isPresent));
        List<User> people = new ArrayList<>(members(s));
        people.addAll(summonedProgramHeads(s));
        return people.stream().map(u -> new AttendeeDto(u.getId(), u.getName(), SessionMapper.seat(u),
                present.getOrDefault(u.getId(), false))).toList();
    }

    // ---------------------------------------------------------------- private

    /** Voting members plus program heads cited to this session. Quorum still uses {@link #members}. */
    private Set<Long> recordableIds(Session s) {
        Set<Long> ids = members(s).stream().map(User::getId).collect(Collectors.toCollection(HashSet::new));
        summonedProgramHeads(s).forEach(user -> ids.add(user.getId()));
        return ids;
    }

    /** Program heads summoned to sit as counselors, excluding anyone who already votes. */
    private List<User> summonedProgramHeads(Session s) {
        Set<Long> voting = members(s).stream().map(User::getId).collect(Collectors.toSet());
        return s.getSummons().stream().map(SessionSummon::getUser)
                .filter(user -> user.isActive() && user.hasRole(RoleName.ROLE_PROGRAM_HEAD) && !voting.contains(user.getId()))
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Voting members summoned for this session. Sessions created before that choice still use the whole faculty council. */
    private List<User> members(Session s) {
        if (s.getSummons().isEmpty()) {
            return users.findActiveByFacultyAndRoles(s.getFaculty().getId(), RoleName.VOTING);
        }
        return s.getSummons().stream().map(SessionSummon::getUser)
                .filter(user -> user.isActive() && user.getRoles().stream().anyMatch(RoleName.VOTING::contains))
                .sorted(Comparator.comparingInt(SessionMapper::summonOrder).thenComparing(User::getName))
                .toList();
    }

    private AttendanceDto buildAttendance(Session s) {
        return new AttendanceDto(s.getStatus(), computeQuorum(s), attendees(s));
    }

    private AgendaItem findItem(Session s, Long itemId) {
        return s.getItems().stream().filter(p -> p.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("The item does not belong to this session"));
    }

    private void requireStatus(Session s, SessionStatus expected, String message) {
        if (!s.hasStatus(expected)) {
            throw ApiException.conflict(message + " (current status: " + s.getStatus() + ")");
        }
    }

    private void requireTransition(Session s, SessionStatus target) {
        if (!s.getStatus().canTransitionTo(target)) {
            throw ApiException.conflict("Transition not allowed: " + s.getStatus() + " → " + target);
        }
    }

    /** Only the voting members who are present can cast a vote, and the result has to match the count. */
    private void checkVotes(DecisionRequest req, int present) {
        int inFavor = req.votesFor();
        int against = req.votesAgainst();
        int abstentions = req.abstentions();
        if (inFavor > present || against > present || abstentions > present || inFavor + against + abstentions > present) {
            throw ApiException.badRequest("Los votos no pueden superar a los " + present + " presentes con voto");
        }
        if (req.outcome() == DecisionOutcome.APPROVED && inFavor <= against) {
            throw ApiException.badRequest("Para aprobar, los votos a favor tienen que ser más que los en contra");
        }
        if (req.outcome() == DecisionOutcome.DENIED && against <= inFavor) {
            throw ApiException.badRequest("Para negar, los votos en contra tienen que ser más que los a favor");
        }
    }

    private void requireQuorum(Session s) {
        QuorumDto q = computeQuorum(s);
        if (!q.reached()) {
            throw ApiException.conflict("No deliberative quorum: " + q.required()
                    + " members must be present and there are " + q.present());
        }
    }
}

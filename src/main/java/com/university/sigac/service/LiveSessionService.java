package com.university.sigac.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.dto.SessionDtos.AgendaItemDto;
import com.university.sigac.dto.SessionDtos.AttendanceDto;
import com.university.sigac.dto.SessionDtos.AttendanceItem;
import com.university.sigac.dto.SessionDtos.AttendanceRequest;
import com.university.sigac.dto.SessionDtos.AttendeeDto;
import com.university.sigac.dto.SessionDtos.DecisionRequest;
import com.university.sigac.dto.SessionDtos.MiscItemRequest;
import com.university.sigac.dto.SessionDtos.NoteRequest;
import com.university.sigac.dto.SessionDtos.QuorumDto;
import com.university.sigac.dto.SessionDtos.SessionDto;
import com.university.sigac.entity.AgendaItem;
import com.university.sigac.entity.Attendance;
import com.university.sigac.entity.Session;
import com.university.sigac.entity.User;
import com.university.sigac.enums.RequestStatus;
import com.university.sigac.enums.RoleName;
import com.university.sigac.enums.SessionStatus;
import com.university.sigac.exception.ApiException;
import com.university.sigac.repository.AttendanceRepository;
import com.university.sigac.repository.UserRepository;
import com.university.sigac.security.AuthenticatedUser;
import com.university.sigac.security.SecurityUtils;
import com.university.sigac.util.HtmlSanitizer;
import com.university.sigac.util.QuorumCalculator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 4: attendance and quorum, session control, decisions, debate notes and miscellaneous items. */
@Service
@RequiredArgsConstructor
public class LiveSessionService {

    private final AgendaService agenda;
    private final AttendanceRepository attendances;
    private final UserRepository users;
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
        Set<Long> members = members(s).stream().map(User::getId).collect(Collectors.toSet());
        for (AttendanceItem item : req.items()) {
            if (!members.contains(item.userId())) {
                throw ApiException.badRequest("User " + item.userId() + " is not a voting member of this Council");
            }
            Attendance a = attendances.findBySessionIdAndUserId(s.getId(), item.userId()).orElseGet(() -> {
                Attendance created = new Attendance();
                created.setSession(s);
                created.setUser(users.getReferenceById(item.userId()));
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

    /** Formal closing: every item must have a decision (precondition for generating the minutes). */
    @Transactional
    public SessionDto finish(Long sessionId) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = agenda.loadWithAccess(sessionId);
        requireTransition(s, SessionStatus.FINISHED);
        List<String> pending = s.getItems().stream().filter(p -> !p.hasDecision())
                .map(p -> p.getPosition() + ". " + p.getTitle()).toList();
        if (!pending.isEmpty()) {
            throw ApiException.unprocessable("There are items without a recorded decision; the session cannot be finished", pending);
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
        int totalVotes = req.votesFor() + req.votesAgainst() + req.abstentions();
        if (totalVotes > quorum.present()) {
            throw ApiException.badRequest("The total number of votes (" + totalVotes + ") exceeds the attendance (" + quorum.present() + ")");
        }
        if (p.hasDecision()) {
            if (req.changeReason() == null || req.changeReason().isBlank()) {
                throw ApiException.badRequest("RN-12: changing a recorded decision requires a reason");
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

    // ---------------------------------------------------------------- Quorum (also used by the minutes generator)

    /** Active members of the Faculty with voting rights. */
    public List<User> members(Session s) {
        return users.findActiveByFacultyAndRoles(s.getFaculty().getId(), RoleName.VOTING);
    }

    public QuorumDto computeQuorum(Session s) {
        List<User> members = members(s);
        Set<Long> ids = members.stream().map(User::getId).collect(Collectors.toSet());
        int present = (int) attendances.findBySessionId(s.getId()).stream()
                .filter(a -> a.isPresent() && ids.contains(a.getUser().getId())).count();
        int percentage = parameters.getInt(ParameterService.QUORUM_PERCENTAGE, 50);
        return new QuorumDto(members.size(), present, QuorumCalculator.required(members.size(), percentage),
                percentage, QuorumCalculator.reached(members.size(), present, percentage));
    }

    // ---------------------------------------------------------------- private

    private AttendanceDto buildAttendance(Session s) {
        Map<Long, Boolean> present = attendances.findBySessionId(s.getId()).stream()
                .collect(Collectors.toMap(a -> a.getUser().getId(), Attendance::isPresent));
        List<AttendeeDto> attendees = members(s).stream().map(u -> new AttendeeDto(u.getId(), u.getName(),
                u.hasRole(RoleName.ROLE_PRESIDENT) ? "President" : "Council member", present.getOrDefault(u.getId(), false))).toList();
        return new AttendanceDto(s.getStatus(), computeQuorum(s), attendees);
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

    private void requireQuorum(Session s) {
        QuorumDto q = computeQuorum(s);
        if (!q.reached()) {
            throw ApiException.conflict("No deliberative quorum: " + q.required()
                    + " members must be present and there are " + q.present());
        }
    }
}

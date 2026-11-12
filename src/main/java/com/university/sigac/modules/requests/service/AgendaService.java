package com.university.sigac.modules.requests.service;

import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.pdf.Letterhead;
import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.core.util.DateUtil;
import com.university.sigac.core.util.HtmlSanitizer;
import com.university.sigac.modules.minutes.entity.Minutes;
import com.university.sigac.modules.minutes.repository.MinutesRepository;
import com.university.sigac.modules.notifications.service.EmailService;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestForm;
import com.university.sigac.modules.requests.dto.SessionDtos.GuestRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionCorrection;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionSummaryDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SummonedDto;
import com.university.sigac.modules.requests.dto.SessionDtos.UrgentItemRequest;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.Attachment;
import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.entity.SessionGuest;
import com.university.sigac.modules.requests.entity.SessionSummon;
import com.university.sigac.modules.requests.enums.DecisionOutcome;
import com.university.sigac.modules.requests.enums.RequestBranch;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.RequestType;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.users.enums.CouncilKind;
import com.university.sigac.modules.requests.repository.AgendaItemRepository;
import com.university.sigac.modules.requests.repository.CouncilRequestRepository;
import com.university.sigac.modules.requests.repository.SessionRepository;
import com.university.sigac.modules.session.repository.AttendanceRepository;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.service.CouncilTypeService;
import com.university.sigac.modules.users.service.HierarchyService;
import com.university.sigac.modules.users.service.UserLookupService;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Module 3 (Council Secretary): sessions, agenda builder and agenda closing with notice (RF-18, RF-19). */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final String NOTICE_TEMPLATE_PATH = "/sigac/templates/citacion.html";
    private static final String NOTICE_TEMPLATE = readNoticeTemplate();
    /**
     * People who can be summoned. The program head is cited when they sit as a counselor.
     * The faculty secretary files requests and is not cited.
     */
    private static final List<RoleName> SUMMONABLE = List.of(
            RoleName.ROLE_PRESIDENT, RoleName.ROLE_COUNCIL_MEMBER, RoleName.ROLE_PROGRAM_HEAD,
            RoleName.ROLE_COUNCIL_SECRETARY);

    private final SessionRepository sessions;
    private final CouncilRequestRepository requests;
    private final AgendaItemRepository agendaItems;
    private final CouncilTypeService councilTypes;
    private final HierarchyService hierarchy;
    private final UserLookupService users;
    private final CouncilRequestService requestService;
    private final PdfService pdf;
    private final Letterhead letterhead;
    private final StorageService storage;
    private final EmailService email;
    private final AuditService audit;
    private final MinutesRepository minutes;
    private final AttendanceRepository attendance;
    private final SessionSupportsLinkService supportsLinks;

    @Transactional
    public SessionDto create(SessionRequest req) {
        AuthenticatedUser u = SecurityUtils.current();
        if (u.facultyId() == null) {
            throw ApiException.forbidden("Your user is not linked to a Faculty");
        }
        CouncilType type = councilTypes.find(req.councilTypeId());
        if (type.getKind() == null) {
            throw ApiException.badRequest("This council type is no longer in use");
        }
        String location = req.location() == null || req.location().isBlank() ? null : req.location().trim();
        if (location == null && type.getKind().requiresLocation()) {
            throw ApiException.badRequest("The location is required for a session of type " + type.getName());
        }
        List<User> chosen = summoned(u.facultyId(), req.memberIds());
        Session s = new Session();
        s.setCouncilType(type);
        s.setFaculty(hierarchy.facultyReference(u.facultyId()));
        s.setScheduledAt(req.scheduledAt());
        s.setLocation(location);
        for (User person : chosen) {
            SessionSummon summon = new SessionSummon();
            summon.setSession(s);
            summon.setUser(person);
            s.getSummons().add(summon);
        }
        sessions.save(s);
        audit.record("EVENT_SESSION_CREATED", "SESSION", s.getId(), type.getName() + ", " + chosen.size() + " summoned");
        return SessionMapper.toDto(s);
    }

    /** Admin correction of the council type, date and place. The agenda and the summons stay as they are. */
    @Transactional
    public SessionDto correct(Long id, SessionCorrection req) {
        Session s = loadWithAccess(id);
        CouncilType type = councilTypes.find(req.councilTypeId());
        if (type.getKind() == null) {
            throw ApiException.badRequest("This council type is no longer in use");
        }
        String location = req.location() == null || req.location().isBlank() ? null : req.location().trim();
        if (location == null && type.getKind().requiresLocation()) {
            throw ApiException.badRequest("The location is required for a session of type " + type.getName());
        }
        s.setCouncilType(type);
        s.setScheduledAt(req.scheduledAt());
        s.setLocation(location);
        audit.record("EVENT_SESSION_UPDATED", "SESSION", s.getId(), type.getName());
        return SessionMapper.toDto(s);
    }

    /**
     * Admin removal of a session, its notice data, attendance and minutes.
     * Requests that were on the agenda go back to approved so they can be scheduled again.
     */
    @Transactional
    public void delete(Long id) {
        Session s = loadWithAccess(id);
        minutes.findBySessionId(id).ifPresent(this::deleteMinutes);
        minutes.flush();
        attendance.deleteBySessionId(id);
        String name = SecurityUtils.current().name();
        for (AgendaItem item : s.getItems()) {
            CouncilRequest request = item.getRequest();
            if (request != null && (request.getStatus() == RequestStatus.ON_AGENDA || request.getStatus() == RequestStatus.PROCESSED)) {
                requestService.changeStatus(request, RequestStatus.APPROVED, "La sesión fue eliminada por el administrador", name);
            }
        }
        sessions.delete(s);
        audit.record("EVENT_SESSION_DELETED", "SESSION", id, s.getCouncilType().getName());
    }

    private void deleteMinutes(Minutes minutesRecord) {
        if (minutesRecord.getFilePath() != null) {
            storage.deleteQuietly(minutesRecord.getFilePath());
        }
        minutes.delete(minutesRecord);
    }

    /** President, counselors and the council secretary of the current faculty, for the schedule form. */
    @Transactional(readOnly = true)
    public List<SummonedDto> summonable() {
        AuthenticatedUser u = SecurityUtils.current();
        if (u.facultyId() == null) {
            throw ApiException.forbidden("Your user is not linked to a Faculty");
        }
        return users.findActiveByFacultyAndRoles(u.facultyId(), SUMMONABLE).stream()
                .sorted(Comparator.comparingInt(SessionMapper::summonOrder).thenComparing(User::getName))
                .map(person -> new SummonedDto(person.getId(), person.getName(), SessionMapper.seat(person)))
                .toList();
    }

    /** Replaces who will receive the notice. After the agenda is closed the citation is already sent. */
    @Transactional
    public SessionDto updateSummons(Long id, List<Long> memberIds) {
        Session s = loadWithOpenAgenda(id);
        List<User> chosen = summoned(s.getFaculty().getId(), memberIds);
        Set<Long> wanted = chosen.stream().map(User::getId).collect(Collectors.toSet());
        s.getSummons().removeIf(summon -> !wanted.contains(summon.getUser().getId()));
        Set<Long> present = s.getSummons().stream().map(summon -> summon.getUser().getId()).collect(Collectors.toSet());
        for (User person : chosen) {
            if (present.contains(person.getId())) {
                continue;
            }
            SessionSummon summon = new SessionSummon();
            summon.setSession(s);
            summon.setUser(person);
            s.getSummons().add(summon);
        }
        sessions.saveAndFlush(s);
        audit.record("EVENT_SESSION_SUMMONS_UPDATED", "SESSION", s.getId(), chosen.size() + " summoned");
        return SessionMapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public Page<SessionSummaryDto> list(SessionStatus status, Pageable pageable) {
        AuthenticatedUser u = SecurityUtils.current();
        LocalDateTime now = LocalDateTime.now();
        Specification<Session> filter = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!u.isGlobal()) {
                ps.add(cb.equal(root.get("faculty").get("id"), u.facultyId()));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                Expression<?> chosen = sortExpression(root, cb, pageable.getSort());
                if (chosen != null) {
                    boolean asc = pageable.getSort().iterator().next().isAscending();
                    query.orderBy(asc ? cb.asc(chosen) : cb.desc(chosen), cb.asc(root.get("id")));
                } else {
                    Expression<LocalDateTime> at = root.get("scheduledAt");
                    Expression<Integer> upcomingFirst = cb.<Integer>selectCase()
                            .when(cb.greaterThanOrEqualTo(at, now), 0)
                            .otherwise(1);
                    Expression<LocalDateTime> soonestFirst = cb.<LocalDateTime>selectCase()
                            .when(cb.greaterThanOrEqualTo(at, now), at)
                            .otherwise(LocalDateTime.of(9999, 12, 31, 23, 59));
                    query.orderBy(cb.asc(upcomingFirst), cb.asc(soonestFirst), cb.desc(at));
                }
            }
            return ps.isEmpty() ? cb.conjunction() : cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return sessions.findAll(filter, page).map(SessionMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public SessionDto get(Long id) {
        return SessionMapper.toDto(loadWithAccess(id));
    }

    // ---------------------------------------------------------------- Agenda

    @Transactional
    public SessionDto addItem(Long sessionId, Long requestId) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = loadForItemChange(sessionId);
        CouncilRequest req = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
        releaseDeferred(req);
        if (!req.getFaculty().getId().equals(s.getFaculty().getId())) {
            throw ApiException.badRequest("The request belongs to another Faculty");
        }
        if (req.getStatus() != RequestStatus.APPROVED) {
            throw ApiException.conflict("Only APPROVED requests can be added (current status: " + req.getStatus() + ")");
        }
        AgendaItem p = new AgendaItem();
        p.setSession(s);
        p.setPosition(s.getItems().size() + 1);
        p.setTitle(req.getTitle());
        p.setDescription(req.getDescription());
        p.setRequest(req);
        s.getItems().add(p);
        requestService.changeStatus(req, RequestStatus.ON_AGENDA, "Added to the agenda of session " + s.getId(), u.name());
        sessions.saveAndFlush(s);
        audit.record("EVENT_AGENDA_ITEM_ADDED", "SESSION", s.getId(), req.getCode());
        return SessionMapper.toDto(s);
    }

    /** Extraordinary Council: an urgent matter goes on the agenda without a filed and approved request. */
    @Transactional
    public SessionDto addUrgentItem(Long sessionId, UrgentItemRequest req) {
        Session s = loadForItemChange(sessionId);
        if (!s.kind().allowsUrgentItems()) {
            throw ApiException.conflict("Urgent items can only be added to an Extraordinary Council session");
        }
        AgendaItem p = new AgendaItem();
        p.setSession(s);
        p.setPosition(s.getItems().size() + 1);
        p.setTitle(req.title().trim());
        p.setDescription(req.description() == null || req.description().isBlank() ? null : req.description().trim());
        p.setUrgent(true);
        s.getItems().add(p);
        sessions.saveAndFlush(s);
        audit.record("EVENT_URGENT_ITEM_ADDED", "SESSION", s.getId(), p.getTitle());
        return SessionMapper.toDto(s);
    }

    @Transactional
    public SessionDto removeItem(Long sessionId, Long itemId) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = loadForItemChange(sessionId);
        AgendaItem p = findItem(s, itemId);
        if (p.hasDecision()) {
            throw ApiException.conflict("A point that already has a decision cannot be removed from the agenda");
        }
        s.getItems().remove(p);
        if (p.getRequest() != null) {
            requestService.changeStatus(p.getRequest(), RequestStatus.APPROVED, "Removed from the agenda", u.name());
        }
        renumber(s);
        sessions.saveAndFlush(s);
        return SessionMapper.toDto(s);
    }

    /** RF-18 (drag & drop): receives the item ids in the new order. */
    @Transactional
    public SessionDto reorder(Long sessionId, List<Long> orderedIds) {
        Session s = loadForItemChange(sessionId);
        Set<Long> current = s.getItems().stream().map(AgendaItem::getId).collect(Collectors.toSet());
        if (orderedIds.size() != current.size() || !current.equals(Set.copyOf(orderedIds))) {
            throw ApiException.badRequest("You must send exactly the agenda items, without repetitions");
        }
        int position = 1;
        for (Long id : orderedIds) {
            findItem(s, id).setPosition(position++);
        }
        s.getItems().sort((a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
        return SessionMapper.toDto(s);
    }

    // ---------------------------------------------------------------- Guests (Expanded Council)

    @Transactional
    public SessionDto addGuest(Long sessionId, GuestRequest req) {
        Session s = loadWithOpenAgenda(sessionId);
        if (!s.kind().allowsGuests()) {
            throw ApiException.conflict("Guests can only be invited to an Expanded Council session");
        }
        SessionGuest g = new SessionGuest();
        g.setSession(s);
        g.setName(req.name().trim());
        g.setEmail(req.email() == null || req.email().isBlank() ? null : req.email().trim().toLowerCase());
        g.setAffiliation(req.affiliation() == null || req.affiliation().isBlank() ? null : req.affiliation().trim());
        s.getGuests().add(g);
        sessions.saveAndFlush(s);
        audit.record("EVENT_SESSION_GUEST_ADDED", "SESSION", s.getId(), g.getName());
        return SessionMapper.toDto(s);
    }

    @Transactional
    public SessionDto removeGuest(Long sessionId, Long guestId) {
        Session s = loadWithOpenAgenda(sessionId);
        SessionGuest g = s.getGuests().stream().filter(x -> x.getId().equals(guestId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("The guest does not belong to this session"));
        s.getGuests().remove(g);
        sessions.saveAndFlush(s);
        return SessionMapper.toDto(s);
    }

    // ---------------------------------------------------------------- Notice

    /**
     * Files a request with the same form used at the counter and puts it on the live agenda as a miscellaneous point.
     */
    @Transactional
    public SessionDto addMiscellaneousRequest(Long sessionId, CouncilRequestForm form, List<MultipartFile> files,
                                              List<String> documentCodes) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = loadForItemChange(sessionId);
        if (!inProgress(s)) {
            throw ApiException.conflict("A miscellaneous request can only be added while the session is in progress (current status: "
                    + s.getStatus() + ")");
        }
        var filed = requestService.file(form, files, documentCodes);
        CouncilRequest req = requests.findById(filed.id()).orElseThrow(() -> ApiException.notFound("Request not found"));
        if (!req.getFaculty().getId().equals(s.getFaculty().getId())) {
            throw ApiException.badRequest("The request belongs to another Faculty");
        }
        AgendaItem p = new AgendaItem();
        p.setSession(s);
        p.setPosition(s.getItems().size() + 1);
        p.setTitle(req.getTitle());
        p.setDescription(req.getDescription());
        p.setRequest(req);
        p.setMiscellaneous(true);
        s.getItems().add(p);
        requestService.changeStatus(req, RequestStatus.ON_AGENDA,
                "Added as a miscellaneous item during session " + s.getId(), u.name());
        sessions.saveAndFlush(s);
        audit.record("EVENT_MISC_REQUEST_ADDED", "SESSION", s.getId(), req.getCode());
        return SessionMapper.toDto(s);
    }

    /** Freezes the agenda, generates the notice PDF and emails it with a ZIP of agenda supports. */
    @Transactional
    public SessionDto closeAgenda(Long sessionId) {
        Session s = loadWithOpenAgenda(sessionId);
        if (s.getItems().isEmpty()) {
            throw ApiException.conflict("The agenda has no items: add at least one approved request or urgent item");
        }
        s.setAgendaClosed(true);
        s.setStatus(SessionStatus.CONVENED);
        audit.record("EVENT_AGENDA_CLOSED", "SESSION", s.getId(), s.getItems().size() + " items");
        issueAndSendNotice(s);
        return SessionMapper.toDto(s);
    }

    /** Regenerates the notice and ZIP, then emails them again to the summoned members and guests. */
    @Transactional
    public SessionDto resendNotice(Long sessionId) {
        Session s = loadWithAccess(sessionId);
        if (!s.isAgendaClosed()) {
            throw ApiException.conflict("The notice has not been issued yet: the agenda is not closed");
        }
        if (s.hasStatus(SessionStatus.FINISHED)) {
            throw ApiException.conflict("The session is already finished; the notice cannot be resent");
        }
        issueAndSendNotice(s);
        audit.record("EVENT_NOTICE_RESENT", "SESSION", s.getId(), "notice resent");
        return SessionMapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public byte[] downloadNotice(Long sessionId) {
        Session s = loadWithAccess(sessionId);
        if (!s.isAgendaClosed()) {
            throw ApiException.conflict("The notice has not been issued yet: the agenda is not closed");
        }
        return renderNotice(s);
    }

    /**
     * Brevo and similar relays often reset the SMTP connection above ~2 MB (MIME expands ~33%).
     * Keep the ZIP on disk always; only attach it when the email stays under the safe limit.
     */
    private static final long MAX_NOTICE_EMAIL_BYTES = 1_800_000L;

    private void issueAndSendNotice(Session s) {
        byte[] notice = renderNotice(s);
        String noticePath = noticePath(s.getId());
        storage.save(noticePath, notice);

        List<String> attachments = new ArrayList<>();
        attachments.add(noticePath);
        boolean zipAttached = false;
        String downloadToken = null;
        String supportsZip = buildSupportsZip(s);
        if (supportsZip != null) {
            // Commit the token before SMTP so a failed recipient cannot invalidate the link.
            downloadToken = supportsLinks.ensureToken(s.getId());
            s.setSupportsDownloadToken(downloadToken);
            long zipBytes = storageSize(supportsZip);
            if (notice.length + zipBytes <= MAX_NOTICE_EMAIL_BYTES) {
                attachments.add(supportsZip);
                zipAttached = true;
            } else {
                log.warn("Supports ZIP for session {} is {} KB; email will carry the notice PDF only",
                        s.getId(), zipBytes / 1024);
            }
        }
        List<String> recipients = noticeRecipients(s);
        s.getGuests().stream().map(SessionGuest::getEmail).filter(e -> e != null && !recipients.contains(e))
                .forEach(recipients::add);
        email.sendNotice(recipients, s.getCouncilType().getName(), s.getFaculty().getName(),
                DateUtil.longDate(s.getScheduledAt().toLocalDate()), DateUtil.time(s.getScheduledAt()),
                s.documentLocation(), attachments, zipAttached, downloadToken);
    }

    /** ZIP of agenda supports (built on close/resend). Rebuilds if the file is missing. */
    @Transactional(readOnly = true)
    public byte[] downloadSupportsZip(Long sessionId) {
        Session s = loadWithAccess(sessionId);
        if (!s.isAgendaClosed()) {
            throw ApiException.conflict("The notice has not been issued yet: the agenda is not closed");
        }
        return readOrBuildSupportsZip(s);
    }

    /** Public download by opaque token from the citation email (no login). */
    @Transactional(readOnly = true)
    public byte[] downloadSupportsByToken(String token) {
        if (token == null || token.isBlank()) {
            throw ApiException.notFound("Download link not found or expired");
        }
        Session s = sessions.findBySupportsDownloadToken(token.trim())
                .orElseThrow(() -> ApiException.notFound("Download link not found or expired"));
        if (!s.isAgendaClosed()) {
            throw ApiException.conflict("The notice has not been issued yet: the agenda is not closed");
        }
        return readOrBuildSupportsZip(s);
    }

    private byte[] readOrBuildSupportsZip(Session s) {
        String path = supportsZipPath(s.getId());
        if (!storage.exists(path)) {
            String built = buildSupportsZip(s);
            if (built == null) {
                throw ApiException.conflict("This session has no support documents to download");
            }
            path = built;
        }
        return storage.read(path);
    }

    private long storageSize(String relativePath) {
        long size = storage.size(relativePath);
        return size < 0 ? Long.MAX_VALUE : size;
    }

    private byte[] renderNotice(Session s) {
        String html = pdf.render(letterhead.embed(NOTICE_TEMPLATE), noticeModel(s));
        return pdf.generatePdfA(HtmlSanitizer.sanitizeDocument(html));
    }

    // ---------------------------------------------------------------- used by the session, minutes and repository modules

    /** Loads the session checking that it belongs to the user's Faculty (RN-02). */
    public Session loadWithAccess(Long id) {
        Session s = sessions.findById(id).orElseThrow(() -> ApiException.notFound("Session not found"));
        SecurityUtils.checkFaculty(SecurityUtils.current(), s.getFaculty().getId());
        return s;
    }

    @Transactional(readOnly = true)
    public long countByFaculty(Long facultyId, Collection<SessionStatus> statuses) {
        return sessions.countByFacultyIdAndStatusIn(facultyId, statuses);
    }

    // ---------------------------------------------------------------- private

    private Session loadWithOpenAgenda(Long id) {
        Session s = loadWithAccess(id);
        if (s.isAgendaClosed()) {
            throw ApiException.conflict("The agenda has already been closed and published; it cannot be modified (RN-09)");
        }
        return s;
    }

    /** Open while the notice is being prepared, and again once the session has started. */
    private Session loadForItemChange(Long id) {
        Session s = loadWithAccess(id);
        if (!s.isAgendaClosed() || inProgress(s)) {
            return s;
        }
        throw ApiException.conflict("The agenda has already been closed and published; it cannot be modified (RN-09)");
    }

    private static boolean inProgress(Session s) {
        return s.hasStatus(SessionStatus.STARTED) || s.hasStatus(SessionStatus.SUSPENDED);
    }

    /** A point deferred at the close of a finished session can be placed on a later agenda. */
    private void releaseDeferred(CouncilRequest req) {
        agendaItems.findByRequest_Id(req.getId()).ifPresent(previous -> {
            if (previous.getSession().hasStatus(SessionStatus.FINISHED) && previous.getOutcome() == DecisionOutcome.POSTPONED) {
                previous.setRequest(null);
                agendaItems.saveAndFlush(previous);
                return;
            }
            throw ApiException.conflict("The request is already on an agenda");
        });
    }

    private AgendaItem findItem(Session s, Long itemId) {
        return s.getItems().stream().filter(p -> p.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("The item does not belong to this session"));
    }

    private void renumber(Session s) {
        int position = 1;
        for (AgendaItem p : s.getItems()) {
            p.setPosition(position++);
        }
    }

    /** Column keys the sessions table may sort by. Anything else keeps the default order. */
    private Expression<?> sortExpression(jakarta.persistence.criteria.Root<Session> root,
                                         jakarta.persistence.criteria.CriteriaBuilder cb, Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            return null;
        }
        String key = sort.iterator().next().getProperty();
        return switch (key) {
            case "type" -> root.get("councilType").get("name");
            case "faculty" -> root.get("faculty").get("name");
            case "date" -> root.get("scheduledAt");
            case "location" -> root.get("location");
            case "status" -> root.get("status");
            case "items" -> cb.size(root.get("items"));
            default -> null;
        };
    }

    private String noticePath(Long sessionId) {
        return "citaciones/" + sessionId + "/citacion.pdf";
    }

    private String supportsZipPath(Long sessionId) {
        return "citaciones/" + sessionId + "/soportes-orden-del-dia.zip";
    }

    /**
     * One folder per agenda item with the request's PDFs. Returns null when there is nothing to pack.
     */
    private String buildSupportsZip(Session s) {
        boolean any = s.getItems().stream()
                .anyMatch(p -> p.getRequest() != null && !p.getRequest().getAttachments().isEmpty());
        if (!any) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Set<String> usedEntries = new HashSet<>();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (AgendaItem point : s.getItems()) {
                CouncilRequest req = point.getRequest();
                if (req == null || req.getAttachments().isEmpty()) {
                    continue;
                }
                String folder = zipFolderName(point, req);
                for (Attachment file : req.getAttachments()) {
                    if (!storage.exists(file.getPath())) {
                        continue;
                    }
                    String entry = uniqueZipEntry(usedEntries, folder + "/" + safeFileName(file.getOriginalName()));
                    zip.putNextEntry(new ZipEntry(entry));
                    zip.write(storage.read(file.getPath()));
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new ApiException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not build the agenda supports ZIP");
        }
        if (usedEntries.isEmpty()) {
            return null;
        }
        String path = supportsZipPath(s.getId());
        storage.save(path, out.toByteArray());
        return path;
    }

    private static String zipFolderName(AgendaItem point, CouncilRequest req) {
        String code = req.getCode() == null || req.getCode().isBlank() ? "solicitud" : req.getCode();
        return String.format(Locale.ROOT, "%02d-%s", point.getPosition(), StorageService.slug(code));
    }

    private static String safeFileName(String original) {
        String name = original == null || original.isBlank() ? "documento.pdf" : original.trim();
        name = name.replace('\\', '_').replace('/', '_');
        return StorageService.slug(name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name)
                + (name.contains(".") ? name.substring(name.lastIndexOf('.')) : ".pdf");
    }

    private static String uniqueZipEntry(Set<String> used, String preferred) {
        if (used.add(preferred)) {
            return preferred;
        }
        int dot = preferred.lastIndexOf('.');
        String base = dot > 0 ? preferred.substring(0, dot) : preferred;
        String ext = dot > 0 ? preferred.substring(dot) : "";
        int n = 2;
        String candidate;
        do {
            candidate = base + "-" + n++ + ext;
        } while (!used.add(candidate));
        return candidate;
    }

    private Map<String, Object> noticeModel(Session s) {
        int year = s.getScheduledAt().getYear();
        long ordinal = sessions.countNoticeOrdinal(s.getFaculty().getId(), year, s.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("university", s.getFaculty().getCampus().getUniversity().getName());
        m.put("campus", s.getFaculty().getCampus().getName());
        m.put("faculty", s.getFaculty().getName());
        m.put("councilType", s.getCouncilType().getName());
        m.put("sessionKindLabel", sessionKindLabel(s.kind()));
        m.put("sessionNumber", String.format(Locale.ROOT, "%03d", ordinal));
        m.put("sessionYear", String.valueOf(year));
        m.put("sessionDate", DateUtil.longDate(s.getScheduledAt().toLocalDate()));
        m.put("sessionWeekdayDate", DateUtil.weekdayLongDate(s.getScheduledAt().toLocalDate()));
        m.put("sessionTime", DateUtil.time(s.getScheduledAt()));
        m.put("location", s.documentLocation());
        m.put("requestGroups", buildRequestGroups(s));
        m.put("hasRequestGroups", s.getItems().stream().anyMatch(p -> p.getRequest() != null || p.isUrgent()));
        List<Map<String, Object>> misc = new ArrayList<>();
        int miscIndex = 1;
        for (AgendaItem p : s.getItems()) {
            if (!p.isMiscellaneous() || p.getRequest() != null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", "5." + miscIndex++);
            row.put("text", noticeItemText(p));
            misc.add(row);
        }
        m.put("miscItems", misc);
        m.put("hasMiscItems", !misc.isEmpty());
        m.put("guests", SessionMapper.toGuestsModel(s));
        m.put("hasGuests", !s.getGuests().isEmpty());
        return m;
    }

    private static String sessionKindLabel(CouncilKind kind) {
        return switch (kind) {
            case ORDINARY -> "ordinaria";
            case EXTRAORDINARY -> "extraordinaria";
            case VIRTUAL -> "virtual";
            case EXPANDED -> "ampliada";
        };
    }

    /**
     * Groups filed agenda items under section 4 (SOLICITUDES), numbered 4.1 / 4.1.1 like a faculty citation.
     */
    private List<Map<String, Object>> buildRequestGroups(Session s) {
        Map<String, List<AgendaItem>> byGroup = new LinkedHashMap<>();
        for (AgendaItem p : s.getItems()) {
            if (p.isMiscellaneous() && p.getRequest() == null) {
                continue;
            }
            String group = agendaGroupTitle(p);
            byGroup.computeIfAbsent(group, key -> new ArrayList<>()).add(p);
        }
        List<String> titles = new ArrayList<>(byGroup.keySet());
        titles.sort(String.CASE_INSENSITIVE_ORDER);
        List<Map<String, Object>> groups = new ArrayList<>();
        int groupIndex = 1;
        for (String title : titles) {
            Map<String, Object> group = new LinkedHashMap<>();
            String groupCode = "4." + groupIndex++;
            group.put("code", groupCode);
            group.put("title", title);
            List<Map<String, Object>> items = new ArrayList<>();
            int itemIndex = 1;
            for (AgendaItem p : byGroup.get(title)) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("code", groupCode + "." + itemIndex++);
                row.put("text", noticeItemText(p));
                items.add(row);
            }
            group.put("items", items);
            groups.add(group);
        }
        return groups;
    }

    private static String agendaGroupTitle(AgendaItem p) {
        if (p.getRequest() == null) {
            return p.isUrgent() ? "ASUNTOS URGENTES" : "OTROS ASUNTOS";
        }
        return switch (p.getRequest().getRequestType()) {
            case COURSE_CANCELLATION, PERIOD_CANCELLATION, CANCELLATION -> "CANCELACIONES";
            case PETITION -> "PETICIONES";
            case COMPLAINT -> "QUEJAS";
            case CLAIM -> "RECLAMOS";
            case REENTRY -> "REINGRESOS";
            case GRADE_RECTIFICATION -> "RECTIFICACIÓN DE NOTAS";
            case SPECIAL_CASE -> "CASOS ESPECIALES";
            case EVALUATION -> "EVALUACIONES";
            case PERMISSION -> "PERMISOS";
            case COMMISSION -> "COMISIONES";
            case CONTEST -> "CONCURSOS";
            case STUDY_PLAN -> "PLANES DE ESTUDIO";
            case SUBJECT -> "ASIGNATURAS";
            case PROJECT_ENDORSEMENT, CALL, RESOURCES, SEEDBED -> "INVESTIGACIÓN";
            case SOCIAL_PROJECT, SOCIAL_PRACTICE, EXTENSION_AGREEMENT, SOCIAL_ACTIVITY -> "PROYECCIÓN SOCIAL";
        };
    }

    private static String noticeItemText(AgendaItem p) {
        CouncilRequest req = p.getRequest();
        if (req == null) {
            String text = p.getTitle() == null ? "Asunto" : p.getTitle().trim();
            if (p.isUrgent()) {
                text = text + " (urgente)";
            }
            return text;
        }
        String subject = noticeSubject(req);
        StringBuilder text = new StringBuilder(subject);
        if (req.getAcademicPeriod() != null && !req.getAcademicPeriod().isBlank()) {
            text.append(' ').append(req.getAcademicPeriod().trim());
        }
        if (req.getRequester() != null && !req.getRequester().isBlank()) {
            boolean student = req.getRequestType().branch() == RequestBranch.STUDENT;
            text.append(student ? ", estudiante " : ", ").append(req.getRequester().trim());
        }
        if (req.getStudentCode() != null && !req.getStudentCode().isBlank()) {
            text.append(", cód. ").append(req.getStudentCode().trim());
        }
        if (req.getProgram() != null && req.getProgram().getName() != null && !req.getProgram().getName().isBlank()) {
            text.append(" (").append(req.getProgram().getName().trim()).append(')');
        }
        return text.toString();
    }

    private static String noticeSubject(CouncilRequest req) {
        RequestType topic = req.getRequestType();
        String base = switch (topic) {
            case PETITION -> "Petición";
            case COMPLAINT -> "Queja";
            case CLAIM -> "Reclamo";
            case REENTRY -> "Reingreso";
            case COURSE_CANCELLATION, CANCELLATION -> "Cancelación de curso";
            case PERIOD_CANCELLATION -> "Cancelación de periodo académico";
            case GRADE_RECTIFICATION -> "Rectificación de notas";
            case SPECIAL_CASE -> "Caso especial";
            case EVALUATION -> "Evaluación";
            case PERMISSION -> "Permiso";
            case COMMISSION -> "Comisión";
            case CONTEST -> "Concurso";
            case STUDY_PLAN -> "Plan de estudios";
            case SUBJECT -> "Asignatura";
            case PROJECT_ENDORSEMENT -> "Aval de proyecto";
            case CALL -> "Convocatoria";
            case RESOURCES -> "Asignación de recursos";
            case SEEDBED -> "Semillero";
            case SOCIAL_PROJECT -> "Aval de proyecto de proyección social";
            case SOCIAL_PRACTICE -> "Práctica social";
            case EXTENSION_AGREEMENT -> "Convenio de extensión";
            case SOCIAL_ACTIVITY -> "Actividad de proyección social";
        };
        if (req.getOtherDetail() == null || req.getOtherDetail().isBlank()) {
            return base;
        }
        return base + ": " + req.getOtherDetail().trim();
    }

    /** Sessions scheduled before this choice keep the previous recipients: the whole faculty council. */
    private List<String> noticeRecipients(Session s) {
        if (s.getSummons().isEmpty()) {
            return new ArrayList<>(users.activeEmailsByFacultyAndRoles(s.getFaculty().getId(), SUMMONABLE));
        }
        return s.getSummons().stream().map(summon -> summon.getUser().getEmail())
                .filter(email -> email != null && !email.isBlank()).distinct().collect(Collectors.toCollection(ArrayList::new));
    }

    private List<User> summoned(Long facultyId, List<Long> memberIds) {
        Map<Long, User> eligible = users.findActiveByFacultyAndRoles(facultyId, SUMMONABLE).stream()
                .collect(Collectors.toMap(User::getId, person -> person));
        List<Long> ids = new ArrayList<>(memberIds);
        Long self = SecurityUtils.current().id();
        if (eligible.containsKey(self) && !ids.contains(self)) {
            ids.add(self);
        }
        List<User> chosen = new ArrayList<>();
        for (Long id : ids.stream().distinct().toList()) {
            User person = eligible.get(id);
            if (person == null) {
                throw ApiException.badRequest("User " + id + " cannot be summoned to this council");
            }
            chosen.add(person);
        }
        boolean voting = chosen.stream().anyMatch(person -> person.getRoles().stream().anyMatch(RoleName.VOTING::contains));
        if (!voting) {
            throw ApiException.badRequest("Summon at least one voting member: the president or a counselor");
        }
        chosen.sort(Comparator.comparingInt(SessionMapper::summonOrder).thenComparing(User::getName));
        return chosen;
    }

    private static String readNoticeTemplate() {
        try (InputStream in = AgendaService.class.getResourceAsStream(NOTICE_TEMPLATE_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + NOTICE_TEMPLATE_PATH);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the notice template", e);
        }
    }
}

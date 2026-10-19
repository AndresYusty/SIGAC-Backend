package com.university.sigac.modules.requests.service;

import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.core.util.DateUtil;
import com.university.sigac.core.util.HtmlSanitizer;
import com.university.sigac.modules.notifications.service.EmailService;
import com.university.sigac.modules.requests.dto.SessionDtos.GuestRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionDto;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionRequest;
import com.university.sigac.modules.requests.dto.SessionDtos.SessionSummaryDto;
import com.university.sigac.modules.requests.dto.SessionDtos.UrgentItemRequest;
import com.university.sigac.modules.requests.entity.AgendaItem;
import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.entity.SessionGuest;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.requests.repository.CouncilRequestRepository;
import com.university.sigac.modules.requests.repository.SessionRepository;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.service.CouncilTypeService;
import com.university.sigac.modules.users.service.HierarchyService;
import com.university.sigac.modules.users.service.UserLookupService;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 3 (Council Secretary): sessions, agenda builder and agenda closing with notice (RF-18, RF-19). */
@Service
@RequiredArgsConstructor
public class AgendaService {

    private static final String NOTICE_TEMPLATE_PATH = "/sigac/templates/notice.html";
    private static final String NOTICE_TEMPLATE = readNoticeTemplate();

    private final SessionRepository sessions;
    private final CouncilRequestRepository requests;
    private final CouncilTypeService councilTypes;
    private final HierarchyService hierarchy;
    private final UserLookupService users;
    private final CouncilRequestService requestService;
    private final PdfService pdf;
    private final StorageService storage;
    private final EmailService email;
    private final AuditService audit;

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
        Session s = new Session();
        s.setCouncilType(type);
        s.setFaculty(hierarchy.facultyReference(u.facultyId()));
        s.setScheduledAt(req.scheduledAt());
        s.setLocation(location);
        sessions.save(s);
        audit.record("EVENT_SESSION_CREATED", "SESSION", s.getId(), type.getName());
        return SessionMapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public Page<SessionSummaryDto> list(SessionStatus status, Pageable pageable) {
        AuthenticatedUser u = SecurityUtils.current();
        Specification<Session> filter = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!u.isGlobal()) {
                ps.add(cb.equal(root.get("faculty").get("id"), u.facultyId()));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return sessions.findAll(filter, pageable).map(SessionMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public SessionDto get(Long id) {
        return SessionMapper.toDto(loadWithAccess(id));
    }

    // ---------------------------------------------------------------- Agenda

    @Transactional
    public SessionDto addItem(Long sessionId, Long requestId) {
        AuthenticatedUser u = SecurityUtils.current();
        Session s = loadWithOpenAgenda(sessionId);
        CouncilRequest req = requests.findById(requestId).orElseThrow(() -> ApiException.notFound("Request not found"));
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
        Session s = loadWithOpenAgenda(sessionId);
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
        Session s = loadWithOpenAgenda(sessionId);
        AgendaItem p = findItem(s, itemId);
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
        Session s = loadWithOpenAgenda(sessionId);
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

    /** Freezes the agenda (RN-09), generates the notice PDF and emails it to the members and guests. */
    @Transactional
    public SessionDto closeAgenda(Long sessionId) {
        Session s = loadWithOpenAgenda(sessionId);
        if (s.getItems().isEmpty()) {
            throw ApiException.conflict("The agenda has no items: add at least one approved request or urgent item");
        }
        byte[] notice = pdf.generatePdfA(HtmlSanitizer.sanitizeDocument(pdf.render(NOTICE_TEMPLATE, noticeModel(s))));
        String noticePath = noticePath(s.getId());
        storage.save(noticePath, notice);

        List<String> attachments = new ArrayList<>();
        attachments.add(noticePath);
        s.getItems().stream().filter(p -> p.getRequest() != null)
                .forEach(p -> p.getRequest().getAttachments().forEach(a -> attachments.add(a.getPath())));
        List<String> recipients = new ArrayList<>(users.activeEmailsByFacultyAndRoles(s.getFaculty().getId(),
                List.of(RoleName.ROLE_PRESIDENT, RoleName.ROLE_COUNCIL_MEMBER, RoleName.ROLE_COUNCIL_SECRETARY)));
        s.getGuests().stream().map(SessionGuest::getEmail).filter(e -> e != null && !recipients.contains(e))
                .forEach(recipients::add);

        s.setAgendaClosed(true);
        s.setStatus(SessionStatus.CONVENED);
        audit.record("EVENT_AGENDA_CLOSED", "SESSION", s.getId(), s.getItems().size() + " items");
        email.sendNotice(recipients, s.getCouncilType().getName(), s.getFaculty().getName(),
                DateUtil.longDate(s.getScheduledAt().toLocalDate()), DateUtil.time(s.getScheduledAt()),
                s.documentLocation(), attachments);
        return SessionMapper.toDto(s);
    }

    @Transactional(readOnly = true)
    public byte[] downloadNotice(Long sessionId) {
        Session s = loadWithAccess(sessionId);
        if (!s.isAgendaClosed()) {
            throw ApiException.conflict("The notice has not been issued yet: the agenda is not closed");
        }
        return storage.read(noticePath(s.getId()));
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

    private String noticePath(Long sessionId) {
        return "notices/" + sessionId + "/notice.pdf";
    }

    private Map<String, Object> noticeModel(Session s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("university", s.getFaculty().getCampus().getUniversity().getName());
        m.put("campus", s.getFaculty().getCampus().getName());
        m.put("faculty", s.getFaculty().getName());
        m.put("councilType", s.getCouncilType().getName());
        m.put("sessionDate", DateUtil.longDate(s.getScheduledAt().toLocalDate()));
        m.put("sessionTime", DateUtil.time(s.getScheduledAt()));
        m.put("location", s.documentLocation());
        List<Map<String, Object>> agenda = new ArrayList<>();
        for (AgendaItem p : s.getItems()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("number", p.getPosition());
            item.put("title", p.getTitle());
            item.put("description", p.getDescription() == null || p.getDescription().isBlank() ? null : p.getDescription());
            item.put("urgent", p.isUrgent());
            agenda.add(item);
        }
        m.put("agenda", agenda);
        m.put("guests", SessionMapper.toGuestsModel(s));
        m.put("hasGuests", !s.getGuests().isEmpty());
        return m;
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

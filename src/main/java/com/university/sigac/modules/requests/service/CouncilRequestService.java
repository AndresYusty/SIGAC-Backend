package com.university.sigac.modules.requests.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.parameter.ParameterService;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.core.sequence.SequenceService;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.core.util.HashUtil;
import com.university.sigac.modules.notifications.service.EmailService;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.AttachmentDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestForm;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.FileContent;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.HistoryDto;
import com.university.sigac.modules.requests.entity.Attachment;
import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.entity.RequestHistory;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.repository.CouncilRequestRepository;
import com.university.sigac.modules.requests.repository.RequestHistoryRepository;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.service.HierarchyService;
import com.university.sigac.modules.users.service.UserLookupService;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Module 3: filing (Secretary 2) and review (Secretary 1) of council requests. */
@Service
@RequiredArgsConstructor
public class CouncilRequestService {

    private static final Set<RequestStatus> EDITABLE = Set.of(RequestStatus.FILED, RequestStatus.RETURNED);

    private final CouncilRequestRepository requests;
    private final RequestHistoryRepository history;
    private final UserLookupService users;
    private final HierarchyService hierarchy;
    private final ParameterService parameters;
    private final StorageService storage;
    private final SequenceService sequences;
    private final AuditService audit;
    private final EmailService email;
    private final AppProperties props;

    // ================================================================ Secretary 2

    @Transactional
    public CouncilRequestDto file(CouncilRequestForm req, List<MultipartFile> files) {
        AuthenticatedUser u = SecurityUtils.current();
        if (u.facultyId() == null) {
            throw ApiException.forbidden("Your user is not linked to a Faculty");
        }
        validatePdfs(files);

        CouncilRequest s = new CouncilRequest();
        s.setTitle(req.title().trim());
        s.setDescription(req.description().trim());
        s.setRequester(req.requester().trim());
        s.setRequestType(req.requestType());
        s.setFaculty(hierarchy.facultyReference(u.facultyId()));
        s.setProgram(resolveProgram(req.programId(), u));
        s.setFiledBy(users.reference(u.id()));
        int year = Year.now(ZoneId.of(props.getZone())).getValue();
        s.setCode("REQ-" + year + "-" + String.format("%06d", sequences.next("REQ-" + year)));
        requests.save(s);

        saveAttachments(s, files);
        recordHistory(s, null, RequestStatus.FILED, "Request filed", u.name());
        audit.record("EVENT_REQUEST_CREATED", "REQUEST", s.getId(), s.getCode());
        return toDto(s);
    }

    /** Correction of a FILED or RETURNED request (RN-09: the others can no longer be modified). */
    @Transactional
    public CouncilRequestDto update(Long id, CouncilRequestForm req, List<MultipartFile> newAttachments) {
        AuthenticatedUser u = SecurityUtils.current();
        CouncilRequest s = load(id);
        if (!s.getFiledBy().getId().equals(u.id())) {
            throw ApiException.forbidden("Only the user who filed the request can modify it");
        }
        if (!EDITABLE.contains(s.getStatus())) {
            throw ApiException.conflict("The request cannot be modified in status " + s.getStatus());
        }
        validatePdfs(newAttachments);
        s.setTitle(req.title().trim());
        s.setDescription(req.description().trim());
        s.setRequester(req.requester().trim());
        s.setRequestType(req.requestType());
        s.setProgram(resolveProgram(req.programId(), u));
        saveAttachments(s, newAttachments);
        if (s.getStatus() == RequestStatus.RETURNED) {
            changeStatus(s, RequestStatus.FILED, "Resubmitted after corrections", u.name());
        }
        audit.record("EVENT_REQUEST_UPDATED", "REQUEST", s.getId(), s.getCode());
        return toDto(s);
    }

    // ================================================================ Queries

    @Transactional(readOnly = true)
    public Page<CouncilRequestDto> search(RequestStatus status, String code, LocalDate from, LocalDate to, Pageable pageable) {
        AuthenticatedUser u = SecurityUtils.current();
        ZoneId zone = ZoneId.of(props.getZone());
        Specification<CouncilRequest> filter = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (!u.isGlobal()) {
                // Secretary 2 only sees what they filed; the other roles see their Faculty (RN-02)
                if (u.hasRole("ROLE_SECRETARY_2")) {
                    ps.add(cb.equal(root.get("filedBy").get("id"), u.id()));
                } else {
                    ps.add(cb.equal(root.get("faculty").get("id"), u.facultyId()));
                }
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (code != null && !code.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("code")), "%" + code.trim().toLowerCase() + "%"));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("filedAt"), from.atStartOfDay(zone).toInstant()));
            }
            if (to != null) {
                ps.add(cb.lessThan(root.get("filedAt"), to.plusDays(1).atStartOfDay(zone).toInstant()));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return requests.findAll(filter, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public CouncilRequestDto get(Long id) {
        return toDto(loadWithAccess(id));
    }

    @Transactional(readOnly = true)
    public List<HistoryDto> history(Long id) {
        loadWithAccess(id);
        return history.findByRequestIdOrderByOccurredAtAscIdAsc(id).stream()
                .map(h -> new HistoryDto(h.getPreviousStatus(), h.getNewStatus(), h.getReason(), h.getUserName(), h.getOccurredAt()))
                .toList();
    }

    /** Downloads an attachment, checking that its SHA-256 fingerprint has not changed. */
    @Transactional(readOnly = true)
    public FileContent downloadAttachment(Long requestId, Long attachmentId) {
        CouncilRequest s = loadWithAccess(requestId);
        Attachment a = s.getAttachments().stream().filter(x -> x.getId().equals(attachmentId)).findFirst()
                .orElseThrow(() -> ApiException.notFound("Attachment not found"));
        byte[] data = storage.read(a.getPath());
        if (!HashUtil.matches(HashUtil.sha256Hex(data), a.getSha256Hash())) {
            audit.record("EVENT_INTEGRITY_VIOLATION", "ATTACHMENT", a.getId(), a.getOriginalName());
            throw ApiException.conflict("The attachment was altered: its SHA-256 fingerprint does not match the recorded one");
        }
        return new FileContent(a.getOriginalName(), data);
    }

    // ================================================================ Secretary 1: review

    @Transactional
    public CouncilRequestDto approve(Long id) {
        AuthenticatedUser u = SecurityUtils.current();
        CouncilRequest s = loadForReview(id, u);
        changeStatus(s, RequestStatus.APPROVED, "Approved by Secretary 1", u.name());
        audit.record("EVENT_REQUEST_APPROVED", "REQUEST", s.getId(), s.getCode());
        return toDto(s);
    }

    @Transactional
    public CouncilRequestDto reject(Long id, String reason) {
        AuthenticatedUser u = SecurityUtils.current();
        CouncilRequest s = loadForReview(id, u);
        changeStatus(s, RequestStatus.REJECTED, reason.trim(), u.name());
        audit.record("EVENT_REQUEST_REJECTED", "REQUEST", s.getId(), s.getCode());
        email.sendRequestReturned(s.getFiledBy().getEmail(), s.getCode(), s.getTitle(), reason.trim(), true);
        return toDto(s);
    }

    /** RN-10: returning a request requires a written justification, which is kept in the history. */
    @Transactional
    public CouncilRequestDto returnRequest(Long id, String reason) {
        AuthenticatedUser u = SecurityUtils.current();
        CouncilRequest s = loadForReview(id, u);
        changeStatus(s, RequestStatus.RETURNED, reason.trim(), u.name());
        audit.record("EVENT_REQUEST_RETURNED", "REQUEST", s.getId(), s.getCode());
        email.sendRequestReturned(s.getFiledBy().getEmail(), s.getCode(), s.getTitle(), reason.trim(), false);
        return toDto(s);
    }

    // ================================================================ Used by other services

    /** Changes the status and leaves a trace in the history. Must be called inside a transaction. */
    public void changeStatus(CouncilRequest s, RequestStatus newStatus, String reason, String userName) {
        RequestStatus previous = s.getStatus();
        s.setStatus(newStatus);
        recordHistory(s, previous, newStatus, reason, userName);
    }

    @Transactional(readOnly = true)
    public long countFiledBy(Long userId, RequestStatus status) {
        return requests.countByFiledByIdAndStatus(userId, status);
    }

    @Transactional(readOnly = true)
    public long countByFaculty(Long facultyId, RequestStatus status) {
        return requests.countByFacultyIdAndStatus(facultyId, status);
    }

    // ================================================================ private

    private void recordHistory(CouncilRequest s, RequestStatus previous, RequestStatus newStatus, String reason, String user) {
        RequestHistory h = new RequestHistory();
        h.setRequest(s);
        h.setPreviousStatus(previous);
        h.setNewStatus(newStatus);
        h.setReason(reason);
        h.setUserName(user);
        history.save(h);
    }

    /** RN-08: PDF only (extension + %PDF- signature) and a configurable maximum size (10 MB by default). */
    private void validatePdfs(List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        int maxMb = parameters.getInt(ParameterService.ATTACHMENTS_MAX_MB, 10);
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) {
                continue;
            }
            String name = f.getOriginalFilename();
            if (name == null || !name.toLowerCase().endsWith(".pdf")) {
                throw ApiException.badRequest("Only documents in PDF format are allowed");
            }
            if (f.getSize() > (long) maxMb * 1024 * 1024) {
                throw ApiException.payloadTooLarge("The document '" + name + "' exceeds the maximum limit of " + maxMb + " MB");
            }
            try (InputStream in = f.getInputStream()) {
                if (!"%PDF-".equals(new String(in.readNBytes(5), StandardCharsets.ISO_8859_1))) {
                    throw ApiException.badRequest("The file '" + name + "' is not a valid PDF");
                }
            } catch (IOException e) {
                throw ApiException.badRequest("Could not read the file '" + name + "'");
            }
        }
    }

    /** Saves each PDF to disk and records its path and SHA-256 hash. If something fails, deletes what was written. */
    private void saveAttachments(CouncilRequest s, List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        List<String> written = new ArrayList<>();
        try {
            int year = Year.now(ZoneId.of(props.getZone())).getValue();
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) {
                    continue;
                }
                byte[] data = f.getBytes();
                String path = "requests/" + year + "/" + s.getCode() + "/" + UUID.randomUUID() + "-"
                        + StorageService.slug(f.getOriginalFilename());
                storage.save(path, data);
                written.add(path);
                Attachment a = new Attachment();
                a.setRequest(s);
                a.setOriginalName(f.getOriginalFilename());
                a.setPath(path);
                a.setSha256Hash(HashUtil.sha256Hex(data));
                a.setSizeBytes(data.length);
                s.getAttachments().add(a);
            }
            requests.saveAndFlush(s);
        } catch (IOException | RuntimeException e) {
            written.forEach(storage::deleteQuietly);
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw ApiException.badRequest("Could not process the attached files");
        }
    }

    private Program resolveProgram(Long programId, AuthenticatedUser u) {
        Long id = programId != null ? programId : u.programId();
        if (id == null) {
            return null;
        }
        Program p = hierarchy.findProgram(id);
        if (!p.getFaculty().getId().equals(u.facultyId())) {
            throw ApiException.badRequest("The Program does not belong to your Faculty");
        }
        return p;
    }

    private CouncilRequest load(Long id) {
        return requests.findById(id).orElseThrow(() -> ApiException.notFound("Request not found"));
    }

    private CouncilRequest loadWithAccess(Long id) {
        AuthenticatedUser u = SecurityUtils.current();
        CouncilRequest s = load(id);
        SecurityUtils.checkFaculty(u, s.getFaculty().getId());
        if (u.hasRole("ROLE_SECRETARY_2") && !u.isGlobal() && !s.getFiledBy().getId().equals(u.id())) {
            throw ApiException.forbidden("You can only view the requests you filed");
        }
        return s;
    }

    private CouncilRequest loadForReview(Long id, AuthenticatedUser u) {
        CouncilRequest s = load(id);
        SecurityUtils.checkFaculty(u, s.getFaculty().getId());
        if (s.getStatus() != RequestStatus.FILED) {
            throw ApiException.conflict("Only FILED requests can be reviewed (current status: " + s.getStatus() + ")");
        }
        return s;
    }

    private CouncilRequestDto toDto(CouncilRequest s) {
        return new CouncilRequestDto(s.getId(), s.getCode(), s.getTitle(), s.getDescription(), s.getRequester(),
                s.getRequestType(), s.getStatus(), s.getFaculty().getId(), s.getFaculty().getName(),
                s.getProgram() == null ? null : s.getProgram().getId(),
                s.getProgram() == null ? null : s.getProgram().getName(),
                s.getFiledBy().getName(), s.getFiledAt(),
                s.getAttachments().stream().map(a -> new AttachmentDto(a.getId(), a.getOriginalName(), a.getSizeBytes(), a.getSha256Hash())).toList());
    }
}

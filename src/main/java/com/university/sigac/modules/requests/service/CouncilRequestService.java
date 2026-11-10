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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.AttachmentDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.BranchDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CatalogDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CouncilRequestForm;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.CourseLine;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.FileContent;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.HistoryDto;
import com.university.sigac.modules.requests.dto.CouncilRequestDtos.TopicDto;
import com.university.sigac.modules.requests.entity.Attachment;
import com.university.sigac.modules.requests.entity.CouncilRequest;
import com.university.sigac.modules.requests.entity.RequestCourse;
import com.university.sigac.modules.requests.entity.RequestHistory;
import com.university.sigac.modules.requests.enums.CancellationKind;
import com.university.sigac.modules.requests.enums.RequestBranch;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.RequestType;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.requests.repository.AgendaItemRepository;
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
import java.util.Arrays;
import java.util.HashSet;
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

/** Module 3: filing (Faculty Secretary) and review (Council Secretary) of council requests. */
@Service
@RequiredArgsConstructor
public class CouncilRequestService {

    private static final Set<RequestStatus> EDITABLE = Set.of(RequestStatus.FILED, RequestStatus.RETURNED);

    private final CouncilRequestRepository requests;
    private final RequestHistoryRepository history;
    private final AgendaItemRepository agendaItems;
    private final UserLookupService users;
    private final HierarchyService hierarchy;
    private final ParameterService parameters;
    private final StorageService storage;
    private final SequenceService sequences;
    private final AuditService audit;
    private final EmailService email;
    private final AppProperties props;
    private final ObjectMapper json;

    // ================================================================ Faculty Secretary

    @Transactional(readOnly = true)
    public CatalogDto catalog() {
        AuthenticatedUser u = SecurityUtils.current();
        String faculty = null;
        String campus = null;
        String program = null;
        Long programId = u.programId();
        if (u.facultyId() != null) {
            Faculty f = hierarchy.findFaculty(u.facultyId());
            faculty = f.getName();
            campus = f.getCampus().getName();
        }
        if (programId != null) {
            program = hierarchy.findProgram(programId).getName();
        }
        List<BranchDto> branches = Arrays.stream(RequestBranch.values())
                .map(branch -> new BranchDto(branch.name(), Arrays.stream(RequestType.values())
                        .filter(topic -> topic.branch() == branch && topic.inCatalog())
                        .map(topic -> new TopicDto(topic.name(), topic.documents(), topic.courses(), topic.grades(),
                                topic.otherDetail(), false))
                        .toList()))
                .toList();
        return new CatalogDto(faculty, campus, program, programId, branches);
    }

    @Transactional
    public CouncilRequestDto file(CouncilRequestForm req, List<MultipartFile> files, List<String> documentCodes) {
        AuthenticatedUser u = SecurityUtils.current();
        if (u.facultyId() == null) {
            throw ApiException.forbidden("Your user is not linked to a Faculty");
        }
        List<MultipartFile> documents = files == null ? List.of() : files;
        List<String> codes = documentCodes == null ? List.of() : documentCodes;
        validatePdfs(documents);
        checkDocumentCodes(req.requestType(), codes);

        CouncilRequest s = new CouncilRequest();
        s.setFaculty(hierarchy.facultyReference(u.facultyId()));
        s.setFiledBy(users.reference(u.id()));
        applyForm(s, req, u);
        int year = Year.now(ZoneId.of(props.getZone())).getValue();
        s.setCode("REQ-" + year + "-" + String.format("%06d", sequences.next("REQ-" + year)));
        requests.save(s);
        replaceCourses(s, req);
        saveAttachments(s, documents, codes);
        requireDocuments(s);
        recordHistory(s, null, RequestStatus.FILED, "Request filed", u.name());
        audit.record("EVENT_REQUEST_CREATED", "REQUEST", s.getId(), s.getCode());
        return toDto(s);
    }

    /** Correction of a FILED or RETURNED request (RN-09: the others can no longer be modified). The admin can correct any filing. */
    @Transactional
    public CouncilRequestDto update(Long id, CouncilRequestForm req, List<MultipartFile> newAttachments,
                                    List<String> documentCodes) {
        AuthenticatedUser u = SecurityUtils.current();
        if (u.hasRole("ROLE_ADMIN")) {
            return correctAsAdmin(id, req, newAttachments, documentCodes);
        }
        CouncilRequest s = load(id);
        if (!s.getFiledBy().getId().equals(u.id())) {
            throw ApiException.forbidden("Only the user who filed the request can modify it");
        }
        if (!EDITABLE.contains(s.getStatus())) {
            throw ApiException.conflict("The request cannot be modified in status " + s.getStatus());
        }
        List<MultipartFile> documents = newAttachments == null ? List.of() : newAttachments;
        List<String> codes = documentCodes == null ? List.of() : documentCodes;
        validatePdfs(documents);
        checkDocumentCodes(req.requestType(), codes);
        applyForm(s, req, u);
        replaceCourses(s, req);
        saveAttachments(s, documents, codes);
        requireDocuments(s);
        if (s.getStatus() == RequestStatus.RETURNED) {
            changeStatus(s, RequestStatus.FILED, "Resubmitted after corrections", u.name());
        }
        audit.record("EVENT_REQUEST_UPDATED", "REQUEST", s.getId(), s.getCode());
        return toDto(s);
    }

    /** Removes the filing, its history and its place on an agenda. */
    @Transactional
    public void delete(Long id) {
        CouncilRequest s = load(id);
        agendaItems.deleteByRequestId(id);
        s.getAttachments().forEach(a -> storage.deleteQuietly(a.getPath()));
        history.deleteByRequestId(id);
        requests.delete(s);
        audit.record("EVENT_REQUEST_DELETED", "REQUEST", id, s.getCode());
    }

    private CouncilRequestDto correctAsAdmin(Long id, CouncilRequestForm req, List<MultipartFile> newAttachments,
                                             List<String> documentCodes) {
        CouncilRequest s = load(id);
        List<MultipartFile> documents = newAttachments == null ? List.of() : newAttachments;
        List<String> codes = documentCodes == null ? List.of() : documentCodes;
        validatePdfs(documents);
        checkDocumentCodes(req.requestType(), codes);
        applyForm(s, req, null);
        s.setProgram(resolveProgramForAdmin(req.programId(), s));
        replaceCourses(s, req);
        saveAttachments(s, documents, codes);
        requireDocuments(s);
        audit.record("EVENT_REQUEST_UPDATED", "REQUEST", s.getId(), s.getCode());
        return toDto(s);
    }

    private Program resolveProgramForAdmin(Long programId, CouncilRequest s) {
        if (programId == null) {
            return null;
        }
        Program p = hierarchy.findProgram(programId);
        if (!p.getFaculty().getId().equals(s.getFaculty().getId())) {
            throw ApiException.badRequest("El programa no pertenece a la facultad de la radicación");
        }
        return p;
    }

    // ================================================================ Queries

    @Transactional(readOnly = true)
    public Page<CouncilRequestDto> search(RequestStatus status, boolean pending, String code, LocalDate from, LocalDate to, Pageable pageable) {
        AuthenticatedUser u = SecurityUtils.current();
        ZoneId zone = ZoneId.of(props.getZone());
        Specification<CouncilRequest> filter = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (u.hasRole("ROLE_FACULTY_SECRETARY")) {
                ps.add(cb.equal(root.get("filedBy").get("id"), u.id()));
                if (u.programId() != null) {
                    ps.add(cb.equal(root.get("program").get("id"), u.programId()));
                }
            } else if (!u.isGlobal()) {
                ps.add(cb.equal(root.get("faculty").get("id"), u.facultyId()));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            } else if (pending) {
                ps.add(root.get("status").in(RequestStatus.FILED, RequestStatus.APPROVED));
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

    // ================================================================ Council Secretary: review

    @Transactional
    public CouncilRequestDto approve(Long id) {
        AuthenticatedUser u = SecurityUtils.current();
        CouncilRequest s = loadForReview(id, u);
        changeStatus(s, RequestStatus.APPROVED, "Approved by Council Secretary", u.name());
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

    private void applyForm(CouncilRequest s, CouncilRequestForm req, AuthenticatedUser u) {
        RequestType topic = req.requestType();
        String requester = req.requester().trim();
        if (!req.academicPeriod().equals("A") && !req.academicPeriod().equals("B")) {
            throw ApiException.badRequest("The academic period must be 1(A) or 2(B)");
        }
        if (topic.branch() == RequestBranch.STUDENT) {
            if (req.studentCode() == null || req.studentCode().isBlank()
                    || req.identityDocument() == null || req.identityDocument().isBlank()) {
                throw ApiException.badRequest("Complete the student identity data");
            }
        }
        if (topic.otherDetail() && (req.otherDetail() == null || req.otherDetail().isBlank())) {
            throw ApiException.badRequest("Specify what the request is about");
        }
        String subject = subjectOf(topic, topic.otherDetail() ? req.otherDetail() : null);
        String title = requester + " — " + subject;
        s.setTitle(title.length() > 200 ? title.substring(0, 200) : title);
        s.setDescription(req.justification().trim());
        s.setRequester(requester);
        s.setRequestType(topic);
        s.setFiledOn(req.filedOn());
        s.setAcademicPeriod(req.academicPeriod());
        s.setStudentCode(blankToNull(req.studentCode()));
        s.setIdentityDocument(blankToNull(req.identityDocument()));
        s.setIdentityIssuedIn(blankToNull(req.identityIssuedIn()));
        s.setEmail(req.email().trim());
        s.setPhone(req.phone().trim());
        s.setElectronicNotice(req.electronicNotice());
        s.setCounselorName(blankToNull(req.counselorName()));
        s.setOtherDetail(topic.otherDetail() ? req.otherDetail().trim() : null);
        s.setCancellationKind(topic.cancellationKind());
        if (u != null) {
            s.setProgram(resolveProgram(req.programId(), u));
        }
    }

    private void replaceCourses(CouncilRequest s, CouncilRequestForm req) {
        List<CourseLine> lines = readCourses(req.coursesJson());
        boolean needed = s.getRequestType().requiresCourses(s.getCancellationKind());
        List<CourseLine> filled = lines.stream().filter(this::courseFilled).toList();
        if (needed && filled.isEmpty()) {
            throw ApiException.badRequest("Add at least one course");
        }
        if (filled.size() > 6) {
            throw ApiException.badRequest("The form allows at most six courses");
        }
        s.getCourses().clear();
        if (!needed) {
            return;
        }
        int line = 1;
        for (CourseLine row : filled) {
            if (row.courseName() == null || row.courseName().isBlank()) {
                throw ApiException.badRequest("A course name is required");
            }
            RequestCourse course = new RequestCourse();
            course.setRequest(s);
            course.setLineNumber(line++);
            course.setCourseName(row.courseName().trim());
            course.setGroupCode(blankToNull(row.groupCode()));
            course.setSubgroup(blankToNull(row.subgroup()));
            course.setCourseNumber(blankToNull(row.courseNumber()));
            if (s.getRequestType().grades()) {
                course.setGradeNumber(blankToNull(row.gradeNumber()));
                course.setGradeWords(blankToNull(row.gradeWords()));
                course.setAttendedRegularly(row.attendedRegularly());
                course.setFailingCourse(row.failingCourse());
            }
            s.getCourses().add(course);
        }
    }

    private List<CourseLine> readCourses(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        try {
            return json.readValue(raw, new TypeReference<List<CourseLine>>() {
            });
        } catch (IOException e) {
            throw ApiException.badRequest("The course list is not valid");
        }
    }

    private boolean courseFilled(CourseLine row) {
        return row != null && ((row.courseName() != null && !row.courseName().isBlank())
                || (row.groupCode() != null && !row.groupCode().isBlank())
                || (row.courseNumber() != null && !row.courseNumber().isBlank()));
    }

    private void checkDocumentCodes(RequestType topic, List<String> codes) {
        for (String code : codes) {
            if (!topic.documents().contains(code)) {
                throw ApiException.badRequest("The document is not required for this request");
            }
        }
    }

    private void requireDocuments(CouncilRequest s) {
        Set<String> present = new HashSet<>();
        s.getAttachments().forEach(a -> {
            if (a.getDocumentCode() != null) {
                present.add(a.getDocumentCode());
            }
        });
        for (String required : s.getRequestType().documents()) {
            if (!present.contains(required)) {
                throw ApiException.badRequest("Missing required document: " + required);
            }
        }
    }

    private static String subjectOf(RequestType topic, String otherDetail) {
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
        if (otherDetail == null || otherDetail.isBlank()) {
            return base;
        }
        String detail = otherDetail.trim();
        return detail.length() > 120 ? base + ": " + detail.substring(0, 120) : base + ": " + detail;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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
    private void saveAttachments(CouncilRequest s, List<MultipartFile> files, List<String> codes) {
        if (files.size() != codes.size()) {
            throw ApiException.badRequest("Each attachment must identify the document it supports");
        }
        List<String> written = new ArrayList<>();
        try {
            int year = Year.now(ZoneId.of(props.getZone())).getValue();
            for (int i = 0; i < files.size(); i++) {
                MultipartFile f = files.get(i);
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
                a.setDocumentCode(codes.get(i));
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
        Long id = u.programId() != null ? u.programId() : programId;
        if (id == null) {
            return null;
        }
        if (u.programId() != null && programId != null && !u.programId().equals(programId)) {
            throw ApiException.badRequest("You can only file requests for your program");
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
        if (u.hasRole("ROLE_FACULTY_SECRETARY")) {
            if (!s.getFiledBy().getId().equals(u.id())) {
                throw ApiException.forbidden("You can only view the requests you filed");
            }
            if (u.programId() != null && (s.getProgram() == null || !u.programId().equals(s.getProgram().getId()))) {
                throw ApiException.forbidden("You can only view the requests of your program");
            }
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
        String campus = s.getFaculty().getCampus().getName();
        return new CouncilRequestDto(s.getId(), s.getCode(), s.getTitle(), s.getDescription(), s.getRequester(),
                s.getRequestType().branch(), s.getRequestType(), s.getCancellationKind(), s.getStatus(),
                s.getFaculty().getId(), s.getFaculty().getName(), campus,
                s.getProgram() == null ? null : s.getProgram().getId(),
                s.getProgram() == null ? null : s.getProgram().getName(),
                s.getFiledBy().getName(), s.getFiledAt(), s.getFiledOn(), s.getAcademicPeriod(),
                s.getStudentCode(), s.getIdentityDocument(), s.getIdentityIssuedIn(), s.getEmail(), s.getPhone(),
                s.isElectronicNotice(), s.getCounselorName(), s.getOtherDetail(),
                s.getCourses().stream().map(c -> new CourseLine(c.getLineNumber(), c.getCourseName(), c.getGroupCode(),
                        c.getSubgroup(), c.getCourseNumber(), c.getGradeNumber(), c.getGradeWords(),
                        c.getAttendedRegularly(), c.getFailingCourse())).toList(),
                s.getAttachments().stream().map(a -> new AttachmentDto(a.getId(), a.getOriginalName(), a.getSizeBytes(),
                        a.getSha256Hash(), a.getDocumentCode())).toList());
    }
}

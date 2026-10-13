package com.university.sigac.modules.minutes.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.ClientIpResolver;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.core.sequence.SequenceService;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.core.util.DateUtil;
import com.university.sigac.core.util.HashUtil;
import com.university.sigac.core.util.HtmlSanitizer;
import com.university.sigac.core.util.XhtmlValidator;
import com.university.sigac.modules.minutes.dto.MinutesDtos.MinutesDto;
import com.university.sigac.modules.minutes.entity.Minutes;
import com.university.sigac.modules.minutes.enums.MinutesStatus;
import com.university.sigac.modules.minutes.repository.MinutesRepository;
import com.university.sigac.modules.notifications.service.EmailService;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.requests.service.AgendaService;
import com.university.sigac.modules.templates.entity.MinutesTemplate;
import com.university.sigac.modules.templates.service.TemplateService;
import com.university.sigac.modules.users.enums.RoleName;
import com.university.sigac.modules.users.service.UserLookupService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modules 5 and 6: minutes generation, draft editing, approval workflow and electronic signature.
 * Flow: DRAFT → EDITING ⇄ PENDING_SIGNATURE → PUBLISHED (frozen, RN-17).
 */
@Service
@RequiredArgsConstructor
public class MinutesService {

    private static final Set<MinutesStatus> EDITABLE = Set.of(MinutesStatus.DRAFT, MinutesStatus.EDITING);

    private final MinutesRepository minutes;
    private final UserLookupService users;
    private final AgendaService agenda;
    private final TemplateService templates;
    private final MinutesModelBuilder modelBuilder;
    private final PdfService pdf;
    private final SequenceService sequences;
    private final StorageService storage;
    private final ClientIpResolver ipResolver;
    private final AuditService audit;
    private final EmailService email;
    private final AppProperties props;

    // ---------------------------------------------------------------- Generate and edit (Secretary 1)

    /** UC-05: compiles the minutes of a FINISHED session with the active template and assigns the serial number (RN-13). */
    @Transactional
    public MinutesDto compile(Long sessionId) {
        Session s = agenda.loadWithAccess(sessionId);
        if (!s.hasStatus(SessionStatus.FINISHED)) {
            throw ApiException.conflict("Minutes can only be generated when the session is FINISHED (current status: " + s.getStatus() + ")");
        }
        if (minutes.existsBySessionId(sessionId)) {
            throw ApiException.conflict("Minutes already exist for this session");
        }
        List<String> pending = modelBuilder.pendingItems(s);
        if (!pending.isEmpty()) {
            throw ApiException.unprocessable("Incomplete information: there are items not yet closed", pending);
        }

        MinutesTemplate template = templates.getActive(s.getCouncilType().getId(), s.getFaculty().getId());
        LocalDateTime date = s.getActualStart() != null ? s.getActualStart() : s.getScheduledAt();
        String code = s.getCouncilType().getCode();
        long number = sequences.next("MIN-" + code + "-" + date.getYear());
        String serialNumber = String.format("MIN-%s-%d-%03d", code, date.getYear(), number);

        String html = pdf.render(template.getContent(), modelBuilder.build(s, serialNumber));
        html = HtmlSanitizer.sanitizeDocument(html);
        pdf.generatePdfA(html); // checks right away that the document can be compiled to PDF/A

        Minutes a = new Minutes();
        a.setSession(s);
        a.setSerialNumber(serialNumber);
        a.setCouncilType(s.getCouncilType());
        a.setFaculty(s.getFaculty());
        a.setSessionDate(date.toLocalDate());
        a.setTemplateVersion(template.getVersion());
        a.setHtmlContent(html);
        minutes.save(a);
        audit.record("EVENT_MINUTES_COMPILED", "MINUTES", a.getId(), serialNumber);
        return toDto(a, true);
    }

    /** RF-28: Secretary 1 polishes the wording before sending it for signature. */
    @Transactional
    public MinutesDto edit(Long id, String htmlContent) {
        Minutes a = load(id);
        requireEditable(a);
        List<String> errors = XhtmlValidator.errors(htmlContent);
        if (!errors.isEmpty()) {
            throw ApiException.unprocessable("The minutes content is not valid XHTML", errors);
        }
        String clean = HtmlSanitizer.sanitizeDocument(htmlContent);
        pdf.generatePdfA(clean);
        a.setHtmlContent(clean);
        a.setStatus(MinutesStatus.EDITING);
        audit.record("EVENT_MINUTES_EDITED", "MINUTES", a.getId(), a.getSerialNumber());
        return toDto(a, true);
    }

    @Transactional(readOnly = true)
    public byte[] preview(Long id) {
        Minutes a = load(id);
        if (a.getStatus() == MinutesStatus.PUBLISHED) {
            throw ApiException.conflict("The minutes are already published: view them in the Digital Repository");
        }
        return pdf.generatePdfA(a.getHtmlContent());
    }

    @Transactional
    public MinutesDto sendToSignature(Long id) {
        Minutes a = load(id);
        requireEditable(a);
        pdf.generatePdfA(a.getHtmlContent());
        a.setStatus(MinutesStatus.PENDING_SIGNATURE);
        a.setRejectionNote(null);
        audit.record("EVENT_MINUTES_SENT_TO_SIGN", "MINUTES", a.getId(), a.getSerialNumber());
        return toDto(a, false);
    }

    // ---------------------------------------------------------------- Approval (President / Secretary 1)

    /** RF-35: returns the draft to editing with notes. */
    @Transactional
    public MinutesDto reject(Long id, String note) {
        Minutes a = load(id);
        requireStatus(a, MinutesStatus.PENDING_SIGNATURE);
        a.setStatus(MinutesStatus.EDITING);
        a.setRejectionNote(note.trim());
        audit.record("EVENT_MINUTES_REJECTED", "MINUTES", a.getId(), note.trim());
        return toDto(a, false);
    }

    /**
     * UC-06: stamps the audit fingerprint (signer, server time, IP, SHA-256 hash), generates the final PDF/A,
     * stores it in the repository with its integrity hash, freezes the minutes (RN-17) and notifies by email (RN-15).
     */
    @Transactional
    public MinutesDto approveAndSign(Long id) {
        AuthenticatedUser u = SecurityUtils.current();
        Minutes a = load(id);
        requireStatus(a, MinutesStatus.PENDING_SIGNATURE);

        Instant now = Instant.now();
        ZoneId zone = ZoneId.of(props.getZone());
        String ip = ipResolver.current();
        String role = u.hasRole("ROLE_PRESIDENT") ? "Council President" : "Council Secretary";
        String contentHash = HashUtil.sha256Hex(a.getHtmlContent());

        String finalHtml = HtmlSanitizer.appendSignatureBlock(a.getHtmlContent(), List.of(
                "Signed by: " + u.name() + " (" + role + ")",
                "Official server date and time: " + DateUtil.longDateTime(now, zone) + " (" + zone + ")",
                "Source IP address: " + ip,
                "SHA-256 hash of the approved content: " + contentHash));
        byte[] file = pdf.generatePdfA(finalHtml);

        String path = repositoryPath(a);
        storage.save(path, file);
        try {
            a.setSignedBy(u.name());
            a.setSignerRole(role);
            a.setSignedAt(now);
            a.setSignatureIp(ip);
            a.setContentHash(contentHash);
            a.setHtmlContent(finalHtml);
            a.setFilePath(path);
            a.setFileHash(HashUtil.sha256Hex(file));
            a.setStatus(MinutesStatus.PUBLISHED);
            minutes.saveAndFlush(a);
        } catch (RuntimeException e) {
            storage.deleteQuietly(path); // do not leave orphan files if saving fails
            throw e;
        }
        audit.record("EVENT_MINUTES_SIGNED", "MINUTES", a.getId(), a.getSerialNumber() + " sha256=" + a.getFileHash());

        List<String> recipients = users.activeEmailsByFacultyAndRoles(a.getFaculty().getId(),
                List.of(RoleName.ROLE_PRESIDENT, RoleName.ROLE_COUNCIL_MEMBER, RoleName.ROLE_SECRETARY_1));
        email.sendMinutesPublished(recipients, a.getSerialNumber(), a.getFaculty().getName(), path);
        return toDto(a, false);
    }

    // ---------------------------------------------------------------- Queries

    @Transactional(readOnly = true)
    public MinutesDto get(Long id) {
        return toDto(load(id), true);
    }

    @Transactional(readOnly = true)
    public Page<MinutesDto> list(MinutesStatus status, Pageable pageable) {
        AuthenticatedUser u = SecurityUtils.current();
        Page<Minutes> page;
        if (u.isGlobal()) {
            page = status == null ? minutes.findAll(pageable) : minutes.findByStatus(status, pageable);
        } else {
            page = status == null ? minutes.findByFacultyId(u.facultyId(), pageable)
                    : minutes.findByFacultyIdAndStatus(u.facultyId(), status, pageable);
        }
        return page.map(a -> toDto(a, false));
    }

    // ---------------------------------------------------------------- private

    /** minutes/University/Campus/Faculty/Year/SERIAL_NUMBER.pdf (institutional hierarchy). */
    private String repositoryPath(Minutes a) {
        var faculty = a.getFaculty();
        return "minutes/" + StorageService.slug(faculty.getCampus().getUniversity().getName())
                + "/" + StorageService.slug(faculty.getCampus().getName())
                + "/" + StorageService.slug(faculty.getName())
                + "/" + a.getSessionDate().getYear()
                + "/" + StorageService.slug(a.getSerialNumber()) + ".pdf";
    }

    private Minutes load(Long id) {
        Minutes a = minutes.findById(id).orElseThrow(() -> ApiException.notFound("Minutes not found"));
        SecurityUtils.checkFaculty(SecurityUtils.current(), a.getFaculty().getId());
        return a;
    }

    private void requireEditable(Minutes a) {
        if (!EDITABLE.contains(a.getStatus())) {
            throw ApiException.conflict("The minutes cannot be modified in status " + a.getStatus()
                    + (a.getStatus() == MinutesStatus.PUBLISHED ? " (RN-17: frozen minutes)" : ""));
        }
    }

    private void requireStatus(Minutes a, MinutesStatus expected) {
        if (a.getStatus() != expected) {
            throw ApiException.conflict("Operation only valid for minutes in status " + expected + " (current: " + a.getStatus() + ")");
        }
    }

    private MinutesDto toDto(Minutes a, boolean withContent) {
        return new MinutesDto(a.getId(), a.getSerialNumber(), a.getStatus(), a.getSession().getId(), a.getCouncilType().getName(),
                a.getFaculty().getName(), a.getSessionDate(), a.getRejectionNote(), a.getSignedBy(), a.getSignedAt(),
                withContent ? a.getHtmlContent() : null);
    }
}

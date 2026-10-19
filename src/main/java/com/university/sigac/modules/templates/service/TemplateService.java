package com.university.sigac.modules.templates.service;

import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.pdf.Letterhead;
import com.university.sigac.core.pdf.PdfService;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.core.util.HtmlSanitizer;
import com.university.sigac.modules.templates.dto.TemplateDtos.SaveTemplateRequest;
import com.university.sigac.modules.templates.dto.TemplateDtos.TemplateDto;
import com.university.sigac.modules.templates.entity.MinutesTemplate;
import com.university.sigac.modules.templates.repository.MinutesTemplateRepository;
import com.university.sigac.modules.templates.util.MockDataFactory;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.service.CouncilTypeService;
import com.university.sigac.modules.users.service.HierarchyService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 2 (RF-10 to RF-14): save, version, activate and preview minutes templates. */
@Service
@RequiredArgsConstructor
public class TemplateService {

    private static final String DEFAULT_TEMPLATE = "/sigac/templates/minutes-default.html";
    private static final String AGREEMENT_DRAFT = "/sigac/templates/agreement-draft.html";

    private final MinutesTemplateRepository templates;
    private final CouncilTypeService councilTypes;
    private final HierarchyService hierarchy;
    private final TemplateValidator validator;
    private final PdfService pdf;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<TemplateDto> list(Long councilTypeId) {
        List<MinutesTemplate> list = councilTypeId == null ? templates.findAllByOrderByIdDesc()
                : templates.findByCouncilTypeIdOrderByVersionDesc(councilTypeId);
        return list.stream().map(p -> toDto(p, false)).toList();
    }

    @Transactional(readOnly = true)
    public TemplateDto get(Long id) {
        return toDto(find(id), true);
    }

    public List<String> validate(String content) {
        return validator.validate(content);
    }

    /** Saves a NEW version and makes it active for its scope (without editing the database by hand, RN-07). */
    @Transactional
    public TemplateDto save(SaveTemplateRequest req) {
        CouncilType type = councilTypes.find(req.councilTypeId());
        Faculty faculty = req.facultyId() == null ? null : hierarchy.findFaculty(req.facultyId());
        requireValid(req.content());

        Optional<MinutesTemplate> latest = faculty == null
                ? templates.findTopByCouncilTypeIdAndFacultyIsNullOrderByVersionDesc(type.getId())
                : templates.findTopByCouncilTypeIdAndFacultyIdOrderByVersionDesc(type.getId(), faculty.getId());
        deactivateCurrent(type.getId(), req.facultyId());

        MinutesTemplate p = new MinutesTemplate();
        p.setCouncilType(type);
        p.setFaculty(faculty);
        p.setVersion(latest.map(x -> x.getVersion() + 1).orElse(1));
        p.setName(req.name() == null || req.name().isBlank() ? "Official template - " + type.getName() : req.name().trim());
        p.setContent(req.content());
        p.setActive(true);
        p.setComment(req.comment());
        p.setCreatedBy(SecurityUtils.current().email());
        templates.save(p);
        audit.record("EVENT_TEMPLATE_SAVED", "TEMPLATE", p.getId(), "Version " + p.getVersion());
        return toDto(p, true);
    }

    /** Reactivates a previous version. */
    @Transactional
    public TemplateDto activate(Long id) {
        MinutesTemplate p = find(id);
        deactivateCurrent(p.getCouncilType().getId(), p.getFaculty() == null ? null : p.getFaculty().getId());
        p.setActive(true);
        audit.record("EVENT_TEMPLATE_ACTIVATED", "TEMPLATE", p.getId(), "Version " + p.getVersion());
        return toDto(p, true);
    }

    /** Faculty Council agreement draft, filled on preview with Acuerdo 028 de 2026. */
    public String agreementDraft() {
        return readResource(AGREEMENT_DRAFT);
    }

    /** RF-12: renders with sample data and returns the PDF/A, without saving anything. */
    public byte[] preview(String content) {
        requireValid(content);
        String html = pdf.render(Letterhead.embed(content), MockDataFactory.create());
        return pdf.generatePdfA(HtmlSanitizer.sanitizeDocument(html));
    }

    /** Active template of the Faculty or, if it has none of its own, the general one of the council type. */
    @Transactional(readOnly = true)
    public MinutesTemplate getActive(Long councilTypeId, Long facultyId) {
        Optional<MinutesTemplate> own = templates.findByCouncilTypeIdAndFacultyIdAndActiveTrue(councilTypeId, facultyId)
                .stream().findFirst();
        return own.or(() -> templates.findByCouncilTypeIdAndFacultyIsNullAndActiveTrue(councilTypeId).stream().findFirst())
                .orElseThrow(() -> ApiException.conflict(
                        "There is no active template for this council type. Ask an administrator to publish one"));
    }

    /** Initial load: creates the default template if the council type does not have any yet. */
    @Transactional
    public void createDefaultIfMissing(CouncilType type) {
        if (templates.findTopByCouncilTypeIdAndFacultyIsNullOrderByVersionDesc(type.getId()).isPresent()) {
            return;
        }
        MinutesTemplate p = new MinutesTemplate();
        p.setCouncilType(type);
        p.setName("Official template - " + type.getName());
        p.setVersion(1);
        p.setContent(readResource(DEFAULT_TEMPLATE));
        p.setActive(true);
        p.setComment("Initial system template");
        p.setCreatedBy("system");
        templates.save(p);
    }

    // ---------------------------------------------------------------- private

    private void deactivateCurrent(Long councilTypeId, Long facultyId) {
        List<MinutesTemplate> current = facultyId == null
                ? templates.findByCouncilTypeIdAndFacultyIsNullAndActiveTrue(councilTypeId)
                : templates.findByCouncilTypeIdAndFacultyIdAndActiveTrue(councilTypeId, facultyId);
        current.forEach(v -> v.setActive(false));
        templates.saveAllAndFlush(current);
    }

    private void requireValid(String content) {
        List<String> errors = validator.validate(content);
        if (!errors.isEmpty()) {
            throw ApiException.unprocessable("The template contains errors and cannot be used", errors);
        }
    }

    private MinutesTemplate find(Long id) {
        return templates.findById(id).orElseThrow(() -> ApiException.notFound("Template not found"));
    }

    private String readResource(String path) {
        try (InputStream in = TemplateService.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }

    private TemplateDto toDto(MinutesTemplate p, boolean withContent) {
        Faculty f = p.getFaculty();
        return new TemplateDto(p.getId(), p.getName(), p.getCouncilType().getId(), p.getCouncilType().getName(),
                f == null ? null : f.getId(), f == null ? null : f.getName(), p.getVersion(), p.isActive(),
                p.getComment(), p.getCreatedBy(), p.getCreatedAt(), withContent ? p.getContent() : null);
    }
}

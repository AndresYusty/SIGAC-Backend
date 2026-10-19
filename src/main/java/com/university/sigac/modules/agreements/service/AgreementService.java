package com.university.sigac.modules.agreements.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.university.sigac.core.audit.AuditService;
import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.SecurityUtils;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementContent;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementDefaultsDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementRequest;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementSummaryDto;
import com.university.sigac.modules.agreements.dto.AgreementDtos.AgreementTypeDto;
import com.university.sigac.modules.agreements.entity.Agreement;
import com.university.sigac.modules.agreements.repository.AgreementRepository;
import com.university.sigac.modules.agreements.util.AgreementTypeCatalog;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.service.HierarchyService;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Faculty Council agreements: drafting from a type, numbering per Faculty and year, and PDF generation. */
@Service
@RequiredArgsConstructor
public class AgreementService {

    private static final Pattern FACULTY_PREFIX = Pattern.compile("(?i)^facultad de\\s+");

    private final AgreementRepository agreements;
    private final AgreementTypeCatalog types;
    private final AgreementRenderer renderer;
    private final HierarchyService hierarchy;
    private final AuditService audit;
    private final ObjectMapper mapper;

    public List<AgreementTypeDto> types() {
        return types.all();
    }

    @Transactional(readOnly = true)
    public AgreementDefaultsDto defaults(Long facultyId) {
        Faculty faculty = faculty(facultyId);
        int year = LocalDate.now().getYear();
        var last = agreements.findTopByFacultyIdOrderByIdDesc(faculty.getId());
        String name = FACULTY_PREFIX.matcher(faculty.getName().trim()).replaceFirst("");
        return new AgreementDefaultsDto(faculty.getId(), faculty.getName(), year,
                agreements.lastNumber(faculty.getId(), year) + 1,
                "EL CONSEJO DE FACULTAD DE " + name.toUpperCase(), city(faculty),
                last.map(Agreement::getPresidentName).orElse(""), last.map(Agreement::getSecretaryName).orElse(""));
    }

    @Transactional(readOnly = true)
    public Page<AgreementSummaryDto> list(Integer year, String typeCode, String text, Pageable pageable) {
        AuthenticatedUser user = SecurityUtils.current();
        Specification<Agreement> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (!user.isGlobal()) {
                p.add(cb.equal(root.get("faculty").get("id"), user.facultyId()));
            }
            if (year != null) {
                p.add(cb.equal(root.get("year"), year));
            }
            if (typeCode != null && !typeCode.isBlank()) {
                p.add(cb.equal(root.get("typeCode"), typeCode));
            }
            if (text != null && !text.isBlank()) {
                String like = "%" + text.trim().toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("subject")), like), cb.like(cb.lower(root.get("content")), like)));
            }
            return cb.and(p.toArray(Predicate[]::new));
        };
        return agreements.findAll(spec, pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AgreementDto get(Long id) {
        return toDto(find(id));
    }

    @Transactional
    public AgreementDto create(AgreementRequest req) {
        types.get(req.typeCode());
        Agreement a = new Agreement();
        a.setFaculty(faculty(req.facultyId()));
        a.setCreatedBy(SecurityUtils.current().email());
        apply(a, req);
        agreements.save(a);
        audit.record("EVENT_AGREEMENT_CREATED", "AGREEMENT", a.getId(), label(a));
        return toDto(a);
    }

    @Transactional
    public AgreementDto update(Long id, AgreementRequest req) {
        types.get(req.typeCode());
        Agreement a = find(id);
        apply(a, req);
        a.setUpdatedAt(Instant.now());
        audit.record("EVENT_AGREEMENT_EDITED", "AGREEMENT", a.getId(), label(a));
        return toDto(a);
    }

    /** PDF of a draft that has not been saved yet (the number is the one it would get). */
    @Transactional(readOnly = true)
    public byte[] preview(AgreementRequest req) {
        Faculty faculty = faculty(req.facultyId());
        int number = req.number() != null ? req.number()
                : agreements.lastNumber(faculty.getId(), req.issueDate().getYear()) + 1;
        return renderer.render(req, number, city(faculty));
    }

    @Transactional(readOnly = true)
    public AgreementFile pdf(Long id) {
        Agreement a = find(id);
        byte[] data = renderer.render(toRequest(a), a.getNumber(), city(a.getFaculty()));
        String type = types.find(a.getTypeCode()).map(AgreementTypeDto::name).orElse(a.getTypeCode());
        return new AgreementFile(label(a) + " - " + type + ".pdf", data);
    }

    public record AgreementFile(String name, byte[] data) {
    }

    // ---------------------------------------------------------------- private

    private void apply(Agreement a, AgreementRequest req) {
        int year = req.issueDate().getYear();
        int number = req.number() != null ? req.number()
                : (a.getId() != null && a.getYear() == year ? a.getNumber()
                : agreements.lastNumber(a.getFaculty().getId(), year) + 1);
        if (agreements.existsByFacultyIdAndYearAndNumberAndIdNot(a.getFaculty().getId(), year, number,
                a.getId() == null ? 0L : a.getId())) {
            throw ApiException.conflict(String.format("Agreement No. %03d of %d already exists in this Faculty", number, year));
        }
        a.setTypeCode(req.typeCode());
        a.setNumber(number);
        a.setYear(year);
        a.setIssueDate(req.issueDate());
        a.setSubject(req.subject().trim());
        a.setCouncilTitle(req.councilTitle().trim());
        a.setLegalBasis(req.legalBasis());
        a.setClosing(req.closing());
        a.setPresidentName(req.presidentName());
        a.setSecretaryName(req.secretaryName());
        a.setContent(write(new AgreementContent(req.values() == null ? Map.of() : req.values(), req.recitals(),
                req.articles())));
    }

    private Faculty faculty(Long requested) {
        AuthenticatedUser user = SecurityUtils.current();
        Long id = requested != null ? requested : user.facultyId();
        if (id == null) {
            throw ApiException.badRequest("Select the Faculty of the agreement");
        }
        SecurityUtils.checkFaculty(user, id);
        return hierarchy.findFaculty(id);
    }

    private Agreement find(Long id) {
        Agreement a = agreements.findById(id).orElseThrow(() -> ApiException.notFound("Agreement not found"));
        SecurityUtils.checkFaculty(SecurityUtils.current(), a.getFaculty().getId());
        return a;
    }

    private static String city(Faculty f) {
        String city = f.getCampus().getCity();
        return city == null || city.isBlank() ? "Neiva" : city.trim();
    }

    private static String label(Agreement a) {
        return String.format("ACUERDO No. %03d DE %d", a.getNumber(), a.getYear());
    }

    private AgreementContent read(Agreement a) {
        try {
            return mapper.readValue(a.getContent(), AgreementContent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted content in agreement " + a.getId(), e);
        }
    }

    private String write(AgreementContent c) {
        try {
            return mapper.writeValueAsString(c);
        } catch (JsonProcessingException e) {
            throw ApiException.badRequest("Invalid agreement content");
        }
    }

    private AgreementRequest toRequest(Agreement a) {
        AgreementContent c = read(a);
        return new AgreementRequest(a.getFaculty().getId(), a.getTypeCode(), a.getNumber(), a.getIssueDate(),
                a.getSubject(), a.getCouncilTitle(), a.getLegalBasis(), a.getClosing(), a.getPresidentName(),
                a.getSecretaryName(), c.values(), c.recitals(), c.articles());
    }

    private String typeName(String code) {
        return types.find(code).map(AgreementTypeDto::name).orElse(code);
    }

    private AgreementSummaryDto toSummary(Agreement a) {
        return new AgreementSummaryDto(a.getId(), label(a), a.getNumber(), a.getYear(), a.getTypeCode(),
                typeName(a.getTypeCode()), a.getFaculty().getName(), a.getIssueDate(), a.getSubject(), a.getCreatedBy());
    }

    private AgreementDto toDto(Agreement a) {
        AgreementContent c = read(a);
        return new AgreementDto(a.getId(), label(a), a.getNumber(), a.getYear(), a.getTypeCode(),
                typeName(a.getTypeCode()), a.getFaculty().getId(), a.getFaculty().getName(), a.getIssueDate(),
                a.getSubject(), a.getCouncilTitle(), a.getLegalBasis(), a.getClosing(), a.getPresidentName(),
                a.getSecretaryName(), c.values(), c.recitals(), c.articles(), a.getCreatedBy(), a.getCreatedAt(),
                a.getUpdatedAt());
    }
}

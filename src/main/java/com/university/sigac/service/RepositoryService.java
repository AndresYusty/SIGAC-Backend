package com.university.sigac.service;

import com.university.sigac.dto.MinutesDtos.MinutesFile;
import com.university.sigac.dto.MinutesDtos.RepositoryMinutesDto;
import com.university.sigac.dto.MinutesDtos.VerificationDto;
import com.university.sigac.entity.Minutes;
import com.university.sigac.entity.User;
import com.university.sigac.enums.MinutesStatus;
import com.university.sigac.enums.RoleName;
import com.university.sigac.exception.ApiException;
import com.university.sigac.repository.MinutesRepository;
import com.university.sigac.repository.UserRepository;
import com.university.sigac.security.AuthenticatedUser;
import com.university.sigac.security.SecurityUtils;
import com.university.sigac.util.HashUtil;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 7: search of published minutes and file delivery with SHA-256 verification (RN-18). */
@Service
@RequiredArgsConstructor
public class RepositoryService {

    private final MinutesRepository minutes;
    private final UserRepository users;
    private final StorageService storage;
    private final AuditService audit;
    private final EmailService email;

    /** RF-37: only PUBLISHED minutes, from the user's Faculty unless the profile is global (RN-02). */
    @Transactional(readOnly = true)
    public Page<RepositoryMinutesDto> search(String serialNumber, Long councilTypeId, LocalDate from, LocalDate to,
                                             Long campusId, Long facultyId, String text, Pageable pageable) {
        AuthenticatedUser u = SecurityUtils.current();
        Specification<Minutes> filter = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("status"), MinutesStatus.PUBLISHED));
            if (!u.isGlobal()) {
                ps.add(cb.equal(root.get("faculty").get("id"), u.facultyId()));
            } else if (facultyId != null) {
                ps.add(cb.equal(root.get("faculty").get("id"), facultyId));
            }
            if (serialNumber != null && !serialNumber.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("serialNumber")), "%" + serialNumber.trim().toLowerCase() + "%"));
            }
            if (councilTypeId != null) {
                ps.add(cb.equal(root.get("councilType").get("id"), councilTypeId));
            }
            if (campusId != null) {
                ps.add(cb.equal(root.get("faculty").get("campus").get("id"), campusId));
            }
            if (from != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("sessionDate"), from));
            }
            if (to != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("sessionDate"), to));
            }
            if (text != null && !text.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("htmlContent")), "%" + text.trim().toLowerCase() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return minutes.findAll(filter, pageable).map(this::toDto);
    }

    /**
     * RN-18 / UC-07: recomputes the SHA-256 of the file on disk and compares it with the one recorded at signing.
     * If they differ, delivery is blocked, the event is logged and the technical profile is alerted.
     */
    @Transactional(readOnly = true)
    public MinutesFile readVerified(Long minutesId, String event) {
        Minutes a = loadPublished(minutesId);
        if (!storage.exists(a.getFilePath())) {
            reportCompromise(a, "File not found in storage");
            throw ApiException.conflict("Compromised document: the file could not be found in storage");
        }
        byte[] data = storage.read(a.getFilePath());
        String computed = HashUtil.sha256Hex(data);
        if (!HashUtil.matches(computed, a.getFileHash())) {
            reportCompromise(a, "Recomputed hash " + computed + " differs from stored " + a.getFileHash());
            throw ApiException.conflict("Compromised document: the SHA-256 fingerprint does not match the one recorded at signing. "
                    + "The download was blocked and the technical profile was notified");
        }
        audit.record(event, "MINUTES", a.getId(), a.getSerialNumber() + " sha256=" + computed);
        return new MinutesFile(a.getSerialNumber() + ".pdf", data, computed);
    }

    @Transactional(readOnly = true)
    public VerificationDto verify(Long minutesId) {
        Minutes a = loadPublished(minutesId);
        String computed = storage.exists(a.getFilePath()) ? HashUtil.sha256Hex(storage.read(a.getFilePath())) : null;
        boolean intact = HashUtil.matches(computed, a.getFileHash());
        if (!intact) {
            reportCompromise(a, "Manual verification: computed hash " + computed);
        }
        return new VerificationDto(a.getSerialNumber(), a.getFileHash(), computed, intact);
    }

    private void reportCompromise(Minutes a, String detail) {
        audit.record("EVENT_INTEGRITY_VIOLATION", "MINUTES", a.getId(), a.getSerialNumber() + ": " + detail);
        List<String> technicians = users.findActiveByRoles(List.of(RoleName.ROLE_DEV)).stream().map(User::getEmail).toList();
        email.sendIntegrityAlert(technicians, a.getSerialNumber());
    }

    private Minutes loadPublished(Long id) {
        Minutes a = minutes.findById(id).orElseThrow(() -> ApiException.notFound("Minutes not found"));
        SecurityUtils.checkFaculty(SecurityUtils.current(), a.getFaculty().getId());
        if (a.getStatus() != MinutesStatus.PUBLISHED) {
            throw ApiException.conflict("Only PUBLISHED minutes are available in the repository");
        }
        return a;
    }

    private RepositoryMinutesDto toDto(Minutes a) {
        var f = a.getFaculty();
        return new RepositoryMinutesDto(a.getId(), a.getSerialNumber(), a.getCouncilType().getName(),
                f.getCampus().getUniversity().getName(), f.getCampus().getName(), f.getName(), a.getSessionDate(),
                a.getSignedBy(), a.getSignedAt(), a.getFileHash());
    }
}

package com.university.sigac.modules.repository.service;

import com.university.sigac.core.audit.AuditLogRepository;
import com.university.sigac.modules.repository.dto.RepositoryDtos.AuditLogDto;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Audit log query (RF-40, read only). Events are written by the core AuditService; the table is append-only (RN-19). */
@Service
@RequiredArgsConstructor
public class AuditQueryService {

    private final AuditLogRepository repository;

    @Transactional(readOnly = true)
    public Page<AuditLogDto> search(String event, String user, Instant from, Instant to, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200));
        return repository.search(blankToNull(event), blankToNull(user), from, to, pageable)
                .map(a -> new AuditLogDto(a.getId(), a.getEvent(), a.getUserEmail(), a.getIp(), a.getEntity(),
                        a.getEntityId(), a.getDetail(), a.getOccurredAt()));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}

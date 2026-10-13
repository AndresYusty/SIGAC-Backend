package com.university.sigac.service;

import com.university.sigac.dto.MinutesDtos.AuditLogDto;
import com.university.sigac.entity.AuditLog;
import com.university.sigac.repository.AuditLogRepository;
import com.university.sigac.security.AuthenticatedUser;
import com.university.sigac.security.ClientIpResolver;
import com.university.sigac.security.SecurityUtils;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit log: who, when and from which IP (RF-40). Saved in its own transaction so that the record
 * persists even if the main operation fails. The table is append-only (RN-19).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ClientIpResolver ipResolver;

    /** Uses the authenticated user of the current request. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String event, String entity, Object entityId, String detail) {
        AuthenticatedUser u = SecurityUtils.currentOrNull();
        save(event, u == null ? null : u.id(), u == null ? null : u.email(), entity, entityId, detail);
    }

    /** For events without a session (failed login, password recovery). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAs(String event, Long userId, String email, String detail) {
        save(event, userId, email, "USER", userId, detail);
    }

    /** Audit log query (read only). */
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

    private void save(String event, Long userId, String email, String entity, Object entityId, String detail) {
        try {
            AuditLog a = new AuditLog();
            a.setEvent(event);
            a.setUserId(userId);
            a.setUserEmail(email);
            a.setIp(ipResolver.current());
            a.setEntity(entity);
            a.setEntityId(entityId == null ? null : String.valueOf(entityId));
            a.setDetail(detail != null && detail.length() > 2000 ? detail.substring(0, 2000) : detail);
            a.setOccurredAt(Instant.now());
            repository.save(a);
        } catch (RuntimeException e) {
            log.error("Could not record the audit event {}", event, e);
        }
    }
}

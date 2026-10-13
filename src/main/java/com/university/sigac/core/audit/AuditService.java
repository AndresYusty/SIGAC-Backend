package com.university.sigac.core.audit;

import com.university.sigac.core.security.AuthenticatedUser;
import com.university.sigac.core.security.ClientIpResolver;
import com.university.sigac.core.security.SecurityUtils;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Audit log: who, when and from which IP (RF-40). Saved in its own transaction so that the record
 * persists even if the main operation fails. The table is append-only (RN-19); queries live in the repository module.
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

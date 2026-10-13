package com.university.sigac.repository;

import com.university.sigac.entity.AuditLog;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/** Deliberately limited repository: insert and query only (no update or delete). */
public interface AuditLogRepository extends Repository<AuditLog, Long> {

    AuditLog save(AuditLog log);

    @Query("select a from AuditLog a where (:event is null or a.event = :event) "
            + "and (:email is null or lower(a.userEmail) like lower(concat('%', :email, '%'))) "
            + "and (:from is null or a.occurredAt >= :from) and (:to is null or a.occurredAt <= :to) "
            + "order by a.occurredAt desc")
    Page<AuditLog> search(@Param("event") String event, @Param("email") String email,
                          @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}

package com.university.sigac.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

/** Immutable audit record (append-only, RN-19). Also protected by MySQL triggers. */
@Entity
@Immutable
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_event", columnList = "event"),
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_user", columnList = "user_email")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String event;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "user_email", length = 150)
    private String userEmail;

    @Column(length = 64)
    private String ip;

    @Column(length = 60)
    private String entity;

    @Column(name = "entity_id", length = 60)
    private String entityId;

    @Column(length = 2000)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}

package com.universidad.sigac.entity;

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

/** Registro de auditoría inmutable (append-only, RN-19). Protegido además por triggers en MySQL. */
@Entity
@Immutable
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_evento", columnList = "evento"),
        @Index(name = "idx_audit_fecha", columnList = "fecha"),
        @Index(name = "idx_audit_usuario", columnList = "usuario_email")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String evento;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "usuario_email", length = 150)
    private String usuarioEmail;

    @Column(length = 64)
    private String ip;

    @Column(length = 60)
    private String entidad;

    @Column(name = "entidad_id", length = 60)
    private String entidadId;

    @Column(length = 2000)
    private String detalle;

    @Column(nullable = false)
    private Instant fecha;
}

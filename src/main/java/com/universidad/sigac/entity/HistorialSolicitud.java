package com.universidad.sigac.entity;

import com.universidad.sigac.enums.EstadoSolicitud;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Historial de estados. RN-10: toda devolución registra su justificación. */
@Entity
@Table(name = "historial_solicitud")
@Getter @Setter @NoArgsConstructor
public class HistorialSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoSolicitud estadoNuevo;

    @Column(length = 2000)
    private String motivo;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(nullable = false)
    private Instant fecha = Instant.now();
}

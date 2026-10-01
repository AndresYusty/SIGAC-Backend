package com.universidad.sigac.entity;

import com.universidad.sigac.enums.EstadoSolicitud;
import com.universidad.sigac.enums.TipoSolicitud;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes")
@Getter @Setter @NoArgsConstructor
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Código de radicado único, p. ej. SOL-2026-000123. */
    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, length = 4000)
    private String descripcion;

    @Column(nullable = false, length = 150)
    private String solicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solicitud", nullable = false, length = 30)
    private TipoSolicitud tipoSolicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facultad_id", nullable = false)
    private Facultad facultad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id")
    private Programa programa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "radicador_id", nullable = false)
    private Usuario radicador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.RADICADO;

    @Column(name = "fecha_radicacion", nullable = false)
    private Instant fechaRadicacion = Instant.now();

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Anexo> anexos = new ArrayList<>();
}

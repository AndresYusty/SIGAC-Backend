package com.universidad.sigac.entity;

import com.universidad.sigac.enums.EstadoSesion;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sesión de Consejo. Contiene su Orden del Día (lista de puntos) y avanza por {@link EstadoSesion}. */
@Entity
@Table(name = "sesiones")
@Getter @Setter @NoArgsConstructor
public class Sesion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_consejo_id", nullable = false)
    private TipoConsejo tipoConsejo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facultad_id", nullable = false)
    private Facultad facultad;

    @Column(name = "fecha_programada", nullable = false)
    private LocalDateTime fechaProgramada;

    @Column(nullable = false, length = 200)
    private String lugar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSesion estado = EstadoSesion.PROGRAMADA;

    /** true cuando se cerró el Orden del Día y se envió la citación (RN-09: ya no se modifica). */
    @Column(name = "agenda_cerrada", nullable = false)
    private boolean agendaCerrada = false;

    @Column(name = "inicio_real")
    private LocalDateTime inicioReal;

    @Column(name = "fin_real")
    private LocalDateTime finReal;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<PuntoAgenda> puntos = new ArrayList<>();

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();

    public boolean esEstado(EstadoSesion e) {
        return estado == e;
    }
}

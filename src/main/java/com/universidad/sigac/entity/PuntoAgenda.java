package com.universidad.sigac.entity;

import com.universidad.sigac.enums.ResultadoDecision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Punto del Orden del Día. También guarda la decisión tomada y las notas de debate (módulo 4). */
@Entity
@Table(name = "puntos_agenda")
@Getter @Setter @NoArgsConstructor
public class PuntoAgenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false)
    private Sesion sesion;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 4000)
    private String descripcion;

    /** Solicitud de origen; nulo en los "puntos varios" creados durante la sesión (RF-26). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitud_id", unique = true)
    private Solicitud solicitud;

    @Column(name = "es_varios", nullable = false)
    private boolean esVarios = false;

    // ---- decisión (nula hasta que se registra) ----
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultadoDecision resultado;

    @Column(name = "votos_favor", nullable = false)
    private int votosFavor;

    @Column(name = "votos_contra", nullable = false)
    private int votosContra;

    @Column(nullable = false)
    private int abstenciones;

    @Column(name = "decidido_por", length = 150)
    private String decididoPor;

    /** Notas de debate en XHTML básico ya sanitizado (RF-25). */
    @Column(name = "notas_debate", columnDefinition = "TEXT")
    private String notasDebate;

    public boolean tieneDecision() {
        return resultado != null;
    }
}

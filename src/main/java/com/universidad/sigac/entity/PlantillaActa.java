package com.universidad.sigac.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Plantilla HTML/Thymeleaf versionada (RF-13). Cada guardado crea una versión nueva y las anteriores se conservan. */
@Entity
@Table(name = "plantillas_acta")
@Getter @Setter @NoArgsConstructor
public class PlantillaActa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_consejo_id", nullable = false)
    private TipoConsejo tipoConsejo;

    /** Si es nula, la plantilla aplica a todas las Facultades del tipo de consejo (RF-14). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id")
    private Facultad facultad;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contenido;

    @Column(nullable = false)
    private boolean activa;

    @Column(length = 300)
    private String comentario;

    @Column(name = "creado_por", length = 150)
    private String creadoPor;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();
}

package com.universidad.sigac.entity;

import com.universidad.sigac.enums.EstadoActa;
import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Acta de una sesión. Concentra el contenido, el estado del circuito de aprobación y la huella de la firma
 * electrónica. RN-17: una vez PUBLICADA, el servicio rechaza cualquier modificación.
 */
@Entity
@Table(name = "actas")
@Getter @Setter @NoArgsConstructor
public class Acta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_id", nullable = false, unique = true)
    private Sesion sesion;

    /** RN-13: p. ej. ACTA-CF-2026-004. */
    @Column(nullable = false, unique = true, length = 40)
    private String consecutivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_consejo_id", nullable = false)
    private TipoConsejo tipoConsejo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "facultad_id", nullable = false)
    private Facultad facultad;

    @Column(name = "fecha_sesion", nullable = false)
    private LocalDate fechaSesion;

    @Column(name = "plantilla_version")
    private Integer plantillaVersion;

    /** XHTML generado desde la plantilla; editable mientras el acta no esté publicada. */
    @Column(name = "contenido_html", nullable = false, columnDefinition = "LONGTEXT")
    private String contenidoHtml;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoActa estado = EstadoActa.BORRADOR;

    @Column(name = "observacion_rechazo", length = 2000)
    private String observacionRechazo;

    // ---- firma electrónica con huella de auditoría (RN-16) ----
    @Column(name = "firmado_por", length = 150)
    private String firmadoPor;

    @Column(name = "rol_firmante", length = 40)
    private String rolFirmante;

    @Column(name = "firmado_en")
    private Instant firmadoEn;

    @Column(name = "firma_ip", length = 64)
    private String firmaIp;

    /** SHA-256 del contenido XHTML aprobado (el que se estampa visualmente en el documento). */
    @Column(name = "hash_contenido", length = 64)
    private String hashContenido;

    // ---- archivo PDF/A final custodiado en disco ----
    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo;

    /** SHA-256 del PDF/A final; se recalcula en cada descarga (RN-18). */
    @Column(name = "hash_archivo", length = 64)
    private String hashArchivo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn = Instant.now();
}

package com.universidad.sigac.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Anexo PDF: se guarda la ruta relativa y su hash SHA-256 de integridad (RF-16). */
@Entity
@Table(name = "anexos")
@Getter @Setter @NoArgsConstructor
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(nullable = false, length = 500)
    private String ruta;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;
}

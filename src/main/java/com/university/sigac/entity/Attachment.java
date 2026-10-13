package com.university.sigac.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** PDF attachment: stores the relative path and its SHA-256 integrity hash (RF-16). */
@Entity
@Table(name = "attachments")
@Getter @Setter @NoArgsConstructor
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CouncilRequest request;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(nullable = false, length = 500)
    private String path;

    @Column(name = "sha256_hash", nullable = false, length = 64)
    private String sha256Hash;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;
}

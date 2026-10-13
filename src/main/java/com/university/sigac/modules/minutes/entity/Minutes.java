package com.university.sigac.modules.minutes.entity;

import com.university.sigac.modules.minutes.enums.MinutesStatus;
import com.university.sigac.modules.requests.entity.Session;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.Faculty;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Minutes of a session. Holds the content, the state of the approval workflow and the electronic
 * signature fingerprint. RN-17: once PUBLISHED, the service rejects any modification.
 */
@Entity
@Table(name = "minutes")
@Getter @Setter @NoArgsConstructor
public class Minutes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    /** RN-13: e.g. MIN-CF-2026-004. */
    @Column(name = "serial_number", nullable = false, unique = true, length = 40)
    private String serialNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_type_id", nullable = false)
    private CouncilType councilType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "template_version")
    private Integer templateVersion;

    /** XHTML generated from the template; editable while the minutes are not published. */
    @Column(name = "html_content", nullable = false, columnDefinition = "LONGTEXT")
    private String htmlContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private MinutesStatus status = MinutesStatus.DRAFT;

    @Column(name = "rejection_note", length = 2000)
    private String rejectionNote;

    // ---- electronic signature with audit fingerprint (RN-16) ----
    @Column(name = "signed_by", length = 150)
    private String signedBy;

    @Column(name = "signer_role", length = 40)
    private String signerRole;

    @Column(name = "signed_at")
    private Instant signedAt;

    @Column(name = "signature_ip", length = 64)
    private String signatureIp;

    /** SHA-256 of the approved XHTML content (the one visually stamped on the document). */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    // ---- final PDF/A file kept on disk ----
    @Column(name = "file_path", length = 500)
    private String filePath;

    /** SHA-256 of the final PDF/A; recomputed on every download (RN-18). */
    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}

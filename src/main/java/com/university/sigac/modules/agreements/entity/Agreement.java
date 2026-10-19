package com.university.sigac.modules.agreements.entity;

import com.university.sigac.modules.users.entity.Faculty;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Faculty Council agreement (acuerdo). The number is unique per Faculty and year. */
@Entity
@Table(name = "agreements", uniqueConstraints = @UniqueConstraint(name = "uk_agreements_number",
        columnNames = {"faculty_id", "agreement_year", "number"}))
@Getter @Setter @NoArgsConstructor
public class Agreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "type_code", nullable = false, length = 60)
    private String typeCode;

    @Column(nullable = false)
    private int number;

    @Column(name = "agreement_year", nullable = false)
    private int year;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(name = "council_title", nullable = false, length = 200)
    private String councilTitle;

    @Column(name = "legal_basis", length = 500)
    private String legalBasis;

    @Column(length = 120)
    private String closing;

    @Column(name = "president_name", length = 150)
    private String presidentName;

    @Column(name = "secretary_name", length = 150)
    private String secretaryName;

    /** JSON with the field values, the recitals and the articles (see AgreementDtos.AgreementContent). */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}

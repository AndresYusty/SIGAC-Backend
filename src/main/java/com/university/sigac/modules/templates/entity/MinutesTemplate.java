package com.university.sigac.modules.templates.entity;

import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.Faculty;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Versioned HTML/Thymeleaf template (RF-13). Every save creates a new version and previous ones are kept. */
@Entity
@Table(name = "minutes_templates")
@Getter @Setter @NoArgsConstructor
public class MinutesTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_type_id", nullable = false)
    private CouncilType councilType;

    /** When null, the template applies to every Faculty of the council type (RF-14). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id")
    private Faculty faculty;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 300)
    private String comment;

    @Column(name = "created_by", length = 150)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}

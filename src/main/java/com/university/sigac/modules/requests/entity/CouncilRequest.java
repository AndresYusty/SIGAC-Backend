package com.university.sigac.modules.requests.entity;

import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.RequestType;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "council_requests")
@Getter @Setter @NoArgsConstructor
public class CouncilRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique filing code, e.g. REQ-2026-000123. */
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 150)
    private String requester;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    private RequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id")
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filed_by_id", nullable = false)
    private User filedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status = RequestStatus.FILED;

    @Column(name = "filed_at", nullable = false)
    private Instant filedAt = Instant.now();

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();
}

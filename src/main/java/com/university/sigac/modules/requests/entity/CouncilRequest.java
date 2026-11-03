package com.university.sigac.modules.requests.entity;

import com.university.sigac.modules.requests.enums.CancellationKind;
import com.university.sigac.modules.requests.enums.RequestStatus;
import com.university.sigac.modules.requests.enums.RequestType;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.entity.Program;
import com.university.sigac.modules.users.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
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
    @Column(name = "request_type", nullable = false, columnDefinition = "varchar(40)")
    private RequestType requestType;

    /** MI-FOR-028 date (day, month and year). */
    @Column(name = "filed_on")
    private LocalDate filedOn;

    /** Academic period: A is 1(A), B is 2(B). */
    @Column(name = "academic_period", length = 1)
    private String academicPeriod;

    @Column(name = "student_code", length = 30)
    private String studentCode;

    @Column(name = "identity_document", length = 30)
    private String identityDocument;

    @Column(name = "identity_issued_in", length = 80)
    private String identityIssuedIn;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "electronic_notice", nullable = false, columnDefinition = "bit not null default 0")
    private boolean electronicNotice;

    @Column(name = "counselor_name", length = 150)
    private String counselorName;

    /** Filled when the topic is a special case ("Otro, ¿cuál?"). */
    @Column(name = "other_detail", length = 200)
    private String otherDetail;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_kind", length = 10)
    private CancellationKind cancellationKind;

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

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineNumber")
    private List<RequestCourse> courses = new ArrayList<>();
}

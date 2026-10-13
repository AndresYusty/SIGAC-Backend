package com.university.sigac.modules.requests.entity;

import com.university.sigac.modules.requests.enums.RequestStatus;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Status history. RN-10: every return records its justification. */
@Entity
@Table(name = "request_history")
@Getter @Setter @NoArgsConstructor
public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private CouncilRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 20)
    private RequestStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 20)
    private RequestStatus newStatus;

    @Column(length = 2000)
    private String reason;

    @Column(name = "user_name", length = 150)
    private String userName;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt = Instant.now();
}

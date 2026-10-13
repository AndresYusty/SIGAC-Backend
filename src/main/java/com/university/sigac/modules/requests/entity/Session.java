package com.university.sigac.modules.requests.entity;

import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.Faculty;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Council session. Contains its agenda (list of items) and moves through {@link SessionStatus}. */
@Entity
@Table(name = "sessions")
@Getter @Setter @NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "council_type_id", nullable = false)
    private CouncilType councilType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(nullable = false, length = 200)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.SCHEDULED;

    /** true once the agenda is closed and the notice has been sent (RN-09: no further changes). */
    @Column(name = "agenda_closed", nullable = false)
    private boolean agendaClosed = false;

    @Column(name = "actual_start")
    private LocalDateTime actualStart;

    @Column(name = "actual_end")
    private LocalDateTime actualEnd;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<AgendaItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean hasStatus(SessionStatus s) {
        return status == s;
    }
}

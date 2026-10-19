package com.university.sigac.modules.requests.entity;

import com.university.sigac.modules.requests.enums.SessionStatus;
import com.university.sigac.modules.users.entity.CouncilType;
import com.university.sigac.modules.users.entity.Faculty;
import com.university.sigac.modules.users.enums.CouncilKind;
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

    public static final String VIRTUAL_LOCATION = "Virtual (by email)";

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

    /** null only for virtual sessions. */
    @Column(length = 200)
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

    /** Guests of an Expanded Council: voice but no vote. */
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<SessionGuest> guests = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public boolean hasStatus(SessionStatus s) {
        return status == s;
    }

    /** Sessions of legacy council types (without a kind) behave as ordinary sessions. */
    public CouncilKind kind() {
        CouncilKind kind = councilType.getKind();
        return kind != null ? kind : CouncilKind.ORDINARY;
    }

    /** Location as printed in the notice and the minutes. */
    public String documentLocation() {
        return location != null ? location : VIRTUAL_LOCATION;
    }
}

package com.university.sigac.modules.requests.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Person invited to an Expanded Council (e.g. a professor) to give an opinion: voice but no vote, no quorum. */
@Entity
@Table(name = "session_guests")
@Getter @Setter @NoArgsConstructor
public class SessionGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false, length = 150)
    private String name;

    /** Receives the session notice when the agenda is closed. */
    @Column(length = 150)
    private String email;

    /** Capacity in which the guest is invited, e.g. "Professor, Systems Engineering". */
    @Column(length = 150)
    private String affiliation;
}

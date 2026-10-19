package com.university.sigac.modules.requests.entity;

import com.university.sigac.modules.requests.enums.DecisionOutcome;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Agenda item. Also stores the decision taken and the debate notes (module 4). */
@Entity
@Table(name = "agenda_items")
@Getter @Setter @NoArgsConstructor
public class AgendaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 4000)
    private String description;

    /** Source request; null for "miscellaneous items" created during the session (RF-26). */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", unique = true)
    private CouncilRequest request;

    @Column(nullable = false)
    private boolean miscellaneous = false;

    /** Urgent item of an Extraordinary Council, added to the agenda without a filed request. */
    @Column(nullable = false)
    private boolean urgent = false;

    // ---- decision (null until recorded) ----
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DecisionOutcome outcome;

    @Column(name = "votes_for", nullable = false)
    private int votesFor;

    @Column(name = "votes_against", nullable = false)
    private int votesAgainst;

    @Column(nullable = false)
    private int abstentions;

    @Column(name = "decided_by", length = 150)
    private String decidedBy;

    /** Debate notes in basic, already sanitized XHTML (RF-25). */
    @Column(name = "debate_notes", columnDefinition = "TEXT")
    private String debateNotes;

    public boolean hasDecision() {
        return outcome != null;
    }
}

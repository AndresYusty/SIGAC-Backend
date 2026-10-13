package com.university.sigac.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Incremental counter per key (replaces PostgreSQL sequences on MySQL). */
@Entity
@Table(name = "sequence_counters")
@Getter
@Setter
@NoArgsConstructor
public class SequenceCounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "counter_key", nullable = false, unique = true, length = 100)
    private String counterKey;

    /** Quoted because LAST_VALUE is a reserved word in MySQL 8. */
    @Column(name = "`last_value`", nullable = false)
    private long lastValue;

    public SequenceCounter(String counterKey, long lastValue) {
        this.counterKey = counterKey;
        this.lastValue = lastValue;
    }
}

package com.university.sigac.modules.users.entity;

import com.university.sigac.modules.users.enums.CouncilKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Council types: one row per {@link CouncilKind}. The code is part of the minutes serial number.
 * Rows without a kind are legacy types kept only for the minutes already issued with them.
 */
@Entity
@Table(name = "council_types")
@Getter @Setter @NoArgsConstructor
public class CouncilType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, length = 20)
    private CouncilKind kind;

    public CouncilType(CouncilKind kind) {
        this.kind = kind;
        this.code = kind.defaultCode();
        this.name = kind.defaultName();
    }
}

package com.university.sigac.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Council types (Faculty, Academic, Superior). The code is part of the minutes serial number. */
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

    public CouncilType(String code, String name) {
        this.code = code;
        this.name = name;
    }
}

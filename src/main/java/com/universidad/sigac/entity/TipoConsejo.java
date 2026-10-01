package com.universidad.sigac.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tipos de consejo (Facultad, Académico, Superior). El código forma parte del consecutivo del acta. */
@Entity
@Table(name = "tipos_consejo")
@Getter @Setter @NoArgsConstructor
public class TipoConsejo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String nombre;

    public TipoConsejo(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }
}

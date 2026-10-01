package com.universidad.sigac.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Contador incremental por clave (reemplaza las secuencias de PostgreSQL en MySQL). */
@Entity
@Table(name = "consecutivos")
@Getter
@Setter
@NoArgsConstructor
public class Consecutivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String clave;

    @Column(nullable = false)
    private long ultimo;

    public Consecutivo(String clave, long ultimo) {
        this.clave = clave;
        this.ultimo = ultimo;
    }
}

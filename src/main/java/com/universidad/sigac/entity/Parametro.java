package com.universidad.sigac.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Parámetro global llave-valor, modificable sin redespliegue (RN-04). */
@Entity
@Table(name = "parametros")
@Getter @Setter @NoArgsConstructor
public class Parametro {

    @Id
    @Column(length = 100)
    private String clave;

    @Column(nullable = false, length = 500)
    private String valor;

    @Column(length = 300)
    private String descripcion;

    public Parametro(String clave, String valor, String descripcion) {
        this.clave = clave;
        this.valor = valor;
        this.descripcion = descripcion;
    }
}

package com.universidad.sigac.entity;

import com.universidad.sigac.enums.RolNombre;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Usuario del sistema, vinculado a una Facultad (y opcionalmente a un Programa) según RN-01. */
@Entity
@Table(name = "usuarios")
@Getter @Setter @NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facultad_id")
    private Facultad facultad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programa_id")
    private Programa programa;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 40)
    private Set<RolNombre> roles = new HashSet<>();

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    public boolean tieneRol(RolNombre rol) {
        return roles.contains(rol);
    }
}

package com.universidad.sigac.enums;

import java.util.List;

public enum RolNombre {
    ROLE_DEV,
    ROLE_ADMIN,
    ROLE_SECRETARIA_1,
    ROLE_SECRETARIA_2,
    ROLE_PRESIDENTE,
    ROLE_CONSEJERO;

    /** Integrantes del Consejo con derecho a voto: base del quórum (RN-11). */
    public static final List<RolNombre> CON_VOTO = List.of(ROLE_PRESIDENTE, ROLE_CONSEJERO);

    /** ADMIN y DEV no pertenecen a una Facultad concreta. */
    public boolean esGlobal() {
        return this == ROLE_DEV || this == ROLE_ADMIN;
    }
}

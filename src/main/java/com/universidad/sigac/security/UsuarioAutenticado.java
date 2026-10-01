package com.universidad.sigac.security;

import java.util.List;

/** Usuario autenticado (principal) reconstruido desde el JWT. */
public record UsuarioAutenticado(Long id, String email, String nombre, List<String> roles, Long facultadId, Long programaId) {

    public boolean tieneRol(String rol) {
        return roles.contains(rol);
    }

    public boolean tieneAlgunRol(String... candidatos) {
        for (String r : candidatos) {
            if (roles.contains(r)) {
                return true;
            }
        }
        return false;
    }

    /** ADMIN y DEV no están acotados a una Facultad. */
    public boolean esGlobal() {
        return roles.contains("ROLE_ADMIN") || roles.contains("ROLE_DEV");
    }
}

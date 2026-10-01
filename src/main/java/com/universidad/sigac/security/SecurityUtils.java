package com.universidad.sigac.security;

import com.universidad.sigac.exception.ApiException;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UsuarioAutenticado current() {
        UsuarioAutenticado u = currentOrNull();
        if (u == null) {
            throw ApiException.unauthorized("Sesión no válida o expirada");
        }
        return u;
    }

    public static UsuarioAutenticado currentOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof UsuarioAutenticado u) {
            return u;
        }
        return null;
    }

    /**
     * RN-02: los permisos se restringen al ámbito de adscripción. ADMIN/DEV no están acotados a una Facultad.
     */
    public static void verificarFacultad(UsuarioAutenticado u, Long facultadId) {
        if (u.esGlobal()) {
            return;
        }
        if (u.facultadId() == null || !Objects.equals(u.facultadId(), facultadId)) {
            throw ApiException.forbidden("No tiene permisos sobre recursos de otra Facultad");
        }
    }
}

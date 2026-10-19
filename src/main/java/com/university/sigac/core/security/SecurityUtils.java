package com.university.sigac.core.security;

import com.university.sigac.core.exception.ApiException;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AuthenticatedUser current() {
        AuthenticatedUser u = currentOrNull();
        if (u == null) {
            throw ApiException.unauthorized("Invalid or expired session");
        }
        return u;
    }

    public static AuthenticatedUser currentOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthenticatedUser u) {
            return u;
        }
        return null;
    }

    /**
     * RN-02: permissions are restricted to the user's assigned scope. ADMIN is not limited to a Faculty.
     */
    public static void checkFaculty(AuthenticatedUser u, Long facultyId) {
        if (u.isGlobal()) {
            return;
        }
        if (u.facultyId() == null || !Objects.equals(u.facultyId(), facultyId)) {
            throw ApiException.forbidden("You do not have permissions on resources of another Faculty");
        }
    }
}

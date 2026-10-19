package com.university.sigac.core.security;

import java.util.List;

/** Authenticated user (principal) rebuilt from the JWT. */
public record AuthenticatedUser(Long id, String email, String name, List<String> roles, Long facultyId, Long programId) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(String... candidates) {
        for (String r : candidates) {
            if (roles.contains(r)) {
                return true;
            }
        }
        return false;
    }

    /** ADMIN is not scoped to a Faculty. */
    public boolean isGlobal() {
        return roles.contains("ROLE_ADMIN");
    }
}

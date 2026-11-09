package com.university.sigac.modules.users.service;

import com.university.sigac.modules.users.entity.User;
import com.university.sigac.modules.users.enums.RoleName;
import java.util.Set;

/**
 * Who may activate or deactivate support staff.
 * The dean ({@link RoleName#ROLE_PRESIDENT}) is the faculty head: secretaries of that faculty.
 * The program head manages only the secretary of their own program.
 */
final class SupportAccess {

    private SupportAccess() {
    }

    static boolean canManage(User actor, User target) {
        if (actor.hasRole(RoleName.ROLE_ADMIN)) {
            return true;
        }
        boolean asDean = actor.hasRole(RoleName.ROLE_PRESIDENT)
                && sameFaculty(actor, target)
                && onlyRoles(target, Set.copyOf(RoleName.FACULTY_SUPPORT));
        boolean asProgramHead = actor.hasRole(RoleName.ROLE_PROGRAM_HEAD)
                && sameProgram(actor, target)
                && onlyRoles(target, Set.copyOf(RoleName.PROGRAM_SUPPORT));
        return asDean || asProgramHead;
    }

    private static boolean onlyRoles(User target, Set<RoleName> allowed) {
        return !target.getRoles().isEmpty() && allowed.containsAll(target.getRoles());
    }

    private static boolean sameFaculty(User actor, User target) {
        if (actor.getFaculty() == null || target.getFaculty() == null || actor.getFaculty().getId() == null) {
            return false;
        }
        return actor.getFaculty().getId().equals(target.getFaculty().getId());
    }

    private static boolean sameProgram(User actor, User target) {
        if (actor.getProgram() == null || target.getProgram() == null || actor.getProgram().getId() == null) {
            return false;
        }
        return actor.getProgram().getId().equals(target.getProgram().getId());
    }
}

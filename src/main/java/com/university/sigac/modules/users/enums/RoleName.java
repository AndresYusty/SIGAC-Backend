package com.university.sigac.modules.users.enums;

import java.util.List;

public enum RoleName {
    ROLE_ADMIN,
    ROLE_COUNCIL_SECRETARY,
    ROLE_FACULTY_SECRETARY,
    ROLE_PRESIDENT,
    ROLE_COUNCIL_MEMBER,
    /** Head of an academic program. Activates and deactivates that program's secretary. */
    ROLE_PROGRAM_HEAD;

    /** Council members with voting rights: the basis for the quorum (RN-11). The dean presides as ROLE_PRESIDENT. */
    public static final List<RoleName> VOTING = List.of(ROLE_PRESIDENT, ROLE_COUNCIL_MEMBER);

    /** Secretaries a dean (faculty head) may activate or deactivate inside the faculty. */
    public static final List<RoleName> FACULTY_SUPPORT = List.of(ROLE_COUNCIL_SECRETARY, ROLE_FACULTY_SECRETARY);

    /** Secretary a program head may activate or deactivate inside the program. */
    public static final List<RoleName> PROGRAM_SUPPORT = List.of(ROLE_FACULTY_SECRETARY);

    /** ADMIN is not scoped to a Faculty and also manages templates. */
    public boolean isGlobal() {
        return this == ROLE_ADMIN;
    }
}

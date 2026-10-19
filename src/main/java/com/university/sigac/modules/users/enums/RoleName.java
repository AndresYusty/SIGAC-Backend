package com.university.sigac.modules.users.enums;

import java.util.List;

public enum RoleName {
    ROLE_ADMIN,
    ROLE_COUNCIL_SECRETARY,
    ROLE_FACULTY_SECRETARY,
    ROLE_PRESIDENT,
    ROLE_COUNCIL_MEMBER;

    /** Council members with voting rights: the basis for the quorum (RN-11). */
    public static final List<RoleName> VOTING = List.of(ROLE_PRESIDENT, ROLE_COUNCIL_MEMBER);

    /** ADMIN is not scoped to a Faculty and also manages templates. */
    public boolean isGlobal() {
        return this == ROLE_ADMIN;
    }
}

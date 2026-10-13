package com.university.sigac.modules.users.enums;

import java.util.List;

public enum RoleName {
    ROLE_DEV,
    ROLE_ADMIN,
    ROLE_SECRETARY_1,
    ROLE_SECRETARY_2,
    ROLE_PRESIDENT,
    ROLE_COUNCIL_MEMBER;

    /** Council members with voting rights: the basis for the quorum (RN-11). */
    public static final List<RoleName> VOTING = List.of(ROLE_PRESIDENT, ROLE_COUNCIL_MEMBER);

    /** ADMIN and DEV do not belong to a specific Faculty. */
    public boolean isGlobal() {
        return this == ROLE_DEV || this == ROLE_ADMIN;
    }
}

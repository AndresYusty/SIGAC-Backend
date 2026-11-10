package com.university.sigac.modules.agreements.enums;

/** Council documents drafted from a type: agreements (acuerdos) and memorandums (memorandos). Each kind has its own numbering. */
public enum DocumentKind {
    AGREEMENT, MEMORANDUM;

    /** Types in the catalog without a kind are agreements. */
    public static DocumentKind of(String value) {
        return value == null || value.isBlank() ? AGREEMENT : valueOf(value.trim());
    }
}

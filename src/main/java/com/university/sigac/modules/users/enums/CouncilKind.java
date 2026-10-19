package com.university.sigac.modules.users.enums;

/** The four kinds of Faculty Council. Each one changes how its sessions are convened and conducted. */
public enum CouncilKind {
    /** Routine session. */
    ORDINARY("CO", "Ordinary Council"),
    /** Something that cannot wait for the next ordinary session: urgent items may be added to the agenda. */
    EXTRAORDINARY("CE", "Extraordinary Council"),
    /** Urgent consultations held by email: no location and no deliberative quorum. */
    VIRTUAL("CV", "Virtual Council"),
    /** Needs the opinion of professors: guests with voice but no vote, who do not count for the quorum. */
    EXPANDED("CAM", "Expanded Council");

    private final String defaultCode;
    private final String defaultName;

    CouncilKind(String defaultCode, String defaultName) {
        this.defaultCode = defaultCode;
        this.defaultName = defaultName;
    }

    public String defaultCode() {
        return defaultCode;
    }

    public String defaultName() {
        return defaultName;
    }

    public boolean requiresQuorum() {
        return this != VIRTUAL;
    }

    public boolean requiresLocation() {
        return this != VIRTUAL;
    }

    public boolean allowsGuests() {
        return this == EXPANDED;
    }

    public boolean allowsUrgentItems() {
        return this == EXTRAORDINARY;
    }
}

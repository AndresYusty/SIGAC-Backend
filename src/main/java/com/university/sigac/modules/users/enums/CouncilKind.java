package com.university.sigac.modules.users.enums;

/** The four kinds of Faculty Council. Each one changes how its sessions are convened and conducted. */
public enum CouncilKind {
    /** Routine session. */
    ORDINARY("CO", "Ordinary Council"),
    /** Something that cannot wait for the next ordinary session: urgent items may be added to the agenda. */
    EXTRAORDINARY("CE", "Extraordinary Council"),
    /** Remote session on a digital platform. It needs the meeting link and the same quorum as the others. */
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

    /** Every council deliberates with the configured quorum, including a remote one. */
    public boolean requiresQuorum() {
        return true;
    }

    /** A room for an in-person council, or the platform link for a virtual one. */
    public boolean requiresLocation() {
        return true;
    }

    public boolean allowsGuests() {
        return this == EXPANDED;
    }

    public boolean allowsUrgentItems() {
        return this == EXTRAORDINARY;
    }
}

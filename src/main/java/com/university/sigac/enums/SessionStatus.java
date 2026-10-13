package com.university.sigac.enums;

/** Lifecycle of a council session. */
public enum SessionStatus {
    SCHEDULED,   // the agenda is being prepared
    CONVENED,    // agenda closed and notice sent
    STARTED,     // session in progress (quorum reached)
    SUSPENDED,
    FINISHED;

    public boolean canTransitionTo(SessionStatus target) {
        return switch (this) {
            case SCHEDULED -> target == CONVENED;
            case CONVENED -> target == STARTED;
            case STARTED -> target == SUSPENDED || target == FINISHED;
            case SUSPENDED -> target == STARTED || target == FINISHED;
            case FINISHED -> false;
        };
    }
}

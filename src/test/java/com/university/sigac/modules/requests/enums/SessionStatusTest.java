package com.university.sigac.modules.requests.enums;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SessionStatusTest {

    @Test
    void normalFlow() {
        assertTrue(SessionStatus.SCHEDULED.canTransitionTo(SessionStatus.CONVENED));
        assertTrue(SessionStatus.CONVENED.canTransitionTo(SessionStatus.STARTED));
        assertTrue(SessionStatus.STARTED.canTransitionTo(SessionStatus.SUSPENDED));
        assertTrue(SessionStatus.SUSPENDED.canTransitionTo(SessionStatus.STARTED));
        assertTrue(SessionStatus.STARTED.canTransitionTo(SessionStatus.FINISHED));
    }

    @Test
    void invalidTransitions() {
        assertFalse(SessionStatus.SCHEDULED.canTransitionTo(SessionStatus.STARTED));
        assertFalse(SessionStatus.FINISHED.canTransitionTo(SessionStatus.STARTED));
        assertFalse(SessionStatus.CONVENED.canTransitionTo(SessionStatus.FINISHED));
    }
}

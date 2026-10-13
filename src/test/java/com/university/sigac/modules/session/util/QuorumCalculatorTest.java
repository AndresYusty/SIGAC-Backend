package com.university.sigac.modules.session.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QuorumCalculatorTest {

    @Test
    void fiftyPercentPlusOne() {
        assertEquals(6, QuorumCalculator.required(10, 50));
        assertEquals(5, QuorumCalculator.required(9, 50));
        assertEquals(3, QuorumCalculator.required(4, 50));
    }

    @Test
    void neverRequiresMoreThanTheTotal() {
        assertEquals(1, QuorumCalculator.required(1, 50));
        assertEquals(3, QuorumCalculator.required(3, 100));
    }

    @Test
    void noMembersMeansNoQuorum() {
        assertEquals(0, QuorumCalculator.required(0, 50));
        assertFalse(QuorumCalculator.reached(0, 0, 50));
    }

    @Test
    void reachedDependsOnAttendance() {
        assertTrue(QuorumCalculator.reached(10, 6, 50));
        assertFalse(QuorumCalculator.reached(10, 5, 50));
    }
}

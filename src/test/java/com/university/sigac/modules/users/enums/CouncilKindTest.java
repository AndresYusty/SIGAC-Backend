package com.university.sigac.modules.users.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class CouncilKindTest {

    @Test
    void onlyTheVirtualCouncilSkipsQuorumAndLocation() {
        for (CouncilKind k : CouncilKind.values()) {
            assertEquals(k != CouncilKind.VIRTUAL, k.requiresQuorum(), k.name());
            assertEquals(k != CouncilKind.VIRTUAL, k.requiresLocation(), k.name());
        }
    }

    @Test
    void guestsAndUrgentItemsBelongToTheirKind() {
        assertTrue(CouncilKind.EXPANDED.allowsGuests());
        assertFalse(CouncilKind.ORDINARY.allowsGuests());
        assertTrue(CouncilKind.EXTRAORDINARY.allowsUrgentItems());
        assertFalse(CouncilKind.VIRTUAL.allowsUrgentItems());
    }

    @Test
    void defaultCodesAreUniqueAndFitTheColumn() {
        Set<String> codes = Arrays.stream(CouncilKind.values()).map(CouncilKind::defaultCode).collect(Collectors.toSet());
        assertEquals(CouncilKind.values().length, codes.size());
        assertTrue(codes.stream().allMatch(c -> c.length() <= 10));
    }
}

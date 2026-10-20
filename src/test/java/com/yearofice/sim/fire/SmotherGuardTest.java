package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SmotherGuardTest {

    @Test
    void firstPourOfTheDayNeedsASecondActionThenIsInstant() {
        SmotherGuard<String> g = new SmotherGuard<>();
        assertFalse(g.allow("a", 3, 100, true), "armed, not poured");
        assertTrue(g.allow("a", 3, 130, true), "the second action within 3 s pours");
        assertTrue(g.allow("a", 3, 4000, true), "later the same day it is instant");
    }

    @Test
    void theArmLapses() {
        SmotherGuard<String> g = new SmotherGuard<>();
        assertFalse(g.allow("a", 1, 0, true));
        assertFalse(g.allow("a", 1, 61, true), "too late: armed again, not poured");
        assertTrue(g.allow("a", 1, 100, true));
    }

    @Test
    void theGuardResetsAtMidnightAndIsPerPlayer() {
        SmotherGuard<String> g = new SmotherGuard<>();
        g.allow("a", 1, 0, true);
        assertTrue(g.allow("a", 1, 10, true));
        assertFalse(g.allow("a", 2, 20, true), "a new day");
        assertFalse(g.allow("b", 1, 20, true), "another player");
    }

    @Test
    void coalsAreNeverGuarded() {
        assertTrue(new SmotherGuard<String>().allow("a", 1, 0, false));
    }
}

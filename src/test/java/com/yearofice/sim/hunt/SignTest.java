package com.yearofice.sim.hunt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SignTest {
    private final Sign.Rules r = Sign.Rules.standard();

    @Test
    void bandsFollowAge() {
        assertEquals(Sign.Age.FRESH, Sign.age(r, 30));
        assertEquals(Sign.Age.HOURS, Sign.age(r, 300));
        assertEquals(Sign.Age.DAY, Sign.age(r, 2000));
        assertEquals(Sign.Age.OLD, Sign.age(r, 5000));
    }

    @Test
    void weatherErasesTheRightGround() {
        assertTrue(Sign.erased(r, Sign.Kind.TRACK, Sign.Substrate.SNOW, 10, Sign.Precip.SNOW));
        assertFalse(Sign.erased(r, Sign.Kind.TRACK, Sign.Substrate.SNOW, 10, Sign.Precip.RAIN));
        assertTrue(Sign.erased(r, Sign.Kind.TRACK, Sign.Substrate.MUD, 10, Sign.Precip.RAIN));
        assertFalse(Sign.erased(r, Sign.Kind.SCAT, Sign.Substrate.MUD, 10, Sign.Precip.RAIN));
        assertTrue(Sign.erased(r, Sign.Kind.TRACK, Sign.Substrate.MOSS, 1500, Sign.Precip.NONE), "moss springs back after a day");
    }

    @Test
    void readingNamesSpeciesAndWay() {
        Sign.Read read = Sign.read(r, "yearofice:moose", Sign.Kind.TRACK, 8 * 60, 10 * 60, 1, 0, 1, 1.0);
        assertEquals("Moose, this morning, heading east", read.text());
        assertEquals("An animal, some hours ago, heading east", Sign.read(r, "moose", Sign.Kind.TRACK, 8 * 60, 10 * 60, 1, 0, 1, 0.5).text());
        assertEquals(0.5, Sign.confidence(0, 40), 1e-9);
        assertEquals(1.0, Sign.confidence(40, 40), 1e-9);
    }
}

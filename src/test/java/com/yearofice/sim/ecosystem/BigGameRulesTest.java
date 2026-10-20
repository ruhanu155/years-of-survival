package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.time.Calendar;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Predation, the predator clock, field care, yields and the dormancy dates. */
class BigGameRulesTest {
    @Test
    void chaseEndsAtItsCap() {
        Predation.Chase c = new Predation.Chase(150);
        for (int i = 0; i < 400 && !c.over(); i++) c.advance(15.0 / 20);
        assertTrue(c.over());
        assertTrue(c.ranM() < 150.8, "ran " + c.ranM());
    }

    @Test
    void ruleHuntWindowWrapsMidnight() {
        Predation.Rule wolf = new Predation.Rule(List.of("yearofice:moose"), 150, 100, 15, 17, 6, 1.5, 10);
        assertTrue(wolf.huntsAt(22));
        assertTrue(wolf.huntsAt(3));
        assertFalse(wolf.huntsAt(12));
        assertTrue(wolf.takes("yearofice:moose"));
        assertFalse(wolf.takes("yearofice:beaver"));
    }

    /** Ready for development M6: a bear reaches an uncleared fish-cleaning spot within 2 game days on 7 of 10 seeds. */
    @Test
    void bearFindsFishSpotWithinTwoDaysOnSevenOfTenSeeds() {
        for (int month : new int[] {7, 8, 9}) {
            int found = 0;
            for (int seed = 1; seed <= 10; seed++) if (Predation.bearArrives(seed * 7919L + month, month, 12, 48)) found++;
            assertTrue(found >= 7, "month " + month + ": " + found + " of 10");
        }
    }

    @Test
    void noBearInWinter() {
        for (int seed = 1; seed <= 10; seed++) assertFalse(Predation.bearArrives(seed, 1, 12, 48));
    }

    @Test
    void fieldCareDeadlines() {
        assertEquals(4, FieldCare.quarterByHours(15));
        assertEquals(12, FieldCare.quarterByHours(5));
        assertEquals(8, FieldCare.quarterByHours(-5));
        assertEquals(4, FieldCare.quarterByHours(-20));
        assertEquals(1.0, FieldCare.dress(0.8, 3, 15).usable(), 1e-9);
        assertEquals(0.75, FieldCare.dress(1.5, 3, 15).usable(), 1e-9);
        assertEquals(0.7, FieldCare.dress(0.5, 5, 15).usable(), 1e-9);
        assertEquals(0.0, FieldCare.dress(0.5, 9, 15).usable(), 1e-9);
    }

    @Test
    void yieldsScaleAndSeasonalFat() {
        assertEquals(1.0, Yields.scale(450, 450), 1e-9);
        assertEquals(0.5, Yields.scale(225, 450), 1e-9);
        assertEquals(1.0, Yields.fatFactor("yearofice:moose", false, Calendar.dayOfYear(10, 5)), 1e-9);
        assertEquals(0.5, Yields.fatFactor("yearofice:moose", true, Calendar.dayOfYear(11, 1)), 1e-9);
        assertEquals(0.25, Yields.fatFactor("yearofice:caribou", true, Calendar.dayOfYear(3, 10)), 1e-9);
        assertEquals(0.15, Yields.fatFactor("yearofice:grizzly", false, Calendar.dayOfYear(5, 1)), 1e-9);
    }

    @Test
    void dormancyDates() {
        assertTrue(Dormancy.denned("yearofice:grizzly", Calendar.dayOfYear(12, 1)));
        assertFalse(Dormancy.denned("yearofice:grizzly", Calendar.dayOfYear(8, 1)));
        assertFalse(Dormancy.denned("yearofice:wolf", Calendar.dayOfYear(12, 1)));
        assertTrue(Dormancy.denned("yearofice:black_bear", Calendar.dayOfYear(10, 20)));
        assertFalse(Dormancy.denned("yearofice:black_bear", Calendar.dayOfYear(4, 21)));
        assertTrue(Dormancy.frozenFrog(Calendar.dayOfYear(1, 5)));
        assertFalse(Dormancy.frozenFrog(Calendar.dayOfYear(6, 5)));
    }
}

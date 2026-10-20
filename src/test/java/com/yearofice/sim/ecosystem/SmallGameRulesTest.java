package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.time.Calendar;
import java.util.List;
import org.junit.jupiter.api.Test;

/** M5 lane A pure rules: the ermine's coat dates, theft chances and protections, the gut pile's life. */
class SmallGameRulesTest {
    static Theft.Context ctx(boolean night, double playerM) { return new Theft.Context(night, false, false, false, false, false, true, playerM); }

    @Test
    void ermineIsWhiteOctFifteenToAprilFifteen() {
        assertEquals(SeasonalLooks.Coat.WINTER, SeasonalLooks.ermine(Calendar.dayOfYear(10, 15)));
        assertEquals(SeasonalLooks.Coat.WINTER, SeasonalLooks.ermine(Calendar.dayOfYear(1, 1)));
        assertEquals(SeasonalLooks.Coat.WINTER, SeasonalLooks.ermine(Calendar.dayOfYear(4, 15)));
        assertEquals(SeasonalLooks.Coat.MOTTLED, SeasonalLooks.ermine(Calendar.dayOfYear(10, 14)));
        assertEquals(SeasonalLooks.Coat.MOTTLED, SeasonalLooks.ermine(Calendar.dayOfYear(4, 16)));
        assertEquals(SeasonalLooks.Coat.SUMMER, SeasonalLooks.ermine(Calendar.dayOfYear(7, 1)));
    }

    @Test
    void chancesFollowTheTableAndItsProtections() {
        assertEquals(0.04, Theft.chance(Theft.Thief.RED_FOX, ctx(true, 100), 1), 1e-12);
        assertEquals(0.0, Theft.chance(Theft.Thief.RED_FOX, ctx(false, 100), 1), 1e-12, "foxes work at night");
        assertEquals(0.08, Theft.chance(Theft.Thief.GRAY_JAY, ctx(false, 100), 1), 1e-12);
        assertEquals(0.0, Theft.chance(Theft.Thief.RED_FOX, ctx(true, 30), 1), 1e-12, "a player within 30 m stops every thief");
        var fenced = new Theft.Context(true, true, false, false, false, false, true, 100);
        assertEquals(0.0, Theft.chance(Theft.Thief.RED_FOX, fenced, 1), 1e-12, "a closed fenced gate stops the fox");
        assertTrue(Theft.chance(Theft.Thief.MARTEN, fenced, 1) > 0, "but not the marten, which climbs");
        var roofed = new Theft.Context(true, false, true, false, false, false, true, 100);
        assertEquals(0.0, Theft.chance(Theft.Thief.MARTEN, roofed, 1), 1e-12, "only a roof stops the marten");
        var wolf = new Theft.Context(true, false, false, false, true, false, true, 100);
        assertEquals(0.04 * 0.25, Theft.chance(Theft.Thief.RED_FOX, wolf, 1), 1e-12, "a tamed wolf multiplies every roll by 0.25");
        var smoker = new Theft.Context(false, false, false, true, false, false, true, 100);
        assertEquals(0.03 * 0.5, Theft.chance(Theft.Thief.RAVEN, smoker, 1), 1e-12, "a lit smoker halves the birds");
        var named = new Theft.Context(false, false, false, false, false, true, true, 100);
        assertEquals(0.0, Theft.chance(Theft.Thief.GRAY_JAY, named, 1), 1e-12, "a named jay takes nothing");
        assertEquals(0.0, Theft.chance(Theft.Thief.MARTEN, new Theft.Context(true, false, false, false, false, false, false, 100), 1), 1e-12, "no spruce, no marten");
    }

    @Test
    void thievesTakeOnlyWhatTheyCanCarry() {
        var meat = new Theft.Stack(Theft.Kind.MEAT, 0.1, 10);
        var hide = new Theft.Stack(Theft.Kind.HIDE, 1.5, 1);
        var bigHide = new Theft.Stack(Theft.Kind.HIDE, 3.0, 1);
        var berries = new Theft.Stack(Theft.Kind.BERRY_SEED, 0.05, 50);
        assertTrue(Theft.takes(Theft.Thief.RED_FOX, hide));
        assertFalse(Theft.takes(Theft.Thief.RED_FOX, bigHide));
        assertTrue(Theft.takes(Theft.Thief.MARTEN, meat));
        assertFalse(Theft.takes(Theft.Thief.MARTEN, hide));
        assertTrue(Theft.takes(Theft.Thief.RED_SQUIRREL, berries));
        assertFalse(Theft.takes(Theft.Thief.RED_SQUIRREL, meat));
        assertEquals(5, Theft.unitsTaken(Theft.Thief.RED_SQUIRREL, berries), "10% of the basket");
        assertEquals(1, Theft.unitsTaken(Theft.Thief.RED_FOX, meat));
    }

    @Test
    void anHourlyRateOverManyHoursMatchesTheTable() {
        var stacks = List.of(new Theft.Stack(Theft.Kind.MEAT, 0.1, 40));
        int hits = 0, n = 20000;
        for (int h = 0; h < n; h++) if (Theft.rollHour(ctx(true, 100), 1, com.yearofice.sim.decay.Thresholds.stream(7L, "t", h), stacks).isPresent()) hits++;
        double expected = 1 - (1 - 0.04) * (1 - 0.02);
        assertEquals(expected, hits / (double) n, 0.01);
        int off = 0;
        for (int h = 0; h < n; h++) if (Theft.rollHour(ctx(true, 5), 1, com.yearofice.sim.decay.Thresholds.stream(7L, "t", h), stacks).isPresent()) off++;
        assertEquals(0, off);
    }

    @Test
    void gutPileLastsThreeDaysInSummerAndThirtyInWinter() {
        assertEquals(3.0, GutPile.lifeDays(15), 1e-12);
        assertEquals(30.0, GutPile.lifeDays(-20), 1e-12);
        assertTrue(GutPile.lifeDays(5) > 3 && GutPile.lifeDays(5) < 30);
        assertEquals(1.0, GutPile.goneShare(72, 15), 1e-12);
    }
}

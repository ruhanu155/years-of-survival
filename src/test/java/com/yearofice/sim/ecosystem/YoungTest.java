package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.time.Calendar;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class YoungTest {
    @Test
    void hidersAreNotOrphansWhileTheMotherVisits() {
        assertFalse(Young.orphaned("snowshoe_hare", 0));
        assertFalse(Young.orphaned("snowshoe_hare", 1));
        assertTrue(Young.orphaned("snowshoe_hare", 2));
        assertFalse(Young.orphaned("mule_deer", 0));
        assertTrue(Young.orphaned("mule_deer", 1));
        assertFalse(Young.orphaned("wolf", 5));
    }

    @Test
    void foundLeveretsAreOrphanedAboutFifteenPercent() {
        SplittableRandom r = new SplittableRandom(3);
        int orphans = 0;
        for (int i = 0; i < 1000; i++) if (Young.orphaned("snowshoe_hare", r.nextDouble() < Young.FOUND_ORPHAN_SHARE ? 2 : 0)) orphans++;
        assertTrue(orphans >= 100 && orphans <= 200, "orphans " + orphans);
    }

    @Test
    void birthWindows() {
        assertTrue(Young.hasYoung("moose", Calendar.dayOfYear(6, 1)));
        assertFalse(Young.hasYoung("moose", Calendar.dayOfYear(12, 1)));
        assertTrue(Young.hasYoung("snowshoe_hare", Calendar.dayOfYear(7, 20)));
        assertFalse(Young.hasYoung("snowshoe_hare", Calendar.dayOfYear(9, 20)));
        assertTrue(Young.hasYoung("porcupine", Calendar.dayOfYear(6, 10)));
        assertEquals(1, Young.TABLE.get("porcupine").litterMax());
    }

    @Test
    void stragglerOddsOverARun() {
        int five = 0, runs = 20000;
        java.util.SplittableRandom seeds = new java.util.SplittableRandom(11);
        for (int k = 0; k < runs; k++) {
            long seed = seeds.nextLong();
            boolean prev = false, any = false;
            for (int y = 1; y <= 5; y++) {
                boolean s = Straggler.roll(seed, y, prev, 80, 4).straggler();
                any |= s;
                prev = s;
            }
            if (any) five++;
        }
        double p = five / (double) runs;
        assertTrue(p > 0.28 && p < 0.38, "5-year run: " + p); // 34% in the blueprint
    }

    @Test
    void neverTwoInARowAndNoneWithoutDeer() {
        assertFalse(Straggler.roll(1, 2, true, 10, 4).straggler());
        assertFalse(Straggler.roll(1, 2, false, 10, 0).straggler());
        assertEquals(0.12, Straggler.chance(30), 1e-9);
    }
}

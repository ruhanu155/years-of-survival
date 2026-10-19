package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M1, stage 4 (Water and rock), over seeds 0–15 (every archetype four times) and the M0 golden seeds:
 * <ul>
 *   <li>every river flows downhill to the edge or a lake (the build spec's M1 test);</li>
 *   <li>the core river's entry and exit match the coarse net within 32 m;</li>
 *   <li>widths, depths and current follow the River geometry formulas, the main river is always a swim, flood
 *       lines are +2/+3.5 m on the main river and +0.6/+1.2 m on creeks, lakes are still;</li>
 *   <li>every lake is 1.5 m deep and 2,000 m² or more and drains by exactly one outlet.</li>
 * </ul>
 */
class M1WaterTest {
    @Test
    void everyRiverFlowsDownhillToTheEdgeOrALake() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = m1(seed);
            assertTrue(m.downhill().isEmpty(), "seed " + seed + ": " + m.downhill());
        }
    }

    @Test
    void coreRiverMeetsTheCoarseNetWithin32m() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = m1(seed);
            assertTrue(m.entryErrorM() <= 32 && m.exitErrorM() <= 32,
                    "seed " + seed + ": entry off by " + m.entryErrorM() + " m, exit by " + m.exitErrorM() + " m");
        }
    }

    @Test
    void widthsDepthsAndCurrentFollowTheFormulas() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = m1(seed);
            assertTrue(m.geometryRules().isEmpty(), "seed " + seed + ": " + m.geometryRules());
        }
    }

    @Test
    void lakesAreDeepEnoughWideEnoughAndHaveOneOutlet() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = m1(seed);
            assertTrue(m.lakeRules().isEmpty(), "seed " + seed + ": " + m.lakeRules());
        }
    }

    @Test
    void formulasMatchTheBlueprintExamples() {
        HydroParams.Water w = Fixture.PIPE.params().hydro().water();
        // "A creek draining 1 km² is 1.5 m wide; the main river at 64–278 km² is 12–25 m wide and 1.3–2.3 m deep."
        assertTrue(Math.abs(w.width(1e6) - 1.5) < 1e-9);
        assertTrue(Math.abs(w.width(64e6) - 12) < 1e-9);
        assertTrue(Math.abs(w.width(278e6) - 25) < 0.02);
        assertTrue(Math.abs(w.depth(12) - 1.26) < 1e-9 && Math.abs(w.depth(25) - 2.3) < 1e-9);
        assertTrue(w.width(1) == 0.8);
        assertTrue(w.speed(0) == 0.6 && w.speed(-1) == 0.3 && w.speed(1) == 3.0);
        assertTrue(w.wadeable(0.7, 1.1) && !w.wadeable(0.8, 0.5) && !w.wadeable(0.5, 1.2));
    }

    static M1Summary m1(long seed) {
        Fixture.Summary s = Fixture.of(seed);
        assertTrue(s.passed(), "seed " + seed + " failed: " + s.failures());
        return s.m1();
    }
}

package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

/**
 * M0: heights in the core stay within 120–900 m, the valley floor sits at 150 ±30 m, and no layer holds NaN.
 * Seeds 1–8 are the human-test debug maps and cover all four archetypes.
 */
class HeightRangeTest {
    @Test
    void coreHeightsFloorAndNoNaN() {
        EnumSet<Archetype> seen = EnumSet.noneOf(Archetype.class);
        for (long seed = 1; seed <= 8; seed++) {
            Fixture.Summary s = Fixture.of(seed);
            Pipeline.Stats st = s.stats();
            String at = "seed " + seed + " (" + s.archetype().path() + ")";
            seen.add(s.archetype());
            assertTrue(s.passed(), at + " failed: " + s.failures());
            assertEquals(0, s.landformNaN(), at + " landform NaN");
            assertEquals(0, s.coarseNaN(), at + " coarse NaN");
            assertEquals(0, st.nanCount(), at + " eroded NaN");
            assertTrue(st.minM() >= 120 && st.maxM() <= 900, at + " heights " + st.minM() + "–" + st.maxM());
            assertTrue(Math.abs(st.floorMedianM() - 150) <= 30, at + " floor median " + st.floorMedianM());
            assertTrue(st.floorMinM() >= 120 && st.floorMaxM() <= 180, at + " floor " + st.floorMinM() + "–" + st.floorMaxM());
            assertTrue(st.massBalance() <= 0.02, at + " mass balance " + st.massBalance());
            assertTrue(s.coarseMin() > 0 && s.coarseMax() < 1500, at + " coarse " + s.coarseMin() + "–" + s.coarseMax());
            System.out.printf("%s: %.1f-%.1f m, floor median %.1f m (%.1f-%.1f), hard faces %d kept %.1f%%%n",
                    at, st.minM(), st.maxM(), st.floorMedianM(), st.floorMinM(), st.floorMaxM(), st.hardFaces(),
                    100 * st.hardFacesKept());
        }
        assertEquals(EnumSet.allOf(Archetype.class), seen, "seeds 1–8 should show every archetype");
    }
}

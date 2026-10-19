package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M1, stage 5: no set piece sits in water or on a slope over 25°, footprints never overlap, spacing holds, each
 * piece's count is in its range, fit rules hold (a wreck on a gravel bar stands on bar); and every one of the 16
 * home-valley regions has a landmark visible from 2 km (the build spec's M1 tests).
 */
class M1PlacesTest {
    @Test
    void noSetPieceInWaterOrOnASlopeOver25Degrees() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = M1WaterTest.m1(seed);
            assertTrue(m.setPieceRules().isEmpty(), "seed " + seed + ": " + m.setPieceRules() + " in " + m.placements());
        }
    }

    @Test
    void everyRegionHasALandmarkVisibleFrom2km() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = M1WaterTest.m1(seed);
            assertTrue(m.landmarkRules().isEmpty(), "seed " + seed + ": " + m.landmarkRules());
            assertTrue(m.landmarksPassing() == 16, "seed " + seed + ": " + m.landmarksPassing() + " regions");
        }
    }
}

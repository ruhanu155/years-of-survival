package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * RECONCILE 236 (M5 lane TUNE): the slice spot is picked within 60 m of the start bank on every seed, by picking or by
 * making a pool spot, and neither moves a stage hash (the made spot is outside {@code spots}; M1HashTest holds the goldens).
 */
class FishSpotNearStartTest {
    @Test
    void sliceSpotWithinSixtyMetresOfTheStart() {
        StringBuilder bad = new StringBuilder();
        for (long seed : new long[] {0, 42, 3141592}) {
            Pipeline pipe = Fixture.PIPE;
            StartSite.Outcome o = StartSite.search(pipe, seed, StartSiteParams.load());
            ValleyWorld w = new ValleyWorld(pipe.params(), o.result(), o.cover(), pipe.setPieces(), o.placements(), true);
            SetPieces.Placement start = w.outpost();
            assertNotNull(w.sliceSpot, "seed " + seed + ": no slice spot");
            double d = Math.hypot(w.sliceSpot.x() - start.x(), w.sliceSpot.z() - start.z());
            if (d > ValleyWorld.SPOT_NEAR_START_M) bad.append(" seed ").append(seed).append(" ").append(Math.round(d)).append(" m");
            assertTrue(!w.spotsNear(w.sliceSpot.x(), w.sliceSpot.z(), 1).isEmpty(), "seed " + seed + ": spotsNear misses the slice spot");
        }
        assertTrue(bad.length() == 0, "slice spot farther than 60 m from the start:" + bad);
    }
}

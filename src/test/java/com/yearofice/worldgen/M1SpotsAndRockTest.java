package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M1: 40–120 fishing spots and at least 20 shelter candidates per map; spot quality inside its band and its
 * ±20%, starting stock q × capacity, every spot in water; the top 20 shelters flagged; 12–18 iron clusters,
 * every node {@code iron_outcrop} on a hard face above 250 m.
 */
class M1SpotsAndRockTest {
    @Test
    void fortyToOneHundredTwentyFishingSpots() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = M1WaterTest.m1(seed);
            assertTrue(m.majorSpots() >= 40 && m.majorSpots() <= 120, "seed " + seed + ": " + m.majorSpots() + " fishing spots");
            assertTrue(m.spotRules().isEmpty(), "seed " + seed + ": " + m.spotRules());
        }
    }

    @Test
    void atLeastTwentyShelterCandidatesAndTheTopTwentyFlagged() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = M1WaterTest.m1(seed);
            assertTrue(m.shelters() >= 20, "seed " + seed + ": " + m.shelters() + " shelter candidates");
            assertEquals(20, m.flagged(), "seed " + seed + " flagged shelters");
        }
    }

    @Test
    void twelveToEighteenIronClustersOnTheShell() {
        for (long seed : Fixture.M1_SEEDS) {
            M1Summary m = M1WaterTest.m1(seed);
            assertTrue(m.ironClusters() >= 12 && m.ironClusters() <= 18, "seed " + seed + ": " + m.ironClusters() + " iron clusters");
            assertTrue(m.ironRules().isEmpty(), "seed " + seed + ": " + m.ironRules());
        }
    }
}

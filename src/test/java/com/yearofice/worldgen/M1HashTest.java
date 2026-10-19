package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * M1: the full M1 layer hash matches across JVMs (the blueprint's "across browsers"), read as golden hashes
 * recorded on Linux x64 (OpenJDK 21) that Windows and macOS CI must reproduce unchanged; the same seed twice in
 * one JVM gives the same layers; and stage 4–5 parameters never reach stages 1–3. A change to these values is a
 * generator version bump, never a quiet edit.
 */
class M1HashTest {
    /** levelSeed, water (stage 4), places (stage 5), full M1 hash (stages 2–5). */
    static final long[][] GOLDEN = {
        {0L, 0xf9982e45L, 0x63225c7cL, 0x2ba6beaeL},
        {1L, 0x4e9e033aL, 0xa5a1dc70L, 0xd29025cbL},
        {2L, 0x9e05501fL, 0xd6353093L, 0x015bafb9L},
        {3L, 0x8c8bcc5fL, 0xc7d4d5daL, 0xf7abd81aL},
        {4L, 0x0050dd3dL, 0x2a0ad2d2L, 0x12a85df1L},
        {5L, 0x42d5d977L, 0xc3a0d827L, 0xf6c5cd69L},
        {6L, 0x11957d59L, 0x59397c2bL, 0x0e89ed53L},
        {7L, 0x7d8dab5dL, 0x84805e5aL, 0xb9c9c9a7L},
        {8L, 0x043a08b3L, 0x389d880eL, 0x6451efddL},
        {9L, 0x7522f623L, 0xa70a7a05L, 0xf634b82fL},
        {10L, 0x9592d42fL, 0x4c2847b0L, 0x665f73a4L},
        {11L, 0x4a28f3cdL, 0x3d0f6926L, 0x0f10c8c8L},
        {12L, 0x4d6ca3f3L, 0xa39b1919L, 0x833829ccL},
        {13L, 0xc98ae03eL, 0x491ef3aeL, 0xbf8ab1c7L},
        {14L, 0x351635ceL, 0x47924d99L, 0x7b1c9e99L},
        {15L, 0x2a85a781L, 0x17697778L, 0xcaa0af58L},
        {42L, 0x1f75cca6L, 0xfeff1458L, 0xde249b75L},
        {3141592L, 0x71d37a3cL, 0xf8528e7bL, 0xa5d3fb8dL},
    };

    @Test
    void goldenHashes() {
        for (long[] g : GOLDEN) {
            M1Summary m = M1WaterTest.m1(g[0]);
            String at = "seed " + g[0];
            assertEquals(Integer.toHexString((int) g[1]), Integer.toHexString(m.waterHash()), at + " water hash");
            assertEquals(Integer.toHexString((int) g[2]), Integer.toHexString(m.placesHash()), at + " places hash");
            assertEquals(Integer.toHexString((int) g[3]), Integer.toHexString(m.m1Hash()), at + " M1 hash");
        }
        assertEquals(Fixture.M1_SEEDS.length, GOLDEN.length, "every M1 seed has a golden row");
    }

    @Test
    void sameSeedSameLayersInOneJvm() {
        Pipeline.Result a = Fixture.PIPE.run(5L);
        Pipeline.Result b = Fixture.PIPE.run(5L);
        assertEquals(a.waterHash(), b.waterHash());
        assertEquals(a.placesHash(), b.placesHash());
        assertEquals(M1WaterTest.m1(5L).waterHash(), a.waterHash());
    }

    @Test
    void stageFourAndFiveParametersNeverReachStagesOneToThree() {
        long seed = 3L;
        Fixture.Summary base = Fixture.of(seed);
        WorldgenParams p = Fixture.PIPE.params();
        HydroParams h = p.hydro();
        HydroParams.Water w = h.water();
        HydroParams.Water wider = new HydroParams.Water(w.riverThresholdM2() * 2, w.widthMinM(), w.widthPerSqrtM2(),
                w.depthBaseM(), w.depthPerWidth(), w.speedMinMS(), w.speedMaxMS(), w.speedBaseMS(), w.speedPerSlope(),
                w.slopeWindowM(), w.wadeMaxDepthM(), w.wadeMaxSpeedMS(), w.bankDropM(), w.creekBankDropM(), w.bankMarginM(),
                w.bendScour(), w.lakeMinDepthM(), w.lakeMinAreaM2(), w.bogMinAreaM2(), w.bogMaxSlopeDeg(),
                w.bogMaxWaterDistM(), w.bogMinSedimentM(), w.weedShelfMaxDepthM(), w.weedShelfWidthM(), w.dropOffGradient(),
                w.dropOffMinDepthM(), w.cutBankMaxRadiusWidths(), w.cutBankMinBankM(), w.mainNormalM(), w.mainFloodM(),
                w.creekNormalM(), w.creekFloodM(), w.springsPerCreek(), w.springMinCreekM(), w.cutoffMinSeparationWidths(),
                w.cutoffMaxGapWidths(), w.oxbowPlugM(), w.braidMaxGradientPct(), w.braidNodeSpacingM(), w.edgePinM(),
                w.stampWidthM(), w.lakeMaxDepthM());
        HydroParams changed = new HydroParams(h.coarse(), wider, h.falls(), h.spots(), h.rock(), h.places(), h.landmarks());
        Pipeline other = new Pipeline(p.withHydro(changed), Archetype.loadAll(), Fixture.PIPE.setPieces());
        Pipeline.Result r = other.attempt(seed, base.attempt());
        assertEquals(base.landformHash(), r.landformHash(), "landform hash moved with a stage 4 parameter");
        assertEquals(base.heightHash(), r.heightHash(), "height hash moved with a stage 4 parameter");
        assertNotEquals(base.m1().waterHash(), r.waterHash(), "the creek threshold had no effect");
    }
}

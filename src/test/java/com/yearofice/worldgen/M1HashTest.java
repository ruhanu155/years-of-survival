package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * M1: the full M1 layer hash matches across JVMs (the blueprint's "across browsers"), read as golden hashes
 * recorded on Linux x64 (OpenJDK 21) that Windows and macOS CI must reproduce unchanged; the same seed twice in
 * one JVM gives the same layers; and stage 4–5 parameters never reach stages 1–3. A change to these values is a
 * generator version bump, never a quiet edit. Regenerated once for M2 (RECONCILE 167): the lake valley's noisy
 * shoreline and the 30 m lake depth cap moved the water hashes; stages 1–3 (the M0 hashes) did not move.
 */
class M1HashTest {
    /** levelSeed, water (stage 4), places (stage 5), full M1 hash (stages 2–5). */
    static final long[][] GOLDEN = {
        {0L, 0x7e59eb92L, 0x30c30bfcL, 0x48d88591L},
        {1L, 0xd65bb844L, 0x652d5563L, 0x9bd66589L},
        {2L, 0x089baf1cL, 0x5ab02ed9L, 0x28a84206L},
        {3L, 0xd07dae5dL, 0x2c9c8849L, 0x1997c55bL},
        {4L, 0xf0cb05a8L, 0x49af9478L, 0x4d2ac935L},
        {5L, 0xacc92088L, 0x293e27aaL, 0xf446e71eL},
        {6L, 0x148654f4L, 0x83f768beL, 0xf18b89f7L},
        {7L, 0x0adf4209L, 0x3bac9c1cL, 0xe9be9eb5L},
        {8L, 0x043a08b3L, 0x3fe54088L, 0x47dcff3fL},
        {9L, 0x975564ceL, 0x1673160eL, 0x27fd0277L},
        {10L, 0xd6e8dfa1L, 0xef535c20L, 0x45c5d3c4L},
        {11L, 0x4a28f3cdL, 0xfbf3333bL, 0xc48bb717L},
        {12L, 0x21de7dc0L, 0x157da85bL, 0xacc1924cL},
        {13L, 0xbcf193d6L, 0x73d9282bL, 0x17000bbcL},
        {14L, 0x0bee29f7L, 0x3abf4c6dL, 0x62830123L},
        {15L, 0x7f6f5342L, 0x4dc68806L, 0x64b2f367L},
        {42L, 0x308660dcL, 0xca9ccd69L, 0xd96b1626L},
        {3141592L, 0x88d7b428L, 0x7f2f85L, 0xe4ec83b8L},
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

package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Worldgen data: snake_case only, every key read, the four archetypes present with their shaping blocks. */
class GenJsonTest {
    @Test
    void shippedDataLoads() {
        WorldgenParams p = WorldgenParams.load();
        assertEquals(1024, p.grid().coreCells());
        assertEquals(256, p.grid().coarseCells());
        assertEquals(60_000, p.erosion().droplets());
        assertEquals(20, p.seed().maxAttempts());
        Archetype.Params[] all = Archetype.loadAll();
        assertEquals(4, all.length);
        assertNotNull(all[Archetype.LAKE_VALLEY.ordinal()].basin());
        assertNotNull(all[Archetype.CANYON.ordinal()].canyon());
        assertNotNull(all[Archetype.OXBOW_MARSH.ordinal()].oxbows());
        assertNotNull(all[Archetype.BRAIDED_FLATS.ordinal()].gravelBeltM());
        assertNull(all[Archetype.BRAIDED_FLATS.ordinal()].basin());
        assertEquals(2.0, all[Archetype.BRAIDED_FLATS.ordinal()].floorSedimentMultiplier());
        for (Archetype.Params a : all) assertTrue(a.archetype().id().startsWith("yearofice:"));
    }

    @Test
    void camelCaseIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GenJson.parse("{\"elevationM\": 3}", "t"));
        assertTrue(e.getMessage().contains("snake_case"));
    }

    @Test
    void unknownKeysAreRejected() {
        GenJson j = GenJson.parse("{\"floor_m\": 150, \"flor_m\": 1}", "t");
        assertEquals(150.0, j.num("floor_m"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, j::done);
        assertTrue(e.getMessage().contains("flor_m"));
    }

    @Test
    void noiseStaysInRange() {
        double lo = 0, hi = 0;
        for (int i = 0; i < 200_000; i++) {
            double x = i * 0.0731, y = i * 0.0377;
            double v = Noise.simplex(12345, x, y);
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
            double r = Noise.ridged(777, x, y, 4);
            assertTrue(r >= 0 && r <= 1);
        }
        assertTrue(lo > -1.1 && hi < 1.1, "simplex range " + lo + ".." + hi);
        assertTrue(lo < -0.6 && hi > 0.6, "simplex too flat " + lo + ".." + hi);
    }
}

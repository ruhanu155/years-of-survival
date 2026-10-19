package com.yearofice.sim.pack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Blueprint's load table and the A11 Strength multiplier. */
class LoadBandsTest {
    final LoadBands b = LoadBands.STANDARD;

    @Test
    void tableAtMultiplierOne() {
        // Strength 57.14... gives a multiplier of exactly 1.
        double s = (1 - 0.8) / 0.35 * 100;
        assertEquals(1.0, b.multiplier(s), 1e-12);
        assertEquals(LoadBands.Band.LIGHT, b.band(14.99, s));
        assertEquals(LoadBands.Band.HEAVY, b.band(15, s));
        assertEquals(LoadBands.Band.LOADED, b.band(30, s));
        assertEquals(LoadBands.Band.OVERLOADED, b.band(45, s));
        assertEquals(0.85, b.speed(LoadBands.Band.HEAVY));
        assertEquals(1.5, b.kcal(LoadBands.Band.LOADED));
        assertEquals(false, LoadBands.Band.LOADED.canRun());
    }

    @Test
    void strengthScales() {
        assertEquals(0.8, b.multiplier(0), 1e-12);
        assertEquals(1.15, b.multiplier(100), 1e-12);
        assertEquals(1.15, b.multiplier(140), 1e-12);
        assertEquals(12, b.from(LoadBands.Band.HEAVY, 0), 1e-9);
        assertEquals(51.75, b.from(LoadBands.Band.OVERLOADED, 100), 1e-9);
        assertEquals(LoadBands.Band.HEAVY, b.band(13, 0));
        assertEquals(LoadBands.Band.LIGHT, b.band(13, 100));
    }
}

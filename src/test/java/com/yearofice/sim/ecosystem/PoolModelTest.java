package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.time.Calendar;
import org.junit.jupiter.api.Test;

class PoolModelTest {
    @Test
    void reachFollowsWindAndRain() {
        double calm = PoolModel.reachM(2000, 1, 0, 0, 1), windy = PoolModel.reachM(2000, 1, 6, 0, 1);
        assertEquals(1000, calm, 1e-9);
        assertTrue(windy > calm);
        assertTrue(PoolModel.reachM(2000, 1, 3, 2, 1) < PoolModel.reachM(2000, 1, 3, 0, 1) / 2);
        assertTrue(PoolModel.reachM(2000, 0.25, 3, 0, 1) < PoolModel.reachM(2000, 1, 3, 0, 1));
    }

    @Test
    void coneRunsDownTheWind() {
        // wind blows toward +x (yaw -90); a bear 800 m east is downwind, 800 m west is upwind, 800 m north is across.
        double toward = PoolModel.bearing(0, 0, 100, 0);
        assertTrue(PoolModel.inReach(0, 0, 1500, toward, 800, 0));
        assertFalse(PoolModel.inReach(0, 0, 1500, toward, -800, 0));
        assertFalse(PoolModel.inReach(0, 0, 1500, toward, 0, -800));
        assertTrue(PoolModel.inReach(0, 0, 1500, toward, -100, 0), "the bubble smells the same everywhere");
        assertFalse(PoolModel.inReach(0, 0, 1500, toward, 1600, 0), "beyond the reach");
    }

    @Test
    void bearsAreKeenBeforeDenningAndIgnoreItInTheDen() {
        int sep = Calendar.dayOfYear(9, 20), jul = Calendar.dayOfYear(7, 10), dec = Calendar.dayOfYear(12, 1);
        assertTrue(PoolModel.season("yearofice:grizzly", sep) > PoolModel.season("yearofice:grizzly", jul));
        assertEquals(0, PoolModel.season("yearofice:black_bear", dec));
        assertTrue(PoolModel.season("yearofice:wolf", dec) > 0);
        var k = PoolModel.kind("yearofice:grizzly");
        assertTrue(PoolModel.interestPerHour(k, 0.8, 2, 1) > PoolModel.interestPerHour(k, 0.2, 2, 1));
        assertEquals(0, PoolModel.interestPerHour(k, 0.8, 0, 1));
    }

    @Test
    void walkStopsShortOfTheSourceAndEntryFindsTheCircle() {
        var a = new PoolModel.Indiv();
        a.x = 1000;
        double d = PoolModel.walk(a, 0, 0, 2, 100_000);
        assertEquals(1000 - PoolModel.ARRIVE_M, d, 1e-9);
        assertEquals(PoolModel.ARRIVE_M, a.x, 1e-9);
        double[] e = PoolModel.entry(1000, 0, -1000, 0, 0, 0, 128);
        assertNotNull(e);
        assertEquals(128, e[0], 1e-6);
        assertNull(PoolModel.entry(1000, 500, -1000, 500, 0, 0, 128));
        assertEquals(10, PoolModel.entry(10, 0, 500, 0, 0, 0, 128)[0], 1e-9, "starting inside enters at the start");
    }
}

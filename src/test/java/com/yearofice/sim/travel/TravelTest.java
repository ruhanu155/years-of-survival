package com.yearofice.sim.travel;

import static com.yearofice.sim.travel.Travel.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TravelTest {
    @Test
    void skatesFlyOnIceAndCrawlOffIt() {
        assertTrue(speed(Gear.SKATES, Surface.ICE, 0, 0) > 1.5 * speed(Gear.NONE, Surface.ICE, 0, 0));
        assertTrue(speed(Gear.SKATES, Surface.SNOW, 5, 0) < 1);
        assertTrue(cracklesUnder(10));
        assertFalse(cracklesUnder(20));
    }

    @Test
    void skisBeatSnowshoesDownhillAndSkinsClimb() {
        double ski = speed(Gear.SKIS, Surface.SNOW, 40, -0.2), shoe = speed(Gear.SNOWSHOES, Surface.SNOW, 40, -0.2);
        assertTrue(ski > shoe);
        assertTrue(speed(Gear.SKIS_SKINS, Surface.SNOW, 40, 0.2) > speed(Gear.SKIS, Surface.SNOW, 40, 0.2));
        assertTrue(speed(Gear.SNOWSHOES, Surface.SNOW, 60, 0) > speed(Gear.NONE, Surface.SNOW, 60, 0));
    }

    @Test
    void brokenTrailSetsOvernightInTheFrostAndFreshSnowBuriesIt() {
        int cold = 0;
        Trail t = Trail.BROKEN;
        for (int h = 0; h < 8; h++) { cold = coldHours(cold, -15); t = nextTrail(t, cold, 0, false); }
        assertEquals(Trail.SET, t);
        assertEquals(Trail.SLED_LANE, nextTrail(t, cold, 0, true));
        assertEquals(Trail.NONE, nextTrail(t, cold, 5, false));
        assertTrue(trailBonus(Trail.SET) > trailBonus(Trail.BROKEN));
        assertEquals(0, coldHours(5, 2)); // a thaw resets
    }

    @Test
    void sealedSnowRoomGoesStaleAndAVentFixesIt() {
        double a = 1;
        for (int i = 0; i < 12 * 60; i++) a = air(a, 1, 2, 1, false, 8);
        assertTrue(stale(a));
        for (int i = 0; i < 60; i++) a = air(a, 1, 2, 1, true, 8);
        assertFalse(stale(a));
    }
}

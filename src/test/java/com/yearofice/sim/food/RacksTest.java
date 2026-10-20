package com.yearofice.sim.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RacksTest {
    static final RackTuning T = RackTuning.standard();

    @Test
    void dryTwoDays() {
        Racks.Drying d = Racks.Drying.start();
        for (int i = 0; i < 47; i++) d = d.hour(false, T);
        assertFalse(d.done(T));
        assertTrue(d.hour(false, T).done(T));
    }

    @Test
    void oneRainHourCostsSix() {
        Racks.Drying d = Racks.Drying.start();
        for (int i = 0; i < 10; i++) d = d.hour(i == 9, T);
        int hours = 10;
        while (!d.done(T)) { d = d.hour(false, T); hours++; }
        assertEquals(54, hours);
    }

    @Test
    void fullRainDayRestarts() {
        Racks.Drying d = new Racks.Drying(40, 0);
        for (int i = 0; i < 24; i++) d = d.hour(true, T);
        assertEquals(0, d.progressH());
    }

    @Test
    void smokeNeedsFireAndCalm() {
        Racks.Smoking s = Racks.Smoking.start(1L, 0, 0, 0, 0, T);
        assertTrue(s.neededH() >= 8 && s.neededH() <= 12);
        assertEquals(0, s.hour(false, 0, T).progressH());
        assertEquals(0, s.hour(true, 9, T).progressH());
        assertEquals(1, s.hour(true, 8, T).progressH());
    }

    @Test
    void fliesAbove15() {
        assertTrue(Racks.fliesLayEggs(16, false));
        assertFalse(Racks.fliesLayEggs(16, true));
        assertFalse(Racks.fliesLayEggs(10, false));
    }
}

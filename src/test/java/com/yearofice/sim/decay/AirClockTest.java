package com.yearofice.sim.decay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The air clock: D(t) = Σ r(T(hour)), linear inside an hour, the same whatever order it is read in. */
class AirClockTest {
    private static final ColdCurve C = ColdCurve.STANDARD;

    @Test
    void constantFiveIsGameHours() {
        AirClock c = new AirClock(h -> 5.0, C::rate);
        assertEquals(0, c.dAt(0), 0);
        assertEquals(1.0, c.dAt(60), 1e-12);
        assertEquals(24.0 * 9, c.dAt(9 * 1440), 1e-9);
        assertEquals(0.5, c.dAt(30), 1e-12);
        assertEquals(-1.0, c.dAt(-60), 1e-12);
    }

    @Test
    void eachHourAtItsOwnRate() {
        // 5 °C at even hours, 15 °C at odd ones: rates 1 and 3.
        AirClock c = new AirClock(h -> h % 2 == 0 ? 5.0 : 15.0, C::rate);
        assertEquals(1.0, c.dAt(60), 1e-12);
        assertEquals(1.0 + 1.5, c.dAt(90), 1e-12);
        assertEquals(4.0, c.dAt(120), 1e-12);
        assertEquals(48.0, c.dAt(1440), 1e-9);
        assertEquals(3.0, c.rateAt(61), 0);
        assertEquals(15.0, c.airAt(61), 0);
    }

    @Test
    void frozenHoursAddNothing() {
        AirClock c = new AirClock(h -> -10.0, C::rate);
        assertEquals(0, c.dAt(30 * 1440), 0);
    }

    @Test
    void readOrderDoesNotMatter() {
        AirClock a = new AirClock(h -> 5 + 10 * Math.sin(h / 7.0), C::rate);
        AirClock b = new AirClock(h -> 5 + 10 * Math.sin(h / 7.0), C::rate);
        double far = a.dAt(400 * 1440 + 17);
        for (int m = 0; m <= 400 * 1440; m += 997) b.dAt(m);
        assertEquals(far, b.dAt(400 * 1440 + 17), 0);
        assertTrue(a.hoursSummed() >= 400 * 24);
        double prev = -1;
        for (int m = 0; m < 3 * 1440; m += 13) {
            double d = a.dAt(m);
            assertTrue(d >= prev, "monotone at " + m);
            prev = d;
        }
    }
}

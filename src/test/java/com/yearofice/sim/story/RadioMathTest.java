package com.yearofice.sim.story;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RadioMathTest {
    @Test void summerBeatsWinter() {
        assertEquals(10, RadioMath.dailyGainPct(7, true, 1), 1e-9);
        assertEquals(30, RadioMath.dailyGainPct(7, true, 1) * 3, 1e-9);
        assertEquals(1, RadioMath.dailyGainPct(1, true, 1), 1e-9);
        assertEquals(3, RadioMath.dailyGainPct(7, true, 3) / 10);
    }

    @Test void flyingNeedsCalmSkyAndWind() {
        assertTrue(RadioMath.canFly(true, 7.9));
        assertFalse(RadioMath.canFly(true, 8));
        assertFalse(RadioMath.canFly(false, 0));
    }

    @Test void outlookIsRoughlySeventyPercentRight() {
        int right = 0;
        for (long d = 0; d < 1000; d++) if (RadioMath.outlookRight(d, 4)) right++;
        assertTrue(right > 640 && right < 760, "right " + right);
        assertTrue(RadioMath.outlookRight(5, 1));
    }
}

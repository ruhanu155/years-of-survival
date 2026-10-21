package com.yearofice.sim.travel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CanoeMathTest {
    @Test
    void calmWaterNeverCapsizes() {
        assertEquals(0, CanoeMath.capsizeChancePerS(3, 11, true, false, 100, 250, 0.05));
    }

    @Test
    void windRapidsAndOverloadAdd() {
        double wind = CanoeMath.capsizeChancePerS(16, 11, true, false, 100, 250, 0.05);
        double both = CanoeMath.capsizeChancePerS(16, 11, true, true, 100, 250, 0.05);
        double all = CanoeMath.capsizeChancePerS(16, 11, true, true, 300, 250, 0.05);
        assertTrue(wind > 0 && both > wind && all > both);
        assertEquals(0, CanoeMath.capsizeChancePerS(16, 11, false, false, 100, 250, 0.05), "sheltered from the wind");
    }
}

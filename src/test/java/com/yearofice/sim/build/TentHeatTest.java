package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TentHeatTest {
    @Test
    void stepIsStepLengthIndependent() {
        double a = TentHeat.step(-30, -30, 900, 21, 4, 1.0);
        double b = -30;
        for (int i = 0; i < 60; i++) b = TentHeat.step(b, -30, 900, 21, 4, 1.0 / 60);
        assertEquals(a, b, 1e-9);
    }

    @Test
    void docNumbers() {
        assertTrue(TentHeat.steadyC(-30, 1.0, 0.9, 21) >= 12);
        double dead = TentHeat.step(TentHeat.steadyC(-30, 1.0, 0.9, 21), -30, 0, 21, 4, 0.5);
        assertTrue(dead < -20);
        assertEquals(0, TentHeat.sparkHoleChance(0.04, false, true));
        assertEquals(0.01, TentHeat.sparkHoleChance(0.04, true, false), 1e-12);
    }
}

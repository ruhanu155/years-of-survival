package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SmeltTest {
    @Test
    void fiveNodulesAndTwentyKgMakeOneBloom() {
        assertEquals(1, Smelt.blooms(5 * Smelt.NODULE, 20));
        assertEquals(0, Smelt.blooms(4 * Smelt.NODULE, 99));
        assertEquals(0, Smelt.blooms(999, 19));
    }

    @Test
    void fortyUnitsOfBlackSandAreOneNodule() {
        assertEquals(Smelt.NODULE, 40 * Smelt.SAND_UNIT);
        assertEquals(1, Smelt.blooms(200 * Smelt.SAND_UNIT, 20));
    }

    @Test
    void practiceAddsHalfAKilo() {
        assertEquals(4, Smelt.barUnits(false));
        assertEquals(5, Smelt.barUnits(true));
    }
}

package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OutpostDecayTest {
    @Test
    void chanceGrowsWithAgeAndRate() {
        assertEquals(0, OutpostDecay.lossChance(5, 0), 1e-9);
        assertTrue(OutpostDecay.lossChance(5, 40) > OutpostDecay.lossChance(5, 10));
        assertTrue(OutpostDecay.lossChance(8, 10) > OutpostDecay.lossChance(2, 10));
        assertEquals(1 - Math.exp(-0.5), OutpostDecay.lossChance(5, 10), 1e-9);
    }

    @Test
    void rollIsStableAndSpreads() {
        assertEquals(OutpostDecay.roll(7, 1, 2, 3), OutpostDecay.roll(7, 1, 2, 3), 0);
        int lost = 0;
        for (int i = 0; i < 4000; i++) if (OutpostDecay.lost(11, i, i % 7, i / 3, 5, 10)) lost++;
        assertEquals(0.393, lost / 4000.0, 0.03);
    }
}

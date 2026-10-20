package com.yearofice.sim.crop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CropModelTest {
    static final double BARLEY_G = 0.6;

    static int sow(int seeds, double soilC, long seed) {
        Random r = new Random(seed);
        double p = CropModel.germination(BARLEY_G, soilC, 1, CropModel.Depth.RIGHT, 0, false, 1.0);
        int up = 0;
        for (int i = 0; i < seeds; i++) if (r.nextDouble() < p) up++;
        return up;
    }

    /** Packet barley (g 0.6) sown in the window on warm soil: over 1000 seeds, 50-65% come up; under 5 C, under 25%. */
    @Test
    void barley_window() {
        for (long seed = 1; seed <= 5; seed++) {
            int up = sow(1000, 11.0, seed);
            assertTrue(up >= 500 && up <= 650, "warm soil: " + up + " of 1000 (seed " + seed + ")");
            int cold = sow(1000, 3.0, seed);
            assertTrue(cold < 250, "cold soil: " + cold + " of 1000 (seed " + seed + ")");
        }
        assertEquals(0.6, CropModel.germination(BARLEY_G, 12, 1, CropModel.Depth.RIGHT, 0, false, 1.0), 1e-9);
        assertEquals(0.18, CropModel.germination(BARLEY_G, 4, 1, CropModel.Depth.RIGHT, 0, false, 1.0), 1e-9);
        assertEquals(0.42, CropModel.germination(BARLEY_G, 7, 1, CropModel.Depth.RIGHT, 0, false, 1.0), 1e-9);
    }

    @Test
    void factorsMultiply() {
        assertEquals(0.6 * 0.6 * 0.7, CropModel.germination(0.6, 12, 0, CropModel.Depth.SHALLOW, 0, false, 1.0), 1e-9);
        assertEquals(0.6 * 0.8, CropModel.germination(0.6, 12, 1, CropModel.Depth.RIGHT, 2, false, 1.0), 1e-9);
        assertEquals(0.95, CropModel.germination(0.85, 12, 1, CropModel.Depth.RIGHT, 0, false, 1.3), 1e-9);
    }

    @Test
    void growthAndStages() {
        assertEquals(0, CropModel.growthUnits(4, 6, 1.0), 1e-9);
        assertEquals(10 * 1.2, CropModel.growthUnits(15, 6, 1.0), 1e-9);
        assertEquals(10, CropModel.growthUnits(15, 8, 1.0), 1e-9);
        assertEquals(0, CropModel.stage(10, 40, 950));
        assertEquals(1, CropModel.stage(60, 40, 950));
        assertEquals(4, CropModel.stage(950, 40, 950));
        assertEquals(0.65, CropModel.standYield(0.3, 0.6), 1e-9);
    }

    @Test
    void headFrost() {
        assertEquals(0.2, CropModel.headFrostMult(-2, 800, 700, 950, -2, 0.2), 1e-9);
        assertEquals(1.0, CropModel.headFrostMult(-2, 300, 700, 950, -2, 0.2), 1e-9);
    }
}

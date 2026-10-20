package com.yearofice.sim.food;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.worldgen.PlacerParams;
import java.util.List;
import org.junit.jupiter.api.Test;

/** M5 lane D2 pure rules: dish sums, dried weight, deadfall odds, black sand rates and placer reaches. */
class DishAndPanTest {
    @Test
    void stewWithBonesTimesOnePointFifteen() {
        Nutrients meat = new Nutrients(1150, 250, 17, 0, 0), hips = new Nutrients(160, 1.6, 0.3, 200, 0);
        Nutrients dish = Dishes.cook(List.of(meat, hips), Dishes.Method.STEW_BONES);
        assertEquals(1310 * 1.15, dish.kcal(), 1e-9);
        assertEquals(100, dish.vitCMg(), 1e-9);
        assertEquals(10, dish.cDays(), 1e-9);
    }

    @Test
    void driedKeepsEveryCalorie() {
        assertEquals(400, Dishes.driedGrams(1000), 1e-9);
        assertEquals(2875, Dishes.driedKcalPerKg(1150), 1e-9);
    }

    @Test
    void deadfallOdds() {
        assertEquals(0, Deadfall.pCatch(0.06, 1, 0), 0);
        assertEquals(1 - Math.pow(0.94, 3), Deadfall.pCatch(0.06, 1, 3), 1e-12);
        int kills = 0;
        DeadfallTuning t = DeadfallTuning.standard();
        for (int i = 0; i < 20_000; i++) if (Deadfall.roll(1, 2, 3, 4, i, 5, 1, false, t.big()) == Deadfall.Outcome.KILL) kills++;
        assertEquals(Deadfall.pCatch(0.06, 1, 5) * 0.9, kills / 20_000.0, 0.01);
    }

    @Test
    void blackSandRates() {
        BlackSand s = BlackSand.standard();
        assertEquals(0.3, s.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 7), 1e-9);
        assertEquals(0.1, s.kgPerHour(BlackSand.Tool.GOLD_PAN, false, 7), 1e-9);
        assertEquals(0.15, s.kgPerHour(BlackSand.Tool.BIRCH_PAN, true, 7), 1e-9);
        assertEquals(0.9, s.kgPerHour(BlackSand.Tool.SLUICE, true, 7), 1e-9);
        assertEquals(0, s.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 12), 0);
        assertEquals(40, BlackSand.KG_PER_NODULE / 0.1, 1e-9);
    }

    @Test
    void placerReachesStayOutOfTheStartRadius() {
        Placer p = PlacerParams.load();
        int hits = 0, near = 0;
        for (int x = -1500; x <= 1500; x += 16) {
            for (int z = -1500; z <= 1500; z += 16) {
                if (p.reachAt(5, x, z, 0, 0)) {
                    hits++;
                    if (Math.hypot(x, z) < 400) near++;
                }
            }
        }
        assertTrue(hits > 0);
        assertEquals(0, near);
        assertFalse(p.reachAt(5, 10, 10, 0, 0));
    }
}

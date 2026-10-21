package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.build.CacheLegs;
import com.yearofice.sim.build.Cellars;
import com.yearofice.sim.build.Dugout;
import com.yearofice.sim.env.FireSetting;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class StoveTest {
    static final Stove.Kind TIN = new Stove.Kind("tin_stove", 90, 0.6, 3, 1.5, 4);

    @Test
    void draftRisesWithPipeAndCaps() {
        double prev = -1;
        for (double h : new double[] {0, 0.5, 1.5, 3, 4, 6, 20}) {
            double d = Stove.draft(TIN, h);
            assertTrue(d >= prev, "monotonic at " + h);
            prev = d;
        }
        assertEquals(1.0, Stove.draft(TIN, 4), 1e-9);
        assertEquals(1.3, Stove.draft(TIN, 20), 1e-9);
        assertEquals(Stove.UNFLUED_DRAFT, Stove.draft(TIN, 0), 1e-9);
    }

    @Test
    void wetWoodIsWorse() {
        assertEquals(1, Stove.creosotePerArmload(Wood.BIRCH, 15), 1e-9);
        assertEquals(2, Stove.creosotePerArmload(Wood.SPRUCE, 15), 1e-9);
        assertEquals(5, Stove.creosotePerArmload(Wood.SPRUCE, 60), 1e-9);
        assertTrue(Stove.creosotePerArmload(Wood.SPRUCE, 35) > Stove.creosotePerArmload(Wood.SPRUCE, 25));
    }

    @Test
    void creosoteBuildsAndChimneyFireNeedsHot() {
        StoveBurn b = new StoveBurn(TIN);
        b.load(Wood.SPRUCE, 60, 2.5);
        assertFalse(b.light(), "all-green box does not catch cold");
        b.load(Wood.BIRCH, 10, 0.2);
        assertTrue(b.light());
        b.step(5000, 1.0, true, new SplittableRandom(1));
        assertTrue(b.creosote() > 8);
        StoveBurn hot = new StoveBurn(TIN);
        hot.setting(FireSetting.HOT);
        hot.creosote(95);
        int fires = 0;
        for (int i = 0; i < 300 && fires == 0; i++) {
            hot.load(Wood.BIRCH, 10, 3);
            hot.light();
            if (hot.step(600, 1.0, true, new SplittableRandom(i)).chimneyFire()) fires++;
            hot.creosote(95);
        }
        assertTrue(fires > 0, "a Hot fire over 70 creosote eventually lights the chimney");
        StoveBurn low = new StoveBurn(TIN);
        low.setting(FireSetting.LOW);
        low.creosote(95);
        low.load(Wood.BIRCH, 10, 3);
        low.light();
        assertFalse(low.step(600, 1.0, true, new SplittableRandom(3)).chimneyFire());
    }

    @Test
    void cellarsAndLegs() {
        double banked = Cellars.rootC(2, -30, 1, 3), bare = Cellars.rootC(2, -30, 0, 0);
        assertTrue(banked > bare && banked > -2 && banked < 3);
        assertTrue(Cellars.permafrostC(-2, 20, 1) <= 0);
        assertTrue(CacheLegs.proof("grizzly_bear", CacheLegs.Leg.BARKED, 3.5));
        assertFalse(CacheLegs.proof("grizzly_bear", CacheLegs.Leg.BARKED, 3.4));
        assertTrue(CacheLegs.proof("marten", CacheLegs.Leg.BANDED, 1));
        assertEquals(1.0, CacheLegs.reach("black_bear", CacheLegs.Leg.BANDED, 6), 1e-9);
        assertEquals("hearth_without_flue", Dugout.verdict(Dugout.Kind.HILLSIDE, 4, 2, 6, 2, true, true, false));
        assertEquals("too_big", Dugout.verdict(Dugout.Kind.HILLSIDE, 5, 2, 6, 2, true, false, false));
    }
}

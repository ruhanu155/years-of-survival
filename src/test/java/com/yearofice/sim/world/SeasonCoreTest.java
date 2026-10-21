package com.yearofice.sim.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.sim.world.SnowPack.SnowClass;
import org.junit.jupiter.api.Test;

class SeasonCoreTest {
    static final SeasonPreset P = SeasonPreset.STANDARD;

    static SeasonCore run(int days) {
        SeasonCore c = new SeasonCore();
        for (int d = 0; d < days; d++) {
            int doy = d % 365;
            c.step(d, doy, 4 + 20 * Math.cos(2 * Math.PI * (doy - 80) / 365.0), d % 4 == 0 ? 5 : 0, d % 9 == 0 ? 9 : 3, P.snow, P.ice);
        }
        return c;
    }

    @Test
    void encodeDecodeRoundTripsAndReplayIsDeterministic() {
        SeasonCore a = run(300), b = run(300);
        assertArrayEquals(a.encode(), b.encode(), 0);
        SeasonCore c = new SeasonCore();
        c.decode(a.encode());
        assertArrayEquals(a.encode(), c.encode(), 0);
    }

    @Test
    void snowClassesOrderLikeTheBlueprint() {
        SeasonCore c = new SeasonCore();
        for (int d = 200; d < 260; d++) c.step(d, d, -15, 4, 8, P.snow, P.ice);
        double open = c.snow(SnowClass.OPEN).depthCm();
        assertTrue(c.snow(SnowClass.UNDER_SPRUCE).depthCm() < open * 0.75, "under spruce is shallower");
        assertTrue(c.snow(SnowClass.LEE_DRIFT).depthCm() > open * 1.4, "the lee drifts");
        for (int d = 310; d < 322; d++) c.step(d, d, 4, 0, 3, P.snow, P.ice);
        assertTrue(c.snow(SnowClass.SOUTH_SLOPE).depthCm() < c.snow(SnowClass.OPEN).depthCm(), "south slopes melt first");
    }

    @Test
    void crustFormsAfterAWetDayThenAFreezeAndFreshSnowBuriesIt() {
        SnowPack.State s = new SnowPack.State();
        SnowPack.step(s, SnowClass.OPEN, P.snow, -10, 20, 2, 300);
        SnowPack.step(s, SnowClass.OPEN, P.snow, 2, 0, 2, 310);
        assertTrue(s.wet);
        SnowPack.step(s, SnowClass.OPEN, P.snow, -10, 0, 2, 311);
        assertTrue(s.crust, "re-freeze after a thaw makes a crust");
        SnowPack.step(s, SnowClass.OPEN, P.snow, -10, 10, 2, 312);
        assertFalse(s.crust, "10 mm of fresh snow buries the crust");
    }

    @Test
    void iceHoldsByThicknessAndRotHalvesIt() {
        IceTuning t = P.ice;
        IceModel.State s = new IceModel.State();
        s.cm = 12;
        assertEquals(IceModel.Stage.SAFE, IceModel.stage(s, t));
        assertTrue(IceModel.holds(IceModel.effectiveCm(s, t), 90, t));
        assertFalse(IceModel.holds(IceModel.effectiveCm(s, t), 250, t), "a loaded sled wants 20 cm");
        s.rotten = true;
        assertEquals(IceModel.Stage.ROTTEN, IceModel.stage(s, t));
        assertFalse(IceModel.holds(IceModel.effectiveCm(s, t), 90, t), "rotten 12 cm counts as 6");
        assertEquals(IceModel.Stage.THIN, IceModel.Stage.SAFE.weaker());
        assertEquals(BodyClass.RIVER, BodyClass.values()[0]);
    }
}

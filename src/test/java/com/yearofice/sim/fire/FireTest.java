package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.HeatIndex;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** The fire state machine against Blueprint › Fire, water and light and Building a campfire. */
class FireTest {
    static final FireTuning T = FireTuning.standard();
    static final FireWeather CALM = FireWeather.CALM;

    /** Bark tinder, two handfuls of kindling and {@code armloads} of dead standing spruce, in order. */
    static Fire laid(double armloads) {
        Fire f = new Fire();
        assertTrue(f.add(LayPart.TINDER, null, 10, 1, T).ok());
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        for (int i = 0; i < Math.round(armloads * 4); i++) f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 0.25, T);
        return f;
    }

    static Fire lit(double armloads) {
        Fire f = laid(armloads);
        assertTrue(f.strikeMatch(0.0, 0, CALM, false, T).ok());
        f.puff(T);
        f.puff(T);
        assertEquals(FireState.TINDER, f.state());
        return f;
    }

    /** Steps until the state is {@code s}; returns the minutes it took, or -1 past {@code max}. */
    static int minutesUntil(Fire f, FireState s, int max, FireWeather w) {
        for (int m = 1; m <= max; m++) {
            f.stepMinute(w, T);
            if (f.state() == s) return m;
        }
        return -1;
    }

    @Test
    void sliceFireFromParts() {
        // Ready for development › M2.5 test 1: bark, kindling and 2 armloads reach a fire in under 10 game minutes
        // and coals in 60 ±10.
        Fire f = lit(2);
        int toFire = minutesUntil(f, FireState.FIRE, 30, CALM);
        assertTrue(toFire > 0 && toFire < 10, "fire after " + toFire + " min");
        assertEquals(FireSetting.STEADY, f.setting());
        int toCoals = toFire + minutesUntil(f, FireState.COALS, 200, CALM);
        assertTrue(toCoals >= 50 && toCoals <= 70, "coals after " + toCoals + " min");
        assertEquals(2.0, f.burnedArmloads(), 1e-6);
        assertEquals(60, minutesUntil(f, FireState.OUT, 200, CALM) - 1, "coals last 1 game hour");
    }

    @Test
    void layOutOfOrderDoesNotLight() {
        Fire f = new Fire();
        f.add(LayPart.TINDER, null, 10, 1, T);
        Fire.Result r = f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 1, T);
        assertTrue(r.ok());
        assertNotNull(r.line(), "the player is told fuel smothers bare tinder");
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        assertFalse(f.layInOrder());
        assertFalse(f.strikeMatch(0.0, 0, CALM, false, T).ok());
        assertEquals(FireState.OUT, f.state());
        // Kindling first, with no tinder under it, is wrong too.
        Fire g = new Fire();
        g.add(LayPart.KINDLING, null, 15, 1, T);
        g.add(LayPart.TINDER, null, 10, 1, T);
        assertFalse(g.layInOrder());
        assertFalse(g.emberCaught(T).ok());
        // Taking the parts back off and relaying fixes it.
        while (f.takeBack() != null) { }
        assertTrue(f.lay().isEmpty());
        assertEquals(0, f.armloads(), 1e-9);
        f.add(LayPart.TINDER, null, 10, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 1, T);
        assertTrue(f.layInOrder());
        assertTrue(f.strikeMatch(0.0, 0, CALM, false, T).ok());
    }

    @Test
    void tinderAndKindlingRules() {
        // Wet tinder (dryness under 60) will not catch.
        Fire wet = new Fire();
        wet.add(LayPart.TINDER, null, 55, 1, T);
        assertFalse(wet.strikeMatch(0.0, 0, CALM, false, T).ok());
        // A tinder flame with no kindling dies after its 2 minutes.
        Fire bare = new Fire();
        bare.add(LayPart.TINDER, null, 10, 1, T);
        bare.emberCaught(T);
        bare.stepMinute(CALM, T);
        bare.stepMinute(CALM, T);
        assertEquals(FireState.TINDER, bare.state());
        Fire.Change c = bare.stepMinute(CALM, T);
        assertEquals(FireState.OUT, bare.state());
        assertTrue(c.line().contains("no kindling"), c.line());
        // A small fire with no armload burns its kindling 20 minutes and goes out: no coals without an armload.
        Fire small = laid(0);
        small.emberCaught(T);
        assertEquals(1, minutesUntil(small, FireState.SMALL, 5, CALM));
        assertEquals(21, minutesUntil(small, FireState.OUT, 40, CALM));
    }

    @Test
    void emberDiesAfterItsLife() {
        Fire f = laid(1);
        f.strikeMatch(0.0, 0, CALM, false, T);
        assertEquals(FireState.EMBER, f.state());
        for (int i = 0; i < 60 * 20 - 1; i++) assertNull(f.tick());
        assertNotNull(f.tick());
        assertEquals(FireState.OUT, f.state());
        assertTrue(f.hasTinder(), "a dead ember leaves the tinder for another try");
        // Wind over 6 m/s halves it unless sheltered.
        assertEquals(30 * 20, Fire.emberLifeTicks(8, false, T));
        assertEquals(60 * 20, Fire.emberLifeTicks(8, true, T));
    }

    @Test
    void matchesLight95InCalmAnd50InWind() {
        SplittableRandom rng = new SplittableRandom(3141592);
        int calm = 0, windy = 0;
        for (int i = 0; i < 1000; i++) {
            if (laid(1).strikeMatch(rng.nextDouble(), 0, CALM, false, T).ok()) calm++;
            if (laid(1).strikeMatch(rng.nextDouble(), 0, CALM.withWind(7), false, T).ok()) windy++;
        }
        assertTrue(calm >= 930 && calm <= 970, "calm " + calm);
        assertTrue(windy >= 450 && windy <= 550, "wind " + windy);
        assertEquals(0, Fire.matchChance(0, 25, T), "a match above 20 wet fails every time");
    }

    @Test
    void woodTableAndSettings() {
        // One armload of seasoned birch on Low lasts 40 × 1.4 ÷ 0.4 = 140 minutes in the plain ring.
        Fire f = new Fire();
        f.add(LayPart.FUEL, Wood.BIRCH, 15, 1, T);
        f.force(FireState.FIRE, FireSetting.LOW, T);
        assertEquals(140, f.minutesLeft(T), 1e-6);
        f.setSetting(FireSetting.HOT);
        assertEquals(140 * 0.4 / 3, f.minutesLeft(T), 1e-6);
        // Heat: Hot +25 × birch 1.1 is clamped to the Blueprint's +25; green wood gives half.
        assertEquals(25, f.heatC(), 1e-9);
        f.setSetting(FireSetting.STEADY);
        assertEquals(18 * 1.1, f.heatC(), 1e-9);
        Fire g = new Fire();
        g.add(LayPart.FUEL, Wood.SPRUCE, 50, 1, T);
        g.force(FireState.FIRE, FireSetting.HOT, T);
        assertEquals(12.5, g.heatC(), 1e-9);
        assertEquals(3, g.smoke(), "green wood smokes twice as hard");
    }

    @Test
    void birchNeedsAFireAndGreenNeedsHot() {
        Fire f = new Fire();
        f.add(LayPart.TINDER, null, 10, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.FUEL, Wood.BIRCH, 15, 1, T);
        f.emberCaught(T);
        assertEquals(-1, minutesUntil(f, FireState.FIRE, 25, CALM), "birch never catches from a small fire");
        assertEquals(FireState.OUT, f.state());
        // Spruce under the birch: the spruce catches, then the fire takes the birch.
        Fire g = laid(1);
        g.add(LayPart.FUEL, Wood.BIRCH, 15, 1, T);
        g.emberCaught(T);
        minutesUntil(g, FireState.FIRE, 10, CALM);
        int burn = minutesUntil(g, FireState.COALS, 300, CALM);
        assertEquals(1 * 40 * 0.7 + 1 * 40 * 1.4, burn, 2);
    }

    @Test
    void heavyRainPutsOutALowFireIn10Minutes() {
        Fire f = new Fire();
        f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 2, T);
        f.force(FireState.FIRE, FireSetting.LOW, T);
        FireWeather rain = FireWeather.heavyRain();
        for (int m = 1; m < 10; m++) {
            f.stepMinute(rain, T);
            assertEquals(FireState.FIRE, f.state(), "minute " + m);
        }
        f.stepMinute(rain, T);
        assertEquals(FireState.OUT, f.state());
        // Light rain takes 30, and a Hot fire drops one setting at a time.
        Fire h = new Fire();
        h.add(LayPart.FUEL, Wood.BIRCH, 15, 6, T);
        h.force(FireState.FIRE, FireSetting.HOT, T);
        h.advance(30, FireWeather.lightRain(), T);
        assertEquals(FireSetting.STEADY, h.setting());
        h.advance(30, FireWeather.lightRain(), T);
        assertEquals(FireSetting.LOW, h.setting());
        h.advance(30, FireWeather.lightRain(), T);
        assertEquals(FireState.OUT, h.state());
        // A roof over it cancels the rain.
        Fire roofed = new Fire();
        roofed.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 2, T);
        roofed.force(FireState.FIRE, FireSetting.LOW, T);
        roofed.advance(30, new FireWeather(com.yearofice.sim.env.Precip.RAIN, true, 0, true), T);
        assertEquals(FireState.FIRE, roofed.state());
    }

    @Test
    void snowPutsOutLowIn20() {
        Fire f = new Fire();
        f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 3, T);
        f.force(FireState.FIRE, FireSetting.LOW, T);
        f.advance(19, FireWeather.snow(), T);
        assertEquals(FireState.FIRE, f.state());
        f.stepMinute(FireWeather.snow(), T);
        assertEquals(FireState.OUT, f.state());
        // Snow slows a Steady fire by a quarter.
        Fire s = new Fire();
        s.add(LayPart.FUEL, Wood.SPRUCE, 15, 1, T);
        s.force(FireState.FIRE, FireSetting.STEADY, T);
        assertEquals(Math.ceil(28 / 0.75), minutesUntil(s, FireState.COALS, 100, FireWeather.snow()), 1);
    }

    @Test
    void bankedCoalsKeepSixHours() {
        Fire f = new Fire();
        f.force(FireState.COALS, null, T);
        assertTrue(f.bank().ok());
        f.advance(360, CALM, T);
        assertEquals(FireState.BANKED, f.state(), "relights at 6 h");
        assertFalse(f.puff(T).ok(), "needs fresh tinder first");
        f.add(LayPart.TINDER, null, 10, 1, T);
        assertTrue(f.puff(T).ok());
        assertEquals(FireState.SMALL, f.state());
        // Birch catches from the coal bed under a relit small fire.
        f.add(LayPart.FUEL, Wood.BIRCH, 15, 1, T);
        assertTrue(minutesUntil(f, FireState.FIRE, 10, CALM) > 0);

        Fire late = new Fire();
        late.force(FireState.COALS, null, T);
        late.bank();
        late.advance(370, CALM, T);
        assertEquals(FireState.OUT, late.state(), "not at 6 h 10 min");
        late.add(LayPart.TINDER, null, 10, 1, T);
        assertFalse(late.puff(T).ok());
        // Banked coals sit under ash: rain does not reach them.
        Fire wet = new Fire();
        wet.force(FireState.COALS, null, T);
        wet.bank();
        wet.advance(120, FireWeather.heavyRain(), T);
        assertEquals(FireState.BANKED, wet.state());
    }

    @Test
    void lightLevelsByState() {
        assertEquals(0, FireRules.light(FireState.OUT, FireSetting.STEADY));
        assertEquals(0, FireRules.light(FireState.EMBER, FireSetting.STEADY));
        assertEquals(3, FireRules.light(FireState.TINDER, FireSetting.STEADY));
        assertEquals(9, FireRules.light(FireState.SMALL, FireSetting.STEADY));
        assertEquals(9, FireRules.light(FireState.FIRE, FireSetting.LOW));
        assertEquals(12, FireRules.light(FireState.FIRE, FireSetting.STEADY));
        assertEquals(15, FireRules.light(FireState.FIRE, FireSetting.HOT));
        assertEquals(6, FireRules.light(FireState.COALS, FireSetting.STEADY));
        assertEquals(0, FireRules.light(FireState.BANKED, FireSetting.STEADY));
    }

    @Test
    void packedTooFullWarnsOnce() {
        Fire f = laid(3);
        Fire.Result r = f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 0.25, T);
        assertEquals("Packed too full: it may spread.", r.line());
        assertNull(f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 0.25, T).line());
        assertFalse(f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 3, T).ok(), "the ring holds 6 armloads");
    }

    @Test
    void heatRecordFeedsTheHeatIndex() {
        Fire f = new Fire();
        f.add(LayPart.FUEL, Wood.DEAD_SPRUCE, 18, 1, T);
        f.force(FireState.FIRE, FireSetting.STEADY, T);
        FireHeat h = FireHeat.of(10, 64, -3, f, T);
        assertEquals(18, h.bonusC(), 1e-9);
        assertEquals(18, HeatIndex.contribution(h.toHeatSource(2, 0)), 1e-9);
        assertEquals(9, HeatIndex.contribution(h.toHeatSource(5.5, 0)), 1e-9);
        assertEquals(0, HeatIndex.contribution(h.toHeatSource(8, 0)), 1e-9);
        assertTrue(HeatIndex.withinFire(List.of(h.toHeatSource(2.5, 0)), 3));
        // kW at Steady: 60 ÷ (40 × 0.7) armloads an hour × 11.8 kWh.
        assertEquals(60.0 / 28 * 11.8, h.kW(), 1e-9);
        // States without heat give a record that warms nothing.
        Fire banked = new Fire();
        banked.force(FireState.BANKED, null, T);
        assertFalse(FireHeat.of(0, 0, 0, banked, T).warm());
        Fire coals = new Fire();
        coals.force(FireState.COALS, null, T);
        assertEquals(15, FireHeat.of(0, 0, 0, coals, T).bonusC(), 1e-9);
    }

    @Test
    void snapshotRoundTrips() {
        Fire f = lit(1.5);
        f.advance(7, CALM, T);
        Fire g = Fire.restore(f.snapshot());
        assertEquals(f.state(), g.state());
        assertEquals(f.armloads(), g.armloads(), 1e-12);
        f.advance(40, CALM, T);
        g.advance(40, CALM, T);
        assertEquals(f.state(), g.state());
        assertEquals(f.burnedArmloads(), g.burnedArmloads(), 1e-12);
    }

    @Test
    void presetsKeepTheRulesAndMoveTheMargins() {
        for (FirePreset p : FirePreset.values()) {
            FireTuning t = p.tuning;
            assertTrue(t.matchCalmPct() > t.matchWindPct(), p.name());
            assertTrue(t.drillPracticedPct() > t.drillFreshPct(), p.name());
            assertTrue(t.heavyRainStepMin() < t.lightRainStepMin(), p.name());
        }
        assertTrue(FirePreset.GENTLE.tuning.bankedMin() >= FirePreset.STANDARD.tuning.bankedMin());
        assertEquals(360, FirePreset.STANDARD.tuning.bankedMin());
    }
}

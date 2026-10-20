package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.FireSetting;
import org.junit.jupiter.api.Test;

/** Fire builds, the ferro rod and the smother, against Blueprint › Fire, water and light and Building a campfire. */
class FireBuildsTest {
    static final FireTuning T = FireTuning.standard();

    static Fire barePlus(FireBuild b) {
        Fire f = new Fire();
        assertTrue(f.setBuild(b).ok());
        assertTrue(f.add(LayPart.TINDER, null, 10, 1, T).ok());
        f.add(LayPart.KINDLING, null, 15, 1, T);
        f.add(LayPart.KINDLING, null, 15, 1, T);
        return f;
    }

    static void light(Fire f) {
        assertTrue(f.strikeMatch(0.0, 0, FireWeather.CALM, false, T).ok());
        f.puff(T);
        f.puff(T);
    }

    /** Burns a one-armload fire from the tinder flame to the end of its fuel; returns the minutes the fuel burned. */
    static int burnMinutes(FireBuild b, Wood wood, FireSetting setting) {
        Fire f = barePlus(b);
        double piece = b == FireBuild.LONG ? 0.5 : 0.25;
        int n = (int) Math.round(1.0 / piece);
        for (int i = 0; i < n; i++) assertTrue(f.add(LayPart.FUEL, wood, 18, piece, T).ok(), "fuel " + i);
        light(f);
        int m = 0;
        while (f.state() != FireState.FIRE && m++ < 40) f.stepMinute(FireWeather.CALM, T);
        assertEquals(FireState.FIRE, f.state());
        f.setSetting(setting);
        int burned = 0;
        while (f.state() == FireState.FIRE && burned < 1000) {
            f.stepMinute(FireWeather.CALM, T);
            burned++;
        }
        return burned;
    }

    @Test
    void burnTimesPerArmloadAtSteady() {
        // Blueprint build table: teepee 1 h, long fire 3 h, Dakota hole 2 h per armload; wood multiplies (spruce x0.7).
        assertEquals(60 * 0.7, burnMinutes(FireBuild.TEEPEE, Wood.SPRUCE, FireSetting.STEADY), 1.5);
        assertEquals(180 * 0.7, burnMinutes(FireBuild.LONG, Wood.SPRUCE, FireSetting.STEADY), 1.5);
        assertEquals(120 * 0.7, burnMinutes(FireBuild.DAKOTA, Wood.SPRUCE, FireSetting.STEADY), 1.5);
        // Seasoned birch is x1.4: a long fire's armload lasts 4.2 hours (birch needs a Fire or coals, so set it burning).
        Fire birch = new Fire();
        birch.setBuild(FireBuild.LONG);
        birch.force(FireState.FIRE, FireSetting.STEADY, T);
        birch.setFuel(1.0, Wood.BIRCH, 18);
        assertEquals(180 * 1.4, birch.minutesLeft(T), 1e-6);
        // The Blueprint's stove line: one birch armload on Low lasts 4 x 1.4 x 2.5 hours in a stove (M8); here the
        // long fire's Low is 3 x 1.4 x 2.5 = 10.5 hours.
        birch.setSetting(FireSetting.LOW);
        assertEquals(180 * 1.4 * 2.5, birch.minutesLeft(T), 1e-6);
    }

    @Test
    void lowBurnsTwoAndAHalfTimesAsLong() {
        double steady = burnMinutes(FireBuild.LONG, Wood.SPRUCE, FireSetting.STEADY);
        double low = burnMinutes(FireBuild.LONG, Wood.SPRUCE, FireSetting.LOW);
        assertEquals(2.5, low / steady, 0.1);
    }

    @Test
    void buildGatesTheFuel() {
        Fire teepee = barePlus(FireBuild.TEEPEE);
        assertFalse(teepee.add(LayPart.FUEL, Wood.SPRUCE, 18, 0.5, T).ok(), "a log in a teepee");
        assertTrue(teepee.add(LayPart.FUEL, Wood.SPRUCE, 18, 0.25, T).ok(), "a split in a teepee");
        Fire lng = barePlus(FireBuild.LONG);
        assertFalse(lng.add(LayPart.FUEL, Wood.SPRUCE, 18, 0.25, T).ok(), "a split in a long fire");
        assertTrue(lng.add(LayPart.FUEL, Wood.SPRUCE, 18, 0.5, T).ok(), "a log in a long fire");
        Fire ring = barePlus(FireBuild.RING);
        assertTrue(ring.add(LayPart.FUEL, Wood.SPRUCE, 18, 0.5, T).ok());
        assertTrue(ring.add(LayPart.FUEL, Wood.SPRUCE, 18, 0.25, T).ok());
    }

    @Test
    void buildIsChosenOnlyOnABareRing() {
        Fire f = new Fire();
        assertTrue(f.bare());
        assertTrue(f.setBuild(FireBuild.LONG).ok());
        f.add(LayPart.TINDER, null, 10, 1, T);
        assertFalse(f.bare());
        assertFalse(f.setBuild(FireBuild.TEEPEE).ok());
        assertEquals(FireBuild.LONG, f.build());
    }

    @Test
    void cycleSkipsTheDakotaHoleWithoutAPit() {
        assertEquals(FireBuild.TEEPEE, FireBuild.RING.next(false));
        assertEquals(FireBuild.LONG, FireBuild.TEEPEE.next(false));
        assertEquals(FireBuild.RING, FireBuild.LONG.next(false));
        assertEquals(FireBuild.DAKOTA, FireBuild.LONG.next(true));
        assertEquals(FireBuild.RING, FireBuild.DAKOTA.next(true));
    }

    @Test
    void dakotaHoleIsOutOfTheWindAndThin() {
        // Wind does not take a match or burn the fuel faster in a Dakota hole; smoke is one step thinner.
        FireWeather gale = FireWeather.CALM.withWind(9);
        Fire open = barePlus(FireBuild.RING);
        assertFalse(open.strikeMatch(0.7, 0, gale, false, T).ok(), "50% roll 0.7 fails in wind");
        Fire pit = barePlus(FireBuild.DAKOTA);
        assertTrue(pit.strikeMatch(0.7, 0, gale, false, T).ok(), "95% in a Dakota hole");
        assertFalse(FireBuild.DAKOTA.sparks());
        assertTrue(FireBuild.TEEPEE.sparks());
        assertEquals(1, FireBuild.DAKOTA.smoke(3));
        assertEquals(3, FireBuild.RING.smoke(3));
    }

    @Test
    void ferroRodLightsWithoutBeingSpent() {
        Fire calm = barePlus(FireBuild.RING);
        assertEquals(0.90, calm.ferroChance(0, T), 1e-9);
        assertEquals(0.60, calm.ferroChance(8, T), 1e-9);
        assertTrue(calm.strikeFerro(0.89, FireWeather.CALM, false, T).ok());
        assertEquals(FireState.EMBER, calm.state());
        calm.puff(T);
        calm.puff(T);
        assertEquals(FireState.TINDER, calm.state());
    }

    @Test
    void ferroRodMissesAndRefusesWetTinder() {
        Fire f = barePlus(FireBuild.RING);
        assertFalse(f.strikeFerro(0.95, FireWeather.CALM, false, T).ok());
        assertEquals(FireState.OUT, f.state());
        Fire wet = new Fire();
        wet.add(LayPart.TINDER, null, 70, 1, T);
        wet.add(LayPart.KINDLING, null, 15, 1, T);
        var r = wet.strikeFerro(0.0, FireWeather.CALM, false, T);
        assertFalse(r.ok());
        assertTrue(r.line().contains("wet"), r.line());
        Fire none = new Fire();
        assertNotNull(none.strikeFerro(0.0, FireWeather.CALM, false, T).line());
    }

    @Test
    void smotherPutsAnyBurningFireOut() {
        Fire f = barePlus(FireBuild.RING);
        light(f);
        assertEquals(FireState.TINDER, f.state());
        assertTrue(f.smother().ok());
        assertEquals(FireState.OUT, f.state());
        assertFalse(f.hasTinder());
        assertFalse(f.smother().ok(), "nothing left to put out");
    }

    @Test
    void smotherKillsBankedCoalsToo() {
        Fire f = new Fire();
        f.force(FireState.BANKED, null, T);
        assertTrue(f.smother().ok());
        assertEquals(FireState.OUT, f.state());
        assertNull(null);
    }

    @Test
    void snapshotKeepsTheBuild() {
        Fire f = barePlus(FireBuild.LONG);
        Fire back = Fire.restore(f.snapshot());
        assertEquals(FireBuild.LONG, back.build());
    }
}

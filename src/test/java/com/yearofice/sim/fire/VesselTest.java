package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.body.WaterPurity.Heat;
import com.yearofice.sim.env.FireSetting;
import org.junit.jupiter.api.Test;

/** Boiling against Blueprint › Fire, water and light › Boiling and Water quality. */
class VesselTest {

    static int minutesTo(Vessel v, Heat h, java.util.function.Predicate<Vessel> done) {
        for (int m = 1; m <= 400; m++) {
            v = v.minute(h);
            if (done.test(v)) return m;
        }
        return -1;
    }

    @Test
    void aTwoLitrePotBoilsOnASteadyFireInThirtyMinutesGive5() {
        // M3 test: a 2 L pot boils on a Steady fire in 30 +-5 game minutes (20 min for 1 L, 1.6 times for 2 L).
        int toBoil = minutesTo(Vessel.of(2, WaterPurity.Source.RIVER), Heat.STEADY_FIRE, Vessel::boiling);
        assertTrue(Math.abs(toBoil - 30) <= 5, "boiled after " + toBoil);
        assertEquals(32, toBoil, 1);
    }

    @Test
    void waterIsSafeFiveMinutesAfterTheBoil() {
        Vessel v = Vessel.of(2, WaterPurity.Source.RIVER);
        int toBoil = minutesTo(v, Heat.STEADY_FIRE, Vessel::boiling);
        int toSafe = minutesTo(v, Heat.STEADY_FIRE, Vessel::boiled);
        assertEquals(5, toSafe - toBoil, 1);
        assertEquals(WaterPurity.minutesToSafe(Heat.STEADY_FIRE, 2), toSafe, 1.0);
    }

    @Test
    void theBoilingTableForOneLitre() {
        assertEquals(30, minutesTo(Vessel.of(1, WaterPurity.Source.LAKE), Heat.SMALL_FIRE, Vessel::boiling), 1);
        assertEquals(20, minutesTo(Vessel.of(1, WaterPurity.Source.LAKE), Heat.STEADY_FIRE, Vessel::boiling), 1);
        assertEquals(12, minutesTo(Vessel.of(1, WaterPurity.Source.LAKE), Heat.HOT_FIRE, Vessel::boiling), 1);
        assertEquals(12, minutesTo(Vessel.of(1, WaterPurity.Source.LAKE), Heat.TEEPEE, Vessel::boiling), 1);
        assertEquals(20, minutesTo(Vessel.of(1, WaterPurity.Source.LAKE), Heat.COALS, Vessel::boiling), 1);
    }

    @Test
    void aStoppedBoilStartsOverAndWaterNeverUnboilsOffTheFire() {
        Vessel v = Vessel.of(1, WaterPurity.Source.CREEK).minutes(Heat.STEADY_FIRE, 22);
        assertTrue(v.boiling());
        assertFalse(v.boiled());
        v = v.minutes(null, 10);
        assertFalse(v.boiling());
        assertEquals(0, v.boilMinutes(), 1e-9, "the rolling boil restarts");
        Vessel done = Vessel.of(1, WaterPurity.Source.CREEK).minutes(Heat.STEADY_FIRE, 40);
        assertTrue(done.boiled());
        Vessel cold = done.minutes(null, 100);
        assertTrue(cold.boiled(), "boiled stays safe");
        assertEquals(0, cold.heat(), 1e-9);
    }

    @Test
    void boiledWaterStaysHotThirtyMinutes() {
        Vessel v = Vessel.of(1, WaterPurity.Source.RIVER).minutes(Heat.STEADY_FIRE, 40);
        assertTrue(v.hot());
        assertTrue(v.minutes(null, 14).hot());
        assertFalse(v.minutes(null, 16).hot(), "hot for the first half of its 30 minutes");
        assertEquals(0, v.minutes(null, 30).heat(), 1e-9);
    }

    @Test
    void heatClassByFireAndBuild() {
        assertEquals(Heat.SMALL_FIRE, Vessel.heatClass(FireState.SMALL, FireSetting.STEADY, FireBuild.RING));
        assertEquals(Heat.STEADY_FIRE, Vessel.heatClass(FireState.FIRE, FireSetting.STEADY, FireBuild.RING));
        assertEquals(Heat.HOT_FIRE, Vessel.heatClass(FireState.FIRE, FireSetting.HOT, FireBuild.LONG));
        assertEquals(Heat.SMALL_FIRE, Vessel.heatClass(FireState.FIRE, FireSetting.LOW, FireBuild.DAKOTA));
        assertEquals(Heat.TEEPEE, Vessel.heatClass(FireState.FIRE, FireSetting.STEADY, FireBuild.TEEPEE));
        assertEquals(Heat.COALS, Vessel.heatClass(FireState.COALS, FireSetting.STEADY, FireBuild.RING));
        assertNull(Vessel.heatClass(FireState.TINDER, FireSetting.STEADY, FireBuild.RING));
        assertNull(Vessel.heatClass(FireState.BANKED, FireSetting.STEADY, FireBuild.RING));
        assertNull(Vessel.heatClass(FireState.OUT, FireSetting.STEADY, FireBuild.RING));
    }

    @Test
    void fillingRules() {
        Vessel empty = Vessel.empty();
        assertTrue(empty.canTake(WaterPurity.Source.RIVER, 2));
        Vessel half = Vessel.of(1, WaterPurity.Source.RIVER);
        assertTrue(half.canTake(WaterPurity.Source.RIVER, 2));
        assertFalse(half.canTake(WaterPurity.Source.BEAVER_POND, 2), "no mixing sources");
        assertFalse(Vessel.of(2, WaterPurity.Source.RIVER).canTake(WaterPurity.Source.RIVER, 2), "full");
        Vessel boiled = Vessel.of(1, WaterPurity.Source.RIVER).minutes(Heat.STEADY_FIRE, 40);
        assertTrue(boiled.boiled());
        assertFalse(boiled.canTake(WaterPurity.Source.RIVER, 2), "pour boiled water out first");
        assertEquals(2.0, half.toppedUp(2).litres(), 1e-9);
    }

    @Test
    void drinkingTakesLitresOutAndKeepsTheRest() {
        Vessel v = Vessel.of(1, WaterPurity.Source.RIVER);
        Vessel after = v.without(0.25);
        assertEquals(0.75, after.litres(), 1e-9);
        assertTrue(v.without(5).isEmpty());
        assertFalse(after.boiled());
    }
}

package com.yearofice.sim.decay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yearofice.sim.decay.ItemTemperature.State;
import org.junit.jupiter.api.Test;

/** Item heat and the thaw countdown (Items › Temperature; Decay and time › The cold curve). */
class ItemTemperatureTest {
    @Test
    void frostClock() {
        ItemTemperature bottle = ItemTemperature.ambient(1.0);
        assertEquals(60, bottle.minutesToFreeze(-10), 1e-9);
        assertEquals(15, bottle.minutesToFreeze(-40), 1e-9);
        assertEquals(State.FROZEN, bottle.after(60, -10).state());
        assertEquals(State.COLD, bottle.after(30, -10).state());
        ItemTemperature frozen = ItemTemperature.frozen(1.0);
        double fire = ItemTemperature.effectiveTempC(-25, true, false, 0);
        assertEquals(20, frozen.minutesToThaw(fire), 1e-9);
        assertEquals(State.AMBIENT, frozen.after(20, fire).state());
        assertEquals(20.0, ItemTemperature.effectiveTempC(-25, false, true, 30), 0);
        assertEquals(-25.0, ItemTemperature.effectiveTempC(-25, false, true, 29), 0);
    }

    @Test
    void warmFadesInThirtyMinutes() {
        ItemTemperature hot = ItemTemperature.ambient(0.5).heated();
        assertEquals(State.WARM, hot.after(29, 5).state());
        assertEquals(State.AMBIENT, hot.after(30, 5).state());
    }

    @Test
    void settleEqualsWalkingAndSaturatesAfter72Hours() {
        double[] hours = Climates.concat(Climates.constant(-10, 10), Climates.constant(3, 5));
        ItemTemperature start = ItemTemperature.ambient(2.0);
        ItemTemperature walked = start;
        for (double t : hours) walked = walked.after(60, t);
        assertEquals(walked, start.settle(hours));
        double[] long_ = Climates.concat(Climates.constant(-1, 100), Climates.constant(-30, 72));
        assertEquals(State.FROZEN, start.settle(long_).state());
    }

    @Test
    void thawCountdown() {
        int feb18 = ThawCountdown.dayOfYear(2, 18);
        assertEquals(57, ThawCountdown.tooltipDays(feb18, ThawCountdown.OPEN_POLE_THAW_DAY, 0, 6));
        assertEquals(51 + 15 + 3, ThawCountdown.tooltipDays(feb18, ThawCountdown.NORTH_CACHE_THAW_DAY, 0.35, 6));
        assertEquals(Double.POSITIVE_INFINITY, ThawCountdown.days(feb18, ThawCountdown.NEVER, 0, 6));
    }
}

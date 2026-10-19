package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yearofice.sim.env.EnvSample;
import org.junit.jupiter.api.Test;

/** The load band's calorie factor on the body's burn (Layer 6, Load; the body tick's step 2). */
class BodyLoadTest {
    static double burnedOneHour(Activity a, double loadKcal) {
        BodyState s = new BodyState();
        BodyModel.Inputs in = BodyModel.Inputs.resting(EnvSample.outdoors(15, 0)).doing(a).carrying(loadKcal);
        BodyModel.step(s, in, BodyTuning.STANDARD, 12 * 60, 60);
        return s.kcalOutToday(12 * 60 + 59);
    }

    @Test
    void awakeBurnScalesByTheBand() {
        double free = burnedOneHour(Activity.WALK, 1.0);
        assertEquals(1.2, burnedOneHour(Activity.WALK, 1.2) / free, 1e-12);
        assertEquals(1.5, burnedOneHour(Activity.WALK, 1.5) / free, 1e-12);
        assertEquals(free, burnedOneHour(Activity.WALK, 0), 0, "no factor reads as unloaded");
    }

    @Test
    void sleepIsNotCarrying() {
        assertEquals(burnedOneHour(Activity.SLEEP, 1.0), burnedOneHour(Activity.SLEEP, 1.5), 0);
    }

    @Test
    void theFactorSurvivesTheWithers() {
        BodyModel.Inputs in = BodyModel.Inputs.resting(EnvSample.outdoors(5, 0)).carrying(1.2);
        assertEquals(1.2, in.doing(Activity.HEAVY).in(EnvSample.outdoors(0, 3)).withHeat(null).loadKcal(), 0);
    }
}

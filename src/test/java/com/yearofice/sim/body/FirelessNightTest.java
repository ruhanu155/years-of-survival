package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.ShelterState;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The fireless first night (RECONCILE 225): May, 0-4 degC, the starting clothes, bare ground, no fire. */
class FirelessNightTest {
    static final double[] AIR = {3.1, 2.0, 0.9, 3.8, 3.2, 2.7, 2.3, 2.0, 2.0};

    /** Returns {lowest warmth, 1 if collapsed, cold wakes}. */
    static double[] night(BodyTuning t) {
        BodyState s = new BodyState();
        s.warmth = 82; // dusk after a day of work
        double low = 100;
        int wakes = 0;
        long m = 21 * 60;
        for (int h = 0; h < AIR.length; h++) {
            EnvSample env = EnvSample.outdoors(AIR[h], 0.5);
            boolean asleep = h >= 1 && s.warmth >= 40; // cold wakes: up at the hour if under 40, down again when over
            if (h >= 1 && !asleep) wakes++;
            BodyModel.Inputs in = asleep
                    ? new BodyModel.Inputs(Activity.SLEEP, true, 0.7, env, List.of(), ShelterState.NONE, com.yearofice.sim.body.ColdWater.Depth.NONE, false)
                    : new BodyModel.Inputs(Activity.REST, false, 0, env, List.of(), ShelterState.NONE, com.yearofice.sim.body.ColdWater.Depth.NONE, false);
            BodyModel.Result r = BodyModel.step(s, in, t, m, 60);
            m += 60;
            low = Math.min(low, s.warmth);
            if (r.collapsed()) return new double[] {0, 1, wakes};
        }
        return new double[] {low, 0, wakes};
    }

    @Test
    void standard_is_miserable_not_fatal() {
        double[] r = night(BodyTuning.STANDARD);
        assertEquals(0, r[1], "no collapse");
        assertTrue(r[0] >= 20 && r[0] <= 40, "warmth bottoms near 30: " + r[0]);
        assertTrue(r[2] >= 1, "the cold wakes the sleeper: " + r[2]);
    }

    @Test
    void brutal_collapses() {
        assertEquals(1, night(BodyTuning.BRUTAL)[1]);
    }

    @Test
    void gentle_never_gets_cold() {
        double[] r = night(BodyTuning.GENTLE);
        assertEquals(0, r[1]);
        assertTrue(r[0] > 70, "" + r[0]);
        assertFalse(r[2] > 0);
    }
}

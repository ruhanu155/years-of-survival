package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SleepSplitTest {
    @Test
    void sleep_170_wake_10_in_one_chunk_credits_170() {
        assertEquals(170, SleepSplit.sleepMinutes(180, 17, 18));
        assertEquals(180, SleepSplit.sleepMinutes(180, 18, 18));
        assertEquals(0, SleepSplit.sleepMinutes(180, 0, 18));
    }

    @Test
    void split_chunk_keeps_the_awake_clock_clear() {
        BodyState s = new BodyState();
        s.awakeMin = 500;
        s.water = 100; s.warmth = 100;
        var sleep = new BodyModel.Inputs(Activity.SLEEP, true, 0.8, com.yearofice.sim.env.EnvSample.outdoors(15, 0), java.util.List.of(),
                com.yearofice.sim.env.ShelterState.NONE, ColdWater.Depth.NONE, false);
        int n = SleepSplit.sleepMinutes(180, 17, 18);
        BodyModel.step(s, sleep, BodyTuning.STANDARD, 0, n);
        assertEquals(0, s.awakeMin); // 170 slept x3 repays all 500
    }
}

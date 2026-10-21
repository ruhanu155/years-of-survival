package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.ShelterState;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The energy cap after sleep (M7 H): the awake clock is paid back by sleep, even a night broken by tending the fire. */
class AwakeCapTest {
    static final EnvSample MILD = EnvSample.outdoors(15, 0);

    static void run(BodyState s, boolean asleep, int minutes, long start) {
        BodyModel.Inputs in = new BodyModel.Inputs(asleep ? Activity.SLEEP : Activity.REST, asleep, asleep ? 0.8 : 0, MILD,
                List.of(), ShelterState.NONE, ColdWater.Depth.NONE, false);
        for (int m = 0; m < minutes; m += 10) {
            s.water = 100; s.warmth = 100;
            BodyModel.step(s, in, BodyTuning.STANDARD, start + m, 10);
        }
    }

    @Test
    void eight_hours_asleep_resets_the_awake_counter() {
        BodyState s = new BodyState();
        s.awakeMin = 30 * 60;
        run(s, true, 480, 0);
        assertEquals(0, s.awakeMin);
        assertEquals(10, Metabolism.energyCap(0, 22.7, 100, s.awakeMin, false));
    }

    @Test
    void six_broken_nights_keep_the_cap_at_ten() {
        BodyState s = new BodyState();
        long m = 6 * 60;
        for (int day = 0; day < 6; day++) {
            run(s, false, 16 * 60, m); m += 16 * 60;
            for (int part = 0; part < 3; part++) { // three sleeps with fire-tending wakes between
                run(s, true, 150, m); m += 150;
                if (part < 2) { run(s, false, 15, m); m += 15; }
            }
            run(s, false, 10, m); m += 10;
            assertEquals(10, Metabolism.energyCap(-340, 22.7, 100, s.awakeMin, false), "morning of day " + day + " awake " + s.awakeMin);
        }
    }

    @Test
    void cap_table_rows() {
        assertEquals(10, Metabolism.energyCap(-340, 22.7, 100, 16 * 60, false));
        assertEquals(8, Metabolism.energyCap(-1600, 22.7, 100, 0, false));
        assertEquals(7, Metabolism.energyCap(0, 22.7, 100, 24 * 60, false));
        assertEquals(5, Metabolism.energyCap(0, 22.7, 100, 48 * 60, false));
        assertEquals(3, Metabolism.energyCap(0, 22.7, 100, 72 * 60, false));
    }

    /** The v5 proxy pattern: 960 awake min a day, an 8 h night broken by three 10 min fire wakes, seven days. */
    @Test
    void v5_pattern_seven_days_cap_ten_every_morning() {
        BodyState s = new BodyState();
        long m = 6 * 60;
        for (int day = 0; day < 7; day++) {
            run(s, false, 960, m); m += 960;
            for (int part = 0; part < 4; part++) {
                run(s, true, 120, m); m += 120;
                if (part < 3) { run(s, false, 10, m); m += 10; }
            }
            assertEquals(10, Metabolism.energyCap(-300, 22.7, 100, s.awakeMin, false), "morning " + day + " awake " + s.awakeMin);
        }
    }

    @Test
    void seven_hours_in_a_ten_hour_window_clears_in_big_chunks() {
        BodyState s = new BodyState();
        s.awakeMin = 40 * 60;
        BodyModel.Inputs in = new BodyModel.Inputs(Activity.SLEEP, true, 0.8, MILD, List.of(), ShelterState.NONE, ColdWater.Depth.NONE, false);
        s.water = 100; s.warmth = 100;
        BodyModel.step(s, in, BodyTuning.STANDARD, 0, 60);
        BodyModel.step(s, in, BodyTuning.STANDARD, 300, 360); // wake gap, then 6 more hours: 7 h inside 10 h
        assertEquals(0, s.awakeMin);
    }

    @Test
    void cap_cause_names_the_lowest_row() {
        assertEquals(Metabolism.CAP_AWAKE, Metabolism.capCause(0, 22, 100, 30 * 60, false, 10));
        assertEquals(Metabolism.CAP_HUNGRY, Metabolism.capCause(-1600, 22, 100, 0, false, 10));
        assertEquals(Metabolism.CAP_THIRST, Metabolism.capCause(0, 22, 10, 0, false, 10));
        assertEquals(Metabolism.CAP_ILL, Metabolism.capCause(0, 22, 100, 0, false, 5));
        assertEquals(Metabolism.CAP_NONE, Metabolism.capCause(0, 22, 100, 0, false, 10));
    }
}

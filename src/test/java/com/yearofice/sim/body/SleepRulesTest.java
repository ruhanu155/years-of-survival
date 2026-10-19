package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.EnvSample;
import org.junit.jupiter.api.Test;

/** Blueprint › Survival tuning › Sleep: when a player may lie down, bed quality and warmth, and what wakes them. */
class SleepRulesTest {
    static final SleepRules.Tuning T = SleepRules.Tuning.STANDARD;

    static SleepRules.Facts facts(int mod, boolean night, int slept, double warmth, double energy) {
        return new SleepRules.Facts(mod, night, slept, warmth, 0.1, 80, energy, 10, false);
    }

    @Test
    void the_clock_defines_night_21_to_06() {
        assertTrue(SleepRules.isNight(21 * 60, T));
        assertTrue(SleepRules.isNight(23 * 60 + 59, T));
        assertTrue(SleepRules.isNight(0, T));
        assertTrue(SleepRules.isNight(5 * 60 + 59, T));
        assertFalse(SleepRules.isNight(6 * 60, T));
        assertFalse(SleepRules.isNight(20 * 60 + 59, T));
        assertFalse(SleepRules.isNight(12 * 60, T));
    }

    @Test
    void a_player_may_sleep_tired_or_at_night() {
        assertFalse(SleepRules.mayLieDown(9, 14 * 60, T), "rested, 14:00");
        assertTrue(SleepRules.mayLieDown(6.9, 14 * 60, T), "tired, 14:00");
        assertTrue(SleepRules.mayLieDown(10, 22 * 60, T), "rested, 22:00");
    }

    @Test
    void bed_quality_and_warmth() {
        assertEquals(0.70, SleepRules.quality(Double.NaN, T), 1e-12, "bare ground");
        assertEquals(0.95, SleepRules.quality(0.95, T), 1e-12, "bough bed");
        assertEquals(0, SleepRules.warmthC(Double.NaN), 1e-12);
        assertEquals(5, SleepRules.warmthC(5), 1e-12);
    }

    @Test
    void wake_causes_in_order() {
        assertEquals(SleepRules.Wake.NONE, SleepRules.check(facts(2 * 60, true, 240, 60, 6), T));
        assertEquals(SleepRules.Wake.COLD, SleepRules.check(facts(3 * 60 + 10, true, 300, 39.9, 6), T));
        assertEquals(SleepRules.Wake.WET, SleepRules.check(new SleepRules.Facts(180, true, 60, 60, 0.61, 80, 6, 10, false), T));
        assertEquals(SleepRules.Wake.THIRST, SleepRules.check(new SleepRules.Facts(180, true, 60, 60, 0.1, 14, 6, 10, false), T));
        assertEquals(SleepRules.Wake.DANGER, SleepRules.check(new SleepRules.Facts(180, true, 60, 60, 0.1, 80, 6, 10, true), T));
        // cold wins over danger and morning: each cause names itself, the first that holds
        assertEquals(SleepRules.Wake.COLD, SleepRules.check(new SleepRules.Facts(6 * 60, true, 60, 30, 0.1, 80, 6, 10, true), T));
    }

    @Test
    void a_night_sleep_ends_in_the_morning_and_a_day_sleep_when_rested() {
        assertEquals(SleepRules.Wake.NONE, SleepRules.check(facts(5 * 60 + 59, true, 400, 60, 9), T));
        assertEquals(SleepRules.Wake.MORNING, SleepRules.check(facts(6 * 60, true, 401, 60, 9), T));
        assertEquals(SleepRules.Wake.NONE, SleepRules.check(facts(6 * 60, true, 0, 60, 9), T), "not slept yet");
        assertEquals(SleepRules.Wake.NONE, SleepRules.check(facts(14 * 60, false, 120, 60, 9), T), "a nap until rested");
        assertEquals(SleepRules.Wake.RESTED, SleepRules.check(facts(16 * 60, false, 240, 60, 10), T));
    }

    @Test
    void a_bough_bed_sleeps_5_c_warmer_and_restores_at_095() {
        EnvSample cold = EnvSample.outdoors(-3, 2);
        BodyState ground = new BodyState(), bed = new BodyState();
        BodyModel.Inputs asleep = BodyModel.Inputs.resting(cold).doing(Activity.SLEEP).onBed(0.70, 0);
        ground.energy = bed.energy = 3;
        BodyModel.step(ground, asleep, BodyTuning.STANDARD, 0, 60);
        BodyModel.step(bed, asleep.onBed(0.95, 5), BodyTuning.STANDARD, 0, 60);
        assertEquals(5, bed.lastFeel.totalC() - ground.lastFeel.totalC(), 1e-9, "feels-like");
        assertEquals(1.25 * 0.95, bed.energy - 3, 1e-9, "an hour in a bough bed");
        assertEquals(1.25 * 0.70, ground.energy - 3, 1e-9, "an hour on the ground");
        assertTrue(bed.warmth > ground.warmth, "warmer in the bed: " + bed.warmth + " vs " + ground.warmth);
    }
}

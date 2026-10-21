package com.yearofice.sim.livestock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** The livestock numbers of Raising animals: the egg curve, the dry doe, the trough, growth, pens and care. */
class LivestockSimTest {
    static final LivestockTuning STD = LivestockTuning.Preset.STANDARD.tuning;

    static Spec spec(String id) throws IOException {
        try (Reader r = new InputStreamReader(LivestockSimTest.class.getResourceAsStream("/data/yearofice/livestock/" + id + ".json"))) {
            return Spec.parse(JsonParser.parseReader(r).getAsJsonObject());
        }
    }

    @Test
    void egg_curve_four_hens_at_12h_light_and_6c_lay_0_40_each() throws IOException {
        Spec hen = spec("hen");
        assertEquals(0.40, Produce.eggsPerDay(12, 6, 100), 0.04);
        assertEquals(0.0, Produce.eggsPerDay(10, 6, 100), 1e-9, "no light, no eggs");
        assertEquals(0.8, Produce.eggsPerDay(14, 6, 100), 1e-9);
        assertEquals(0.0, Produce.eggsPerDay(14, -2, 100), 1e-9, "a frozen coop lays nothing");
        assertEquals(0.4, Produce.eggsPerDay(14, 2, 100), 1e-9, "0 to 5 C halves it");
        assertEquals(0.0, Produce.eggsPerDay(14, 6, 20), 1e-9, "below fed 30 nothing");
        assertEquals(0.4, Produce.eggsPerDay(14, 6, 45), 1e-9);
        assertTrue(hen.eggsPerDayMax() >= 0.8);
    }

    @Test
    void goat_is_dry_on_day_241_after_kidding() throws IOException {
        Spec goat = spec("goat");
        assertTrue(Produce.milkLPerDay(goat, 0, 100, false) > 1.99);
        assertEquals(1.0, Produce.milkLPerDay(goat, 239, 100, false), 0.01, "half the peak on the last milking day");
        assertFalse(Produce.dry(goat, 239));
        assertTrue(Produce.dry(goat, 241));
        assertEquals(0.0, Produce.milkLPerDay(goat, 241, 100, false), 1e-9);
        assertEquals(0.75 * Produce.milkLPerDay(goat, 100, 100, false), Produce.milkLPerDay(goat, 100, 100, true), 1e-9, "hay alone gives 0.75");
    }

    @Test
    void a_full_trough_keeps_two_goats_fed_above_30_for_three_days_and_no_longer() throws IOException {
        Spec goat = spec("goat");
        double left = Care.Trough.full(goat, 2).left();
        Care.State a = Care.State.FRESH, b = Care.State.FRESH;
        double ration = goat.feedKgPerDay() / 24;
        int below = -1;
        for (int h = 1; h <= 120; h++) {
            double need = 2 * ration;
            double take = Math.min(need, left);
            left -= take;
            double f = take / need;
            a = Care.step(a, goat, STD, f, 1, 5);
            b = Care.step(b, goat, STD, f, 1, 5);
            if (h == 72) assertTrue(a.fed() > 30 && b.fed() > 30, "still fed after 3 days: " + a.fed());
            if (below < 0 && a.fed() < 30) below = h;
        }
        assertTrue(below > 72 && below <= 96, "fed falls under 30 within a day of the trough running dry: hour " + below);
    }

    @Test
    void care_rules_stop_produce_lose_condition_and_kill_at_zero_for_two_days() throws IOException {
        Spec goat = spec("goat");
        Care.State s = Care.State.FRESH;
        int stopped = -1, died = -1;
        for (int h = 1; h <= 400 && died < 0; h++) {
            s = Care.step(s, goat, STD, 0, 0, 5);
            if (stopped < 0 && !s.producing()) stopped = h;
            if (s.dead()) died = h;
        }
        assertTrue(stopped > 0 && stopped < 30, "produce stops when a need passes 30: hour " + stopped);
        assertTrue(died > 0, "an unfed animal dies");
        assertTrue(s.zeroHours() >= 48 || s.condition() <= 0);
        Care.State warm = Care.State.FRESH;
        for (int h = 0; h < 200; h++) warm = Care.step(warm, goat, STD, 1, 1, 5);
        assertTrue(warm.producing() && warm.condition() >= 99 && !warm.dead());
    }

    @Test
    void a_cold_pen_lowers_warm_and_a_gentle_world_decays_slower() throws IOException {
        Spec hen = spec("hen");
        Care.State s = Care.State.FRESH;
        for (int h = 0; h < 100; h++) s = Care.step(s, hen, STD, 1, 1, -20);
        assertTrue(s.warm() < 10, "a hen in a -20 C coop: " + s.warm());
        Care.State gentle = Care.State.FRESH, brutal = Care.State.FRESH;
        for (int h = 0; h < 30; h++) {
            gentle = Care.step(gentle, hen, LivestockTuning.Preset.GENTLE.tuning, 0, 1, 10);
            brutal = Care.step(brutal, hen, LivestockTuning.Preset.BRUTAL.tuning, 0, 1, 10);
        }
        assertTrue(gentle.fed() > brutal.fed());
    }

    @Test
    void the_winter_feed_debt_is_what_the_stack_does_not_cover() throws IOException {
        Spec goat = spec("goat");
        // two goats from Oct 1 (day 153) to May 1: about 212 days, 2 kg each
        double need = Care.hayNeededKg(2, goat.feedKgPerDay(), 153, 0);
        assertEquals(2 * 2.0 * 212, need, 1e-9);
        assertEquals(0, Care.feedDebtKg(900, need), 1e-9);
        assertEquals(need - 200, Care.feedDebtKg(200, need), 1e-9);
        assertEquals(50, Care.storeDays(200, 2, 2.0), 1e-9);
    }

    @Test
    void a_kid_reaches_adult_weight_on_steady_feeding_and_hunger_stunts_it() throws IOException {
        Spec goat = spec("goat");
        double p = 0, age = 0;
        for (; p < 1 && age < 1000; age++) p = Growth.dayStep(goat, STD, p, age, 100, true);
        assertTrue(age >= 235 && age <= 245, "a kid is grown at 240 days: " + age);
        assertEquals(goat.adultKg(), Growth.massKg(goat, p), 1e-9);
        assertEquals(Growth.Stage.ADULT, Growth.stage(goat, p, age));
        double q = 0;
        for (int d = 0; d < 240; d++) q = Growth.dayStep(goat, STD, q, d, 40, true);
        assertTrue(q < 0.6 && Growth.massKg(goat, q) < goat.adultKg() * 0.9, "a half-fed kid is stunted: " + q);
        double s = 0;
        for (int d = 0; d < 20; d++) s = Growth.dayStep(goat, STD, s, d, 100, false);
        assertEquals(0.0, s, 1e-9, "a suckling with no milk does not grow");
        assertEquals(Growth.Stage.SUCKLING, Growth.stage(goat, 0.1, 20));
        assertTrue(Growth.inBirthMonth(goat, 5) && Growth.inRut(goat, 10) && Growth.inRut(goat, 11) && !Growth.inRut(goat, 3));
        assertTrue(Growth.daysToAdult(goat, STD, 1) < Growth.daysToAdult(goat, LivestockTuning.Preset.BRUTAL.tuning, 1));
    }

    @Test
    void predators_know_the_fence() {
        for (var f : new PenRisk.Fence[] {PenRisk.Fence.BRUSH, PenRisk.Fence.POLE, PenRisk.Fence.STOCKADE})
            assertTrue(PenRisk.attackChance(f, false, false, STD) > PenRisk.attackChance(f, true, false, STD));
        assertEquals(0.25, PenRisk.attackChance(PenRisk.Fence.BRUSH, false, false, STD), 1e-9);
        assertEquals(0.08, PenRisk.attackChance(PenRisk.Fence.POLE, false, false, STD), 1e-9);
        assertEquals(0.02, PenRisk.attackChance(PenRisk.Fence.STOCKADE, false, false, STD), 1e-9);
        assertEquals(0.08 * 0.3, PenRisk.attackChance(PenRisk.Fence.POLE, true, false, STD), 1e-9);
        assertEquals(0.25, PenRisk.attackChance(PenRisk.Fence.STOCKADE, false, true, STD), 1e-9, "an open door counts as brush");
        assertEquals(0.125, PenRisk.attackChance(PenRisk.Fence.BRUSH, false, false, LivestockTuning.Preset.GENTLE.tuning), 1e-9);
        assertEquals(0.05, PenRisk.escapeChance(PenRisk.Fence.BRUSH, STD), 1e-9);
        assertTrue(PenRisk.unpennedChance(STD) >= 0.4, "a hen out at dusk is taken 40% of nights");
    }

    @Test
    void escapes_over_a_year_follow_the_fence() {
        SplittableRandom r = new SplittableRandom(7);
        int brush = 0, pole = 0;
        for (int d = 0; d < 36500; d++) {
            if (r.nextDouble() < PenRisk.escapeChance(PenRisk.Fence.BRUSH, STD)) brush++;
            if (r.nextDouble() < PenRisk.escapeChance(PenRisk.Fence.POLE, STD)) pole++;
        }
        assertEquals(0.05, brush / 36500.0, 0.006);
        assertEquals(0.01, pole / 36500.0, 0.003);
    }
}

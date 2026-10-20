package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.EnvSample;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * The body tick's slice math (Blueprint › Survival tuning; Minecraft development › The body tick): BMR and BMI,
 * burn, weight at 2,400 kcal a kg, water, energy and its caps, warmth through the Warmth core, collapse, and the
 * closed form.
 */
class BodyModelTest {
    static final EnvSample MILD = EnvSample.outdoors(15, 0);

    static BodyModel.Inputs rest(EnvSample env) { return BodyModel.Inputs.resting(env); }

    @Test
    void bmr_default_body_is_1794() {
        assertEquals(1794, Metabolism.bmr(77, 183, 0), 1.0);
        assertEquals(23.0, Metabolism.bmi(77, 183), 0.05);
        assertEquals(17 * 1.83 * 1.83, Metabolism.kgForBmi(17, 183), 1e-9);
    }

    @Test
    void rates_scale_with_bmr() {
        double full = Metabolism.burnPerMinute(Activity.WALK, 77, 183, 0, 100, 1.0) * 60;
        assertEquals(250, full, 0.2);
        double thin = Metabolism.burnPerMinute(Activity.WALK, 57, 183, 0, 100, 1.0) * 60;
        assertEquals(250 * Metabolism.bmr(57, 183, 0) / 1794, thin, 0.2);
        assertEquals(125, Metabolism.burnPerMinute(Activity.WALK, 77, 183, 0, 100, 0.5) * 60, 0.2);
        // shivering adds 300 kcal/h at warmth 30 and below, on top of the activity
        assertEquals(75 + 300, Metabolism.burnPerMinute(Activity.REST, 77, 183, 0, 25, 1.0) * 60, 0.2);
    }

    @Test
    void chores_day_is_in_the_slice_window() {
        double plan = DayPlan.CHORES.kcalPerDay(1.0);
        assertEquals(2955, plan, 1e-9);
        BodyState s = new BodyState();
        DayPlan.CHORES.run(s, rest(MILD), BodyTuning.STANDARD, 0, 1440);
        double burned = Arrays.stream(s.kcalOut).sum();
        assertTrue(burned >= 2600 && burned <= 3200, "a chores day burns " + burned);
    }

    @Test
    void weight_moves_at_2400_kcal_a_kg_on_standard_and_3500_on_gentle() {
        for (BodyTuning t : new BodyTuning[] {BodyTuning.STANDARD, BodyTuning.GENTLE}) {
            BodyState s = new BodyState();
            s.stomachKg = 0;
            BodyModel.eat(s, t, 0, 100, 2400, 0, 0, 0, 0);
            double before = s.kg;
            BodyModel.step(s, rest(MILD), t, 0, 1);
            double burned = s.kcalOut[0];
            assertEquals((2400 - burned) / t.kcalPerKg(), s.kg - before, 1e-9, t.preset());
        }
    }

    static double starvationDays(BodyTuning t) {
        BodyState s = new BodyState();
        long start = 6 * 60;
        for (int day = 0; day < 120; day++) {
            long m = start + day * 1440L;
            BodyModel.drink(s, m, 3.0, -1);
            BodyModel.Result r = DayPlan.CHORES.run(s, rest(MILD), t, m, 1440);
            if (r.collapsed()) {
                assertEquals(BodyModel.Collapse.BMI, r.collapse());
                return (r.minute() - start) / 1440.0;
            }
        }
        return Double.NaN;
    }

    @Test
    void starvation_reaches_bmi17_on_day_15_to_21_on_standard() {
        double std = starvationDays(BodyTuning.STANDARD);
        assertTrue(std >= 15 && std <= 21, "Standard: BMI 17 after " + std + " days");
        double brutal = starvationDays(BodyTuning.BRUTAL);
        assertTrue(brutal < std, "Brutal is sooner: " + brutal);
        // Gentle burns half and moves weight at 3,500 kcal a kg (RECONCILE 135), so it is about three times later.
        double gentle = starvationDays(BodyTuning.GENTLE);
        assertTrue(gentle > 2 * std, "Gentle is later: " + gentle);
    }

    @Test
    void soaked_at_minus_20_hits_warmth_0_in_13_to_17_minutes() {
        BodyState s = new BodyState();
        Arrays.fill(s.slotWet, 1.0);
        BodyModel.Inputs in = rest(EnvSample.outdoors(-20, 0));
        int minutes = 0;
        BodyModel.Result r = BodyModel.Result.NONE;
        while (!r.collapsed() && minutes < 120) r = BodyModel.step(s, in, BodyTuning.STANDARD, minutes++, 1);
        assertEquals(BodyModel.Collapse.WARMTH, r.collapse());
        assertTrue(minutes >= 13 && minutes <= 17, "warmth 0 after " + minutes);
    }

    @Test
    void dry_at_minus_20_cools_by_the_feels_like_rate_only() {
        BodyState s = new BodyState();
        BodyModel.step(s, rest(EnvSample.outdoors(-20, 0)), BodyTuning.STANDARD.withKit(0), 0, 10);
        // feels-like −20 → dW = −0.02 · 30 = −0.6 a minute
        assertEquals(100 - 6, s.warmth, 1e-9);
        BodyState g = new BodyState();
        BodyModel.step(g, rest(EnvSample.outdoors(-20, 0)), BodyTuning.GENTLE.withKit(0), 0, 10);
        assertEquals(100 - 3, g.warmth, 1e-9);
    }

    @Test
    void rain_soaks_bare_skin_over_time() {
        BodyState s = new BodyState();
        BodyModel.step(s, rest(EnvSample.outdoors(5, 0).withPrecip(com.yearofice.sim.env.Precip.RAIN, 2.5)),
                BodyTuning.STANDARD.withKit(0), 0, 60);
        assertTrue(s.wetness() > 0.9, "an hour of rain soaks bare skin: " + s.wetness());
    }

    @Test
    void water_drains_3_5_an_hour_resting_and_a_sip_is_8_points() {
        BodyState s = new BodyState();
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 0, 600);
        assertEquals(100 - 35, s.water, 1e-6);
        BodyState hot = new BodyState();
        BodyModel.step(hot, rest(EnvSample.outdoors(25, 0)), BodyTuning.STANDARD, 0, 600);
        assertEquals(100 - 35 * 1.2, hot.water, 1e-6);
        BodyModel.drink(s, 600, 0.25, -1);
        assertEquals(65 + 8.333, s.water, 0.01);
        assertEquals(250, s.drunkMlToday(600), 1e-9);
    }

    @Test
    void resting_player_who_never_drinks_runs_dry_after_about_29_hours() {
        BodyState s = new BodyState();
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 0, 28 * 60);
        assertTrue(s.water > 0);
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 28 * 60, 60);
        assertEquals(0, s.water, 1e-9);
    }

    @Test
    void energy_drains_by_activity_and_8_hours_of_good_sleep_refill_10() {
        BodyState s = new BodyState();
        BodyModel.step(s, rest(MILD).doing(Activity.WALK), BodyTuning.STANDARD, 0, 600);
        assertEquals(10 - 4.0, s.energy, 1e-6);
        BodyState t = new BodyState();
        t.energy = 0;
        t.awakeMin = 16 * 60;
        BodyModel.step(t, rest(MILD).doing(Activity.SLEEP), BodyTuning.STANDARD, 600, 8 * 60);
        assertEquals(10, t.energy, 1e-6);
        assertEquals(0, t.awakeMin, "a 4 h sleep clears the awake count");
    }

    @Test
    void energy_cap_table() {
        assertEquals(10, Metabolism.energyCap(0, 23, 100, 0, false));
        assertEquals(8, Metabolism.energyCap(-1600, 23, 100, 0, false));
        assertEquals(7, Metabolism.energyCap(0, 18, 100, 0, false));
        assertEquals(7, Metabolism.energyCap(0, 23, 29, 0, false));
        assertEquals(4, Metabolism.energyCap(0, 23, 14, 0, false));
        assertEquals(7, Metabolism.energyCap(0, 23, 100, 24 * 60, false));
        assertEquals(3, Metabolism.energyCap(0, 23, 100, 72 * 60, false));
        assertEquals(7, Metabolism.energyCap(0, 23, 100, 0, true));
    }

    @Test
    void sleep_refills_only_to_the_cap() {
        BodyState s = new BodyState();
        s.energy = 2;
        s.water = 10;
        BodyModel.step(s, rest(MILD).doing(Activity.SLEEP), BodyTuning.STANDARD, 0, 8 * 60);
        assertEquals(4, s.energy, 1e-6);
    }

    @Test
    void bins_split_today_from_the_last_24_hours() {
        BodyState s = new BodyState();
        long evening = 20 * 60;
        BodyModel.eat(s, BodyTuning.STANDARD, evening, 300, 500, 0, 10, 50, 0);
        assertEquals(500, s.kcalInToday(evening), 1e-9);
        long morning = 1440 + 8 * 60;
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, evening, (int) (morning - evening));
        assertEquals(0, s.kcalInToday(morning), 1e-9, "yesterday's supper is not today's");
        assertTrue(s.balance24h() > -2000 && s.balance24h() < 500 - 800, "the 24 h balance still holds it");
        assertTrue(s.macroMeter(Metabolism.PROTEIN) > 0.2);
    }

    @Test
    void gut_illness_halves_absorbed_calories_from_its_onset() {
        BodyState s = new BodyState();
        BodyModel.drink(s, 0, 0.25, 60);
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 0, 61);
        assertTrue(s.gutIll(61));
        BodyModel.eat(s, BodyTuning.STANDARD, 61, 100, 1000, 0, 0, 0, 0);
        assertEquals(500, s.pendingKcal, 1e-9);
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 61, 2 * 1440);
        assertFalse(s.gutIll(61 + 2 * 1440 + 1));
    }

    @Test
    void the_stomach_fills_by_mass_and_empties_7_percent_an_hour_awake() {
        BodyState s = new BodyState();
        assertTrue(BodyModel.eat(s, BodyTuning.STANDARD, 0, 900, 1000, 0, 0, 0, 0));
        assertEquals(0.5, s.stomachPct(1.8), 1e-9);
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 0, 60);
        assertEquals(0.43, s.stomachPct(1.8), 1e-9);
        s.stomachKg = 1.8;
        assertFalse(BodyModel.eat(s, BodyTuning.STANDARD, 60, 100, 100, 0, 0, 0, 0), "full stomach refuses");
    }

    @Test
    void closed_form_matches_stepping_within_half_a_percent() {
        for (Activity a : new Activity[] {Activity.WALK, Activity.HEAVY, Activity.SLEEP}) {
            BodyModel.Inputs in = rest(EnvSample.outdoors(5, 1)).doing(a);
            BodyState x = new BodyState(), y = new BodyState();
            x.energy = y.energy = 5;
            for (int i = 0; i < 600; i++) BodyModel.step(x, in, BodyTuning.STANDARD, i, 1);
            BodyModel.step(y, in, BodyTuning.STANDARD, 0, 600);
            assertEquals(x.kg, y.kg, 0.005 * y.kg, a + " kg");
            assertEquals(x.water, y.water, 0.005 * y.water, a + " water");
            assertEquals(x.energy, y.energy, 0.005 * Math.max(1, y.energy), a + " energy");
            assertEquals(x.warmth, y.warmth, 0.005 * Math.max(1, y.warmth), a + " warmth");
        }
    }

    @Test
    void charged_work_burns_like_any_other() {
        BodyState s = new BodyState();
        BodyModel.charge(s, 0, 2400);
        assertEquals(2400, s.kcalOutToday(0), 1e-9);
        double before = s.kg;
        BodyModel.step(s, rest(MILD), BodyTuning.STANDARD, 0, 1);
        assertEquals(-1.0, s.kg - before, 0.001);
    }

    @Test
    void sos_return_body() {
        BodyState s = new BodyState();
        s.kg = 55;
        s.warmth = 0;
        s.water = 3;
        s.energy = 0.5;
        s.sos = 2;
        BodyModel.sosReturn(s, BodyTuning.STANDARD);
        assertEquals(19, s.bmi(), 1e-9);
        assertEquals(80, s.condition(), 1e-9);
        assertEquals(100, s.warmth);
        assertEquals(100, s.water);
        assertEquals(10, s.energy);
        assertEquals(0, s.sos);
    }

    @Test
    void strength_follows_condition_and_bmi() {
        assertEquals(100, Metabolism.strength(100, 23), 1e-9);
        assertEquals(50, Metabolism.strength(100, 20), 1e-9);
        assertEquals(0, Metabolism.strength(100, 17), 1e-9);
    }

    // --- M4 lane B ---------------------------------------------------------------------------------------------

    @Test
    void bmr_scaling() {
        assertEquals(1794, Metabolism.bmr(77, 183, 0), 1.0);
        // 10 per kg, 6.25 per cm, -5 per year of age; sex 1 is 166 lower.
        assertEquals(10, Metabolism.bmr(78, 183, 0) - Metabolism.bmr(77, 183, 0), 1e-9);
        assertEquals(6.25, Metabolism.bmr(77, 184, 0) - Metabolism.bmr(77, 183, 0), 1e-9);
        assertEquals(-166, Metabolism.bmr(77, 183, 1) - Metabolism.bmr(77, 183, 0), 1e-9);
        // Every activity rate scales with BMR over 1,794 and with the burn slider.
        for (Activity a : Activity.values()) {
            double full = Metabolism.burnPerMinute(a, 77, 183, 0, 100, 1.0) * 60;
            double thin = Metabolism.burnPerMinute(a, 62, 183, 0, 100, 1.0) * 60;
            assertEquals(Metabolism.kcalPerHour(a), full, 0.2, a.name());
            assertEquals(full * Metabolism.bmr(62, 183, 0) / Metabolism.bmr(77, 183, 0), thin, 0.2, a.name());
            assertEquals(full * 1.25, Metabolism.burnPerMinute(a, 77, 183, 0, 100, 1.25) * 60, 0.2, a.name());
        }
    }

    @Test
    void bmi17_window() {
        double std = starvationDays(BodyTuning.STANDARD);
        assertTrue(std >= 15 && std <= 21, "Standard: BMI 17 after " + std + " days");
        double gentle = starvationDays(BodyTuning.GENTLE);
        // About twice as late: Gentle burns half and moves weight at 3,500 kcal a kg (RECONCILE 135).
        assertTrue(gentle >= 1.8 * std && gentle <= 3.5 * std, "Gentle " + gentle + " days against Standard " + std);
        assertTrue(starvationDays(BodyTuning.BRUTAL) < std);
    }

    /** The first game day (from 0) the scurvy illness shows, feeding enough calories and no vitamin C. */
    static double scurvyDay(BodyTuning t) {
        BodyState s = new BodyState();
        long start = 6 * 60;
        for (int day = 0; day < 60; day++) {
            long m = start + day * 1440L;
            BodyModel.drink(s, m, 3.0, -1);
            BodyModel.eat(s, t, m, 900, 1500, 0, 0, 0, 0);
            for (int h = 0; h < 24; h++) {
                long hm = m + h * 60L;
                if (h == 6) BodyModel.eat(s, t, hm, 900, 1500, 0, 0, 0, 0);
                BodyModel.Result r = BodyModel.step(s, rest(MILD).doing(DayPlan.CHORES.at(hm)), t, hm, 60);
                assertFalse(r.collapsed(), "collapsed on day " + day + ": " + r.collapse());
                if (IllnessModel.active(s, "scurvy", hm + 60)) return (hm + 60 - start) / 1440.0;
            }
        }
        return Double.NaN;
    }

    @Test
    void scurvy_day() {
        double d = scurvyDay(BodyTuning.STANDARD);
        assertTrue(d >= 30 && d <= 31, "scurvy showed on day " + d);
        // Vitamin C food keeps it off.
        BodyState s = new BodyState();
        s.vitC = 0.5;
        IllnessModel.addVitC(s, 3);
        assertEquals(3.5, s.vitC, 1e-9);
        assertEquals(30, (IllnessModel.VITC_FULL_DAYS), 0);
    }

    @Test
    void order_deterministic() {
        // The same illnesses added in either order, stepped by minutes or in closed form, end in the same body.
        BodyTuning t = BodyTuning.STANDARD;
        BodyState a = new BodyState(), b = new BodyState();
        IllnessModel.begin(a, "tapeworm", 0, 0.0, WellnessTuning.STANDARD);
        IllnessModel.begin(a, "botulism", 0, 0.5, WellnessTuning.STANDARD);
        IllnessModel.begin(a, "trichinella", 0, 1.0, WellnessTuning.STANDARD);
        IllnessModel.begin(b, "trichinella", 0, 1.0, WellnessTuning.STANDARD);
        IllnessModel.begin(b, "botulism", 0, 0.5, WellnessTuning.STANDARD);
        IllnessModel.begin(b, "tapeworm", 0, 0.0, WellnessTuning.STANDARD);
        assertEquals(a.ill, b.ill);
        for (BodyState s : new BodyState[] {a, b}) {
            BodyModel.eat(s, t, 0, 800, 2400, 0, 0, 0, 0);
            DayPlan.CHORES.run(s, rest(MILD), t, 0, 3 * 1440);
        }
        assertEquals(a.kg, b.kg, 0);
        assertEquals(a.parts.condition(), b.parts.condition(), 0);
        assertEquals(a.nausea, b.nausea, 0);
        assertEquals(a.water, b.water, 0);
        assertEquals(a.energy, b.energy, 0);
        // A second run from the same start is identical to the first.
        BodyState c = new BodyState();
        IllnessModel.begin(c, "tapeworm", 0, 0.0, WellnessTuning.STANDARD);
        IllnessModel.begin(c, "botulism", 0, 0.5, WellnessTuning.STANDARD);
        IllnessModel.begin(c, "trichinella", 0, 1.0, WellnessTuning.STANDARD);
        BodyModel.eat(c, t, 0, 800, 2400, 0, 0, 0, 0);
        DayPlan.CHORES.run(c, rest(MILD), t, 0, 3 * 1440);
        assertEquals(a.kg, c.kg, 0);
        assertEquals(a.parts.condition(), c.parts.condition(), 0);
    }
}

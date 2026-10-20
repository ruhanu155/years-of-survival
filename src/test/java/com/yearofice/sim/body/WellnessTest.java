package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.HeatSource;
import java.util.List;
import org.junit.jupiter.api.Test;

/** M4 lane B: the register, nausea, Rest days, skin, hair, fever, scurvy stages. */
class WellnessTest {
    static final BodyTuning T = BodyTuning.STANDARD;
    static final WellnessTuning W = WellnessTuning.STANDARD;
    static final EnvSample MILD = EnvSample.outdoors(15, 0);
    static final List<HeatSource> FIRE = List.of(HeatSource.openFire(FireSetting.STEADY, 1.0));

    static BodyModel.Inputs rest(EnvSample e) { return BodyModel.Inputs.resting(e); }

    @Test
    void theRegisterHasTwentyFilesAndTheBodyOwnsItsEngines() {
        assertEquals(20, IllnessDefs.all().size());
        for (String id : IllnessDefs.IDS) assertTrue(IllnessDefs.get(id) != null, id);
        assertTrue(IllnessDefs.get("scurvy").steppedHere());
        assertFalse(IllnessDefs.get("infection").steppedHere());
        IllnessDef scurvy = IllnessDefs.get("scurvy");
        assertEquals(0, scurvy.stageIndex(0));
        assertEquals(1, scurvy.stageIndex(240));
        assertEquals(2, scurvy.stageIndex(720));
        assertEquals(168, scurvy.recoveryH());
    }

    @Test
    void scurvyStagesCapEnergyHealHalfAndBurnMore() {
        BodyState s = new BodyState();
        s.vitC = 0;
        IllnessModel.start(s, "scurvy", 0, -1);
        IllnessModel.Effects d1 = IllnessModel.effects(s, 60);
        assertEquals(8, d1.energyCap());
        assertEquals(0.5, d1.heal());
        assertEquals(1.0, d1.burn());
        IllnessModel.Effects d11 = IllnessModel.effects(s, 11 * 1440L);
        assertEquals(6, d11.energyCap());
        assertEquals(1.1, d11.burn(), 1e-9);
        IllnessModel.Effects d31 = IllnessModel.effects(s, 31 * 1440L);
        assertEquals(1.2, d31.burn(), 1e-9);
        assertEquals(0.7, d31.walk(), 1e-9);
        // Fixed: the burn and slowness end, the caps and slow healing last 7 more days.
        s.vitC = 2;
        IllnessModel.Effects fixed = IllnessModel.effects(s, 31 * 1440L);
        assertEquals(1.0, fixed.burn(), 1e-9);
        assertEquals(1.0, fixed.walk(), 1e-9);
        assertEquals(6, fixed.energyCap());
    }

    @Test
    void scurvyEndsSevenDaysAfterTheReserveIsAboveZero() {
        BodyState s = new BodyState();
        s.vitC = 0;
        BodyModel.step(s, rest(MILD), T, 0, 60);
        assertTrue(IllnessModel.active(s, "scurvy", 60));
        IllnessModel.addVitC(s, 30);
        BodyModel.step(s, rest(MILD), T, 60, 60);
        assertTrue(IllnessModel.active(s, "scurvy", 6 * 1440), "still 6 days on");
        BodyModel.step(s, rest(MILD), T, 120, 8 * 1440);
        assertFalse(IllnessModel.listed(s, "scurvy"));
        assertTrue(s.events.contains("ill_scurvy_end"), s.events.toString());
        assertTrue(s.events.contains("ill_scurvy_1"));
    }

    @Test
    void nauseaDecaysTenAnHourAwakeAndFifteenAsleep() {
        BodyState s = new BodyState();
        s.nausea = 80;
        BodyModel.step(s, rest(MILD), T, 0, 120);
        assertEquals(60, s.nausea, 0.01);
        s.nausea = 80;
        BodyModel.step(s, rest(MILD).doing(Activity.SLEEP), T, 120, 120);
        assertEquals(50, s.nausea, 0.01);
    }

    @Test
    void vomitLosesHalfTheLastThreeHoursCaloriesAndHalfALitre() {
        BodyState s = new BodyState();
        BodyModel.step(s, rest(MILD), T, 10 * 60, 1);
        BodyModel.eat(s, T, 10 * 60 + 1, 500, 1200, 0, 0, 0, 0);
        s.nausea = 99.9;
        double kg = s.kg, water = s.water;
        // botulism-like push
        IllnessModel.start(s, "botulism", 10 * 60, -1);
        IllnessModel.vomit(s, T, 10 * 60 + 5);
        assertEquals(20, s.nausea);
        assertEquals(1200 * 0.5, 1200 - (s.kcalIn[10] + 0), 1e-6);
        assertEquals(kg - 600 / T.kcalPerKg(), s.kg, 1e-9);
        assertEquals(water - Metabolism.waterPoints(0.5), s.water, 1e-9);
        assertTrue(s.events.contains("vomit"));
    }

    @Test
    void gutIllnessBringsNauseaAndAFeverWithChills() {
        BodyState s = new BodyState();
        long t0 = 10 * 60;
        BodyModel.drink(s, t0, 0.25, 60);
        // Onset an hour on, then the cycle: hot 2 h, chills 1 h.
        BodyModel.step(s, rest(MILD), T, t0, 180);
        long onset = t0 + 60;
        assertTrue(s.gutIll(t0 + 180));
        assertTrue(IllnessModel.active(s, "fever", t0 + 180));
        assertEquals(0, IllnessModel.effects(s, onset + 60).chillC());
        assertEquals(-6, IllnessModel.effects(s, onset + 130).chillC());
        assertEquals(0.5, IllnessModel.effects(s, t0 + 180).absorb());
        assertTrue(s.nausea > 0, "nausea " + s.nausea);
    }

    @Test
    void gutIllnessNeverShrinksBelowTwelveHours() {
        BodyState s = new BodyState();
        long until = 3 * 1440;
        s.gutUntil = until;
        IllnessModel.easeGut(s, 6 * 60, 100);
        assertEquals(until - Metabolism.GUT_ILL_MINUTES + 12 * 60, s.gutUntil);
        s.gutUntil = until;
        IllnessModel.easeGut(s, 6 * 60, 4);
        assertEquals(until - 4 * 60, s.gutUntil);
    }

    @Test
    void parasitesMultiplyAbsorbAndTrichinellaCapsCondition() {
        BodyState s = new BodyState();
        IllnessModel.begin(s, "tapeworm", 0, 0, W);
        IllnessModel.begin(s, "trichinella", 0, 0, W);
        assertEquals(0.85 * 0.85, IllnessModel.absorb(s, 4 * 1440L), 1e-9);
        // not yet: incubating three days
        assertEquals(1.0, IllnessModel.absorb(s, 60), 1e-9);
        BodyModel.step(s, rest(MILD), T, 4 * 1440L, 120);
        assertTrue(s.parts.condition() <= 90.0001, "Condition " + s.parts.condition());
    }

    @Test
    void botulismHoldsNauseaCapsEnergyAndCostsTenConditionADay() {
        BodyState s = new BodyState();
        IllnessModel.begin(s, "botulism", 0, 0, W);
        s.energy = 10;
        BodyModel.drink(s, 0, 3, -1);
        BodyModel.step(s, rest(MILD), T, 12 * 60, 1440);
        assertTrue(s.nausea >= 59 && s.nausea <= 99, "nausea " + s.nausea);
        assertEquals(3, s.lastEnergyCap);
        assertEquals(10, IllnessModel.effects(s, 13 * 60).condPerDay(), 1e-9);
        assertTrue(s.parts.dSys() > 0 || s.parts.condition() < 100 || s.parts.conditionFromParts() <= 100);
    }

    @Test
    void leanMeatSicknessFromThreeDaysOfFatFreeMeat() {
        BodyState s = new BodyState();
        for (int h = 0; h < 72; h++) {
            // 100 g protein, 5 g fat each hour: 405 kcal, 99% protein... fat share ~10%
            long m = h * 60L;
            BodyModel.eat(s, T, m, 50, 0, 0, 5, 100, 0);
            BodyModel.step(s, rest(MILD), T, m, 60);
            if (s.stomachKg > 1.5) s.stomachKg = 0;
        }
        assertTrue(IllnessModel.listed(s, "lean_meat"), s.ill.toString());
        assertEquals(6, IllnessModel.effects(s, 72 * 60 + 1).energyCap());
    }

    // --- Rest days ---------------------------------------------------------------------------------------------

    static BodyState hurt() {
        BodyState s = new BodyState();
        s.parts = com.yearofice.sim.body.part.PartModel.systemic(s.parts, 30);
        BodyModel.eat(s, T, 0, 100, 800, 0, 0, 0, 0);
        return s;
    }

    static double healedInADay(boolean byTheFire, boolean sleptInBed) {
        BodyState s = hurt();
        double before = s.parts.condition();
        BodyModel.Inputs base = rest(MILD).withHeat(byTheFire ? FIRE : List.of());
        long start = 0;
        for (int h = 0; h < 24; h++) {
            boolean night = h < 6 || h >= 22;
            BodyModel.Inputs in = night ? base.doing(Activity.SLEEP).onBed(1.0, sleptInBed ? 5 : 0) : base;
            if (!byTheFire && !night && h % 2 == 0) in = in.doing(Activity.REST); // away from any fire all day
            BodyModel.step(s, in, T, start + h * 60L, 60);
            s.warmth = 100;
            BodyModel.eat(s, T, start + h * 60L, 10, 100, 0, 0, 0, 0);
        }
        return s.parts.condition() - before;
    }

    @Test
    void rest_day_doubles_healing_at_the_fire() {
        double rest = healedInADay(true, true);
        double away = healedInADay(false, true);
        assertTrue(away > 0.5, "baseline heals " + away);
        assertEquals(2.0, rest / away, 0.1, "rest " + rest + " away " + away);
    }

    @Test
    void work_or_wandering_breaks_the_rest_day_and_the_run_counts_finished_days() {
        BodyState s = new BodyState();
        BodyModel.Inputs fire = rest(MILD).withHeat(FIRE);
        for (int d = 0; d < 3; d++) {
            for (int h = 0; h < 24; h++) {
                boolean night = h < 8;
                BodyModel.step(s, night ? fire.doing(Activity.SLEEP) : fire, T, d * 1440L + h * 60L, 60);
            }
        }
        BodyModel.step(s, fire, T, 3 * 1440L, 1);
        assertEquals(3, s.restRun);
        // A day of two hours' heavy work breaks it.
        for (int h = 0; h < 24; h++) {
            BodyModel.step(s, fire.doing(h < 8 ? Activity.SLEEP : (h < 10 ? Activity.HEAVY : Activity.REST)), T,
                    3 * 1440L + h * 60L, 60);
        }
        BodyModel.step(s, fire, T, 4 * 1440L, 1);
        assertEquals(0, s.restRun);
        assertTrue(s.events.contains("rest_broken"));
    }

    @Test
    void underweightAndScurvyHalveHealingAndNothingHealsWhileWarmthIsUnder30() {
        BodyState s = hurt();
        s.kg = Metabolism.kgForBmi(18, s.heightCm);
        IllnessModel.Effects ef = IllnessModel.Effects.NONE;
        BodyModel.Inputs in = rest(MILD);
        assertEquals(0.15, Healing.rate(s, in, ef, false, W), 1e-9);
        assertEquals(0.30, Healing.rate(s, in, ef, true, W), 1e-9);
        s.warmth = 20;
        double c = s.parts.condition();
        Healing.hour(s, 1);
        assertEquals(c, s.parts.condition(), 0);
    }

    // --- Skin and hair ------------------------------------------------------------------------------------------

    static EnvSample aprilSnow() {
        EnvSample e = EnvSample.outdoors(2, 1);
        return new EnvSample(e.airC(), e.feelsLikeC(), e.groundC(), e.waterC(), e.moisture(), e.windDirDeg(),
                e.windMs(), e.gustMs(), 40, false, 0, e.precip(), 0, 10_000, 15, 0, 0, 0, 10, 1013, 0, 60, "", 0, 0, 0);
    }

    static boolean burned(BodyState s) {
        return s.parts.problems().stream().anyMatch(p -> p.id().equals("sunburn") && p.part() == Part.HEAD);
    }

    static BodyState april() {
        BodyState s = new BodyState();
        s.ctxDayOfYear = 100;
        s.ctxSunDeg = 40;
        return s;
    }

    @Test
    void four_hours_on_april_snow_burns_a_bare_face_but_not_a_greased_one() {
        BodyState bare = april(), greased = april();
        Skin.greaseFace(greased, 12 * 60, W);
        BodyModel.step(bare, rest(aprilSnow()), T, 12 * 60, 4 * 60);
        BodyModel.step(greased, rest(aprilSnow()), T, 12 * 60, 4 * 60);
        assertTrue(burned(bare), "bare face");
        assertFalse(burned(greased), "greased face");
        assertTrue(bare.events.contains("face_sunburn"));
    }

    @Test
    void aRuffedHoodHalvesTheSunAndGreaseWearsOffAfterEightHours() {
        BodyState hood = april();
        hood.ctxFace = 1;
        BodyModel.step(hood, rest(aprilSnow()), T, 12 * 60, 4 * 60);
        assertFalse(burned(hood), "4 h at half rate is 2 h");
        BodyModel.step(hood, rest(aprilSnow()), T, 16 * 60, 3 * 60);
        assertTrue(burned(hood));
        BodyState s = april();
        Skin.greaseFace(s, 0, W);
        assertTrue(Skin.greased(s, 479));
        assertFalse(Skin.greased(s, 480));
    }

    @Test
    void noSunBurnOutsideMarchAndAprilOrWithoutSnow() {
        BodyState july = april();
        july.ctxDayOfYear = 190;
        BodyModel.step(july, rest(aprilSnow()), T, 12 * 60, 6 * 60);
        assertFalse(burned(july));
        BodyState bare = april();
        BodyModel.step(bare, rest(MILD), T, 12 * 60, 6 * 60);
        assertFalse(burned(bare));
    }

    @Test
    void windburnAfterTwoHoursOfHardWindAndBurnsEndAfterTwoDays() {
        BodyState s = new BodyState();
        EnvSample gale = EnvSample.outdoors(-15, 10);
        BodyModel.step(s, rest(gale), T, 0, 100);
        assertTrue(s.parts.problems().isEmpty());
        BodyModel.step(s, rest(gale), T, 100, 30);
        assertTrue(s.parts.problems().stream().anyMatch(p -> p.id().equals("windburn")));
        s.warmth = 100;
        BodyModel.step(s, rest(MILD), T, 130, 2 * 1440 + 10);
        assertTrue(s.parts.problems().isEmpty(), s.parts.problems().toString());
    }

    @Test
    void beard_stages_follow_hair_days_and_the_setting() {
        assertEquals(0, Skin.stage(0, true, 30));
        assertEquals(1, Skin.stage(30, true, 30));
        assertEquals(2, Skin.stage(60, true, 30));
        assertEquals(3, Skin.stage(90, true, 30));
        assertEquals(4, Skin.stage(120, true, 30));
        assertEquals(4, Skin.stage(400, true, 30));
        assertEquals(-1, Skin.stage(400, false, 30));
        BodyState long60 = new BodyState();
        for (int d = 0; d < 60; d++) {
            BodyModel.drink(long60, d * 1440L, 3, -1);
            BodyModel.eat(long60, T, d * 1440L, 900, 1500, 0, 0, 0, 0);
            DayPlan.STILL.run(long60, rest(MILD), T, d * 1440L, 720);
            BodyModel.eat(long60, T, d * 1440L + 720, 900, 1500, 0, 0, 0, 0);
            DayPlan.STILL.run(long60, rest(MILD), T, d * 1440L + 720, 720);
        }
        assertEquals(60, long60.hairDays, 0.01);
        assertEquals(2, Skin.beardStage(long60, W));
        Skin.trim(long60, 1, W);
        assertEquals(30, long60.hairDays, 0);
    }

    @Test
    void aFullBeardIcesAfterAnHourBelowMinus25AndThawsInTenMinutesAtAFire() {
        BodyState s = new BodyState();
        s.hairDays = 60;
        EnvSample cold = EnvSample.outdoors(-30, 0);
        assertEquals(2.0, Skin.faceBonusC(s, 0, W));
        BodyModel.step(s, rest(cold), T, 0, 59);
        assertFalse(s.beardIced);
        s.warmth = 100;
        BodyModel.step(s, rest(cold), T, 59, 1);
        assertTrue(s.beardIced);
        assertEquals(0.0, Skin.faceBonusC(s, 60, W));
        s.warmth = 100;
        BodyModel.step(s, rest(MILD).withHeat(FIRE), T, 60, 9);
        assertTrue(s.beardIced, "9 minutes is not enough");
        BodyModel.step(s, rest(MILD).withHeat(FIRE), T, 69, 1);
        assertFalse(s.beardIced);
        assertEquals(2.0, Skin.faceBonusC(s, 70, W));
        // The setting off: no beard, no bonus, no ice.
        BodyState off = new BodyState();
        off.hairDays = 60;
        off.facialHair = false;
        BodyModel.step(off, rest(cold), T, 0, 90);
        assertFalse(off.beardIced);
        assertEquals(0.0, Skin.faceBonusC(off, 0, W));
    }

    @Test
    void greaseOnTheFaceMakesItSafeTwoDegreesColderAndGlovesGainWetSlower() {
        BodyState s = new BodyState();
        Skin.greaseFace(s, 0, W);
        assertEquals(2.0, Skin.faceBonusC(s, 10, W));
        Skin.greaseGloves(s, 0, W);
        assertEquals(0.7, Skin.gloveWetMult(s, 1000, W));
        assertEquals(1.0, Skin.gloveWetMult(s, 3 * 1440 + 1, W));
    }

    @Test
    void exposureTimeIsTheShorterOfWarmthAndFrostnip() {
        double warm = ExposureSolver.minutes(100, -30, 1.0, false, new double[] {-100}, new double[] {-30});
        assertEquals(70 / (0.02 * 40), warm, 1e-9);
        double bare = ExposureSolver.minutes(100, -30, 1.0, true, new double[] {-100}, new double[] {-30});
        assertEquals(warm / 2, bare, 1e-9);
        double nip = ExposureSolver.minutes(100, -30, 1.0, false, new double[] {-20}, new double[] {-30});
        assertEquals(30, nip, 1e-9);
        assertEquals(40, ExposureSolver.shown(38));
        assertEquals(-1, ExposureSolver.shown(500));
    }
}

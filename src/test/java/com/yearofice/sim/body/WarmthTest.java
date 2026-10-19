package com.yearofice.sim.body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.env.ShelterState;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Blueprint › Warmth, Clothing and the M4 test lines. */
class WarmthTest {

    private static int minutesToZero(ClothingState c, EnvSample env, List<HeatSource> heat, double coldScale) {
        double w = 100;
        for (int m = 1; m <= 600; m++) {
            FeelsLike f = Warmth.feelsLike(env, c, heat);
            w = Warmth.step(w, Warmth.Step.of(f, c, env, heat, coldScale));
            if (w <= 0) return m;
        }
        return -1;
    }

    @Test
    void soakedPlayerAtMinus20WithNoFireReachesZeroIn13To17Minutes() {
        ClothingState soaked = Wardrobe.start().withAllWet(1.0);
        int m = minutesToZero(soaked, EnvSample.outdoors(-20, 0), List.of(), BodyPreset.STANDARD.coldScale);
        assertTrue(m >= 13 && m <= 17, "reached 0 at minute " + m);
    }

    @Test
    void soakedPlayerLosesSixWarmthAMinute() {
        ClothingState soaked = Wardrobe.start().withAllWet(1.0);
        EnvSample env = EnvSample.outdoors(-20, 0);
        FeelsLike f = Warmth.feelsLike(env, soaked, List.of());
        assertEquals(-6.0, Warmth.ratePerMinute(Warmth.Step.of(f, soaked, env, List.of(), 1.0)), 1e-9);
    }

    @Test
    void aFireWithin3mEndsTheSoakedRule() {
        ClothingState soaked = Wardrobe.start().withAllWet(1.0);
        EnvSample env = EnvSample.outdoors(-20, 0);
        List<HeatSource> fire = List.of(HeatSource.openFire(FireSetting.HOT, 2.5));
        FeelsLike f = Warmth.feelsLike(env, soaked, fire);
        double r = Warmth.ratePerMinute(Warmth.Step.of(f, soaked, env, fire, 1.0));
        assertTrue(r > -6, "rate by the fire " + r);
        assertEquals(Warmth.rateFromFeel(f.totalC()), r, 1e-9);
    }

    @Test
    void brutalColdIsFasterAndGentleSlower() {
        ClothingState soaked = Wardrobe.start().withAllWet(1.0);
        EnvSample env = EnvSample.outdoors(-20, 0);
        int standard = minutesToZero(soaked, env, List.of(), BodyPreset.STANDARD.coldScale);
        assertTrue(minutesToZero(soaked, env, List.of(), BodyPreset.BRUTAL.coldScale) < standard);
        assertTrue(minutesToZero(soaked, env, List.of(), BodyPreset.GENTLE.coldScale) > standard);
    }

    @Test
    void aSoakedFleeceGivesExactly70PercentOfItsWarmth() {
        ClothingState dry = ClothingState.of(Wardrobe.FLEECE);
        ClothingState soaked = dry.withWet(Wardrobe.FLEECE.slot(), 1.0);
        assertEquals(5.0, Clothing.warmthC(dry), 1e-12);
        assertEquals(3.5, Clothing.warmthC(soaked), 1e-12);
    }

    @Test
    void aGarmentWithNoKeptValueUsesTheDefaultPointThree() {
        ClothingState shell = ClothingState.of(Wardrobe.RAIN_SHELL).withAllWet(1.0);
        assertEquals(0.3, Clothing.warmthC(shell), 1e-12);
    }

    @Test
    void wornOutGarmentsGiveHalfTheirWarmth() {
        ClothingState c = ClothingState.naked().wear(new WornGarment(Wardrobe.HIDE_COAT, 0.29));
        assertEquals(6.0, Clothing.warmthC(c), 1e-12);
        assertEquals(0.4 * 0.45, Clothing.windBlock(c), 1e-12);
    }

    @Test
    void octoberSetAtMinus30WithNoWindHoldsAboveTheHypothermiaLineWhileWorking() {
        EnvSample env = EnvSample.outdoors(-30, 0);
        FeelsLike f = Warmth.feelsLike(env, Wardrobe.october(), List.of(),
                WarmthContext.RESTING.with(Activity.HEAVY));
        assertTrue(f.totalC() >= 10, "feels-like " + f.totalC());
        assertEquals(0, Warmth.rateFromFeel(f.totalC()), 1e-12);
    }

    @Test
    void windChillIsCappedAndOnlyBelow10AndCutByWindBlock() {
        assertEquals(16, Warmth.windChillC(-10, 30, 1, 0), 1e-12);
        assertEquals(8, Warmth.windChillC(-10, 10, 1, 0), 1e-12);
        assertEquals(0, Warmth.windChillC(10, 10, 1, 0), 1e-12);
        assertEquals(4, Warmth.windChillC(-10, 10, 0.5, 0), 1e-12);
        assertEquals(0.8, Warmth.windChillC(-10, 10, 1, 0.9), 1e-12);
    }

    @Test
    void feelsLikeShowsEachTerm() {
        EnvSample env = EnvSample.outdoors(-10, 5);
        ClothingState c = Wardrobe.start();
        FeelsLike f = Warmth.feelsLike(env, c, List.of(HeatSource.openFire(FireSetting.STEADY, 1)),
                new WarmthContext(ShelterState.LEAN_TO, Activity.WALK, -1));
        double wind = 4 * 0.5 * (1 - Clothing.windBlock(c));
        assertEquals(wind, f.windChillC(), 1e-12);
        assertEquals(0, f.wetPenaltyC(), 1e-12);
        assertEquals(3, f.shelterC(), 1e-12);
        assertEquals(18, f.fireC(), 1e-12);
        assertEquals(-10 - wind + Clothing.warmthC(c) + 3 + 18 + 3 - 1, f.totalC(), 1e-12);
    }

    @Test
    void insideARoomTheRoomTemperatureIsTheAirWithNoWindOrShelter() {
        EnvSample env = EnvSample.outdoors(-30, 12).inRoom(7, 15);
        FeelsLike f = Warmth.feelsLike(env, ClothingState.naked(), List.of(HeatSource.stove(1)),
                WarmthContext.RESTING.with(ShelterState.SNOW_TRENCH));
        assertEquals(15, f.airC(), 1e-12);
        assertEquals(0, f.windChillC(), 1e-12);
        assertEquals(0, f.shelterC(), 1e-12);
        assertEquals(10, f.fireC(), 1e-12);
    }

    @Test
    void nullHeatSourcesReadTheEnvLayersHeatIndex() {
        EnvSample env = EnvSample.outdoors(0, 0).withHeatBonus(12);
        assertEquals(12, Warmth.feelsLike(env, ClothingState.naked(), null).fireC(), 1e-12);
    }

    @Test
    void warmthRateFollowsTheThreeBands() {
        assertEquals(-0.02 * 20, Warmth.rateFromFeel(-10), 1e-12);
        assertEquals(0, Warmth.rateFromFeel(10), 1e-12);
        assertEquals(0, Warmth.rateFromFeel(18), 1e-12);
        assertEquals(0.05 * 4, Warmth.rateFromFeel(22), 1e-12);
    }

    @Test
    void intakeChangesWarmthAtOnce() {
        assertEquals(55, Warmth.afterIntake(50, Warmth.Intake.HOT_DRINK), 1e-12);
        assertEquals(42, Warmth.afterIntake(50, Warmth.Intake.RAW_SNOW_HALF_LITRE), 1e-12);
        assertEquals(60, Warmth.afterIntake(60, Warmth.Intake.VERY_COLD_WATER_HALF_LITRE), 1e-12);
        assertEquals(38, Warmth.afterIntake(40, Warmth.Intake.VERY_COLD_WATER_HALF_LITRE), 1e-12);
        assertEquals(100, Warmth.afterIntake(98, Warmth.Intake.HOT_DRINK), 1e-12);
    }

    @Test
    void hypothermiaStagesAndShivering() {
        assertEquals(Hypothermia.WARM, Hypothermia.of(70));
        assertEquals(Hypothermia.COOL, Hypothermia.of(50));
        assertEquals(Hypothermia.SHIVERING, Hypothermia.of(49.9));
        assertEquals(Hypothermia.SLOWED, Hypothermia.of(29));
        assertEquals(Hypothermia.COLLAPSE_RISK, Hypothermia.of(5));
        assertEquals(Hypothermia.COLLAPSE, Hypothermia.of(0));
        assertEquals(0, Hypothermia.shiverKcalPerHour(50), 1e-12);
        assertEquals(225, Hypothermia.shiverKcalPerHour(40), 1e-12);
        assertEquals(300, Hypothermia.shiverKcalPerHour(20), 1e-12);
        assertEquals(0.7, Hypothermia.SLOWED.walkFactor(), 1e-12);
        assertFalse(Hypothermia.SLOWED.canRun());
        assertEquals(1.5, Hypothermia.SLOWED.workTimeFactor(), 1e-12);
        assertEquals(0.75, Hypothermia.SHIVERING.fineWindowFactor(), 1e-12);
        assertEquals(30, Hypothermia.COLLAPSE_RISK.stumbleEverySeconds());
    }

    @Test
    void wadingInThighWaterAt2CostsThreePointSixAMinute() {
        assertEquals(-3.6, ColdWater.contactWarmthPerMin(2, ColdWater.Depth.THIGH), 1e-12);
        assertEquals(-6, ColdWater.swimWarmthPerMin(2), 1e-12);
        assertEquals(-1, ColdWater.swimWarmthPerMin(16), 1e-12);
        assertEquals(10, ColdWater.usefulMinutes(3), 1e-12);
        assertEquals(8, ColdWater.usefulMinutes(3, 0.8), 1e-12);
        assertEquals(12, ColdWater.usefulMinutes(10, 0.6), 1e-12);
        assertTrue(Double.isInfinite(ColdWater.usefulMinutes(18, 0.6)));
    }

    @Test
    void swimmingReplacesTheFormulaAndHotPoolsWarm() {
        ClothingState c = Wardrobe.start().withAllWet(1);
        EnvSample env = EnvSample.outdoors(5, 0);
        FeelsLike f = Warmth.feelsLike(env, c, List.of());
        Warmth.Step s = Warmth.Step.of(f, c, env, List.of(), 1).inWater(2, ColdWater.Depth.SWIM);
        assertEquals(-6, Warmth.ratePerMinute(s), 1e-12);
        assertEquals(100, Warmth.step(0, s.inWater(38, ColdWater.Depth.SWIM), 10), 1e-12);
    }
}

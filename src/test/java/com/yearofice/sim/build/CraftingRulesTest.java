package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Crafting and building in depth › Tiers and gates, Practice, Tool durability, and the A7 box rules. */
class CraftingRulesTest {

    @Test
    void startIsOpenAndGatesNeverClose() {
        assertTrue(Gates.isOpen(Gates.NEW_WORLD, Tier.START));
        assertFalse(Gates.isOpen(Gates.NEW_WORLD, Tier.BUSHCRAFT));
        int m = Gates.open(Gates.NEW_WORLD, Tier.IRON);
        assertTrue(Gates.isOpen(m, Tier.IRON), "gates open independently: Iron opens with Hide and bone shut");
        assertFalse(Gates.isOpen(m, Tier.HIDE_AND_BONE));
        assertEquals(m, Gates.open(m, Tier.IRON), "opening twice changes nothing");
        assertEquals(2, Gates.count(m));
        assertTrue(Gates.isOpen(Gates.open(0, Tier.HOMESTEAD), Tier.START), "an empty mask still has Start open");
    }

    @Test
    void tierIdsRoundTrip() {
        for (Tier t : Tier.values()) assertEquals(t, Tier.byId(t.id()).orElseThrow());
        assertEquals(5, Tier.values().length);
        assertTrue(Tier.byId("steel").isEmpty());
    }

    @Test
    void skillTableMatchesTheDoc() {
        assertEquals(10, Skill.FIRE.n);
        assertEquals(25, Skill.KNAPPING.n);
        assertEquals(60, Skill.SPLITTING.n);
        assertEquals(300, Skill.HAULING.n);
        assertEquals(17, Skill.values().length);
        assertEquals(Skill.LOG_BUILDING, Skill.byId("log_building").orElseThrow());
    }

    /** Practice test: 10 bow-drill fires over 5 or more days bring game time to x0.6 of the first fire's. */
    @Test
    void tenFiresOverFiveDaysReachPracticed() {
        int n = Skill.FIRE.n;
        double reps = 0;
        int days = 0;
        while (reps < n) {
            double today = 0;
            while (true) {
                double g = Practice.gain(today, Practice.JOB_REP, n);
                if (g <= 0) break;
                today += g;
                reps += g;
            }
            days++;
            if (days > 50) throw new AssertionError("never practiced");
        }
        assertEquals(5, days, "N ÷ 5 = 2 reps a day, so 10 reps take exactly 5 days");
        assertEquals(0.6, Practice.timeFactor(reps, n), 1e-9);
        assertEquals(1.0, Practice.timeFactor(0, n), 1e-9);
        assertEquals(Practice.Word.PRACTICED, Practice.word(reps, n));
    }

    @Test
    void dailyCapIsAFifthOfNWithAFloorOfTwo() {
        assertEquals(2.0, Practice.dailyCap(8), 1e-9);
        assertEquals(2.0, Practice.dailyCap(10), 1e-9);
        assertEquals(12.0, Practice.dailyCap(60), 1e-9);
        assertEquals(0.5, Practice.gain(1.5, 1.0, 10), 1e-9, "the cap cuts the rep");
        assertEquals(0.0, Practice.gain(2.0, 1.0, 10), 1e-9);
    }

    @Test
    void previewWords() {
        assertEquals(Practice.Word.CLUMSY, Practice.word(0, 30));
        assertEquals(Practice.Word.CLUMSY, Practice.word(9.9, 30));
        assertEquals(Practice.Word.HANDY, Practice.word(10, 30));
        assertEquals(Practice.Word.HANDY, Practice.word(29.5, 30));
        assertEquals(Practice.Word.PRACTICED, Practice.word(30, 30));
        assertEquals(Practice.Word.PRACTICED, Practice.word(99, 30), "reps past N still read Practiced");
    }

    /** The doc's example recipe: a longbow takes 240 min of light work for 400 kcal; a bow drill 20 min of medium work for 83. */
    @Test
    void kcalIsTheEffortRateOverTheBaseTime() {
        assertEquals(400, CraftMath.kcal(Effort.LIGHT, 240, 1, 1, 1), 1e-9);
        assertEquals(83, CraftMath.kcal(Effort.MEDIUM, 20, 1, 1, 1), 0.5);
        assertEquals(450, CraftMath.kcal(Effort.HIGH, 60, 1, 1, 1), 1e-9);
    }

    @Test
    void practiceSpeedsTheJobButNeverTheCalories() {
        double kcalClumsy = CraftMath.kcal(Effort.MEDIUM, 60, 1, 1, 1);
        double kcalPracticed = CraftMath.kcal(Effort.MEDIUM, 60, 1, 1, 1);
        assertEquals(kcalClumsy, kcalPracticed, 1e-9);
        assertEquals(60, CraftMath.minutes(60, 1, 1, 1, 0, 25), 1e-9);
        assertEquals(36, CraftMath.minutes(60, 1, 1, 1, 25, 25), 1e-9);
        assertEquals(120, CraftMath.minutes(60, 1, CraftMath.lightMult(true), 1, 0, 25), 1e-9, "the dark doubles the time");
        assertEquals(2 * kcalClumsy, CraftMath.kcal(Effort.MEDIUM, 60, 1, CraftMath.lightMult(true), 1), 1e-9);
    }

    @Test
    void bodySlowdownsMultiply() {
        assertEquals(1.0, CraftMath.bodyMult(8, 50, 1), 1e-9);
        assertEquals(1.5, CraftMath.bodyMult(2.9, 50, 1), 1e-9);
        assertEquals(0.9, CraftMath.bodyMult(8, 71, 1), 1e-9);
        assertEquals(1.1, CraftMath.bodyMult(8, 29, 1), 1e-9);
        assertEquals(1.5 * 1.1 * 1.2, CraftMath.bodyMult(1, 10, 1.2), 1e-9);
    }

    @Test
    void edgeWearAndDullness() {
        double perUse = ToolWear.edgeLossPerUse(400);
        assertEquals(0.25, perUse, 1e-9, "a crate knife loses 100 ÷ 400 edge a use");
        assertEquals(0.0, ToolWear.afterUses(100, 400, 400), 1e-9, "400 uses take it from sharp to dull-as-zero");
        assertFalse(ToolWear.dull(30));
        assertTrue(ToolWear.dull(29.9));
        assertEquals(1.5, ToolWear.timeMult(10), 1e-9);
        assertEquals(1.3, ToolWear.kcalMult(10), 1e-9);
        assertEquals(1.0, ToolWear.timeMult(100), 1e-9);
        assertEquals(70, ToolWear.hone(10, ToolWear.HONE_STONE), 1e-9);
        assertEquals(100, ToolWear.hone(100, ToolWear.HONE_STONE), 1e-9, "a hone never dulls");
        assertEquals(0, ToolWear.edgeLossPerUse(0), 1e-9);
    }

    @Test
    void coldWearAndWarning() {
        assertEquals(2, ToolWear.durabilityCost(-31, true));
        assertEquals(1, ToolWear.durabilityCost(-31, false));
        assertEquals(1, ToolWear.durabilityCost(-29, true));
        assertTrue(ToolWear.worn(4000, 5000), "20% left is under the 25% warning");
        assertFalse(ToolWear.worn(0, 5000));
        assertTrue(ToolWear.worn(3750, 5000));
        assertFalse(ToolWear.worn(3749, 5000));
    }

    @Test
    void boxLiftCap() {
        assertTrue(BoxLift.check(12, 18, BoxLift.CAP_KG).ok(), "a chest with 18 kg inside is 30 kg in all and lifts");
        BoxLift.Verdict v = BoxLift.check(12, 18.1, BoxLift.CAP_KG);
        assertFalse(v.ok());
        assertEquals(0.1, v.overKg(), 1e-6);
        assertEquals(30.1, v.totalKg(), 1e-9);
    }

    @Test
    void frozenDownNeedsAllFourConditions() {
        assertTrue(BoxLift.frozenDown(24, BoxLift.FROZEN_AFTER_H, -5, true, false));
        assertFalse(BoxLift.frozenDown(23.9, BoxLift.FROZEN_AFTER_H, -5, true, false));
        assertFalse(BoxLift.frozenDown(30, BoxLift.FROZEN_AFTER_H, 1, true, false), "air above 0");
        assertFalse(BoxLift.frozenDown(30, BoxLift.FROZEN_AFTER_H, -5, false, false), "not on frozen ground");
        assertFalse(BoxLift.frozenDown(30, BoxLift.FROZEN_AFTER_H, -5, true, true), "inside a warm room");
        assertTrue(BoxLift.frozenDown(12, 12, -5, true, false), "the hours are a dial");
    }
}

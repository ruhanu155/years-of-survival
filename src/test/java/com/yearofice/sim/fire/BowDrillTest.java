package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.fire.BowDrillJudge.Claim;
import com.yearofice.sim.fire.BowDrillJudge.Facts;
import com.yearofice.sim.fire.BowDrillJudge.Outcome;
import com.yearofice.sim.fire.BowDrillJudge.Summary;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** The bow drill: Blueprint › The bow drill, step by step; Building a campfire (the method table); Practice. */
class BowDrillTest {
    static final FireTuning T = FireTuning.standard();
    static final double DT = 0.05;

    /**
     * A bot that strokes 1.5 times a second with pressure in the green band, putting each stroke on the beat with
     * probability {@code onBeat} and up to 0.4 s off it otherwise. Returns the real seconds to an ember, or -1.
     */
    static double secondsToEmber(double onBeat, boolean wetBoard, long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        BowDrill.HeatMeter m = new BowDrill.HeatMeter(wetBoard);
        double t = 0;
        int k = 0;
        while (t < 180) {
            double beat = k * BowDrill.BEAT_S;
            double at = rng.nextDouble() < onBeat ? beat + (rng.nextDouble() - 0.5) * 0.2 : beat + 0.2 + rng.nextDouble() * 0.2;
            while (t + DT <= at) {
                t += DT;
                m.tick(DT);
            }
            m.stroke(Math.max(0, at), 0.55);
            if (m.ember()) return at + BowDrill.SEAT_S;
            k++;
        }
        return -1;
    }

    @Test
    void steadyStrokesTake60To90Seconds() {
        double perfect = secondsToEmber(1.0, false, 1);
        assertTrue(perfect >= 58 && perfect <= 64, "perfect " + perfect);
        double sum = 0;
        for (int i = 0; i < 50; i++) {
            double s = secondsToEmber(0.7, false, 100 + i);
            assertTrue(s >= 60 && s <= 90, "average rhythm " + s);
            sum += s;
        }
        assertTrue(sum / 50 > 68 && sum / 50 < 82, "mean " + sum / 50);
        // 60–90 s of real strokes charge 6–9 game minutes on the close clock.
        assertEquals(6, BowDrill.gameMinutes(60), 1e-9);
        assertEquals(9, BowDrill.gameMinutes(90), 1e-9);
    }

    @Test
    void wetBoardNeverGivesAnEmber() {
        assertEquals(-1, secondsToEmber(1.0, true, 7));
        BowDrill.HeatMeter m = new BowDrill.HeatMeter(true);
        for (int i = 0; i < 200; i++) m.stroke(i * BowDrill.BEAT_S, 0.55);
        assertEquals(60, m.heat(), 1e-9);
        assertEquals(2, m.smoke() + 1, "thin smoke at 60, never thick");
    }

    @Test
    void pressureRules() {
        BowDrill.HeatMeter m = new BowDrill.HeatMeter(false);
        m.stroke(0, 0.2);
        assertEquals(0, m.heat(), 1e-9, "too little pressure gives no heat");
        for (int i = 1; i <= 30; i++) m.stroke(i * BowDrill.BEAT_S, 0.55);
        double before = m.heat();
        assertTrue(m.stroke(31 * BowDrill.BEAT_S, 0.95), "too much stalls");
        assertEquals(before - 15, m.heat(), 1e-9);
        assertTrue(m.stalled());
        m.stroke(31.5 * BowDrill.BEAT_S, 0.55);
        assertEquals(before - 15, m.heat(), 1e-9, "no strokes count while the string resets");
        m.tick(2.1);
        assertFalse(m.stalled());
        for (int i = 40; i < 70; i++) m.stroke(i * BowDrill.BEAT_S, 0.55);
        // Cooling: 8 points a second once two beats pass with no stroke.
        double h = m.heat();
        assertTrue(h > 20, "heat " + h);
        m.tick(BowDrill.COOL_AFTER_S);
        assertEquals(h, m.heat(), 1e-9, "no cooling within two beats");
        m.tick(1.0);
        assertEquals(h - 8, m.heat(), 1e-9);
    }

    @Test
    void autoChanceRisesWithPractice() {
        assertEquals(0.70, BowDrill.autoChance(0, 0, false, T), 1e-9);
        assertEquals(0.825, BowDrill.autoChance(5, 0, false, T), 1e-9);
        assertEquals(0.95, BowDrill.autoChance(10, 0, false, T), 1e-9);
        assertEquals(0.95, BowDrill.autoChance(40, 0, false, T), 1e-9);
        assertEquals(0.70 * 0.85, BowDrill.autoChance(0, 8, false, T), 1e-9);
        assertEquals(0.70, BowDrill.autoChance(0, 8, true, T), 1e-9);
        SplittableRandom rng = new SplittableRandom(42);
        int lit = 0;
        for (int i = 0; i < 1000; i++) if (rng.nextDouble() < BowDrill.autoChance(0, 0, false, T)) lit++;
        assertTrue(lit >= 650 && lit <= 750, "fresh bot lit " + lit);
    }

    static Summary goodStrokes() { return new Summary(Claim.FLAME, 100, 85, 100, 3, false); }

    static Facts facts(double elapsedS, int board, int tinder, double energy) { return new Facts(elapsedS, 0, board, tinder, energy); }

    @Test
    void judgeAcceptsAPlausibleFire() {
        BowDrillJudge.Verdict v = BowDrillJudge.judge(goodStrokes(), facts(75, 15, 10, 8), T);
        assertEquals(Outcome.FLAME, v.outcome());
        assertEquals(60, v.kcal());
        assertEquals(1, v.reps(), 1e-9);
        assertEquals(7.5, v.gameMinutes(), 1e-9);
    }

    @Test
    void judgeRejectsTheImpossible() {
        // Too fast: under 70% of the fastest legal run.
        assertEquals(Outcome.REJECTED, BowDrillJudge.judge(goodStrokes(), facts(30, 15, 10, 8), T).outcome());
        // Strokes that could not have built 100 heat.
        assertEquals(Outcome.REJECTED, BowDrillJudge.judge(new Summary(Claim.FLAME, 60, 60, 100, 3, false), facts(75, 15, 10, 8), T).outcome());
        // The held shortcut before Practiced.
        assertEquals(Outcome.REJECTED, BowDrillJudge.judge(new Summary(Claim.FLAME, 0, 0, 100, 3, true), facts(10, 15, 10, 8), T).outcome());
        // A rejected result still costs its kcal and counts as a failed try.
        BowDrillJudge.Verdict v = BowDrillJudge.judge(goodStrokes(), facts(30, 15, 10, 8), T);
        assertEquals(60, v.kcal());
        assertEquals(0.5, v.reps(), 1e-9);
        // At Practiced the 6-second hold is legal.
        Facts practiced = new Facts(9, 10, 15, 10, 4);
        assertEquals(Outcome.FLAME, BowDrillJudge.judge(new Summary(Claim.FLAME, 0, 0, 100, 3, true), practiced, T).outcome());
    }

    @Test
    void judgeAppliesWetWoodWetTinderAndEnergy() {
        assertEquals(Outcome.WET_BOARD, BowDrillJudge.judge(goodStrokes(), facts(75, 55, 10, 8), T).outcome());
        assertEquals(Outcome.WET_TINDER, BowDrillJudge.judge(goodStrokes(), facts(75, 15, 50, 8), T).outcome());
        assertEquals(Outcome.TOO_TIRED, BowDrillJudge.judge(goodStrokes(), facts(75, 15, 10, 3.5), T).outcome());
        // Quitting before a stroke costs nothing.
        BowDrillJudge.Verdict quit = BowDrillJudge.judge(new Summary(Claim.ABORTED, 0, 0, 0, 0, false), facts(1, 15, 10, 8), T);
        assertEquals(0, quit.kcal());
        assertEquals(0, quit.reps(), 1e-9);
    }

    @Test
    void practiceIsCappedPerDay() {
        assertEquals(2, FirePractice.dailyCap(10), 1e-9);
        assertEquals(1, FirePractice.gain(0, 1, 10), 1e-9);
        assertEquals(0.5, FirePractice.gain(1.5, 1, 10), 1e-9);
        assertEquals(0, FirePractice.gain(2, 0.5, 10), 1e-9);
        // So Practiced takes at least 5 game days.
        double reps = 0;
        int days = 0;
        while (reps < 10) {
            double today = 0;
            for (int i = 0; i < 6; i++) {
                double g = FirePractice.gain(today, 1, 10);
                today += g;
                reps += g;
            }
            days++;
        }
        assertEquals(5, days);
    }
}

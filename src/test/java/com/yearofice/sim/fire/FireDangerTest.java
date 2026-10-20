package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Every concept from the video › Fire danger. */
class FireDangerTest {
    static final FireTuning T = FireTuning.standard();

    @Test
    void indexIsTheFormula() {
        // 14 dry days, a 25 C high, 4 m/s wind: 14 * (1 + 1.0) * (1 + 0.5) = 42.
        assertEquals(42.0, FireDanger.index(14, 25, 4), 1e-9);
        // The high is floored at 15 and the dry days capped at 14.
        assertEquals(14.0, FireDanger.index(40, 3, 0), 1e-9);
        assertEquals(0.0, FireDanger.index(0, 30, 8), 1e-9);
    }

    @Test
    void levelBands() {
        assertEquals(FireDanger.Level.LOW, FireDanger.level(4.99));
        assertEquals(FireDanger.Level.MODERATE, FireDanger.level(5));
        assertEquals(FireDanger.Level.MODERATE, FireDanger.level(11.99));
        assertEquals(FireDanger.Level.HIGH, FireDanger.level(12));
        assertEquals(FireDanger.Level.HIGH, FireDanger.level(25));
        assertEquals(FireDanger.Level.EXTREME, FireDanger.level(25.01));
    }

    @Test
    void daysDryCountsBackToFiveMillimetres() {
        List<FireDanger.Day> past = List.of(new FireDanger.Day(0), new FireDanger.Day(4.9), new FireDanger.Day(5.0), new FireDanger.Day(0));
        assertEquals(2, FireDanger.daysDry(past));
        List<FireDanger.Day> dry = new ArrayList<>();
        for (int i = 0; i < 30; i++) dry.add(new FireDanger.Day(0));
        assertEquals(14, FireDanger.daysDry(dry));
    }

    @Test
    void lowOutsideTheWindow() {
        List<FireDanger.Day> dry = new ArrayList<>();
        for (int i = 0; i < 14; i++) dry.add(new FireDanger.Day(0));
        assertEquals(FireDanger.Level.EXTREME, FireDanger.today(60, 0, dry, 30, 6));
        assertEquals(FireDanger.Level.LOW, FireDanger.today(123, 0, dry, 30, 6), "from September 1");
        assertEquals(FireDanger.Level.LOW, FireDanger.today(5, 8, dry, 30, 6), "snow still lies");
    }

    @Test
    void spreadChanceTableByLevel() {
        assertEquals(2, T.spreadPct(FireDanger.Level.LOW));
        assertEquals(4, T.spreadPct(FireDanger.Level.MODERATE));
        assertEquals(8, T.spreadPct(FireDanger.Level.HIGH));
        assertEquals(15, T.spreadPct(FireDanger.Level.EXTREME));
    }

    @Test
    void spreadRollMatchesItsPercentOver1000Hours() {
        for (FireDanger.Level level : FireDanger.Level.values()) {
            SplittableRandom rng = new SplittableRandom(level.ordinal() + 11);
            int hits = 0;
            for (int i = 0; i < 20_000; i++) if (FireDanger.spreads(T.spreadPct(level), rng)) hits++;
            double got = hits / 20_000.0 * 100;
            assertEquals(T.spreadPct(level), got, 0.6, level.word);
        }
    }

    @Test
    void sixHoursAtHighAndExtreme() {
        // Fire danger table: 8% and 15% an hour make 39% and 62% over 6 game hours.
        for (var e : new Object[][] {{FireDanger.Level.HIGH, 0.39}, {FireDanger.Level.EXTREME, 0.62}}) {
            FireDanger.Level level = (FireDanger.Level) e[0];
            SplittableRandom rng = new SplittableRandom(99);
            int lit = 0, runs = 5000;
            for (int r = 0; r < runs; r++) {
                for (int h = 0; h < 6; h++) {
                    if (FireDanger.spreads(T.spreadPct(level), rng)) {
                        lit++;
                        break;
                    }
                }
            }
            assertEquals((double) e[1], lit / (double) runs, 0.03, level.word);
        }
        assertTrue(true);
    }
}

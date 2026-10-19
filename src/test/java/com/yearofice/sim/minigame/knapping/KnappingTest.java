package com.yearofice.sim.minigame.knapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.minigame.knapping.KnappingJudge.Result;
import com.yearofice.sim.minigame.knapping.KnappingJudge.Strike;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnappingTest {
    @Test
    void patternMatchShiftsAndMirrors() {
        KnappingPattern point = KnappingPattern.of(false, " # ", "###", "###");
        KnappingPattern grid = KnappingPattern.of(true,
                "     ",
                "  #  ",
                " ### ",
                " ### ",
                "     ");
        assertTrue(grid.matches(point));
        KnappingPattern hook = KnappingPattern.of(false, "## ", " ##");
        KnappingPattern mirrored = KnappingPattern.of(true,
                "     ",
                " ##  ",
                "##   ",
                "     ",
                "     ");
        assertTrue(mirrored.matches(hook));
        assertFalse(mirrored.matches(point));
        // Outside cells must equal default_on.
        KnappingPattern smallOn = KnappingPattern.of(true, "# ");
        assertFalse(grid.matches(smallOn));
    }

    @Test
    void cellsAndCounts() {
        KnappingPattern full = KnappingPattern.full();
        assertEquals(25, full.onCount());
        KnappingPattern one = full.with(0, 0, false);
        assertEquals(24, one.onCount());
        assertFalse(one.get(0, 0));
        assertEquals(one, full.with(0, 0, false));
    }

    private static Strike good(int i) { return new Strike(true, 70, i % 4, i % 5, i / 5 % 5); }

    private static Strike bad(int side) { return new Strike(true, 30, side, 2, 2); }

    @Test
    void fourFlakesMakeABlankSixFinish() {
        List<Strike> s = new ArrayList<>();
        for (int i = 0; i < 4; i++) s.add(good(i));
        assertEquals(Result.BLANK, KnappingJudge.judge(1, 1, 1, s, 0, false, null).result());
        s.add(good(5));
        s.add(good(6));
        KnappingJudge.Verdict v = KnappingJudge.judge(1, 1, 1, s, 0, false, null);
        assertEquals(Result.FINISHED, v.result());
        assertEquals(19, v.shape().onCount());
        // A miss off the platform takes nothing.
        assertEquals(0, KnappingJudge.judge(1, 1, 1, List.of(new Strike(false, 70, 0, 0, 0)), 0, false, null).goodFlakes());
    }

    @Test
    void secondBadStrikeOnACrackedSideShattersHalfTheTime() {
        int n = 2000, shattered = 0;
        for (int a = 0; a < n; a++) {
            if (KnappingJudge.judge(3, 9, a, List.of(bad(1), bad(1)), 0, false, null).result() == Result.SHATTERED) shattered++;
        }
        assertEquals(0.5, (double) shattered / n, 0.04);
        // Bad strikes on different sides only crack.
        assertEquals(Result.IN_PROGRESS, KnappingJudge.judge(3, 9, 1, List.of(bad(0), bad(1), bad(2)), 0, false, null).result());
        // Practiced: the first shatter-eligible strike on a side is free.
        for (int a = 0; a < 200; a++) {
            assertEquals(Result.IN_PROGRESS, KnappingJudge.judge(3, 9, a, List.of(bad(1), bad(1)), 25, false, null).result());
        }
    }

    @Test
    void practiceWidensShiveringNarrows() {
        assertEquals(60, KnappingJudge.window(0, false)[0], 1e-12);
        assertEquals(85, KnappingJudge.window(25, false)[1], 1e-12);
        double[] cold = KnappingJudge.window(0, true);
        assertEquals(15, cold[1] - cold[0], 1e-12);
        assertEquals(0.25, KnappingJudge.autoShatterChance(0), 1e-12);
        assertEquals(0.05, KnappingJudge.autoShatterChance(40), 1e-12);
    }

    @Test
    void targetShapeMustMatch() {
        KnappingPattern target = KnappingPattern.of(true,
                "#### ",
                "#####",
                "#####",
                "#####",
                "#####");
        List<Strike> s = new ArrayList<>();
        for (int i = 0; i < 6; i++) s.add(new Strike(true, 70, 0, 4, 0)); // the same corner six times
        assertEquals(Result.FINISHED, KnappingJudge.judge(1, 1, 1, s, 0, false, target).result());
        List<Strike> wrong = new ArrayList<>();
        for (int i = 0; i < 6; i++) wrong.add(good(i));
        assertEquals(Result.BLANK, KnappingJudge.judge(1, 1, 1, wrong, 0, false, target).result());
    }
}

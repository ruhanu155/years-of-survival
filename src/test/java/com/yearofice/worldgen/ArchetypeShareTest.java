package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Stage 1 (M0): over 200 seeds each archetype appears 25% ±5%, and every record passes its range check. */
class ArchetypeShareTest {
    static final int SEEDS = 200;

    @Test
    void eachArchetypeAboutAQuarter() {
        int[] count = new int[Archetype.values().length];
        for (long seed = 1; seed <= SEEDS; seed++) {
            SeedStage.SeedParams s = Fixture.PIPE.seed(seed, 0);
            count[s.archetype().ordinal()]++;
            List<String> fails = s.check(Fixture.PIPE.params());
            assertTrue(fails.isEmpty(), "seed " + seed + ": " + fails);
        }
        StringBuilder shares = new StringBuilder();
        for (Archetype a : Archetype.values()) {
            double share = (double) count[a.ordinal()] / SEEDS;
            shares.append(a.path()).append(' ').append(String.format("%.1f%% ", 100 * share));
            assertTrue(share >= 0.20 && share <= 0.30, a + " share " + share);
        }
        System.out.println("archetype shares over " + SEEDS + " seeds: " + shares);
    }

    @Test
    void attemptsDrawFreshParameters() {
        SeedStage.SeedParams a0 = Fixture.PIPE.seed(99, 0), a1 = Fixture.PIPE.seed(99, 1);
        assertTrue(a0.attemptSeed() != a1.attemptSeed());
        assertTrue(a0.seed() == a1.seed());
    }
}

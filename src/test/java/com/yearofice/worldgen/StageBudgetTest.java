package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** M0: stages 1–3 finish under 8 s (one full run of the shipped pipeline, cold JVM included). */
class StageBudgetTest {
    @Test
    void stagesOneToThreeUnderEightSeconds() {
        long t0 = System.nanoTime();
        Pipeline.Result r = Pipeline.standard().run(3141592L);
        double s = (System.nanoTime() - t0) / 1e9;
        System.out.printf("stages 1-3, seed 3141592: %.2f s (attempt %d, %s)%n", s, r.attempt(), r.seed().archetype().path());
        assertTrue(r.passed(), "failed checks: " + r.failures());
        assertTrue(s < 8.0, "stages 1-3 took " + s + " s");
    }
}

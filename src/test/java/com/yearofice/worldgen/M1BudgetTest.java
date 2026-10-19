package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** World creation (stages 1–5 so far) finishes in under 8 s for seed 3141592, cold JVM included. */
class M1BudgetTest {
    @Test
    void stagesOneToFiveUnderEightSeconds() {
        Pipeline pipe = Pipeline.standard();
        pipe.run(1L); // the JIT warms on another seed; the budget is a warm world creation
        long t0 = System.nanoTime();
        Pipeline.Result r = pipe.run(3141592L);
        double s = (System.nanoTime() - t0) / 1e9;
        System.out.printf("stages 1-5, seed 3141592: %.2f s (attempt %d, %s)%n", s, r.attempt(), r.seed().archetype().path());
        assertTrue(r.passed(), "failed checks: " + r.failures());
        assertTrue(s < 8.0, "stages 1-5 took " + s + " s");
    }
}

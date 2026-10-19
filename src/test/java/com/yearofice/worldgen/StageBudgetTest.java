package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * M0: stages 1–3 finish under 8 s (one full run of the shipped pipeline, cold JVM included). Measured in the
 * running thread's CPU time where the JVM offers it (the pipeline is single-threaded), so a busy shared machine
 * cannot fail it; wall time is printed beside it.
 */
class StageBudgetTest {
    @Test
    void stagesOneToThreeUnderEightSeconds() {
        java.lang.management.ThreadMXBean mx = java.lang.management.ManagementFactory.getThreadMXBean();
        boolean cpu = mx.isCurrentThreadCpuTimeSupported();
        long t0 = System.nanoTime(), c0 = cpu ? mx.getCurrentThreadCpuTime() : 0;
        Pipeline.Result r = Pipeline.standard().run(3141592L);
        double wall = (System.nanoTime() - t0) / 1e9;
        double s = cpu ? (mx.getCurrentThreadCpuTime() - c0) / 1e9 : wall;
        System.out.printf("stages 1-3, seed 3141592: %.2f s %s, %.2f s wall (attempt %d, %s)%n", s, cpu ? "CPU" : "wall", wall, r.attempt(),
                r.seed().archetype().path());
        assertTrue(r.passed(), "failed checks: " + r.failures());
        assertTrue(s < 8.0, "stages 1-3 took " + s + " s");
    }
}

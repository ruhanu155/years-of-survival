package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

/**
 * World creation finishes in under 8 s for seed 3141592 (warm JIT): stages 1–5 as at M1, and since M2 stage 6
 * (cover) and the chunk builder's bind too, which is what a new world waits for before its first chunk. Measured in
 * the running thread's CPU time where the JVM offers it (all of it runs on one thread), so a busy shared machine
 * cannot fail it; wall time is printed beside it.
 */
class M1BudgetTest {
    @Test
    void stagesOneToFiveUnderEightSeconds() {
        Pipeline pipe = Pipeline.standard();
        pipe.run(1L); // the JIT warms on another seed; the budget is a warm world creation
        ThreadMXBean mx = ManagementFactory.getThreadMXBean();
        boolean cpu = mx.isCurrentThreadCpuTimeSupported();
        long t0 = System.nanoTime(), c0 = cpu ? mx.getCurrentThreadCpuTime() : 0;
        Pipeline.Result r = pipe.run(3141592L);
        long c1 = cpu ? mx.getCurrentThreadCpuTime() : 0, t1 = System.nanoTime();
        Cover.Layers cover = Cover.run(r.seed(), r.landform(), r.erosion(), r.water(), pipe.params());
        ValleyWorld w = new ValleyWorld(pipe.params(), r, cover, pipe.setPieces());
        long c2 = cpu ? mx.getCurrentThreadCpuTime() : 0, t2 = System.nanoTime();
        double s5 = cpu ? (c1 - c0) / 1e9 : (t1 - t0) / 1e9, s6 = cpu ? (c2 - c0) / 1e9 : (t2 - t0) / 1e9;
        System.out.printf("stages 1-5, seed 3141592: %.2f s; with cover and bind %.2f s (%s; wall %.2f s) (attempt %d, %s, slice %s)%n",
                s5, s6, cpu ? "thread CPU" : "wall", (t2 - t0) / 1e9, r.attempt(), r.seed().archetype().path(), w.slice.complete());
        assertTrue(r.passed(), "failed checks: " + r.failures());
        assertTrue(s6 < 8.0, "world creation (stages 1-6 and the bind) took " + s6 + " s");
    }
}

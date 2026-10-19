package com.yearofice.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runs the shipped pipeline (stages 1–5) once per seed for all worldgen tests and keeps only the small numbers:
 * the layers of one seed are tens of megabytes, so they are summarised and dropped.
 */
final class Fixture {
    private Fixture() {}

    static final Pipeline PIPE = Pipeline.standard();

    record Summary(long levelSeed, int attempt, Archetype archetype, boolean passed, String failures,
                   int landformHash, int heightHash, Pipeline.Stats stats, int landformNaN, int coarseNaN,
                   float coarseMin, float coarseMax, M1Summary m1) {}

    /** The M1 seeds: 0–15 hold every archetype four times (the stratified draw), plus the M0 golden seeds. */
    static final long[] M1_SEEDS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 42, 3141592};

    private static final Map<Long, Summary> CACHE = new LinkedHashMap<>();

    static synchronized Summary of(long levelSeed) {
        return CACHE.computeIfAbsent(levelSeed, s -> summarise(PIPE.run(s)));
    }

    static Summary summarise(Pipeline.Result r) {
        float cmin = Float.MAX_VALUE, cmax = -Float.MAX_VALUE;
        for (float v : r.landform().coarse()) { cmin = Math.min(cmin, v); cmax = Math.max(cmax, v); }
        return new Summary(r.seed().levelSeed(), r.attempt(), r.seed().archetype(), r.passed(),
                String.join("; ", r.failures()), r.landformHash(), r.heightHash(), r.stats(),
                Pipeline.countNaN(r.landform().height()), Pipeline.countNaN(r.landform().coarse()), cmin, cmax,
                r.water() == null ? null : M1Summary.of(r, PIPE));
    }
}

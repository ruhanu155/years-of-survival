package com.yearofice.sim.food;

import com.yearofice.sim.decay.Thresholds;

/**
 * Where placer reaches lie (video 2, A10): rust-stained gravel on cut banks and riffles, kept {@code minStartM} or
 * more from the start so the iron-distance test holds. A reach is one cell of the {@code cellM} grid whose seeded draw
 * falls under {@code share}; the draw also keys the breakup re-deposit, so each year's breakup fills the reach again.
 * The parameters are the {@code placer} entry of {@code worldgen_params/valley.json}. Pure Java; original Year of Ice code.
 */
public record Placer(int cellM, double share, double minStartM, int quantityKg) {

    public Placer {
        if (cellM < 2 || !(share >= 0 && share <= 1) || !(minStartM >= 0) || quantityKg < 1) throw new IllegalArgumentException("placer numbers");
    }

    /** Is the block column (x, z) inside a placer reach, given the start site (sx, sz)? */
    public boolean reachAt(long seed, int x, int z, int sx, int sz) {
        double dx = x - sx, dz = z - sz;
        if (dx * dx + dz * dz < minStartM * minStartM) return false;
        int cx = Math.floorDiv(x, cellM), cz = Math.floorDiv(z, cellM);
        return Thresholds.stream(seed, "placer_reach", cx, 0, cz, 0).nextDouble() < share;
    }

    /** Black sand left in a reach this year: refilled at every breakup (a new {@code year}), drained by panning. */
    public static double remainingKg(double fullKg, double takenThisYearKg) { return Math.max(0, fullKg - takenThisYearKg); }
}

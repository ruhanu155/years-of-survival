package com.yearofice.sim.food;

import com.yearofice.sim.decay.Thresholds;

/**
 * The two racks' batch rules, one game hour at a time (Blueprint › Preserving in detail: Drying; The smoker).
 *
 * <p>Drying: strips need {@code dryHours} dry hours (2 dry days of sun or wind). An hour of uncovered rain adds
 * {@code rainPenaltyH} hours to the batch, and {@code rainResetH} rain hours in a row (a full day of rain) send it back
 * to the start. Smoking: hot smoke runs 8–12 game hours, drawn once per batch from a named stream; a hour with the
 * fire out, or in wind over {@code smokeWindMs}, adds nothing.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class Racks {
    private Racks() {}

    /** A drying batch: hours of drying banked and the run of rain hours in a row. */
    public record Drying(double progressH, int rainRun) {
        public static Drying start() { return new Drying(0, 0); }

        public Drying hour(boolean uncoveredRain, RackTuning t) {
            if (!uncoveredRain) return new Drying(progressH + 1, 0);
            int run = rainRun + 1;
            if (run >= t.rainResetH()) return new Drying(0, run);
            // the hour passes (+1) and costs rainPenaltyH more than it gave: net -(penalty - 1)
            return new Drying(Math.max(0, progressH + 1 - t.rainPenaltyH()), run);
        }

        public boolean done(RackTuning t) { return progressH >= t.dryHours(); }
    }

    /** A smoking batch: hours of hot smoke banked against the hours this batch needs. */
    public record Smoking(double progressH, double neededH) {
        public static Smoking start(long seed, int x, int y, int z, long batch, RackTuning t) {
            double u = Thresholds.stream(seed, "smoke_rack", x, y, z, batch).nextDouble();
            return new Smoking(0, t.smokeMinH() + (t.smokeMaxH() - t.smokeMinH()) * u);
        }

        public Smoking hour(boolean fireHot, double windMs, RackTuning t) {
            if (!fireHot || windMs > t.smokeWindMs()) return this;
            return new Smoking(progressH + 1, neededH);
        }

        public boolean done() { return progressH >= neededH; }
    }

    /** Flies lay eggs on drying strips above 15 C unless a smoky fire burns within 3 m. */
    public static boolean fliesLayEggs(double airC, boolean smokyFireWithin3m) { return airC > 15.0 && !smokyFireWithin3m; }
}

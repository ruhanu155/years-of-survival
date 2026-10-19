package com.yearofice.sim.ecosystem;

import com.yearofice.sim.decay.Thresholds;

/**
 * The hare cycle (Blueprint › Populations): hare K is multiplied each year by a fixed 10-step table, starting at a
 * phase drawn per seed. Pure Java.
 */
public final class HareCycle {
    private HareCycle() {}

    public static final double[] TABLE = {0.3, 0.5, 0.9, 1.5, 2.2, 2.5, 1.8, 0.9, 0.4, 0.25};

    /** The seed's phase, 0–9: the cycle year of the run's first year. */
    public static int phase(long seed) { return Thresholds.stream(seed, "hare_cycle").nextInt(TABLE.length); }

    /** Cycle year (0–9) of run year {@code year}. */
    public static int cycleYear(int phase, long year) { return (int) Math.floorMod(phase + year, TABLE.length); }

    public static double multiplier(int phase, long year) { return TABLE[cycleYear(phase, year)]; }
}

package com.yearofice.sim.fishing;

/**
 * The fish weir's fence (Blueprint › Fishing methods: a weir across a creek funnels fish into a willow basket).
 * Stakes in an unbroken line either side of the basket form the fence; the basket alone is the M5 weir and keeps
 * its 0.2 mean, and each metre of fence across the channel raises the take up to {@link #MAX_BONUS}. Pure Java.
 */
public final class WeirFence {
    private WeirFence() {}

    /** Most the fence adds to the basket's hourly mean (×2.5 in all with a fence across the channel). */
    public static final double MAX_BONUS = 1.5;
    /** A fence needs at least this many stakes in all to count as a fence. */
    public static final int MIN_STAKES = 3;

    /** Share of the channel the fence closes: stakes plus the basket's gap over the width in blocks, capped at 1. */
    public static double coverage(int stakes, double channelWidthBlocks) {
        if (stakes < MIN_STAKES || channelWidthBlocks <= 0) return 0;
        return Math.min(1.0, (stakes + 1) / channelWidthBlocks);
    }

    /** The multiplier on the weir's hourly mean: 1 for a lone basket, up to 2.5 for a closed fence. */
    public static double multiplier(int stakes, double channelWidthBlocks) {
        return 1.0 + MAX_BONUS * coverage(stakes, channelWidthBlocks);
    }
}

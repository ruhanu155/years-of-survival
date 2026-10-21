package com.yearofice.sim.world;

import java.util.SplittableRandom;

/**
 * An unvisited camp ages by its pieces, not by a script (Decay and time › The home after 2, 5, 10 and 30 years). Each
 * piece rolls once for the whole absence at a chance of {@code 1 − (1 − r)^years} for its rot rate {@code r} a year, from
 * a stable hash of the seed and place, so the same camp aged twice is block for block the same and live equals
 * catch-up. Rates are percent a year, as in {@code data_maps/block/decay.json}.
 */
public final class OldCamp {
    private OldCamp() {}

    /** The chance a piece with this rate (percent a year) has rotted or fallen after {@code years}. */
    public static double chance(double ratePctPerYear, double years) { return 1 - Math.pow(1 - ratePctPerYear / 100.0, years); }

    public static boolean lost(long seed, int x, int y, int z, double ratePctPerYear, double years) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + y * 0x165667B19E3779F9L + z * 0x27D4EB2F165667C5L;
        return new SplittableRandom(h).nextDouble() < chance(ratePctPerYear, years);
    }
}

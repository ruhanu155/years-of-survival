package com.yearofice.sim.build;

/**
 * Wood and thatch left to the weather (Decay and time › Wood): a piece rots at a yearly rate that depends on its
 * exposure, so the chance it is gone after {@code years} is 1 - exp(-rate × years). Pure; the roll is a hash of the
 * position, so ageing a cabin twice gives the same ruin.
 */
public final class OutpostDecay {
    private OutpostDecay() {}

    /** Chance a piece rotting {@code ratePctPerYear} percent a year is lost after {@code years}. */
    public static double lossChance(double ratePctPerYear, double years) {
        if (ratePctPerYear <= 0 || years <= 0) return 0;
        return 1 - Math.exp(-ratePctPerYear / 100.0 * years);
    }

    /** A fixed 0-1 roll for one block of one structure. */
    public static double roll(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 33; h *= 0xFF51AFD7ED558CCDL; h ^= h >>> 33; h *= 0xC4CEB9FE1A85EC53L; h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Whether the piece at the position is lost at this age. */
    public static boolean lost(long seed, int x, int y, int z, double ratePctPerYear, double years) {
        return roll(seed, x, y, z) < lossChance(ratePctPerYear, years);
    }
}

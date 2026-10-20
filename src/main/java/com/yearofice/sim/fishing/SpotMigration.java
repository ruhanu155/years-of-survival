package com.yearofice.sim.fishing;

/**
 * Fish moving in from neighbouring water to refill a fished-down spot (RECONCILE 237: a stock that only regrew
 * monthly ran out in a week and stayed out). Each game day a share {@code m} of the gap between N and K swims
 * in, so after d days the gap is {@code (K - N) (1 - m)^d}. Spots with no neighbouring water (a tarn, a beaver
 * pond with no outlet) do not migrate. Pure Java. Original Year of Ice code.
 */
public final class SpotMigration {
    private SpotMigration() {}

    /** N after {@code days} game days of migration at {@code perDay} of the gap a day. */
    public static double after(double n, double k, double perDay, long days) {
        if (days <= 0 || perDay <= 0 || n >= k) return Math.min(n, k);
        return k - (k - n) * Math.pow(1.0 - Math.min(0.999, perDay), days);
    }

    /** True for a spot type with neighbouring water to draw from. */
    public static boolean migrates(String spotType) {
        return !(spotType.startsWith("tarn") || spotType.equals("beaver_pond") || spotType.equals("pond") || spotType.equals("bog_pool"));
    }
}

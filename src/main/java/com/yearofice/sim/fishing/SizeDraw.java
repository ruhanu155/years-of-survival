package com.yearofice.sim.fishing;

/**
 * The size of a hooked fish (Blueprint › Fishing in depth › Fish species, size draw; Waters and fish › Rules the
 * video rows needed, row 198).
 *
 * <pre>
 *   kg = kg_min + (kg_max − kg_min) · u^e · (0.7 + 0.3 · N/K)       e = 2, or 1.6 deeper than 2 m, 1.3 deeper than 5 m
 * </pre>
 *
 * A fished-down spot gives smaller fish; deep water gives bigger ones (a 4.0 kg mean pike averages about 4.5 kg
 * on a deep drop-off). At spots with q of 0.7 or more, 2% of pike and sheefish draw from the trophy band.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class SizeDraw {
    private SizeDraw() {}

    public static final double TROPHY_CHANCE = 0.02;
    public static final double TROPHY_MIN_Q = 0.7;

    /** The exponent for a bait at this depth. */
    public static double exponent(double baitDepthM) {
        if (baitDepthM > 5.0) return 1.3;
        if (baitDepthM > 2.0) return 1.6;
        return 2.0;
    }

    /**
     * @param u       uniform draw for the size
     * @param uTrophy uniform draw for the trophy check
     * @param uBand   uniform draw inside the trophy band
     */
    public static double kg(FishSpecies s, double baitDepthM, double stockFrac, double spotQ,
                            double u, double uTrophy, double uBand) {
        if (s.trophyKg() != null && spotQ >= TROPHY_MIN_Q && uTrophy < TROPHY_CHANCE) {
            return s.trophyKg()[0] + (s.trophyKg()[1] - s.trophyKg()[0]) * uBand;
        }
        double f = Math.max(0.0, Math.min(1.0, stockFrac));
        return s.kgMin() + (s.kgMax() - s.kgMin()) * StrictMath.pow(u, exponent(baitDepthM)) * (0.7 + 0.3 * f);
    }

    /** The expected size, for tests and the spot's notebook line: E[u^e] = 1 ÷ (e + 1). */
    public static double meanKg(FishSpecies s, double baitDepthM, double stockFrac) {
        double e = exponent(baitDepthM);
        return s.kgMin() + (s.kgMax() - s.kgMin()) / (e + 1.0) * (0.7 + 0.3 * stockFrac);
    }

    /** Flesh eaten roasted: kcal per kg times live weight. */
    public static double kcal(FishSpecies s, double kg) { return s.kcalPerKg() * kg; }
}

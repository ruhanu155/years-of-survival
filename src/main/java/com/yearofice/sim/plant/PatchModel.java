package com.yearofice.sim.plant;

/**
 * Patch health (Plants and fungi › Regrowth and patch health). H runs 0.2-1.3 and multiplies the seed value S.
 * Y_season = S * H * f_year. Picked share p above the stripped threshold sets next H to 0.5, otherwise H climbs by
 * 0.25 to the cap (1.0, or 1.2 tended).
 */
public final class PatchModel {
    private PatchModel() {}

    public static final double H_MIN = 0.2, H_MAX = 1.3, H_CAP = 1.0, H_CAP_TENDED = 1.2;

    public static double season(double s, double h, double fYear) { return s * h * fYear; }

    public static double nextHealth(double h, double picked, double threshold, boolean tended) {
        if (picked > threshold) return 0.5;
        return Math.min(tended ? H_CAP_TENDED : H_CAP, h + 0.25);
    }

    public static double clampH(double h) { return Math.max(H_MIN, Math.min(H_MAX, h)); }

    /** "good" (0.9 or more), "recovering" (0.6-0.9) or "worn". */
    public static String word(double h) { return h >= 0.9 ? "good" : h >= 0.6 ? "recovering" : "worn"; }

    /** {@code taken} 0-3 after {@code unitsTaken} of {@code units}: +1 at each third of the capacity. */
    public static int taken(double unitsTaken, double units) {
        if (units <= 0) return 3;
        return (int) Math.min(3, Math.floor(unitsTaken / units * 3.0 + 1e-9));
    }
}

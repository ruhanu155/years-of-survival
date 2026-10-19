package com.yearofice.sim.time;

/**
 * Driving vanilla time (Minecraft engine › Driving vanilla time): a 181-entry lookup, elevation −90° to +90° in
 * 1° steps, maps the true sun elevation to the vanilla day time whose sun angle gives the same elevation, using
 * the morning half while the sun rises and the afternoon half while it sets. Vanilla's own day counter and moon
 * phase are never read, so only the time of day within a vanilla day matters.
 *
 * <p>The table inverts vanilla's {@code DimensionType.timeOfDay} once at class load (a property of vanilla's
 * renderer, not of the sky), and the lookup itself is two array reads.
 */
public final class VanillaTime {
    private VanillaTime() {}

    public static final int TICKS_PER_DAY = 24_000;

    /** Afternoon half: day time 6,000 (noon, +90°) down to 18,000 (midnight, −90°). Index = elevation + 90. */
    static final double[] SETTING = new double[181];
    /** Morning half: day time 18,000 (−90°) up through 0 (sunrise) to 6,000 (+90°), as 18,000–30,000. */
    static final double[] RISING = new double[181];

    static {
        fill(SETTING, 6_000, 18_000);
        fill(RISING, 18_000, 30_000);
    }

    /** One pass over a monotone half, 1/8 tick apart, linearly interpolating each whole degree's crossing. */
    private static void fill(double[] out, double from, double to) {
        int steps = (int) ((to - from) * 8);
        double prevT = from, prevE = vanillaElevation(from);
        java.util.Arrays.fill(out, Double.NaN);
        for (int i = 1; i <= steps; i++) {
            double t = from + (to - from) * i / steps;
            double e = vanillaElevation(t);
            double lo = Math.min(prevE, e), hi = Math.max(prevE, e);
            for (int deg = (int) Math.ceil(lo); deg <= Math.floor(hi); deg++) {
                if (Double.isNaN(out[deg + 90])) out[deg + 90] = hi == lo ? t : prevT + (t - prevT) * (deg - prevE) / (e - prevE);
            }
            prevT = t;
            prevE = e;
        }
        for (int i = 0; i < out.length; i++) if (Double.isNaN(out[i])) throw new IllegalStateException("no vanilla time for " + (i - 90) + " deg");
    }

    /** Vanilla's sun elevation at day time {@code t}: 90° − the angle from the zenith. */
    public static double vanillaElevation(double t) {
        double d0 = frac(t / TICKS_PER_DAY - 0.25);
        double d1 = 0.5 - Math.cos(d0 * Math.PI) / 2.0;
        double theta = (d0 * 2.0 + d1) / 3.0 * 360.0;
        return 90.0 - Math.min(theta, 360.0 - theta);
    }

    private static double frac(double v) { return v - Math.floor(v); }

    /** The vanilla time of day, 0–23,999, for a true elevation, on the rising or setting half. */
    public static long dayTime(double elevationDeg, boolean rising) {
        double e = Math.max(-90, Math.min(90, elevationDeg));
        int i = (int) Math.floor(e + 90);
        if (i >= 180) i = 179;
        double f = e + 90 - i;
        double[] t = rising ? RISING : SETTING;
        double v = t[i] + (t[i + 1] - t[i]) * f;
        return Math.floorMod(Math.round(v), TICKS_PER_DAY);
    }
}

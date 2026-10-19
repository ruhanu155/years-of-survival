package com.yearofice.sim.pack;

/**
 * Load bands (Blueprint › Inventory by weight; Minecraft engine › Weight and carrying), scaled by Strength (video 2,
 * A11, RECONCILE 170): every band edge is multiplied by 0.8 at Strength 0 up to 1.15 at Strength 100, linear.
 *
 * <pre>
 * Under 15 kg     full speed
 * 15–30 kg        85% speed, 20% more calories
 * 30–45 kg        65% speed, 50% more calories, no running
 * Over 45 kg      drag only, at 30% speed
 * </pre>
 *
 * TODO(M4): the Hauling practice track (+up to 10% after about a game year) and each pack's minimum Strength
 * (frame pack 40, large frame pack 60) join the multiplier with the body's M4 work.
 *
 * @param edgesKg     the three band edges at multiplier 1: 15, 30, 45
 * @param speed       speed factor per band
 * @param kcal        calorie factor per band (over 45 kg the Blueprint gives none; it keeps the 30–45 factor)
 * @param strengthLow multiplier at Strength 0 (0.8)
 * @param strengthHigh multiplier at Strength 100 (1.15)
 */
public record LoadBands(double[] edgesKg, double[] speed, double[] kcal, double strengthLow, double strengthHigh) {

    public enum Band {
        LIGHT, HEAVY, LOADED, OVERLOADED;

        /** Running (sprint) is allowed below 30 kg. */
        public boolean canRun() { return ordinal() < LOADED.ordinal(); }
    }

    public static final LoadBands STANDARD = new LoadBands(new double[] {15, 30, 45}, new double[] {1.0, 0.85, 0.65, 0.30},
            new double[] {1.0, 1.2, 1.5, 1.5}, 0.8, 1.15);

    /** The edge multiplier for Strength 0–100. */
    public double multiplier(double strength) {
        double s = Math.max(0, Math.min(100, strength)) / 100.0;
        return strengthLow + (strengthHigh - strengthLow) * s;
    }

    public Band band(double kg, double strength) {
        double m = multiplier(strength);
        if (kg < edgesKg[0] * m) return Band.LIGHT;
        if (kg < edgesKg[1] * m) return Band.HEAVY;
        if (kg < edgesKg[2] * m) return Band.LOADED;
        return Band.OVERLOADED;
    }

    public double speed(Band b) { return speed[b.ordinal()]; }

    public double kcal(Band b) { return kcal[b.ordinal()]; }

    /** The band edge above which {@code b} starts, in kg at this Strength (0 for the first band). */
    public double from(Band b, double strength) { return b == Band.LIGHT ? 0 : edgesKg[b.ordinal() - 1] * multiplier(strength); }
}

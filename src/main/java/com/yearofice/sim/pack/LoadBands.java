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
 * The Hauling practice track multiplies the edges by up to 1 + {@code haulingBonus} ({@code min(n, 300) / 300} of
 * it), and a pack worn below its {@code min_strength} holds only {@link #WEAK_PACK_KG} and counts anything over
 * that as Overloaded (A11, "Packs need strength").
 *
 * @param edgesKg     the three band edges at multiplier 1: 15, 30, 45
 * @param speed       speed factor per band
 * @param kcal        calorie factor per band (over 45 kg the Blueprint gives none; it keeps the 30–45 factor)
 * @param strengthLow multiplier at Strength 0 (0.8)
 * @param strengthHigh multiplier at Strength 100 (1.15)
 * @param haulingBonus extra multiplier at full Hauling practice (0.10)
 * @param haulingReps  reps to Practiced (300)
 */
public record LoadBands(double[] edgesKg, double[] speed, double[] kcal, double strengthLow, double strengthHigh,
                        double haulingBonus, double haulingReps) {

    /** What a pack holds when its wearer is below its minimum Strength (kg). */
    public static final double WEAK_PACK_KG = 30;

    public LoadBands(double[] edgesKg, double[] speed, double[] kcal, double strengthLow, double strengthHigh) {
        this(edgesKg, speed, kcal, strengthLow, strengthHigh, 0.10, 300);
    }

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

    /** The edge multiplier f with Hauling practice: (0.8 + 0.35 Strength/100) * (1 + 0.10 min(n, 300)/300). */
    public double multiplier(double strength, double haulingReps) {
        return multiplier(strength) * (1 + haulingBonus * Math.max(0, Math.min(haulingReps, this.haulingReps)) / this.haulingReps);
    }

    public Band band(double kg, double strength) { return band(kg, strength, 0); }

    public Band band(double kg, double strength, double haulingReps) {
        return bandOf(kg, multiplier(strength, haulingReps));
    }

    /** As above, for a pack worn at {@code strength} against its {@code minStrength}: weak, it is Overloaded past 30 kg. */
    public Band band(double kg, double strength, double haulingReps, double minStrength) {
        Band b = band(kg, strength, haulingReps);
        return strength < minStrength && kg > WEAK_PACK_KG ? Band.OVERLOADED : b;
    }

    /** A pack's usable limit: its {@code maxKg}, or 30 kg when the wearer is under {@code minStrength}. */
    public static double usableMaxKg(double maxKg, double minStrength, double strength) {
        return strength < minStrength ? Math.min(maxKg, WEAK_PACK_KG) : maxKg;
    }

    private Band bandOf(double kg, double m) {
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

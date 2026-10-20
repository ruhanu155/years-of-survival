package com.yearofice.sim.body.injury;

/**
 * B15 Back strain (Body tab › Injury sources). A lift of 90% or more of a load's hand-carry cap has a base 3%
 * chance of a strained back; a squat lift (x0.3), being tired or cold (x2) and Practiced log building or hauling
 * (x0.5) change it. At most once per 10 game days; off on Gentle, x1.5 on Brutal (the dials carry both).
 */
public final class BackStrain {
    private BackStrain() {}

    public static final double HEAVY_FRACTION = 0.9;

    public record Lift(double loadKg, double capKg, boolean squat, double energyBolts, double warmth,
                       boolean practiced) {}

    public static boolean heavy(Lift l) { return l.capKg() > 0 && l.loadKg() >= HEAVY_FRACTION * l.capKg(); }

    /**
     * @param base      the base chance, 0.03 (0 turns it off)
     * @param squatMult 0.3
     * @param presetMult 1 Standard, 0 Gentle, 1.5 Brutal
     */
    public static double chance(Lift l, double base, double squatMult, double presetMult) {
        if (!heavy(l)) return 0;
        double p = base * presetMult;
        if (l.squat()) p *= squatMult;
        if (l.energyBolts() < 3 || l.warmth() < 40) p *= 2;
        if (l.practiced()) p *= 0.5;
        return Math.min(1, Math.max(0, p));
    }

    /** True when the last strain is old enough (or there was none: {@code lastMinute < 0}). */
    public static boolean cooldownOver(long lastMinute, long now, double cooldownDays) {
        return lastMinute < 0 || now - lastMinute >= cooldownDays * 1440;
    }

    /** Duration 2 to 4 game days from a draw u in [0,1). */
    public static int durationMinutes(double u) { return (int) Math.round((2 + 2 * u) * 1440); }
}

package com.yearofice.sim.ecosystem;

/**
 * Dressing time for small game (Bestiary › Dressing by size class). A hare guts in 3 game minutes, skins in 5 and
 * joints in 5; a species without a listed time scales from the nearest reference animal by
 * {@code t = t_ref · (W ÷ W_ref)^0.6} (hare 1.5 kg). The work runs as a time-lapse at the Work rate, 5 game minutes
 * a real second (Minecraft engine › The four rates). Pure Java.
 */
public final class Dressing {
    private Dressing() {}

    public static final double HARE_REF_KG = 1.5;
    public static final double WORK_GAME_MIN_PER_S = 5.0;

    public static double scaled(double refMinutes, double refKg, double kg) {
        if (refKg <= 0 || kg <= 0) throw new IllegalArgumentException("weights must be positive");
        return refMinutes * Math.pow(kg / refKg, 0.6);
    }

    /** Real ticks the hold takes for {@code gameMinutes} of work. */
    public static int holdTicks(double gameMinutes, double workMinPerS) {
        return Math.max(1, (int) Math.round(gameMinutes / workMinPerS * 20.0));
    }
}

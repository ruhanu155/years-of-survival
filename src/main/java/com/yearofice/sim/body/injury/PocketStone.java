package com.yearofice.sim.body.injury;

/**
 * The pocket stone (Ideas from viewers › The pocket stone). A heated stone in the coat's inner pocket warms the
 * torso +2 °C and, with both hands empty, the hands +6 °C, fading evenly to nothing over 90 game minutes. At most
 * two stones count. A stone in the pack or on the hotbar cools in 20 game minutes and gives nothing.
 */
public final class PocketStone {
    private PocketStone() {}

    public static final double TORSO_C = 2, HANDS_C = 6, FADE_MINUTES = 90, PACK_COOL_MINUTES = 20, MAX_STONES = 2;

    /** Fraction of the warmth left, 1 fresh to 0 after the fade. */
    public static double remaining(double minutesSinceHeated, double fadeMinutes) {
        if (minutesSinceHeated <= 0) return 1;
        return Math.max(0, 1 - minutesSinceHeated / fadeMinutes);
    }

    /** Torso feels-like bonus of {@code stones} (at most two count) of the given ages, °C. */
    public static double torsoBonus(double[] agesMinutes, double fadeMinutes) {
        return sum(agesMinutes, fadeMinutes, TORSO_C);
    }

    public static double handsBonus(double[] agesMinutes, double fadeMinutes, boolean handsEmpty) {
        return handsEmpty ? sum(agesMinutes, fadeMinutes, HANDS_C) : 0;
    }

    private static double sum(double[] ages, double fade, double each) {
        double t = 0;
        int n = 0;
        for (double a : ages) {
            if (n++ >= MAX_STONES) break;
            t += each * remaining(a, fade);
        }
        return t;
    }
}

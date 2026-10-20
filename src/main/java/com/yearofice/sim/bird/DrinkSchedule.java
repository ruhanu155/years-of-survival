package com.yearofice.sim.bird;

/**
 * When animals come down to water (Water, calls and encounters › Water draws animals): the drinking schedule is
 * a low floor all day with peaks at dawn (around 06:00) and dusk (around 19:00), each a cosine bump 2.5 hours wide
 * each side; night holds 0.25 for the wolf and the bear. Returns 0 to 1. Pure Java.
 */
public final class DrinkSchedule {
    private DrinkSchedule() {}

    public static final double DAWN_MIN = 6 * 60, DUSK_MIN = 19 * 60, HALF_WIDTH_MIN = 150;

    public static double strength(int minuteOfDay) {
        double s = 0.25 + bump(minuteOfDay, DAWN_MIN) + bump(minuteOfDay, DUSK_MIN);
        return Math.min(1.0, s);
    }

    private static double bump(double m, double centre) {
        double d = Math.abs(m - centre);
        d = Math.min(d, 1440 - d);
        if (d >= HALF_WIDTH_MIN) return 0;
        return 0.75 * 0.5 * (1 + Math.cos(Math.PI * d / HALF_WIDTH_MIN));
    }
}

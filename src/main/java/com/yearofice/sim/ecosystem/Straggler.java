package com.yearofice.sim.ecosystem;

import java.util.SplittableRandom;

/**
 * The mule deer straggler (Land animals in depth › Deer, RECONCILE 152): each May 1, if the Interior's mule deer N is
 * above 0, roll 8% for a straggler year (times 1.5 after a winter whose snow peaked under 45 cm). Never two
 * straggler years in a row. He is always a yearling buck; he stays 3–20 game days between June 1 and October 31,
 * then walks out (60%) or is killed by wolves (40%).
 */
public final class Straggler {
    private Straggler() {}

    public static final double BASE = 0.08;
    public static final double MILD_WINTER_X = 1.5;
    public static final double MILD_WINTER_SNOW_CM = 45;
    public static final double WOLF_KILL = 0.40;

    public record Year(boolean straggler, int arrivalDay, int stayDays, boolean killedByWolves) {}

    public static double chance(double winterSnowPeakCm) { return BASE * (winterSnowPeakCm < MILD_WINTER_SNOW_CM ? MILD_WINTER_X : 1.0); }

    /** The year's roll. {@code previous} is whether the last year was a straggler year. Days are 0 = June 1. */
    public static Year roll(long worldSeed, int year, boolean previous, double winterSnowPeakCm, int interiorMuleDeerN) {
        SplittableRandom r = new SplittableRandom(worldSeed * 31 + year * 7919L + 0x5712A66L);
        double u = r.nextDouble();
        int arrival = r.nextInt(153);
        int stay = 3 + r.nextInt(18);
        boolean wolves = r.nextDouble() < WOLF_KILL;
        if (interiorMuleDeerN <= 0 || previous || u >= chance(winterSnowPeakCm)) return new Year(false, 0, 0, false);
        return new Year(true, arrival, stay, wolves);
    }
}

package com.yearofice.sim.companion;

import java.util.random.RandomGenerator;

/**
 * B12, the caribou lead fence (Land animals in depth): a band meeting a wing within 30 degrees of its heading follows
 * it to the mouth, unless it scents the player (wind toward the band: none follow). 20 segments (80 m) hold 50 % of
 * bands and 50 segments (200 m) 80 %, in between by length. The mouth gives a 30-90 s shot window at 10-20 m.
 */
public final class LeadFenceMath {
    private LeadFenceMath() {}

    public static final double CATCH_ANGLE_DEG = 30;
    public static final int SHORT_SEGMENTS = 20, LONG_SEGMENTS = 50;

    public static double followChance(CompanionTuning t, int wingSegments) {
        if (wingSegments <= 0) return 0;
        if (wingSegments <= SHORT_SEGMENTS) return t.leadFenceFollowShort() * wingSegments / SHORT_SEGMENTS;
        if (wingSegments >= LONG_SEGMENTS) return t.leadFenceFollowLong();
        double f = (wingSegments - SHORT_SEGMENTS) / (double) (LONG_SEGMENTS - SHORT_SEGMENTS);
        return t.leadFenceFollowShort() + f * (t.leadFenceFollowLong() - t.leadFenceFollowShort());
    }

    /** Smallest absolute difference of two headings in degrees. */
    public static double angleDiff(double a, double b) {
        double d = Math.abs(((a - b) % 360 + 540) % 360 - 180);
        return d;
    }

    /** One band: true when it turns along the wing (within the catch angle, unscented, and the roll lands). */
    public static boolean bandFollows(CompanionTuning t, int wingSegments, double bandHeadingDeg, double wingHeadingDeg,
            boolean windTowardBand, RandomGenerator rng) {
        if (windTowardBand) return false;
        if (angleDiff(bandHeadingDeg, wingHeadingDeg) > CATCH_ANGLE_DEG) return false;
        return rng.nextDouble() < followChance(t, wingSegments);
    }

    /** Fences where this many caribou were taken in one season see half the route use next year. */
    public static double routeUse(CompanionTuning t, int takenLastSeason) { return takenLastSeason >= t.fenceLearnKills() ? 0.5 : 1.0; }
}

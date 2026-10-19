package com.yearofice.sim.body;

import com.yearofice.sim.body.part.Part;

/**
 * Per-part frost counters ({@code part_frost} × 10, RECONCILE 120 C8).
 *
 * <pre>
 *   t_nip = 300 / (T_limit − T_feel)   game minutes
 * </pre>
 *
 * A counter holds frost points, and one period of {@code t_nip} is 300 points, so a minute below the limit
 * adds {@code T_limit − T} points (rounded, at least 1) and whole-degree gaps are exact. 300 is frostnip,
 * 600 superficial, 900 deep ("each further period of the same length advances one stage"). Past 900 the
 * counter counts deep minutes, one a minute, so 6 game hours of deep frostbite unrewarmed reads 1260.
 * Above the limit the counter holds until a rewarm treatment runs: rewarming stops it advancing and clears
 * frostnip in 60 minutes. The counter is a short in the record.
 */
public final class FrostModel {
    private FrostModel() {}

    public static final int PERIOD = 300;
    public static final int NIP = PERIOD, SUPERFICIAL = 2 * PERIOD, DEEP = 3 * PERIOD;
    /** Deep frostbite not rewarmed in 6 h takes a digit and is a Hardcore severe condition. */
    public static final int DEEP_DIGIT_MINUTES = 360;
    public static final int MAX = Short.MAX_VALUE;

    public static double tNipMinutes(double limitC, double feelC) {
        return feelC >= limitC ? Double.POSITIVE_INFINITY : 300.0 / (limitC - feelC);
    }

    /** A part's limit: its zone's, the wet limit when that part alone is wet, 5 °C warmer holding bare metal below −20 °C (P8). */
    public static double partLimitC(double zoneLimitC, boolean partWet, boolean bareMetalBelowMinus20) {
        double l = partWet ? Math.max(zoneLimitC, Clothing.WET_SAFE_TO_C) : zoneLimitC;
        return bareMetalBelowMinus20 ? l + 5 : l;
    }

    /** 0 none, 1 frostnip, 2 superficial, 3 deep. */
    public static int stage(int points) {
        if (points >= DEEP) return 3;
        if (points >= SUPERFICIAL) return 2;
        if (points >= NIP) return 1;
        return 0;
    }

    /** Result of one minute on one part. */
    public record PartStep(int points, int stageBefore, int stageAfter, boolean digitLost) {
        public boolean stageRose() { return stageAfter > stageBefore; }
    }

    public static PartStep stepMinute(int points, double limitC, double feelC, boolean rewarming, boolean digitLossOn) {
        int before = stage(points);
        int p = points;
        if (rewarming) {
            if (before <= 1) p = Math.max(0, p - (NIP + 59) / 60); // frostnip clears within 60 minutes
        } else if (p >= DEEP) {
            p = Math.min(MAX, p + 1);
        } else if (feelC < limitC) {
            p = Math.min(MAX, p + (int) Math.max(1, Math.round(limitC - feelC)));
        }
        boolean digit = digitLossOn && points < DEEP + DEEP_DIGIT_MINUTES && p >= DEEP + DEEP_DIGIT_MINUTES;
        return new PartStep(p, before, stage(p), digit);
    }

    /** A zone's frost value is the highest of its parts' (C8). */
    public static int zoneValue(int[] partFrost, Zone zone) {
        int max = 0;
        for (Part p : Part.values()) if (p.zone == zone) max = Math.max(max, partFrost[p.ordinal()]);
        return max;
    }
}

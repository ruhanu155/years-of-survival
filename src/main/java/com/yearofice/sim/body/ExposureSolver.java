package com.yearofice.sim.body;

/**
 * "Outside like this: about 40 min" (Every concept from the video › Exposure time). Solves the warmth formula for
 * the time until warmth reaches Slowed (30), and the weakest zone's frostnip clock, and returns the shorter:
 *
 * <pre>t_out = min( (W − 30) / (0.02 (10 − T_feel) m_cold),  min_zone 300 / (T_limit − T_feel_zone) )</pre>
 *
 * A bare head below −25 °C feels-like doubles the warmth term's loss rate. Minutes; {@code +∞} when nothing cools.
 */
public final class ExposureSolver {
    private ExposureSolver() {}

    public static double minutes(double warmth, double feelC, double coldScale, boolean bareHeadBelow25,
                                 double[] limitC, double[] zoneFeelC) {
        double t = Double.POSITIVE_INFINITY;
        double rate = 0.02 * (10 - feelC) * coldScale * (bareHeadBelow25 && feelC < -25 ? 2 : 1);
        if (rate > 0) t = Math.max(0, warmth - 30) / rate;
        for (int i = 0; i < limitC.length; i++) {
            double gap = limitC[i] - zoneFeelC[i];
            if (gap > 0) t = Math.min(t, 300.0 / gap);
        }
        return t;
    }

    /** The display step: 5 minutes to an hour, 15 minutes to six hours, then "hours" (−1). */
    public static int shown(double minutes) {
        if (minutes < 10) return 0;
        if (minutes <= 60) return (int) (Math.round(minutes / 5) * 5);
        if (minutes <= 360) return (int) (Math.round(minutes / 15) * 15);
        return -1;
    }
}

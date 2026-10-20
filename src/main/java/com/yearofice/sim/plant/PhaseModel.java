package com.yearofice.sim.plant;

import java.util.Map;

/**
 * Phases from degree-days (Plants and fungi › Phases through the year): each species lists a sum above 5 C per phase
 * counted from melt-out; a cold summer makes everything late. {@link #ddAt} is the normal-year season curve from the
 * Foraging calendar (about 120 in May, 315 June, 370 July, 280 August, 90 September).
 */
public final class PhaseModel {
    private PhaseModel() {}

    /** Day of year (0 = May 1) of the first hard frost that ends the season in a normal year (about Sept 5). */
    public static final int FIRST_FROST_DOY = 127;
    /** Monthly degree-day totals from May to September (normal year), 31/30/31/31/30 days. */
    private static final double[] MONTH_DD = {120, 315, 370, 280, 90};
    private static final int[] MONTH_LEN = {31, 30, 31, 31, 30};

    /** Degree-days above 5 C accumulated by day {@code doy} (0 = May 1) in a normal year, times {@code warmth}. */
    public static double ddAt(int doy, double warmth) {
        double dd = 0;
        int d = Math.max(0, doy);
        for (int m = 0; m < MONTH_DD.length && d > 0; m++) {
            int take = Math.min(d, MONTH_LEN[m]);
            dd += MONTH_DD[m] * take / MONTH_LEN[m];
            d -= take;
        }
        return dd * warmth;
    }

    /** The phase for a degree-day sum: the latest listed phase whose sum has been reached. */
    public static PlantPhase phaseFor(Map<PlantPhase, Double> sums, double dd, int doy, boolean snowBuried) {
        if (doy >= FIRST_FROST_DOY + 20) return snowBuried ? PlantPhase.BURIED : PlantPhase.DORMANT;
        if (doy >= FIRST_FROST_DOY) return PlantPhase.SENESCENT;
        PlantPhase best = PlantPhase.DORMANT;
        double bestSum = -1;
        for (Map.Entry<PlantPhase, Double> e : sums.entrySet()) {
            if (e.getKey() == PlantPhase.SENESCENT || e.getKey() == PlantPhase.DORMANT) continue;
            if (dd >= e.getValue() && e.getValue() > bestSum) { best = e.getKey(); bestSum = e.getValue(); }
        }
        return best;
    }

    /** The degree-day-sum a year can shift a window by: +/- {@code jitterDays} of warmth turned into dd at ~11 dd/day. */
    public static double jitterDd(double unit, double jitterDays) { return (unit * 2 - 1) * jitterDays * 11.0; }
}

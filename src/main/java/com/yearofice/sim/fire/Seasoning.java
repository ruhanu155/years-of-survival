package com.yearofice.sim.fire;

import com.yearofice.sim.time.Calendar;

/**
 * B4, seasoning by the calendar (Crafting and building in depth › Trees, felling and firewood; Proposed, in the
 * current numbers): split wood loses moisture points a game day by the month's rate on a covered rack; the woodshed
 * is ×1.3, a stack with no top cover ×0.7, unsplit rounds ×0.4 and whole logs ×0.25. Pure: the month's rate × the
 * place and form multipliers, summed over whole days.
 *
 * <p>Two edges the table states in words are dials: April's rate runs from melt-out, and October's while the daily
 * mean is above 0 °C. With melt-out on April 15 and October's thaw ending October 10, the sums give the doc's own
 * "What it means" table (March 20 to about July 4, May 25 to about August 10, June 1 to about August 20, July 10 to
 * about May 15 next year, on a covered rack).
 */
public final class Seasoning {
    private Seasoning() {}

    /** Where the wood lies. */
    public enum Place { RACK, WOODSHED, UNCOVERED }

    /** What the wood is: split, an unsplit round, or a whole log. */
    public enum Form { SPLIT, ROUND, LOG }

    /** Seasoned means under this moisture %. */
    public static final double SEASONED_PCT = 20;
    /** Green wood. */
    public static final double GREEN_PCT = 50;
    /** Standing dead spruce. */
    public static final double SNAG_PCT = 18;

    /**
     * The dials.
     *
     * @param monthly      points a day on a covered rack, split, by calendar month (index 0 is January)
     * @param aprilFromDay April's rate applies from this day of April (melt-out)
     * @param octoberToDay October's rate applies through this day of October (daily mean above 0 °C)
     * @param floorPct     wood dries no lower than this in the open air
     */
    public record Rates(double[] monthly, int aprilFromDay, int octoberToDay, double woodshedMult, double uncoveredMult,
                        double unsplitMult, double logMult, double floorPct) {
        public static final Rates STANDARD = new Rates(
                new double[] {0, 0, 0, 0.10, 0.45, 0.45, 0.35, 0.30, 0.15, 0.05, 0, 0}, 15, 10, 1.3, 0.7, 0.4, 0.25, 12);

        public double month(int calendarMonth) { return monthly[calendarMonth - 1]; }
    }

    /** Points lost on day of year {@code dayOfYear} (0 = May 1) by wood of {@code form} at {@code place}. */
    public static double rate(Rates r, int dayOfYear, Place place, Form form) {
        int m = Calendar.month(dayOfYear);
        int dom = Calendar.dayOfMonth(dayOfYear);
        if (m == 4 && dom < r.aprilFromDay()) return 0;
        if (m == 10 && dom > r.octoberToDay()) return 0;
        double v = r.month(m);
        v *= switch (place) {
            case RACK -> 1.0;
            case WOODSHED -> r.woodshedMult();
            case UNCOVERED -> r.uncoveredMult();
        };
        v *= switch (form) {
            case SPLIT -> 1.0;
            case ROUND -> r.unsplitMult();
            case LOG -> r.logMult();
        };
        return v;
    }

    /** Points lost over absolute days [fromDay, toDay): each whole day at its own rate. */
    public static double loss(Rates r, long fromDay, long toDay, Place place, Form form) {
        double sum = 0;
        for (long d = fromDay; d < toDay; d++) sum += rate(r, Calendar.dayOfYearOfDay(d), place, form);
        return sum;
    }

    /** Moisture after drying from {@code fromDay} to {@code toDay}, never under the floor (nor raised to it). */
    public static double dry(Rates r, double moisture, long fromDay, long toDay, Place place, Form form) {
        if (toDay <= fromDay || moisture <= r.floorPct()) return moisture;
        return Math.max(r.floorPct(), moisture - loss(r, fromDay, toDay, place, form));
    }

    /**
     * The first absolute day on which wood at {@code moisture} on {@code fromDay} is under {@code targetPct}, or -1
     * if not within {@code maxDays} (a stack's hover forecast, "Dry by about Aug 7").
     */
    public static long forecast(Rates r, double moisture, long fromDay, double targetPct, Place place, Form form, int maxDays) {
        double m = moisture;
        if (m < targetPct) return fromDay;
        for (long d = fromDay; d < fromDay + maxDays; d++) {
            m -= rate(r, Calendar.dayOfYearOfDay(d), place, form);
            if (m < targetPct) return d + 1;
        }
        return -1;
    }
}

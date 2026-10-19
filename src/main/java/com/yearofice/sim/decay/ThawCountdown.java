package com.yearofice.sim.decay;

/**
 * The frozen tooltip (Decay and time › The cold curve): "Frozen · keeps until the thaw, about 58 days".
 *
 * <p>The figure is the days until the store's <i>normal</i> thaw date plus the safe life left at the cool rate
 * (every unsmoked frozen item restarts at the cool rate of 6 days at 5 °C when the air first passes −5 °C).
 * It uses the climate normal, never the drawn year, so it does not leak the weather. A fresh quarter hung on
 * February 18 with the open-air normal thaw on April 10 shows 51 + 6 = 57 days.
 *
 * <p>Days are days of a 365-day year, 1 = January 1. Pure Java. Original Year of Ice code.
 */
public final class ThawCountdown {
    private ThawCountdown() {}

    public static final int DAYS_PER_YEAR = 365;
    private static final int[] MONTH_START = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
    /** Normal thaw for a meat pole in the open (Decay and time › The cold curve). */
    public static final int OPEN_POLE_THAW_DAY = dayOfYear(4, 10);
    /** Normal thaw for a north-side snow-covered cache. */
    public static final int NORTH_CACHE_THAW_DAY = dayOfYear(4, 25);
    /** The permafrost cellar never thaws. */
    public static final int NEVER = -1;

    /** Day of year (1-based, no leap day) for a month 1–12 and day of month. */
    public static int dayOfYear(int month, int day) {
        if (month < 1 || month > 12) throw new IllegalArgumentException("month");
        return MONTH_START[month - 1] + day;
    }

    /** Whole days from {@code today} forward to {@code thawDay}, wrapping the year; 0 on the day itself. */
    public static int daysUntil(int today, int thawDay) {
        return Math.floorMod(thawDay - today, DAYS_PER_YEAR);
    }

    /**
     * Safe days left at 5 °C for a stack at progress p whose safe life at 5 °C is {@code safeLifeDaysAt5c}:
     * the share of the safe life (p up to 0.7) still unspent.
     */
    public static double safeDaysAtCool(double p, double safeLifeDaysAt5c) {
        double left = (DecayCore.SAFE_P - p) / DecayCore.SAFE_P;
        return Math.max(0.0, left) * safeLifeDaysAt5c;
    }

    /**
     * The countdown in days; {@code Double.POSITIVE_INFINITY} when the store never thaws.
     */
    public static double days(int today, int normalThawDay, double p, double safeLifeDaysAt5c) {
        if (normalThawDay == NEVER) return Double.POSITIVE_INFINITY;
        return daysUntil(today, normalThawDay) + safeDaysAtCool(p, safeLifeDaysAt5c);
    }

    /** The tooltip figure, rounded to whole days. */
    public static long tooltipDays(int today, int normalThawDay, double p, double safeLifeDaysAt5c) {
        return Math.round(days(today, normalThawDay, p, safeLifeDaysAt5c));
    }
}

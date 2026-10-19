package com.yearofice.sim.ecosystem;

/**
 * The calendar the slice reads until the Clock and calendar layer lands (Blueprint › Open decisions, Year length:
 * 365 game days, one per calendar day, starting May 1). Game minutes count from midnight of day 0; day 0 is May 1.
 *
 * <p>Traps roll twice a day, at dawn and dusk (Blueprint › Traps: "Ground sets roll at dawn and dusk"). Roll k is
 * on day k ÷ 2, at dawn when k is even and at dusk when it is odd. Pure Java.
 */
public final class SliceCalendar {
    private SliceCalendar() {}

    public static final int DAY_MIN = 1440;
    public static final int YEAR_DAYS = 365;
    /** Days in each month from May. */
    static final int[] MONTH_DAYS = {31, 30, 31, 31, 30, 31, 30, 31, 31, 28, 31, 30};
    /** Calendar month numbers from May. */
    static final int[] MONTHS = {5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4};

    public static long day(long minute) { return Math.floorDiv(minute, DAY_MIN); }

    public static int minuteOfDay(long minute) { return (int) Math.floorMod(minute, DAY_MIN); }

    /** Day of the year, 0 = May 1. */
    public static int dayOfYear(long minute) { return (int) Math.floorMod(day(minute), YEAR_DAYS); }

    /** The run's year, 0 for the first. */
    public static long year(long minute) { return Math.floorDiv(day(minute), YEAR_DAYS); }

    /** Day of the year for a calendar date. */
    public static int doy(int month, int dayOfMonth) {
        int d = 0;
        for (int i = 0; i < MONTHS.length; i++) {
            if (MONTHS[i] == month) {
                if (dayOfMonth < 1 || dayOfMonth > MONTH_DAYS[i]) throw new IllegalArgumentException("bad day " + dayOfMonth);
                return d + dayOfMonth - 1;
            }
            d += MONTH_DAYS[i];
        }
        throw new IllegalArgumentException("bad month " + month);
    }

    /** "Oct 21" for a day of the year. */
    public static String date(int doy) {
        int d = Math.floorMod(doy, YEAR_DAYS);
        String[] names = {"May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Apr"};
        for (int i = 0; i < MONTH_DAYS.length; i++) {
            if (d < MONTH_DAYS[i]) return names[i] + " " + (d + 1);
            d -= MONTH_DAYS[i];
        }
        throw new IllegalStateException();
    }

    /** True when {@code doy} lies in the window from {@code fromDoy} to {@code toDoy}, inclusive, wrapping at May 1. */
    public static boolean within(int doy, int fromDoy, int toDoy) {
        return fromDoy <= toDoy ? doy >= fromDoy && doy <= toDoy : doy >= fromDoy || doy <= toDoy;
    }

    /** Game minute of roll k. */
    public static long rollMinute(long k, int dawnMin, int duskMin) {
        long day = Math.floorDiv(k, 2);
        return day * DAY_MIN + (Math.floorMod(k, 2) == 0 ? dawnMin : duskMin);
    }

    public static boolean isDawn(long k) { return Math.floorMod(k, 2) == 0; }

    /** The last roll at or before {@code minute}. */
    public static long rollAtOrBefore(long minute, int dawnMin, int duskMin) {
        if (!(0 <= dawnMin && dawnMin < duskMin && duskMin < DAY_MIN)) throw new IllegalArgumentException("dawn must come before dusk");
        long day = day(minute);
        int m = minuteOfDay(minute);
        if (m >= duskMin) return day * 2 + 1;
        if (m >= dawnMin) return day * 2;
        return day * 2 - 1;
    }
}

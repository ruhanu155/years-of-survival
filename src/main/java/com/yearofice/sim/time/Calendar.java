package com.yearofice.sim.time;

import java.util.Locale;

/**
 * {@code sim.time.Calendar}: game time is {@code game_ds}, game deciseconds since 00:00 on May 1 of year 1
 * (Layer 1). A year is 365 days with no leap years and starts on May 1, so day of year 0 is May 1 and day 364 is
 * April 30. Months are the calendar's own, numbered 1–12 (May is 5).
 */
public final class Calendar {
    private Calendar() {}

    public static final long DS_PER_MINUTE = 600;
    public static final int MINUTES_PER_DAY = 1440;
    public static final long DS_PER_DAY = DS_PER_MINUTE * MINUTES_PER_DAY;
    public static final int DAYS_PER_YEAR = 365;
    public static final long DS_PER_YEAR = DS_PER_DAY * DAYS_PER_YEAR;

    /** Month numbers in year order from May, and their lengths. */
    static final int[] MONTHS = {5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3, 4};
    static final int[] LENGTHS = {31, 30, 31, 31, 30, 31, 30, 31, 31, 28, 31, 30};
    static final String[] NAMES = {"January", "February", "March", "April", "May", "June", "July", "August",
            "September", "October", "November", "December"};
    /** Day of year (0 = May 1) of the 1st of each calendar month, indexed by month − 1. */
    static final int[] FIRST = new int[12];

    static {
        int d = 0;
        for (int i = 0; i < 12; i++) {
            FIRST[MONTHS[i] - 1] = d;
            d += LENGTHS[i];
        }
    }

    /** Absolute game minute, 0 at 00:00 on May 1 of year 1. */
    public static long minute(long ds) { return Math.floorDiv(ds, DS_PER_MINUTE); }

    /** Absolute day, 0 on May 1 of year 1. */
    public static long absDay(long ds) { return Math.floorDiv(ds, DS_PER_DAY); }

    public static long absDayOfMinute(long minute) { return Math.floorDiv(minute, MINUTES_PER_DAY); }

    /** Minute of the day 0–1,439 of an absolute minute. */
    public static int minuteOfDayOfMinute(long minute) { return (int) Math.floorMod(minute, (long) MINUTES_PER_DAY); }

    /** Day of the year 0–364 (May 1 = 0) of an absolute minute. */
    public static int dayOfYearOfMinute(long minute) { return dayOfYearOfDay(absDayOfMinute(minute)); }

    /** Year, 1-based. */
    public static int year(long ds) { return (int) Math.floorDiv(ds, DS_PER_YEAR) + 1; }

    /** Year of an absolute day, 1-based. */
    public static int yearOfDay(long absDay) { return (int) Math.floorDiv(absDay, DAYS_PER_YEAR) + 1; }

    /** Day of the year 0–364, May 1 = 0. */
    public static int dayOfYear(long ds) { return dayOfYearOfDay(absDay(ds)); }

    public static int dayOfYearOfDay(long absDay) { return (int) Math.floorMod(absDay, (long) DAYS_PER_YEAR); }

    public static int minuteOfDay(long ds) { return (int) Math.floorMod(minute(ds), (long) MINUTES_PER_DAY); }

    public static int hour(long ds) { return minuteOfDay(ds) / 60; }

    /** Calendar month 1–12 of a day of year. */
    public static int month(int dayOfYear) {
        int d = dayOfYear;
        for (int i = 0; i < 12; i++) {
            if (d < LENGTHS[i]) return MONTHS[i];
            d -= LENGTHS[i];
        }
        throw new IllegalArgumentException("day of year " + dayOfYear);
    }

    /** Day of the month, 1-based. */
    public static int dayOfMonth(int dayOfYear) { return dayOfYear - FIRST[month(dayOfYear) - 1] + 1; }

    /** Day of year (0 = May 1) of {@code day} of calendar month {@code month}. */
    public static int dayOfYear(int month, int day) { return FIRST[month - 1] + day - 1; }

    /** Days in calendar month {@code month}. */
    public static int lengthOf(int month) {
        for (int i = 0; i < 12; i++) if (MONTHS[i] == month) return LENGTHS[i];
        throw new IllegalArgumentException("month " + month);
    }

    public static String monthName(int month) { return NAMES[month - 1]; }

    /** "Oct 21" for a day of the year. */
    public static String shortDate(int dayOfYear) {
        int doy = Math.floorMod(dayOfYear, DAYS_PER_YEAR);
        return monthName(month(doy)).substring(0, 3) + " " + dayOfMonth(doy);
    }

    /** True when {@code doy} lies in the window from {@code fromDoy} to {@code toDoy}, inclusive, wrapping at May 1. */
    public static boolean within(int doy, int fromDoy, int toDoy) {
        return fromDoy <= toDoy ? doy >= fromDoy && doy <= toDoy : doy >= fromDoy || doy <= toDoy;
    }

    /** "May 1, year 1, 06:30". */
    public static String format(long ds) {
        int doy = dayOfYear(ds);
        int m = minuteOfDay(ds);
        return String.format(Locale.ROOT, "%s %d, year %d, %02d:%02d", monthName(month(doy)), dayOfMonth(doy), year(ds),
                m / 60, m % 60);
    }

    /** "HH:MM" of a minute of day. */
    public static String hhmm(int minuteOfDay) {
        int m = Math.floorMod(minuteOfDay, MINUTES_PER_DAY);
        return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60);
    }
}

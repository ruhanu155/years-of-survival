package com.yearofice.sim.story;

import java.util.function.LongUnaryOperator;

/**
 * Time-of-day arithmetic for the guided night, on absolute game minutes (minute 0 is 00:00 on May 1 of year 1, as
 * {@code ClockService} counts them). The sun's times come in as functions of the absolute day, so the server passes
 * {@code ClockService}'s 64°N table and the tests pass whatever they need.
 */
public final class StoryDay {
    private StoryDay() {}

    public static final int DAY_MIN = 1440;

    public static int minuteOfDay(long minute) { return (int) Math.floorMod(minute, DAY_MIN); }

    public static long absDay(long minute) { return Math.floorDiv(minute, DAY_MIN); }

    /** 1-based day number, as the messenger shows it. */
    public static int day(long minute) { return (int) absDay(minute) + 1; }

    public static String hhmm(long minute) {
        int m = minuteOfDay(minute);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", m / 60, m % 60);
    }

    /**
     * The first morning: the first dawn at least 12 game hours after the night began. A player who stays up
     * instead of sleeping is done then (Blueprint › Start site, step 15; Morning prompts: "or at 06:00 if they
     * stayed up", with the clock's dawn in place of 06:00).
     *
     * @param dawnOf absolute day to the absolute minute of its dawn
     */
    public static long firstMorning(long startMinute, LongUnaryOperator dawnOf) {
        long d = absDay(startMinute);
        for (int i = 0; i < 4; i++, d++) {
            long dawn = dawnOf.applyAsLong(d);
            if (dawn >= startMinute + 12 * 60) return dawn;
        }
        return startMinute + DAY_MIN;
    }

    /**
     * The dusk-warning window: from {@code leadMin} before a day's sunset until the next day's dawn. Checks the
     * evening of today and of yesterday, so the hours after midnight still count.
     *
     * @param sunsetOf absolute day to the absolute minute of its sunset
     * @param dawnOf   absolute day to the absolute minute of its dawn
     */
    public static boolean inDuskWindow(long minute, int leadMin, LongUnaryOperator sunsetOf, LongUnaryOperator dawnOf) {
        long d = absDay(minute);
        for (long e = d - 1; e <= d; e++) {
            long from = sunsetOf.applyAsLong(e) - leadMin;
            long to = dawnOf.applyAsLong(e + 1);
            if (minute >= from && minute < to) return true;
        }
        return false;
    }
}

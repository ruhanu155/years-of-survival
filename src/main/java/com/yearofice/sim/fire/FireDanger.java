package com.yearofice.sim.fire;

import java.util.List;
import java.util.SplittableRandom;

/**
 * The fire danger index (Every concept from the video › Fire danger): one of four levels from snow-off to
 * August 31, worked once a game day from the last two weeks of weather. Pure Java; {@code com.yearofice.fire.FireDangerIndex}
 * feeds it from the Environment field's drawn days.
 *
 * <pre>D = d_dry * (1 + (T_max - 15) / 10) * (1 + w / 8)</pre>
 *
 * with {@code d_dry} the days since 5 mm or more of rain (capped at 14), {@code T_max} today's high (floored at 15)
 * and {@code w} the mean wind in m/s. Low is under 5, Moderate 5–12, High 12–25, Extreme over 25.
 */
public final class FireDanger {
    private FireDanger() {}

    public enum Level {
        LOW("Low"), MODERATE("Moderate"), HIGH("High"), EXTREME("Extreme");

        public final String word;

        Level(String word) { this.word = word; }
    }

    public static final int DRY_CAP_DAYS = 14;
    public static final double RAIN_DAY_MM = 5.0;
    /** The index runs from snow-off to August 31: day of year 0 is May 1, so August 31 is day 122. */
    public static final int LAST_DOY = 122;

    /** One past day's weather, as the index reads it. */
    public record Day(double precipMm) {}

    public static double index(int daysDry, double tMaxC, double windMs) {
        int d = Math.max(0, Math.min(DRY_CAP_DAYS, daysDry));
        return d * (1 + (Math.max(15, tMaxC) - 15) / 10.0) * (1 + Math.max(0, windMs) / 8.0);
    }

    public static Level level(double d) {
        if (d < 5) return Level.LOW;
        if (d < 12) return Level.MODERATE;
        if (d <= 25) return Level.HIGH;
        return Level.EXTREME;
    }

    /**
     * Days since 5 mm or more of rain, from the past days newest first (index 0 is yesterday); capped at 14. A
     * list shorter than the cap with no wet day counts as dry for its whole length.
     */
    public static int daysDry(List<Day> pastNewestFirst) {
        int n = 0;
        for (Day d : pastNewestFirst) {
            if (d.precipMm() >= RAIN_DAY_MM) return n;
            if (++n >= DRY_CAP_DAYS) return DRY_CAP_DAYS;
        }
        return n;
    }

    /**
     * Today's level: Low outside the window (after August 31, or while snow lies), else from the formula.
     *
     * @param doy         day of the game year, 0 on May 1
     * @param snowCm      snow on open ground today
     * @param pastNewestFirst the last days' weather, yesterday first
     */
    public static Level today(int doy, double snowCm, List<Day> pastNewestFirst, double tMaxC, double windMs) {
        if (doy > LAST_DOY || snowCm > 0.5) return Level.LOW;
        return level(index(daysDry(pastNewestFirst), tMaxC, windMs));
    }

    /**
     * One game hour of the packed-fire spread roll: true when a spark lands. The chance is {@code pct} percent;
     * {@code rng} is drawn once whether or not it lands, so a stream stays aligned.
     */
    public static boolean spreads(double pct, SplittableRandom rng) {
        return rng.nextDouble() * 100 < pct;
    }
}

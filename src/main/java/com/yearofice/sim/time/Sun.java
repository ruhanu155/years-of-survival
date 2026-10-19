package com.yearofice.sim.time;

/**
 * {@code sim.time.Sun}: the sun at the home valley from the 365-row daylight table of
 * {@code weather/interior_alaska.json} (Minecraft engine › Calendar, daylight and moon). Elevation and azimuth are
 * read at the whole hour and interpolated linearly by minute; runtime code never computes trig for the sky.
 *
 * <p><b>Dawn and dusk</b> (Layer 1): dawn is the minute the sun climbs through −6°, dusk the minute it sinks
 * through −6°. On a night when the sun never sinks that far, dawn fires 120 minutes after the night's lowest sun
 * and dusk 120 minutes before it. Both are precomputed per table day: {@link #dawnMinute(int)} is minutes after
 * that day's midnight (the morning of the day), {@link #duskMinute(int)} too, and can pass 1,440 when dusk falls
 * after midnight.
 */
public final class Sun {
    public static final double TWILIGHT_DEG = -6.0;
    public static final int FALLBACK_MIN = 120;

    private final float[][] elev;
    private final float[][] azim;
    private final int[] sunrise;
    private final int[] sunset;
    private final int[] dawn;
    private final int[] dusk;
    private final boolean[] dawnFallback;
    private final boolean[] duskFallback;

    /**
     * @param elevationDeg [365][24] geometric elevation at each whole hour, day 0 = May 1
     * @param azimuthDeg   [365][24] degrees clockwise from north
     * @param sunriseMin   [365] sunrise at −0.833°, minutes after midnight, −1 if none
     * @param sunsetMin    [365] the following sunset, may exceed 1,440, −1 if none
     */
    public Sun(float[][] elevationDeg, float[][] azimuthDeg, int[] sunriseMin, int[] sunsetMin) {
        if (elevationDeg.length != Calendar.DAYS_PER_YEAR || azimuthDeg.length != Calendar.DAYS_PER_YEAR
                || sunriseMin.length != Calendar.DAYS_PER_YEAR || sunsetMin.length != Calendar.DAYS_PER_YEAR) {
            throw new IllegalArgumentException("the daylight table needs 365 rows");
        }
        for (int d = 0; d < Calendar.DAYS_PER_YEAR; d++) {
            if (elevationDeg[d].length != 24 || azimuthDeg[d].length != 24) throw new IllegalArgumentException("row " + d + " needs 24 hours");
        }
        this.elev = elevationDeg;
        this.azim = azimuthDeg;
        this.sunrise = sunriseMin.clone();
        this.sunset = sunsetMin.clone();
        int n = Calendar.DAYS_PER_YEAR;
        dawn = new int[n];
        dusk = new int[n];
        dawnFallback = new boolean[n];
        duskFallback = new boolean[n];
        for (int d = 0; d < n; d++) {
            computeDawn(d);
            computeDusk(d);
        }
    }

    /** Elevation in degrees at absolute day-of-table {@code day} (any integer; wraps yearly) and minute of day. */
    public double elevation(long absDay, int minuteOfDay) {
        return lerp(elev, absDay, minuteOfDay);
    }

    /** Elevation at a minute counted from the day's midnight; values past 1,440 read the next day. */
    double elevationAt(int day, int minuteFromMidnight) {
        long d = day + Math.floorDiv(minuteFromMidnight, Calendar.MINUTES_PER_DAY);
        return lerp(elev, d, Math.floorMod(minuteFromMidnight, Calendar.MINUTES_PER_DAY));
    }

    /** Azimuth in degrees clockwise from north, interpolated the short way round. */
    public double azimuth(long absDay, int minuteOfDay) {
        int d = row(absDay);
        int h = minuteOfDay / 60;
        double f = (minuteOfDay % 60) / 60.0;
        double a = azim[d][h];
        double b = h == 23 ? azim[row(absDay + 1)][0] : azim[d][h + 1];
        double diff = ((b - a) % 360 + 540) % 360 - 180;
        return ((a + diff * f) % 360 + 360) % 360;
    }

    /** True while the sun climbs (the next minute is higher). */
    public boolean rising(long absDay, int minuteOfDay) {
        int next = minuteOfDay + 1;
        long d = next >= Calendar.MINUTES_PER_DAY ? absDay + 1 : absDay;
        return elevation(d, next % Calendar.MINUTES_PER_DAY) > elevation(absDay, minuteOfDay);
    }

    public int sunriseMinute(long absDay) { return sunrise[row(absDay)]; }

    public int sunsetMinute(long absDay) { return sunset[row(absDay)]; }

    /** Hours of daylight (sunrise to sunset at −0.833°); 24 or 0 when the sun never sets or rises. */
    public double daylightHours(long absDay) {
        int d = row(absDay);
        if (sunrise[d] < 0) return elev[d][12] > 0 ? 24 : 0;
        return (sunset[d] - sunrise[d]) / 60.0;
    }

    /** Dawn of the morning of {@code absDay}, minutes after its midnight. */
    public int dawnMinute(long absDay) { return dawn[row(absDay)]; }

    /** Dusk of the evening of {@code absDay}, minutes after its midnight; over 1,440 when after midnight. */
    public int duskMinute(long absDay) { return dusk[row(absDay)]; }

    public boolean dawnIsFallback(long absDay) { return dawnFallback[row(absDay)]; }

    public boolean duskIsFallback(long absDay) { return duskFallback[row(absDay)]; }

    static int row(long absDay) { return (int) Math.floorMod(absDay, (long) Calendar.DAYS_PER_YEAR); }

    private static double lerp(float[][] t, long absDay, int minuteOfDay) {
        int d = row(absDay);
        int h = minuteOfDay / 60;
        double f = (minuteOfDay % 60) / 60.0;
        double a = t[d][h];
        double b = h == 23 ? t[row(absDay + 1)][0] : t[d][h + 1];
        return a + (b - a) * f;
    }

    /** The night before the day: from 12:00 of the previous day to 12:00 of this one. */
    private void computeDawn(int d) {
        int lowest = -720;
        double low = Double.MAX_VALUE;
        for (int m = -720; m <= 720; m++) {
            double e = elevationAt(d, m);
            if (e < low) {
                low = e;
                lowest = m;
            }
        }
        double prev = elevationAt(d, lowest);
        for (int m = lowest + 1; m <= 720; m++) {
            double e = elevationAt(d, m);
            if (prev < TWILIGHT_DEG && e >= TWILIGHT_DEG) {
                dawn[d] = m;
                return;
            }
            prev = e;
        }
        dawn[d] = lowest + FALLBACK_MIN;
        dawnFallback[d] = true;
    }

    /** The night after the day: from 12:00 of this day to 12:00 of the next. */
    private void computeDusk(int d) {
        int lowest = 720;
        double low = Double.MAX_VALUE;
        for (int m = 720; m <= 2160; m++) {
            double e = elevationAt(d, m);
            if (e < low) {
                low = e;
                lowest = m;
            }
        }
        double prev = elevationAt(d, 720);
        for (int m = 721; m <= lowest; m++) {
            double e = elevationAt(d, m);
            if (prev >= TWILIGHT_DEG && e < TWILIGHT_DEG) {
                dusk[d] = m;
                return;
            }
            prev = e;
        }
        dusk[d] = lowest - FALLBACK_MIN;
        duskFallback[d] = true;
    }
}

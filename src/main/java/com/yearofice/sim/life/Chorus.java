package com.yearofice.sim.life;

import com.yearofice.sim.time.Calendar;

/**
 * The dawn chorus (Birds and the air › The dawn chorus): C = v(month) x g(t - dawn) x w(weather). Month curve v is the
 * valley's singers together; timing g peaks at 03:30 through the white nights (May 15 to July 28) and at sunrise
 * otherwise, starting 45 minutes before it and lasting 2 hours. Pure Java. Original Year of Ice code.
 */
public final class Chorus {
    private Chorus() {}

    /** Song month curve, January to December (the loudest month is June; July halves it). */
    static final double[] MONTH = {0.05, 0.05, 0.25, 0.6, 0.9, 1.0, 0.5, 0.3, 0.15, 0.15, 0.05, 0.05};

    public static double month(int month) { return MONTH[month - 1]; }

    public static boolean whiteNights(int doy) {
        return doy >= Calendar.dayOfYear(5, 15) && doy <= Calendar.dayOfYear(7, 28);
    }

    /** Timing, 0 to 1: a triangle over the chorus window, peaking at 03:30 in the white nights, else at sunrise. */
    public static double timing(int doy, int minuteOfDay, int sunriseMin) {
        int from, peak, to;
        if (whiteNights(doy)) {
            from = 150; peak = 210; to = 330;
        } else {
            from = sunriseMin - 45; peak = sunriseMin; to = sunriseMin + 75;
        }
        if (minuteOfDay <= from || minuteOfDay >= to) return 0;
        return minuteOfDay <= peak ? (minuteOfDay - from) / (double) (peak - from) : (to - minuteOfDay) / (double) (to - peak);
    }

    /** Rain x0.3, wind over 8 m/s x0.4, a May cold snap under -5 C x0.2, the first clear dawn after a storm x1.3; they multiply. */
    public static double weather(boolean rain, double windMs, double airC, int month, boolean firstClearAfterStorm) {
        double w = 1;
        if (rain) w *= 0.3;
        if (windMs > 8) w *= 0.4;
        if (month == 5 && airC < -5) w *= 0.2;
        if (firstClearAfterStorm) w *= 1.3;
        return w;
    }

    public static double volume(int doy, int minuteOfDay, int sunriseMin, double weatherMult) {
        return month(Calendar.month(doy)) * timing(doy, minuteOfDay, sunriseMin) * weatherMult;
    }

    /** Voices the client mixes: proportional to the volume, 1 at the quietest audible dawn, capped. */
    public static int voices(double volume, int cap) {
        if (volume <= 0.02) return 0;
        return Math.max(1, Math.min(cap, (int) Math.ceil(volume * cap)));
    }
}

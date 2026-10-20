package com.yearofice.sim.fire;

/**
 * How a time left is worded (Every concept from the video › Fire time and the fire roster): "Times under 1 h show
 * minutes, then 'h min', then 'about {n} days'." Game time, so it holds under any clock rate. Pure Java.
 */
public final class FireTime {
    private FireTime() {}

    /** A day of game minutes. */
    private static final int DAY = 1440;

    public static String format(double minutes) {
        int m = (int) Math.round(Math.max(0, minutes));
        if (m < 60) return m + " min";
        if (m < DAY) return m % 60 == 0 ? (m / 60) + " h" : (m / 60) + " h " + (m % 60) + " min";
        long days = Math.round(m / (double) DAY);
        return "about " + days + (days == 1 ? " day" : " days");
    }
}

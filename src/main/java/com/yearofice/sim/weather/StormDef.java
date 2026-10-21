package com.yearofice.sim.weather;

import java.util.Map;

/**
 * One storm from {@code weather/storm/<id>.json}. Ranges are {min, max}; {@code startPerDay} is the chance a storm of this
 * id starts on a day of each calendar month (January first); {@code tempC} gates on the day's mean air; {@code then} is
 * the Markov row: the chance a given storm follows this one straight away.
 */
public record StormDef(String id, StormKind kind, StormKind.PrecipKind precip, double[] startPerDay, double[] startHour,
                       double[] durationH, double[] intensity, double[] windMs, double[] precipMmH, double[] visibilityM,
                       double[] tempC, boolean whiteout, double strikeEveryMin, Map<String, Double> then) {

    public double chance(int month) { return startPerDay[month - 1]; }

    static double lerp(double[] r, double t) { return r[0] + (r[1] - r[0]) * t; }
}

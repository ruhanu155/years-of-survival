package com.yearofice.sim.ecosystem;

/**
 * The slice's population hook (the first form of Ecology's counts): animals per km² of good habitat at K, times
 * the share of good habitat, times the year's multiplier. A trap or a sensor asks how many individuals' home
 * ranges cover a point; a spawner asks how many live in a cell. Pure Java.
 */
public final class Density {
    private Density() {}

    /** Individuals whose home range (a circle of radius r) covers a point. */
    public static double coveringPoint(double perKm2, double homeRangeM, double habitatShare, double multiplier) {
        double areaKm2 = Math.PI * homeRangeM * homeRangeM / 1e6;
        return Math.max(0, perKm2 * areaKm2 * clamp01(habitatShare) * Math.max(0, multiplier));
    }

    /** Expected individuals (or groups) in a square cell. */
    public static double inCell(double perKm2, double cellM, double habitatShare, double multiplier) {
        return Math.max(0, perKm2 * cellM * cellM / 1e6 * clamp01(habitatShare) * Math.max(0, multiplier));
    }

    /** floor(E), plus one more when u falls under the fraction: a whole count from an expectation. */
    public static int count(double expected, double u) {
        int n = (int) Math.floor(expected);
        return u < expected - n ? n + 1 : n;
    }

    static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}

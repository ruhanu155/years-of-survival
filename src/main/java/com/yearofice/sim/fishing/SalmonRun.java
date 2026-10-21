package com.yearofice.sim.fishing;

import java.util.Map;

/**
 * Salmon runs by date (Blueprint › Fishing in depth › Salmon runs; Waters and fish › Salmon runs in detail).
 * Salmon have no resident stock: each year draws a run size {@code R = R_mean · y · e}, y triangular from 0.5
 * to 1.6 (mean 1.0), e the echo of the harvest share {@code returnsAfter} years earlier
 * ({@link FishStock#echo}). Daily passage is a triangle across the window peaking on the peak date; run strength
 * is today's passage over the peak's, 0 to 1. Game-year days count from May 1 ({@link FishSeason}).
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class SalmonRun {
    private SalmonRun() {}

    /** One run's table row. */
    public record Run(String species, int rMean, int from, int peak, int to, int returnsAfter) {}

    public static final Map<String, Run> RUNS = Map.of(
            "chinook_salmon", new Run("chinook_salmon", 200, FishSeason.day(6, 25), FishSeason.day(7, 10), FishSeason.day(7, 31), 5),
            "sockeye_salmon", new Run("sockeye_salmon", 600, FishSeason.day(7, 20), FishSeason.day(8, 1), FishSeason.day(8, 25), 5),
            "chum_salmon", new Run("chum_salmon", 1500, FishSeason.day(7, 15), FishSeason.day(8, 5), FishSeason.day(8, 31), 4),
            "coho_salmon", new Run("coho_salmon", 400, FishSeason.day(8, 10), FishSeason.day(9, 5), FishSeason.day(9, 30), 3),
            "bering_cisco", new Run("bering_cisco", 3000, FishSeason.day(9, 1), FishSeason.day(9, 25), FishSeason.day(10, 15), 4),
            "broad_whitefish", new Run("broad_whitefish", 800, FishSeason.day(9, 10), FishSeason.day(10, 1), FishSeason.day(10, 20), 6),
            "sheefish", new Run("sheefish", 120, FishSeason.day(9, 15), FishSeason.day(9, 28), FishSeason.day(10, 10), 7));

    public static boolean isRun(String species) { return RUNS.containsKey(species); }

    /** Run strength on a game-year day: 0 outside the window, 1 on the peak, linear between. */
    public static double strength(String species, int day) {
        Run r = RUNS.get(species);
        if (r == null) return 0;
        if (day < r.from() || day > r.to()) return 0;
        double s = day <= r.peak() ? (day - r.from() + 1.0) / (r.peak() - r.from() + 1.0) : (r.to() - day + 1.0) / (r.to() - r.peak() + 1.0);
        return Math.max(0, Math.min(1, s));
    }

    /** The year factor y: triangular 0.5 to 1.6 with mode 0.9 (mean 1.0), from a uniform draw. */
    public static double yearFactor(double u) {
        double a = 0.5, b = 1.6, c = 0.9, f = (c - a) / (b - a);
        return u < f ? a + Math.sqrt(u * (b - a) * (c - a)) : b - Math.sqrt((1 - u) * (b - a) * (b - c));
    }

    /** The year's y for a species: a hash of (seed, species, year), so a replay draws the same run. */
    public static double yearFactor(long seed, String species, int year) {
        return yearFactor(Draws.u(seed, year, Draws.name(species), 7));
    }

    /** Run size R for a year, given the harvest share of the run that returned {@code returnsAfter} years before. */
    public static double runSize(long seed, String species, int year, double harvestShareEarlier) {
        Run r = RUNS.get(species);
        if (r == null) return 0;
        return r.rMean() * yearFactor(seed, species, year) * FishStock.echo(harvestShareEarlier);
    }
}

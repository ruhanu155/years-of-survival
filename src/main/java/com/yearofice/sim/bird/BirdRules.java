package com.yearofice.sim.bird;

import java.util.List;

/**
 * The M6 birds' pure rules: which role a species plays, how big its flock is, how a flock member steers, when a
 * scavenger arrives and what it eats, and how often an eagle takes a fish. No Minecraft imports; the adapter in
 * {@code ecosystem.bird} reads the world and calls these. Original Year of Ice code.
 */
public final class BirdRules {
    private BirdRules() {}

    public enum Role { CORVID, JAY, RAPTOR, OWL, DABBLER, DIVER, GULL }

    /** The role of a species path, or null for a species this lane does not own. */
    public static Role role(String path) {
        return switch (path) {
            case "raven" -> Role.CORVID;
            case "gray_jay", "stellers_jay" -> Role.JAY;
            case "bald_eagle" -> Role.RAPTOR;
            case "great_horned_owl" -> Role.OWL;
            case "mallard", "northern_pintail" -> Role.DABBLER;
            case "common_loon" -> Role.DIVER;
            case "mew_gull" -> Role.GULL;
            default -> null;
        };
    }

    public static boolean flocks(Role r) { return r == Role.DABBLER || r == Role.GULL; }

    public static boolean scavenges(Role r) { return r == Role.CORVID || r == Role.JAY || r == Role.RAPTOR || r == Role.GULL; }

    /** Members of a flock for a uniform draw u in [0, 1); one for the birds that fly alone. */
    public static int flockSize(Role r, BirdTuning t, double u) {
        int lo, hi;
        switch (r) {
            case DABBLER -> { lo = t.dabblerMin(); hi = t.dabblerMax(); }
            case GULL -> { lo = t.gullMin(); hi = t.gullMax(); }
            default -> { return 1; }
        }
        return lo + (int) Math.min(hi - lo, Math.floor(u * (hi - lo + 1)));
    }

    /**
     * One steering step for a flock member at (x, z) among the others ({@code others[i] = {x, z}}): toward the
     * flock's centre when farther from it than the cohesion radius, away from a neighbour closer than the
     * separation distance. Returns the displacement {dx, dz} for this step, at most {@code step} long.
     */
    public static double[] steer(double x, double z, List<double[]> others, BirdTuning t, double step) {
        if (others.isEmpty()) return new double[] {0, 0};
        double cx = 0, cz = 0;
        for (double[] o : others) { cx += o[0]; cz += o[1]; }
        cx /= others.size();
        cz /= others.size();
        double dx = 0, dz = 0;
        double toC = Math.hypot(cx - x, cz - z);
        if (toC > t.flockCohesionM()) {
            dx += (cx - x) / toC;
            dz += (cz - z) / toC;
        }
        for (double[] o : others) {
            double d = Math.hypot(o[0] - x, o[1] - z);
            if (d < t.flockSeparationM()) {
                if (d < 1e-6) { dx += 1; continue; }
                dx += (x - o[0]) / d;
                dz += (z - o[1]) / d;
            }
        }
        double len = Math.hypot(dx, dz);
        if (len < 1e-9) return new double[] {0, 0};
        double s = Math.min(step, len * step);
        return new double[] {dx / len * s, dz / len * s};
    }

    /** Game minutes after a death at which this scavenger arrives, for a stable draw u in [0, 1). */
    public static double arrivalDelayMin(Role r, BirdTuning t, double u) {
        return switch (r) {
            case CORVID -> u * t.corvidArriveMin();
            case JAY -> 30 + u * t.corvidArriveMin();
            case GULL -> 60 + u * t.corvidArriveMin();
            default -> 120 + u * 240; // eagles come on the day
        };
    }

    /** Grams a scavenger of this role eats from a carcass in the given game minutes. */
    public static double eatGrams(Role r, BirdTuning t, double minutes) {
        double kgPerDay = switch (r) {
            case CORVID -> t.ravenKgPerDay();
            case RAPTOR -> t.eagleKgPerDay();
            case JAY -> t.jayGramsPerVisit() / 1000.0 * 6; // a visit now and then: 6 a day at most
            case GULL -> 0.2;
            default -> 0;
        };
        return kgPerDay * 1000.0 * minutes / 1440.0;
    }

    /** Chance an eagle takes a fish in the given game minutes from a spot at stock fraction {@code frac} of K. */
    public static double eagleTakeChance(BirdTuning t, double frac, double minutes) {
        double perMin = t.eagleFishPerDay() * Math.max(0, Math.min(1, frac)) / 1440.0;
        return 1.0 - Math.pow(1.0 - Math.min(0.999, perMin), Math.max(0, minutes));
    }

    /** Months a species is in the valley (1 to 12): migrants come after ice-out and leave before freeze-up. */
    public static boolean inSeason(String path, int month) {
        return switch (path) {
            case "northern_pintail" -> month >= 4 && month <= 9;
            case "mallard" -> month >= 4 && month <= 10;
            case "common_loon", "mew_gull" -> month >= 5 && month <= 9;
            case "bald_eagle" -> month >= 4 && month <= 11;
            default -> true;
        };
    }

    /** Waking hours: the owl is out at night, every other bird by day (hour 0 to 23, daylight from 5 to 21). */
    public static boolean active(Role r, int hour) {
        boolean day = hour >= 5 && hour < 22;
        return r == Role.OWL ? !day : day;
    }
}

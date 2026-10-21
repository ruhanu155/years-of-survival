package com.yearofice.sim.ecosystem;

import com.yearofice.sim.time.Calendar;

/**
 * The valley's off-screen population (RECONCILE 306): bears, wolf packs, wolverines and lynx that exist as counted
 * individuals with a home range, and the arithmetic that lets a scent pull one of them toward a player. Pure Java:
 * the adapter ({@code ecosystem.pool.ValleyPool}) owns the SavedData, the sources and the bodies.
 *
 * <p>A source's reach is {@code baseM × strength × wind factor × rain factor}, a cone along the wind (30 degrees
 * either side) plus a short all-round bubble. An individual inside it rolls interest each game hour:
 * {@code curiosity × (0.3 + hunger) × season × interestScale}, and on a hit walks at its approach speed to the source.
 */
public final class PoolModel {
    private PoolModel() {}

    public enum Mode { RANGING, DENNED, APPROACHING, RETURNING }

    /** Half-angle of the scent cone, and the share of the reach that smells the same in every direction. */
    public static final double CONE_DEG = 30, BUBBLE = 0.12;
    /** An abstract approach ends this far from the source (the bear is "at the pile"). */
    public static final double ARRIVE_M = 12;

    public record Kind(String id, double curiosity, double hungerPerH) {}

    public static Kind kind(String id) {
        String p = id.substring(id.indexOf(':') + 1);
        return switch (p) {
            case "grizzly" -> new Kind(id, 0.22, 0.012);
            case "black_bear" -> new Kind(id, 0.28, 0.012);
            case "wolf" -> new Kind(id, 0.12, 0.015);
            case "wolverine" -> new Kind(id, 0.25, 0.015);
            default -> new Kind(id, 0.10, 0.015); // lynx: a hunter, not a scavenger
        };
    }

    /** One individual (a bear, a wolf pack): where it lives and what it is doing, abstractly. */
    public static final class Indiv {
        public String id, species, region;
        public double homeX, homeZ, x, z, tx, tz, hunger;
        public int members = 1, live;
        public boolean dead;
        public Mode mode = Mode.RANGING;
        public long sourceKey; // the source an approach is aimed at
    }

    /** Scent scale from the wind: calm air carries it half as far, a 6 m/s wind one and a half times, capped at 2. */
    public static double windFactor(double windMs) { return Math.max(0.5, Math.min(2.0, 0.5 + windMs / 6.0)); }

    /** Rain washes scent out: 1 dry, a third at 2 mm/h. */
    public static double rainFactor(double precipMmH) { return 1.0 / (1.0 + Math.max(0, precipMmH)); }

    public static double reachM(double baseM, double strength, double windMs, double precipMmH, double reachScale) {
        return baseM * Math.max(0, Math.min(1, strength)) * windFactor(windMs) * rainFactor(precipMmH) * reachScale;
    }

    /** Minecraft yaw from one point toward another, as {@code Sensing.bearing} computes it. */
    public static double bearing(double fx, double fz, double tx, double tz) {
        return Math.atan2(tz - fz, tx - fx) * (180.0 / Math.PI) - 90.0;
    }

    /** True when the individual smells the source: inside the bubble, or inside the cone down the wind and the reach. */
    public static boolean inReach(double sx, double sz, double reachM, double windTowardDeg, double ix, double iz) {
        double d = Math.hypot(ix - sx, iz - sz);
        if (d <= reachM * BUBBLE) return true;
        if (d > reachM) return false;
        double off = Math.abs(wrap(bearing(sx, sz, ix, iz) - windTowardDeg));
        return off <= CONE_DEG;
    }

    static double wrap(double deg) {
        double d = deg % 360;
        if (d > 180) d -= 360;
        if (d <= -180) d += 360;
        return d;
    }

    /** Bears are keen as they fatten for the den (Sep 1 to Oct 19); a spring bear is hungry too; denned ones ignore all. */
    public static double season(String species, int doy) {
        if (Dormancy.denned(species, doy)) return 0;
        boolean bear = species.endsWith("bear") || species.endsWith("grizzly");
        if (bear) {
            if (Calendar.within(doy, Calendar.dayOfYear(9, 1), Calendar.dayOfYear(10, 19))) return 2.0;
            if (Calendar.within(doy, Calendar.dayOfYear(4, 21), Calendar.dayOfYear(6, 15))) return 1.2;
            return 1.0;
        }
        return Calendar.within(doy, Calendar.dayOfYear(11, 1), Calendar.dayOfYear(3, 31)) ? 1.4 : 1.0;
    }

    /** Chance per game hour that an individual in reach starts an approach. */
    public static double interestPerHour(Kind k, double hunger, double season, double interestScale) {
        return Math.max(0, Math.min(0.9, k.curiosity() * (0.3 + Math.max(0, Math.min(1, hunger))) * season * interestScale));
    }

    /** One game hour of hunger. */
    public static double hungrier(Kind k, double hunger) { return Math.min(1, hunger + k.hungerPerH()); }

    /**
     * Moves the individual {@code dt} seconds toward {@code (tx, tz)} at {@code mps}, stopping {@link #ARRIVE_M} short.
     * Returns the distance walked.
     */
    public static double walk(Indiv a, double tx, double tz, double mps, double dtS) {
        double d = Math.hypot(tx - a.x, tz - a.z);
        double goal = Math.max(0, d - ARRIVE_M);
        double step = Math.min(goal, mps * dtS);
        if (d > 0) {
            a.x += (tx - a.x) / d * step;
            a.z += (tz - a.z) / d * step;
        }
        return step;
    }

    /**
     * Where the segment {@code (x0,z0) -> (x1,z1)} first enters the circle of radius {@code r} around {@code (cx,cz)}, or
     * null when it never does (a segment that starts inside enters at its start).
     */
    public static double[] entry(double x0, double z0, double x1, double z1, double cx, double cz, double r) {
        double dx = x1 - x0, dz = z1 - z0, fx = x0 - cx, fz = z0 - cz;
        if (fx * fx + fz * fz <= r * r) return new double[] {x0, z0};
        double a = dx * dx + dz * dz, b = 2 * (fx * dx + fz * dz), c = fx * fx + fz * fz - r * r;
        if (a == 0) return null;
        double disc = b * b - 4 * a * c;
        if (disc < 0) return null;
        double t = (-b - Math.sqrt(disc)) / (2 * a);
        if (t < 0 || t > 1) return null;
        return new double[] {x0 + t * dx, z0 + t * dz};
    }
}

package com.yearofice.sim.companion;

/** The sled team's pull, load and speed (Ideas from viewers › wolves pull sleds; Minecraft development › Pack and sled). */
public final class SledMath {
    private SledMath() {}

    public enum Surface { LANE, SNOW, ICE, BARE }

    public static double factor(CompanionTuning t, Surface s) {
        return switch (s) { case LANE -> t.sledLaneFactor(); case SNOW -> t.sledSnowFactor(); case ICE -> t.sledIceFactor(); case BARE -> t.sledBareFactor(); };
    }

    public static int team(CompanionTuning t, int wolves) { return Math.max(0, Math.min(t.sledMaxTeam(), wolves)); }

    /** Most kg (rider and cargo) the team moves on this surface: each wolf's pull, scaled by the surface. */
    public static double capacityKg(CompanionTuning t, int wolves, Surface s) {
        return team(t, wolves) * t.sledPullKg() * factor(t, s);
    }

    /** m/s for a team on a surface under a load; 0 when under the two-wolf minimum or over capacity. */
    public static double speedMps(CompanionTuning t, int wolves, Surface s, double loadKg) {
        int n = team(t, wolves);
        if (n < 2) return 0;
        double cap = capacityKg(t, n, s);
        if (loadKg > cap) return 0;
        double teamFactor = Math.min(1.0, 0.6 + 0.1 * n);
        return t.sledTrotMps() * factor(t, s) * teamFactor * (1 - 0.5 * loadKg / cap);
    }
}

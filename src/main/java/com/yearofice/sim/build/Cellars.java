package com.yearofice.sim.build;

/**
 * Cellar and container climate (Decay and time › Snow as a cold store; Every concept › Root cellar). No Minecraft
 * imports. A cellar sits between the ground and the air by how well it is banked; ice packing holds it at 0 C.
 */
public final class Cellars {
    private Cellars() {}

    /** Share of the ground's temperature a root cellar takes: 0.6 bare, up to 0.97 fully banked under 3 m of cover. */
    public static double rootInsulation(double bankShare, double coverM) {
        double b = Math.max(0, Math.min(1, bankShare));
        return Math.min(0.97, 0.6 + 0.3 * b + 0.03 * Math.max(0, Math.min(3, coverM)));
    }

    public static double rootC(double groundC, double airC, double bankShare, double coverM) {
        double k = rootInsulation(bankShare, coverM);
        return k * groundC + (1 - k) * airC;
    }

    /**
     * Permafrost cellar: the frozen ground (never taken warmer than -1 C) and the air, by the packing of ice and
     * sawdust round the room; while the packing is at least half ice it holds the room at 0 C or colder.
     */
    public static double permafrostC(double groundC, double airC, double packShare) {
        double p = Math.max(0, Math.min(1, packShare));
        double k = Math.min(0.98, 0.7 + 0.28 * p);
        double t = k * Math.min(groundC, -1.0) + (1 - k) * airC;
        return p >= 0.5 ? Math.min(t, 0.0) : t;
    }

    /** A container's temperature: the air, pulled toward the day's mean by how well it buffers the swing (0 open rail, 1 sealed). */
    public static double containerC(double airC, double dayMeanC, double buffer) {
        return airC + Math.max(0, Math.min(1, buffer)) * (dayMeanC - airC);
    }
}

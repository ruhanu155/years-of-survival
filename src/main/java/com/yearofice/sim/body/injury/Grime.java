package com.yearofice.sim.body.injury;

/**
 * The grime meter (Ideas from viewers › Cauterizing and grime): 0 clean to 100 filthy. Work and wounds dirty the
 * body; washing clears it (soap all of it, plain water half). Grime multiplies the chance a wound infects.
 */
public final class Grime {
    private Grime() {}

    public static final double PER_WORK_MINUTE = 0.05;

    public static double work(double grime, double workMinutes) {
        return Math.min(100, grime + PER_WORK_MINUTE * workMinutes);
    }

    public static double wash(double grime, boolean soap) { return soap ? 0 : grime / 2; }

    /** 1 clean, up to 2 filthy. */
    public static double infectionMult(double grime) { return 1 + Math.min(100, Math.max(0, grime)) / 100.0; }

    public static String word(double grime) {
        if (grime < 25) return "clean";
        if (grime < 60) return "grimy";
        return "filthy";
    }
}

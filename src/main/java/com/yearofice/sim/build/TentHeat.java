package com.yearofice.sim.build;

/**
 * B11, the hot tent as a lumped room (Structures and buildables › B11): one air node of {@code capWhC} Wh/°C losing
 * {@code lossWpc} W/°C to the outside and gaining the stove's heat. Exact exponential step, so any step length gives
 * the same answer. Pure.
 */
public final class TentHeat {
    private TentHeat() {}

    /** The tent's whole-envelope loss: cover plus floor (boughs 3 W/°C, bare snow 10), plus a leak per spark hole. */
    public static double lossWpc(double coverWpc, double floorWpc, int sparkHoles, double holeWpc) {
        return coverWpc + floorWpc + sparkHoles * holeWpc;
    }

    /** The steady inside temperature with the stove fed: outside + kept heat / loss. */
    public static double steadyC(double outC, double stoveKw, double keptShare, double lossWpc) {
        return outC + stoveKw * 1000 * keptShare / lossWpc;
    }

    /** Inside temperature after {@code hours} with constant heat {@code watts} (0 once the fire dies). */
    public static double step(double inC, double outC, double watts, double lossWpc, double capWhC, double hours) {
        double target = outC + watts / lossWpc;
        double tau = capWhC / lossWpc;
        return target + (inC - target) * Math.exp(-hours / tau);
    }

    /** Chance a night burned on Hot holes the canvas: the base rate, a quarter of it for hide, none behind a spark screen. */
    public static double sparkHoleChance(double baseHot, boolean hide, boolean screen) {
        return screen ? 0 : baseHot * (hide ? 0.25 : 1.0);
    }
}

package com.yearofice.sim.build;

/**
 * The log raft (Open world in depth › Traversal › Raft): it drifts at the current, 0.8-1.5 m/s on the main river,
 * is poled upstream at 0.25 m/s in slow water only, carries 200 kg, and breaks up in rapids. Pure.
 */
public final class RaftModel {
    private RaftModel() {}

    public static final double CAPACITY_KG = 200;
    public static final double POLE_MS = 0.25;
    /** Water slower than this lets the pole push upstream. */
    public static final double SLOW_WATER_MS = 0.5;
    /** Current at which a raft starts to break up. */
    public static final double RAPIDS_MS = 2.0;

    /** Metres a second along the flow: drifts at the current; a pole adds {@value #POLE_MS} against it in slow water. */
    public static double speedMs(double currentMs, boolean poling) {
        if (poling && currentMs < SLOW_WATER_MS) return currentMs - POLE_MS;
        return currentMs;
    }

    /** Chance a second that the lashings give: none below the rapids, 10% a second in them (more at 2.5), +20% overloaded. */
    public static double breakPerS(double currentMs, double loadKg) {
        double p = currentMs < RAPIDS_MS ? 0 : 0.1 + 0.2 * Math.min(1, (currentMs - RAPIDS_MS) / 1.0);
        if (loadKg > CAPACITY_KG && currentMs >= 0.5) p += 0.2;
        return Math.min(1, p);
    }
}

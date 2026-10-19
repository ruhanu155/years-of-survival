package com.yearofice.sim.ecosystem;

/**
 * Turns a gait in m/s (Land animals in depth › The roster and sheet) into Minecraft's movement input.
 *
 * <p>On level ground a mob's move control sets both its speed and its forward input to s, so it accelerates by s²
 * a tick, and ground friction keeps 0.546 of its velocity each tick (block friction 0.6 × 0.91). The terminal
 * speed is s² ÷ 0.454 blocks a tick, 44 s² m/s; so s = √(v ÷ 44). With the movement-speed attribute at 1.0 the
 * navigation speed modifier is s itself. Pure Java.
 */
public final class Gaits {
    private Gaits() {}

    public static final double GROUND_KEEP = 0.6 * 0.91;
    public static final double MPS_PER_S2 = 20.0 / (1.0 - GROUND_KEEP);

    /** The speed input s for a ground speed in m/s. */
    public static double input(double mps) { return Math.sqrt(Math.max(0, mps) / MPS_PER_S2); }

    /** The ground speed in m/s that an input s reaches. */
    public static double mps(double input) { return input * input * MPS_PER_S2; }
}

package com.yearofice.sim.livestock;

/** Predators and escapes (Raising animals › Predators know, Escapes). */
public final class PenRisk {
    private PenRisk() {}

    public enum Fence { NONE, BRUSH, POLE, STOCKADE }

    /** One night's attack chance on a pen when predators are in the region: brush 25%, poles 8%, stockade 2%. */
    public static double attackChance(Fence f, boolean bondedWolfNear, boolean doorOpen, LivestockTuning t) {
        Fence eff = doorOpen && f.ordinal() > Fence.BRUSH.ordinal() ? Fence.BRUSH : f;
        double base = switch (eff) { case NONE -> 0.40; case BRUSH -> 0.25; case POLE -> 0.08; case STOCKADE -> 0.02; };
        if (bondedWolfNear) base *= 0.3;
        return Math.min(1, base * t.predatorScale());
    }

    /** A goat gets out of a brush fence 5% of days, a pole fence 1%. */
    public static double escapeChance(Fence f, LivestockTuning t) {
        double base = switch (f) { case NONE -> 1.0; case BRUSH -> 0.05; case POLE -> 0.01; case STOCKADE -> 0.0; };
        return Math.min(1, base * t.escapeScale());
    }

    /** Unpenned stock a predator takes in a night: the same roll, with no fence at all. */
    public static double unpennedChance(LivestockTuning t) { return attackChance(Fence.NONE, false, false, t); }
}

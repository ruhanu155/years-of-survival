package com.yearofice.sim.fishing;

/**
 * What the fight reads from tackle (Blueprint › Fishing in depth › Tackle): the line's strength, the rod's flex,
 * the retrieve speed and pull, line carried, and the hook.
 *
 * <pre>
 *   S_eff = S_rated · w_wet · (0.7 + 0.3 · condition / 100)
 * </pre>
 *
 * Tension 100% on the fight's arc is always S_eff right now. Pure Java. Original Year of Ice code.
 */
public record Tackle(double lineRatedKg, double wetFactor, boolean wet, double conditionPct, double lineCarriedM,
                     double flex, double retrieveSpeedMps, double retrievePullKg, boolean drag, double dragShare,
                     boolean handLine, boolean gorge, boolean wireLeader) {

    public static final double LIFT_FLEX = 0.2;
    public static final double GIVE_LINE_ROD = 0.4;
    public static final double GIVE_LINE_HAND = 0.5;

    public double effectiveStrengthKg() {
        return lineRatedKg * (wet ? wetFactor : 1.0) * (0.7 + 0.3 * conditionPct / 100.0);
    }

    /** Share of the fish's pull felt while giving line: 0.4, or 0.5 through the fingers on a hand line. */
    public double giveLineShare() { return handLine ? GIVE_LINE_HAND : GIVE_LINE_ROD; }

    public Tackle withWet(boolean w) {
        return new Tackle(lineRatedKg, wetFactor, w, conditionPct, lineCarriedM, flex, retrieveSpeedMps,
                retrievePullKg, drag, dragShare, handLine, gorge, wireLeader);
    }

    public Tackle withCondition(double c) {
        return new Tackle(lineRatedKg, wetFactor, wet, c, lineCarriedM, flex, retrieveSpeedMps,
                retrievePullKg, drag, dragShare, handLine, gorge, wireLeader);
    }

    public Tackle withLeader(boolean leader) {
        return new Tackle(lineRatedKg, wetFactor, wet, conditionPct, lineCarriedM, flex, retrieveSpeedMps,
                retrievePullKg, drag, dragShare, handLine, gorge, leader);
    }

    /** Plant-fiber line (4 kg, wet ×0.75) on a willow rod and winder, steel hook. */
    public static Tackle plantFiberWillow() {
        return new Tackle(4.0, 0.75, false, 100, 30, 0.3, 0.7, 1.5, false, 0, false, false, false);
    }

    /** Sinew line (7 kg, wet ×0.85) on a willow rod and wooden reel. */
    public static Tackle sinewReel() {
        return new Tackle(7.0, 0.85, false, 100, 40, 0.3, 1.0, 1.0, false, 0, false, false, false);
    }

    /** Monofilament (10 kg) on the steel rod and spinning reel with drag at 80%. */
    public static Tackle monoSpinning() {
        return new Tackle(10.0, 1.0, false, 100, 100, 0.5, 1.4, 1.0, true, 0.8, false, false, true);
    }

    /** A hand line of plant fiber: flex 0, 30 m on a winder stick. */
    public static Tackle plantFiberHandLine() {
        return new Tackle(4.0, 0.75, false, 100, 30, 0.0, 0.5, 1.5, false, 0, true, false, false);
    }
}

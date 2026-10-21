package com.yearofice.sim.ecosystem;

/**
 * The bear's contact sequence (Blueprint › Danger in detail › How a bear decides): notice (stand, sniff, stare 5-15 s),
 * huff and jaw-pop, then a rush that a bluff stops 3-10 m short and a real one carries to contact. The numbers; the
 * outcome rolls are {@code sim.hunt.EncounterRules.bearCharge}'s. Pure Java.
 */
public final class BearContact {
    private BearContact() {}

    public enum Phase { NONE, NOTICE, HUFF, CHARGE, LEAVE }

    /** Surprised inside 25 m is Defensive; a bear on a carcass or gut pile is Defensive inside 50 m. */
    public static final double SURPRISED_M = 25, ON_CARRION_M = 50;
    public static final int HUFF_TICKS = 30, RUSH_TIMEOUT_TICKS = 300, LEAVE_TICKS = 100, COOLDOWN_TICKS = 2400;
    public static final double CONTACT_M = 2.5, STOP_MIN_M = 3, STOP_MAX_M = 10;

    public static boolean provokes(double distM, boolean playerMoved, boolean onCarrion) {
        return (distM <= SURPRISED_M && playerMoved) || (onCarrion && distM <= ON_CARRION_M);
    }

    public static int noticeTicks(double roll) { return (int) Math.round((5 + 10 * clamp(roll)) * 20); }

    public static double bluffStopM(double roll) { return STOP_MIN_M + (STOP_MAX_M - STOP_MIN_M) * clamp(roll); }

    /** Fast is a run (it chases), slow is backing away, still is standing ground. */
    public static com.yearofice.sim.hunt.EncounterRules.Reaction reaction(double playerMps) {
        if (playerMps > 3.5) return com.yearofice.sim.hunt.EncounterRules.Reaction.RUN;
        if (playerMps > 0.5) return com.yearofice.sim.hunt.EncounterRules.Reaction.BACK_AWAY;
        return com.yearofice.sim.hunt.EncounterRules.Reaction.STAND_GROUND;
    }

    static double clamp(double v) { return Math.max(0, Math.min(1, v)); }
}

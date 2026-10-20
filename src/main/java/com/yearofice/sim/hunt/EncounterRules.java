package com.yearofice.sim.hunt;

/**
 * Danger in detail (Blueprint › Danger in detail › How a bear decides; Every concept › Wounds that last): the choices
 * of a bear that has noticed the player and the shape of a wolf pack's circling. Pure arithmetic; the adapter posts
 * {@code EncounterStateEvent}s from the outcome.
 */
public final class EncounterRules {
    private EncounterRules() {}

    public enum Charge { NONE, BLUFF, CONTACT }

    public enum Reaction { STAND_GROUND, BACK_AWAY, RUN, PLAY_DEAD, SPRAY }

    /**
     * @param distM      distance when the bear commits
     * @param wounded    a wounded bear's aggression rises one step: fewer bluffs
     * @param grizzly    grizzlies defend, so a rush is more often real
     * @param reaction   what the player does
     * @param roll       uniform [0,1)
     */
    public static Charge bearCharge(double distM, boolean wounded, boolean grizzly, Reaction reaction, double roll, HuntTuning t) {
        if (reaction == Reaction.RUN) return distM <= 40 ? Charge.CONTACT : Charge.NONE; // running triggers a chase
        if (distM > 25) return Charge.NONE;
        double bluff = t.bluffShare() * (wounded ? 0.5 : 1.0) * (grizzly ? 0.8 : 1.0);
        if (reaction == Reaction.SPRAY && distM <= 8) bluff = Math.min(0.97, bluff + 0.25);
        if (reaction == Reaction.STAND_GROUND) bluff = Math.min(0.95, bluff + 0.1);
        return roll < bluff ? Charge.BLUFF : Charge.CONTACT;
    }

    /** Wolves close and testing: circle at 15-25 m, at a speed that closes a full lap in about 20 s, narrowing as the player stays alone. */
    public static double circleRadiusM(int wolves, double secondsSeen) {
        double base = 25 - Math.min(10, secondsSeen / 6.0);
        return Math.max(12, base - Math.max(0, wolves - 2));
    }

    /** True for a pack of three or more that has circled for 20 s and finds the player alone and still. */
    public static boolean packRushes(int wolves, double secondsSeen, boolean playerAlone, boolean fire) {
        return wolves >= 3 && secondsSeen >= 20 && playerAlone && !fire;
    }
}

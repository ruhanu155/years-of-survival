package com.yearofice.sim.wood;

import java.util.random.RandomGenerator;

/** Felling mishaps that need only numbers (Trees, felling and firewood › What goes wrong). Pure. */
public final class Mishap {
    private Mishap() {}

    /** The ways a fell goes wrong that hurt. */
    public enum Kind {
        /** The trunk splits up the middle and the butt kicks back: injury source {@code kickback}. */
        BARBER_CHAIR("kickback"),
        /** A hung tree drops on someone in its line: {@code widowmaker}. */
        HUNG_TREE("widowmaker"),
        /** The trunk lands on the player: {@code falling_tree}. */
        FALLING_TREE("falling_tree"),
        /** The crown's fan or a thin trunk: {@code glancing_branch}. */
        GLANCING_BRANCH("glancing_branch");

        public final String injurySource;

        Kind(String s) { injurySource = s; }
    }

    /** 30% when the lean is over 15 degrees and the back cut came before the notch was done (or every swing from one side); a wedge stops it. */
    public static double barberChance(double leanDeg, boolean backCutFirst, boolean oneSided, boolean wedge, double base) {
        if (wedge || leanDeg <= 15) return 0;
        return backCutFirst || oneSided ? base : 0;
    }

    /** 50% when the fall line passes within a block of a living trunk taller than half the felled tree. */
    public static double hangUpChance(double lineDistM, double otherHeightM, double felledHeightM, double base) {
        return lineDistM <= 1.0 && otherHeightM > felledHeightM / 2 ? base : 0;
    }

    /** Whether a mishap of chance {@code p} happens on this roll of {@code r}. */
    public static boolean happens(RandomGenerator r, double p) { return r.nextDouble() < p; }
}

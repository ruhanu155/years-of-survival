package com.yearofice.sim.world;

import java.util.SplittableRandom;

/**
 * Wildfire (Weather and storms › Ignition; RECONCILE 38). A strike that lights a tree hands over to the wildfire
 * producer only in a drawn wildfire year or at Extreme fire danger. The class of a fire climbs with the danger index
 * in the dry season, and sets how many cells it burns before it goes out.
 */
public final class Wildfire {
    private Wildfire() {}

    public enum Danger { LOW, MODERATE, HIGH, EXTREME }

    public enum FireClass {
        SMOULDER(0), SURFACE(8), RUNNING(40), CROWN(160);

        /** Cells (8 m) burned at the Standard preset. */
        public final int cells;

        FireClass(int c) { cells = c; }
    }

    /** True when {@code year} is a wildfire year, drawn once from the world seed. */
    public static boolean wildfireYear(long seed, int year, LongWorldTuning t) {
        return new SplittableRandom(seed * 0x9E3779B97F4A7C15L + year * 0xC2B2AE3D27D4EB4FL).nextDouble() * 100 < t.wildfireYearPct();
    }

    /** Does one lit strike escalate? Never outside a wildfire year below Extreme. */
    public static boolean escalates(boolean wildfireYear, Danger danger, SplittableRandom rng, LongWorldTuning t) {
        double roll = rng.nextDouble() * 100;
        if (danger == Danger.EXTREME) return roll < t.escalateExtremePct();
        return wildfireYear && roll < t.escalateWildfirePct();
    }

    /** The class an escalated fire starts at; each dry spell day above the index's threshold can raise it one step. */
    public static FireClass classFor(Danger danger, int daysDry) {
        FireClass c = switch (danger) { case LOW -> FireClass.SMOULDER; case MODERATE -> FireClass.SURFACE; case HIGH -> FireClass.RUNNING; case EXTREME -> FireClass.CROWN; };
        if (daysDry >= 10 && c.ordinal() < FireClass.CROWN.ordinal() && danger != Danger.LOW) c = FireClass.values()[c.ordinal() + 1];
        return c;
    }
}

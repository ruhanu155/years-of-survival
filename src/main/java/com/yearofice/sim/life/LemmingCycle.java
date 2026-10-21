package com.yearofice.sim.life;

import com.yearofice.sim.decay.Thresholds;

/**
 * The lemming index L (Land animals in depth › Lemmings): a 4-year cycle 0.2, 0.7, 2.5, 0.3 with its own seeded phase,
 * separate from the vole index and the hare cycle. Pure Java. Original Year of Ice code.
 */
public final class LemmingCycle {
    private LemmingCycle() {}

    public static final double[] TABLE = {0.2, 0.7, 2.5, 0.3};

    /** The cycle's four years, in order; {@code /yoi lemming set <phase>} names one. */
    public enum Phase {
        LOW, RISE, PEAK, CRASH;

        public double index() { return TABLE[ordinal()]; }

        public static Phase of(String s) {
            for (Phase p : values()) if (p.name().equalsIgnoreCase(s)) return p;
            return null;
        }
    }

    /** The seed's phase, 0 to 3: the cycle year of the run's first year. */
    public static int phase(long seed) { return Thresholds.stream(seed, "lemming_cycle").nextInt(TABLE.length); }

    public static Phase phaseOf(int seedPhase, long year) { return Phase.values()[(int) Math.floorMod(seedPhase + year, TABLE.length)]; }

    public static double index(int seedPhase, long year) { return phaseOf(seedPhase, year).index(); }

    /** Brown lemmings per 100 m² of tundra at index {@code l}: one at L = 1. */
    public static double per100m2(double l) { return l; }

    /** At L of 2 or more a lemming stops fleeing and squeaks at the player's boot. */
    public static boolean bold(double l) { return l >= 2.0; }

    /** Fish in tundra ponds bite x1.3 in a peak May, when lemmings swim and drown. */
    public static double pikeBiteMult(double l, int month) { return l >= 2.0 && month == 5 ? 1.3 : 1.0; }

    /**
     * The collared lemming's coat byte (0 summer, 1 mottled, 2 winter, as {@code SeasonalLooks.Coat}): white October 25
     * to May 15, mottled for ten days either side.
     */
    public static int collaredCoat(int doy) {
        int white = 177, whiteEnd = 14;
        if (doy >= white || doy <= whiteEnd) return 2;
        if ((doy >= white - 10 && doy < white) || (doy > whiteEnd && doy <= whiteEnd + 10)) return 1;
        return 0;
    }
}

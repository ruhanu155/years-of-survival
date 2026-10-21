package com.yearofice.sim.fire;

import java.util.SplittableRandom;

/**
 * A controlled burn (Land and seasons in play › Controlled burns). Meadow and grass carry the fire at the forest-fire
 * spread rate ×0.3; a 10 m break, water or bare ground stops it. A spruce cell it reaches burns at the full rule, which
 * is how a burn becomes a wildfire. The model: a front of {@code frontM} metres burns for {@code burnHours}; each
 * hour each front metre can throw an ember that clears {@code d} metres with a chance of {@code exp(-d/λ)}, λ growing
 * with wind and dryness. With no break the grass touches the spruce and it catches with {@link #catchChance}.
 */
public final class ControlledBurn {
    private ControlledBurn() {}

    public static final double SPREAD_MULT = 0.3, FRONT_M = 20, BURN_HOURS = 2;

    /**
     * @param daysDry   days since 5 mm of rain
     * @param windMs    mean wind
     * @param inWindow  May 1–25, wet ground, wind under 4 m/s
     */
    public record Day(int daysDry, double windMs, boolean inWindow) {
        /** The window: rain within 3 days (or melt-out under 10 days ago) and wind under 4 m/s. */
        public static Day window() { return new Day(1, 2.0, true); }

        /** A 7-day July dry spell with a stiff breeze. */
        public static Day julyDry() { return new Day(7, 6.0, false); }
    }

    /** Ember throw scale in metres: dry fuel and wind carry sparks further. */
    public static double lambdaM(Day d) {
        double dry = d.inWindow() ? 0.35 : 0.6 + 0.35 * Math.min(14, d.daysDry());
        return SPREAD_MULT * 3.0 * dry * (1 + d.windMs() / 4.0);
    }

    /** Chance spruce touching burning grass catches, by dryness. */
    public static double catchChance(Day d) { return d.inWindow() ? 0.08 : Math.min(0.98, 0.25 + 0.1 * d.daysDry()); }

    /** One burn: true when it reaches a spruce cell across a break of {@code breakM} metres (0 = none). */
    public static boolean reachesSpruce(Day d, double breakM, SplittableRandom rng) {
        if (breakM <= 0) return rng.nextDouble() < 1 - Math.pow(1 - catchChance(d), FRONT_M * BURN_HOURS / 4);
        double p = Math.exp(-(breakM + 1) / lambdaM(d));
        double tries = FRONT_M * BURN_HOURS * 0.1;
        return rng.nextDouble() < 1 - Math.pow(1 - p, tries);
    }
}

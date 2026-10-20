package com.yearofice.sim.ecosystem;

/**
 * A gut pile's life (Decay and time › Carcasses and gut piles): it rots away in 3 game days in summer and lasts about
 * 30 in winter while scavengers eat it, ravens 1 kg a day each. Freezing stops rot, not scavengers; the lifespan
 * here is the scavenged-or-rotted whole. Pure Java.
 */
public final class GutPile {
    private GutPile() {}

    public static final double SUMMER_DAYS = 3, WINTER_DAYS = 30;
    /** At or below this the pile is frozen and only scavengers work on it. */
    public static final double FREEZE_C = 0;
    /** At or above this the pile rots at the summer rate. */
    public static final double WARM_C = 10;

    /** Days a pile lasts at this air temperature, between the winter and summer figures. */
    public static double lifeDays(double airC) {
        if (airC <= FREEZE_C) return WINTER_DAYS;
        if (airC >= WARM_C) return SUMMER_DAYS;
        double f = (airC - FREEZE_C) / (WARM_C - FREEZE_C);
        return WINTER_DAYS + (SUMMER_DAYS - WINTER_DAYS) * f;
    }

    /** Share of the pile gone after {@code hours} at this temperature, 0 to 1. */
    public static double goneShare(double hours, double airC) { return Math.min(1, hours / (lifeDays(airC) * 24.0)); }
}

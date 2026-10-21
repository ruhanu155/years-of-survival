package com.yearofice.sim.crop;

/**
 * The stovehouse (Crop farming; Structures and buildables › Greenhouse): a heated grow room that adds three weeks at each
 * end of the season and keeps winter greens growing. A crop inside grows on the room's air, not the outside's, scaled by
 * the glazing's light and the day's length. Pure.
 */
public final class Stovehouse {
    private Stovehouse() {}

    public enum Glazing {
        GLASS(1.0), PLEXIGLASS(0.9), RAWHIDE(0.5), DIM(0.15);

        public final double light;

        Glazing(double light) { this.light = light; }
    }

    /** A manure hotbed (0.5 m of fresh goat manure) warms the soil 8 °C for 3 weeks. */
    public static final double HOTBED_C = 8, HOTBED_DAYS = 21;

    /** The day's growth units inside: degrees of room air above 5 °C, times the glazing's light and the day length against 12 h, capped at 1. */
    public static double growthUnits(double roomC, double daylightH, Glazing g) {
        double lightDay = Math.max(0.1, Math.min(1.0, daylightH / 12.0));
        return Math.max(0, roomC - 5.0) * g.light * lightDay;
    }

    /** True when the room's air kills greens: below −4 °C even a cabbage dies. */
    public static boolean killsGreens(double roomC) { return roomC < -4; }

    /** The night low a crop in the room sees: the room's air, which only the stove and the walls decide. */
    public static double nightLowC(double roomC, double outsideLowC) { return Math.max(roomC - 1.0, outsideLowC); }

    /** Days a crop needing {@code ripeUnits} takes at a steady room temperature, or −1 when it never ripens. */
    public static double daysToRipe(double ripeUnits, double roomC, double daylightH, Glazing g) {
        double u = growthUnits(roomC, daylightH, g);
        return u <= 0 ? -1 : ripeUnits / u;
    }
}

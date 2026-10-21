package com.yearofice.sim.travel;

/**
 * Winter travel rules (Blueprint › Traversal, research round B). Pure maths: speed multipliers for skates, skis and
 * snowshoes, trail wear that sets overnight, and stale air in snow shelters. Adapters read these; tuning numbers
 * live in the config presets.
 */
public final class Travel {
    private Travel() {}

    /** The gear in the Curios snowshoes slot. */
    public enum Gear { NONE, SNOWSHOES, SKATES, SKIS, SKIS_SKINS }

    /** Surfaces a step can land on. */
    public enum Surface { BARE, SNOW, ICE }

    /** Ice thinner than this (cm) crackles under a skater; under {@link #ICE_BEARS_CM} it will not bear one. */
    public static final double CRACKLE_CM = 12, ICE_BEARS_CM = 8;

    public static boolean cracklesUnder(double iceCm) { return iceCm < CRACKLE_CM; }

    /**
     * Walking-speed multiplier. Snow depth in cm, slope as rise/run (positive uphill).
     * Skates: fast on ice, useless elsewhere. Skis: glide downhill, herringbone uphill (skins grip, no slide back).
     * Snowshoes: keep a walker on top of deep snow. Bare boots wade deep snow.
     */
    public static double speed(Gear g, Surface s, double snowCm, double slope) {
        double wade = snowCm <= 10 ? 1 : Math.max(0.35, 1 - (snowCm - 10) / 80);
        switch (g) {
            case SKATES:
                return s == Surface.ICE ? 1.8 : 0.5;
            case SKIS:
            case SKIS_SKINS: {
                if (s != Surface.SNOW) return 0.5;
                if (slope < -0.02) return Math.min(2.2, 1.3 + -slope * 6); // glide
                if (slope > 0.02) return g == Gear.SKIS_SKINS ? 0.8 : Math.max(0.3, 0.7 - slope * 2); // herringbone
                return 1.2;
            }
            case SNOWSHOES:
                return s == Surface.SNOW ? Math.max(0.8, wade + 0.35) : 0.9;
            default:
                return s == Surface.SNOW ? wade : 1;
        }
    }

    /** Trail wear states on a snow layer. */
    public enum Trail { NONE, BROKEN, SET, SLED_LANE }

    /** Hours of hard frost (below this °C) that set a broken trail. */
    public static final double SET_BELOW_C = -5;
    public static final int SET_AFTER_H = 6;

    /** One hour: a broken trail sets after {@link #SET_AFTER_H} cold hours; fresh snow over 3 cm buries it. */
    public static int coldHours(int coldHours, double tempC) {
        return tempC <= SET_BELOW_C ? coldHours + 1 : 0;
    }

    public static Trail nextTrail(Trail t, int coldHours, double newSnowCm, boolean sledPassed) {
        if (newSnowCm > 3) return Trail.NONE;
        if (t == Trail.SET && sledPassed) return Trail.SLED_LANE;
        if (t == Trail.BROKEN && coldHours >= SET_AFTER_H) return Trail.SET;
        return t;
    }

    /** Trail speed multiplier over untouched snow of the same depth. */
    public static double trailBonus(Trail t) {
        return switch (t) { case NONE -> 1.0; case BROKEN -> 1.15; case SET -> 1.4; case SLED_LANE -> 1.6; };
    }

    /**
     * Stale air in a closed snow room: air 0..1. Occupants burn it, a flame burns it faster, a vent hole or
     * an open door refreshes it. Returns the new air after {@code minutes}.
     */
    public static double air(double air, int minutes, int people, int flames, boolean vented, double volumeM3) {
        double use = (people * 0.0025 + flames * 0.006) / Math.max(1, volumeM3 / 4.0);
        double gain = vented ? 0.03 : 0;
        return Math.max(0, Math.min(1, air + (gain - use) * minutes));
    }

    /** Below this the room is stale: headaches and the warn-once {@code snow_vent}. */
    public static final double STALE_AIR = 0.35;

    public static boolean stale(double air) { return air < STALE_AIR; }
}

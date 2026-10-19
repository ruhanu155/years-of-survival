package com.yearofice.sim.fire;

import java.util.Locale;

/**
 * The Blueprint's Wood table (Fire, water and light › Wood): each wood multiplies the build's burn time and heat,
 * and says what it lights from. Green wood and wet wood are moisture rules ({@link FireRules}), not woods: any
 * wood at 50% moisture or more burns as green. The Blueprint table wins over the Video tab's relative numbers
 * (Every concept from the video › Firewood drying).
 */
public enum Wood {
    DEAD_SPRUCE(0.7, 1.0, LightsFrom.SMALL_FIRE),
    SPRUCE(0.7, 1.0, LightsFrom.SMALL_FIRE),
    BIRCH(1.4, 1.1, LightsFrom.FIRE_OR_COALS),
    DRIFT(0.6, 0.9, LightsFrom.SMALL_FIRE),
    WILLOW(0.8, 0.9, LightsFrom.FIRE),
    ALDER(0.8, 0.9, LightsFrom.FIRE);

    /** What a wood catches from. */
    public enum LightsFrom { SMALL_FIRE, FIRE, FIRE_OR_COALS }

    /** Burn time × (the build's minutes per armload are multiplied by this). */
    public final double burnFactor;
    /** Heat ×. */
    public final double heatFactor;
    public final LightsFrom lightsFrom;

    Wood(double burnFactor, double heatFactor, LightsFrom lightsFrom) {
        this.burnFactor = burnFactor;
        this.heatFactor = heatFactor;
        this.lightsFrom = lightsFrom;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Wood byId(String id) {
        for (Wood w : values()) if (w.id().equals(id)) return w;
        throw new IllegalArgumentException("unknown wood '" + id + "'");
    }
}

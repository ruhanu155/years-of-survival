package com.yearofice.sim.body;

/**
 * The five clothing zones and their share of the whole body (Blueprint › Clothing). Order matches the
 * schema-1 {@code zone_frost} order: head, torso, hands, legs, feet.
 */
public enum Zone {
    HEAD(0.10),
    TORSO(0.40),
    HANDS(0.10),
    LEGS(0.25),
    FEET(0.15);

    public final double share;

    Zone(double share) { this.share = share; }

    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
}

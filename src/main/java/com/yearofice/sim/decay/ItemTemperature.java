/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Adapted for Year of Ice from TerraFirmaCraft/TerraFirmaCraft@c3b4d9536b82e5a9746dd793c586720a74108057,
 * src/main/java/net/dries007/tfc/common/component/heat/HeatComponent.java and HeatCapability.java
 * (a stored value plus a stamp, moved linearly toward a target and settled lazily on read, with heat capacity
 * scaling the rate; reimplemented for our frost clock, no lines copied).
 */
package com.yearofice.sim.decay;

/**
 * Item heat: the value of {@code yearofice:item_temp} (Items › Temperature; Minecraft development › Layer 5).
 *
 * <p><b>From TerraFirmaCraft.</b> TFC's item heat stores the last temperature and the tick it was read; reading
 * it moves the value linearly toward ambient by elapsed time over heat capacity, and heating adds toward a
 * target each tick. Nothing ticks while nobody looks.
 *
 * <p><b>Our rule.</b> An item holding water or fat keeps <i>frost points</i>. Each game minute it gains points
 * equal to how far the air is below 0 °C and loses points equal to how far it is above; it is Frozen at
 * 600 points per kg (the heat capacity). A 1 kg bottle at −10 °C freezes in 60 game minutes and at −40 °C in
 * 15. A fire within 2 m counts as +30 °C, so the bottle thaws in 20 minutes; the inner pocket holds +20 °C while
 * warmth is 30 or more. Warm items (hot stones, boiled water, a hot-water bottle) fade to Ambient over 30 game
 * minutes, which is TFC's linear passive cooling.
 *
 * <p>Pure Java, immutable. Values are per stack; {@code massKg} is the stack's mass.
 */
public record ItemTemperature(double frostPoints, double warmth, double massKg) {
    public static final double FREEZE_POINTS_PER_KG = 600.0;
    /** A fire within 2 m counts as this air temperature (Items › Temperature). */
    public static final double FIRE_EQUIVALENT_C = 30.0;
    /** The inner pocket while warmth is 30 or more. */
    public static final double POCKET_C = 20.0;
    public static final double POCKET_MIN_WARMTH = 30.0;
    /** A Warm item fades to Ambient over this many game minutes. */
    public static final double WARM_FADE_MINUTES = 30.0;
    /** Settling walks at most this many hourly steps, then saturates (Layer 5). */
    public static final int MAX_SETTLE_HOURS = 72;

    public enum State { FROZEN, COLD, AMBIENT, WARM }

    public ItemTemperature {
        if (!(massKg > 0)) throw new IllegalArgumentException("mass must be positive");
        double cap = FREEZE_POINTS_PER_KG * massKg;
        frostPoints = Math.max(0.0, Math.min(cap, frostPoints));
        warmth = Math.max(0.0, Math.min(1.0, warmth));
    }

    public static ItemTemperature ambient(double massKg) { return new ItemTemperature(0, 0, massKg); }

    public static ItemTemperature frozen(double massKg) {
        return new ItemTemperature(FREEZE_POINTS_PER_KG * massKg, 0, massKg);
    }

    public double capacity() { return FREEZE_POINTS_PER_KG * massKg; }

    public State state() {
        if (frostPoints >= capacity()) return State.FROZEN;
        if (frostPoints > 0) return State.COLD;
        if (warmth > 0) return State.WARM;
        return State.AMBIENT;
    }

    public boolean frozen() { return state() == State.FROZEN; }

    /** The temperature an item feels: +30 °C by a fire, +20 °C in the inner pocket, otherwise the air. */
    public static double effectiveTempC(double airC, boolean fireWithin2m, boolean innerPocket, double bodyWarmth) {
        if (fireWithin2m) return FIRE_EQUIVALENT_C;
        if (innerPocket && bodyWarmth >= POCKET_MIN_WARMTH) return POCKET_C;
        return airC;
    }

    /** The state after {@code minutes} at a constant effective temperature: one linear segment. */
    public ItemTemperature after(double minutes, double effectiveC) {
        if (minutes < 0) throw new IllegalArgumentException("time runs forward");
        double points = frostPoints - effectiveC * minutes; // below 0 °C, −T·dt adds points
        double w = warmth - minutes / WARM_FADE_MINUTES;
        return new ItemTemperature(points, w, massKg);
    }

    /** Heated (boiled water, a hot stone): Warm, and thawed. */
    public ItemTemperature heated() { return new ItemTemperature(0, 1.0, massKg); }

    /** Game minutes at a constant temperature until Frozen; ∞ if the air is not below 0 °C. */
    public double minutesToFreeze(double effectiveC) {
        if (effectiveC >= 0) return Double.POSITIVE_INFINITY;
        return (capacity() - frostPoints) / -effectiveC;
    }

    /** Game minutes until fully thawed; ∞ if the air is not above 0 °C. For "thaws in 20 min by a fire". */
    public double minutesToThaw(double effectiveC) {
        if (effectiveC <= 0) return Double.POSITIVE_INFINITY;
        return frostPoints / effectiveC;
    }

    /**
     * Settle over an hourly series of effective temperatures since the stamp (oldest first). Walks at most the
     * last 72 hours; if the series is longer, it starts from the state the hour before that window saturates
     * to (frozen if below 0 °C, thawed if above), as Layer 5 rules.
     */
    public ItemTemperature settle(double[] hourlyEffectiveC) {
        int n = hourlyEffectiveC.length;
        ItemTemperature s = this;
        int start = 0;
        if (n > MAX_SETTLE_HOURS) {
            start = n - MAX_SETTLE_HOURS;
            double before = hourlyEffectiveC[start - 1];
            if (before < 0) s = frozen(massKg);
            else if (before > 0) s = ambient(massKg);
            else s = new ItemTemperature(frostPoints, 0, massKg);
        }
        for (int h = start; h < n; h++) s = s.after(60.0, hourlyEffectiveC[h]);
        return s;
    }
}

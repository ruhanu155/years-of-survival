package com.yearofice.sim.build;

/**
 * Lift a full box (video 2, A7; Minecraft development › Hand-carried loads and carcasses): a chest, barrel or crate
 * of 30 kg or less, contents included, can be picked up whole. Pure rules; the block and attachment work is
 * {@code item.inventory.BoxCarry}.
 */
public final class BoxLift {
    private BoxLift() {}

    public static final double CAP_KG = 30.0;
    /** Game hours a box must stand on frozen ground in air below 0 °C, outside a warm room, to freeze down. */
    public static final double FROZEN_AFTER_H = 24;

    public record Verdict(boolean ok, double totalKg, double overKg) {}

    /** The total weight against the cap: over it, the verdict says how much to take out. */
    public static Verdict check(double boxKg, double contentsKg, double capKg) {
        double total = boxKg + contentsKg;
        double over = total - capKg;
        return new Verdict(over <= 1e-9, total, Math.max(0, over));
    }

    /** True when the box has stood {@code afterH} hours (24 by default) and is frozen to the ground: it needs the 2 s chip first. */
    public static boolean frozenDown(double hoursStood, double afterH, double airC, boolean groundFrozen, boolean inWarmRoom) {
        return hoursStood >= afterH && airC < 0 && groundFrozen && !inWarmRoom;
    }
}

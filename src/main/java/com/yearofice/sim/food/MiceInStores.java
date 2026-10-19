package com.yearofice.sim.food;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.decay.Thresholds;

/**
 * Mice in chests (Items › Item decay).
 *
 * <p>October to April, in a chest, cache or cellar with fewer than 3 mouse deadfalls within 3 blocks: food
 * loses 2–3% of its units a week (rolled weekly) and the stack gains the {@code droppings} flag; leather and
 * hide lose 5% condition a week. Barrels, sealed tins and the ice house are immune. The weekly roll is a named
 * stream keyed by the store's position and the week, so a store opened every day and one opened in May lose
 * the same food.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class MiceInStores {
    private MiceInStores() {}

    public static final double LOSS_MIN = 0.02;
    public static final double LOSS_MAX = 0.03;
    public static final int RING_DEADFALLS = 3;
    public static final double HIDE_CONDITION_LOSS_PER_WEEK = 0.05;
    /** A meal from a stack flagged with droppings: 20% gut illness until the top is trimmed (−10% of the units). */
    public static final double DROPPINGS_GUT_ILLNESS = 0.20;
    public static final double TRIM_SHARE = 0.10;

    public enum Store {
        CHEST(true), CACHE(true), CELLAR(true), BARREL(false), SEALED_TIN(false), ICE_HOUSE(false);

        private final boolean miceReach;

        Store(boolean miceReach) { this.miceReach = miceReach; }

        public boolean miceReach() { return miceReach; }
    }

    /** True in the mouse months, October to April (month 1–12). */
    public static boolean mouseSeason(int month) { return month >= 10 || month <= 4; }

    public static boolean exposed(Store store, int deadfallsWithin3, int month) {
        return store.miceReach() && deadfallsWithin3 < RING_DEADFALLS && mouseSeason(month);
    }

    /** The week's loss share for a store: 2–3%, drawn once per store and week. */
    public static double weeklyShare(long seed, int x, int y, int z, long week) {
        return LOSS_MIN + (LOSS_MAX - LOSS_MIN) * Thresholds.stream(seed, "mice", x, y, z, week).nextDouble();
    }

    /**
     * Units a food stack loses this week. Fractions round by a second draw from the same stream, so small stacks
     * lose a unit now and then and the expected share stays 2–3%.
     */
    public static int unitsLost(long seed, int x, int y, int z, long week, int slot, int units,
                                Store store, int deadfallsWithin3, int month) {
        if (units <= 0 || !exposed(store, deadfallsWithin3, month)) return 0;
        Mulberry32 r = Thresholds.stream(seed, "mice", x, y, z, week);
        double share = LOSS_MIN + (LOSS_MAX - LOSS_MIN) * r.nextDouble();
        double frac = Thresholds.stream(seed, "mice_unit", x, y, z, week, slot).nextDouble();
        int lost = (int) StrictMath.floor(units * share + frac);
        return Math.min(units, lost);
    }
}

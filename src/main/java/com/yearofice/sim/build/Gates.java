package com.yearofice.sim.build;

/**
 * Gate state as a bit mask, one bit per {@link Tier} (Crafting and building in depth › Tiers and gates): Start is
 * always open, the other four open on their own events, and a gate never closes. Pure; the player attachment holds
 * the int.
 */
public final class Gates {
    private Gates() {}

    /** A new world: Start open, nothing else. */
    public static final int NEW_WORLD = Tier.START.bit();

    public static boolean isOpen(int mask, Tier tier) { return tier == Tier.START || (mask & tier.bit()) != 0; }

    /** The mask with {@code tier} open. There is no close: this is the only way a mask changes. */
    public static int open(int mask, Tier tier) { return mask | tier.bit() | NEW_WORLD; }

    /** How many tiers are open, Start included. */
    public static int count(int mask) { return Integer.bitCount(mask | NEW_WORLD); }

    /** A tier's recipe shows once its gate is open (the notebook's other condition, holding an input, is the caller's). */
    public static boolean recipeVisible(int mask, Tier recipeTier) { return isOpen(mask, recipeTier); }
}

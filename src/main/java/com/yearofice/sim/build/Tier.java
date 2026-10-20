package com.yearofice.sim.build;

import java.util.Locale;
import java.util.Optional;

/**
 * The five crafting tiers (Crafting and building in depth › Tiers and gates). Start is open in a new world; each
 * of the others is opened by an event in the world, never a menu. Gates open independently and never close.
 */
public enum Tier {
    START(1), BUSHCRAFT(2), HIDE_AND_BONE(3), IRON(4), HOMESTEAD(5);

    /** The tier's number in the doc, 1 to 5. */
    public final int number;

    Tier(int number) { this.number = number; }

    /** The data id: {@code start}, {@code bushcraft}, {@code hide_and_bone}, {@code iron}, {@code homestead}. */
    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The bit this tier takes in a gate mask. */
    public int bit() { return 1 << ordinal(); }

    public static Optional<Tier> byId(String id) {
        if (id == null) return Optional.empty();
        for (Tier t : values()) if (t.id().equals(id)) return Optional.of(t);
        return Optional.empty();
    }
}

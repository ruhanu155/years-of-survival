package com.yearofice.sim.wood;

import java.util.Locale;

/**
 * The firewood woods (Crafting and building in depth › Trees, felling and firewood › Limbing and bucking: "Wood is
 * spruce, birch, aspen, poplar, tamarack, willow, alder or drift"). Each names its {@code <wood>_round} and
 * {@code <wood>_split} items and the row of the fire's Wood table it burns as.
 *
 * <p>The fire's table has no aspen, poplar or tamarack row (Blueprint › Fire, water and light › Wood). Until it
 * does, aspen and poplar burn as willow (soft, quick hardwoods that light from a Fire) and tamarack as spruce; the
 * mapping is data in {@code fuel.json}, so the fire table can take its own rows later without a code change.
 */
public enum WoodKind {
    SPRUCE("spruce"),
    BIRCH("birch"),
    ASPEN("willow"),
    POPLAR("willow"),
    TAMARACK("spruce"),
    WILLOW("willow"),
    ALDER("alder"),
    DRIFT("drift");

    /** The fire's Wood row ({@code sim.fire.Wood} id) this wood's splits burn as. */
    public final String fireWood;

    WoodKind(String fireWood) { this.fireWood = fireWood; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public String roundId() { return id() + "_round"; }

    public String splitId() { return id() + "_split"; }

    public static WoodKind byId(String id) {
        for (WoodKind w : values()) if (w.id().equals(id)) return w;
        return null;
    }
}

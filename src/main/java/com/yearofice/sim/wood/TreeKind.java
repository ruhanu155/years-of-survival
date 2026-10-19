package com.yearofice.sim.wood;

import java.util.Locale;

/**
 * Standing tree kinds and their diameter factor (Trees, felling and firewood › Reading a tree: d = k × height in
 * m, in cm). M2 grows white and black spruce and paper birch; the other rows wait for their logs.
 */
public enum TreeKind {
    WHITE_SPRUCE(1.6, WoodKind.SPRUCE, "White spruce"),
    BLACK_SPRUCE(1.1, WoodKind.SPRUCE, "Black spruce"),
    BIRCH(1.5, WoodKind.BIRCH, "Paper birch"),
    ASPEN(1.5, WoodKind.ASPEN, "Aspen"),
    BALSAM_POPLAR(2.0, WoodKind.POPLAR, "Balsam poplar"),
    TAMARACK(1.3, WoodKind.TAMARACK, "Tamarack");

    /** cm of diameter per m of height. */
    public final double k;
    public final WoodKind wood;
    public final String display;

    TreeKind(double k, WoodKind wood, String display) {
        this.k = k;
        this.wood = wood;
        this.display = display;
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** Diameter at breast height in cm for a tree {@code heightM} tall (1 block = 1 m). */
    public double diameterCm(double heightM) { return k * heightM; }

    /** True for the spruces, whose limbs give boughs. */
    public boolean spruce() { return wood == WoodKind.SPRUCE; }

    public static TreeKind byOrdinal(int i) { return i >= 0 && i < VALUES.length ? VALUES[i] : null; }

    private static final TreeKind[] VALUES = values();
}

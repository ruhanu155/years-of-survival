package com.yearofice.sim.wood;

/**
 * Limbing, bucking and cutting single blocks (Trees, felling and firewood › Limbing and bucking; Placed logs never
 * fall). Rounds are 0.5 m; 4 rounds make the 15 kg armload, so a round takes a quarter of the tool's minutes per
 * armload. One log block is 1 m; cutting one block out gives 2 rounds.
 */
public final class Bucking {
    private Bucking() {}

    public static final double ROUND_M = 0.5;
    public static final int ROUNDS_PER_ARMLOAD = 4;
    public static final double ROUND_KG = 3.75;
    /** Rounds from one cut-out log block (1 m). */
    public static final int ROUNDS_PER_BLOCK = 2;

    /** Spruce limbing: boughs, sticks, and a pole from the top of a tree over 8 m. */
    public static final int SPRUCE_BOUGHS = 40, SPRUCE_STICKS = 6, POLE_OVER_M = 8;
    /** Birch limbing: sticks and forked branches. */
    public static final int BIRCH_STICKS = 6, BIRCH_FORKS = 2;

    /** The usable trunk of a felled tree of {@code trunkBlocks} logs: all but the base, which stays as the stump. */
    public static double usableM(int trunkBlocks) { return Math.max(0, trunkBlocks - 1); }

    /** Rounds a log of {@code lengthM} bucks into. */
    public static int rounds(double lengthM) { return (int) Math.floor(lengthM / ROUND_M + 1e-9); }

    /** Game minutes to buck one round with a tool that bucks an armload in {@code buckMin}. */
    public static double roundMinutes(double buckMin) { return buckMin / ROUNDS_PER_ARMLOAD; }

    /** Game minutes to cut one placed log block out: the tool's buck time for one armload. */
    public static double placedBlockMinutes(double buckMin) { return buckMin; }

    /** Game minutes to cut a standing trunk's top block with Sneak: the buck time times {@code sneakMult} (2). */
    public static double sneakTopMinutes(double buckMin, double sneakMult) { return buckMin * sneakMult; }

    /** Sticks from limbing a felled tree. */
    public static int sticks(TreeKind k) { return k.spruce() ? SPRUCE_STICKS : BIRCH_STICKS; }

    /** Boughs from limbing (spruce only). */
    public static int boughs(TreeKind k) { return k.spruce() ? SPRUCE_BOUGHS : 0; }
}

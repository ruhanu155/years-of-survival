package com.yearofice.sim.build;

/**
 * The tool meters' rules (Crafting and building in depth › Tool durability). A tool has durability (vanilla damage
 * against its max) and, if it cuts, an Edge of 0 to 120 where a sharpened tool reads 100 and each use costs
 * {@code 100 ÷ the Edge column}, the uses from sharp to dull. Pure; the component and the item hooks live in
 * {@code build.craft.Wear}.
 */
public final class ToolWear {
    private ToolWear() {}

    public static final double EDGE_MAX = 120, EDGE_SHARP = 100, DULL_BELOW = 30;
    /** Dull tools: jobs take ×1.5 time and ×1.3 kcal. */
    public static final double DULL_TIME = 1.5, DULL_KCAL = 1.3;
    /** The warning share of durability left (25%). */
    public static final double WARN_SHARE = 0.25;
    /** Below this air temperature wooden handles and stone heads wear twice as fast. */
    public static final double COLD_WEAR_C = -30, COLD_WEAR_MULT = 2.0;
    /** Below this an iron edge chips on 1% of swings, losing 20 edge. */
    public static final double COLD_CHIP_C = -40, COLD_CHIP_CHANCE = 0.01, COLD_CHIP_EDGE = 20;
    /** Edge after a river-stone hone (3 game minutes), a whetstone (2) and the trapper's flat file (1). */
    public static final double HONE_STONE = 70, HONE_WHETSTONE = 100, HONE_FILE = 100;

    /** Edge lost per use for a tool whose Edge column is {@code edgeUses} (uses from sharp to dull). */
    public static double edgeLossPerUse(double edgeUses) { return edgeUses <= 0 ? 0 : EDGE_SHARP / edgeUses; }

    public static boolean dull(double edge) { return edge < DULL_BELOW; }

    /** The edge after {@code uses} uses (never below 0). */
    public static double afterUses(double edge, double edgeUses, double uses) {
        return Math.max(0, edge - edgeLossPerUse(edgeUses) * uses);
    }

    /** The time multiplier of an edge: 1.5 when dull. */
    public static double timeMult(double edge) { return dull(edge) ? DULL_TIME : 1.0; }

    /** The kcal multiplier of an edge: 1.3 when dull. */
    public static double kcalMult(double edge) { return dull(edge) ? DULL_KCAL : 1.0; }

    /** Durability points one use costs: 2 below −30 °C for wood and stone, else 1. */
    public static int durabilityCost(double airC, boolean woodOrStone) {
        return woodOrStone && airC < COLD_WEAR_C ? (int) COLD_WEAR_MULT : 1;
    }

    /** True once at or under a quarter of durability is left: the tool warns once. */
    public static boolean worn(int damage, int maxDamage) {
        return maxDamage > 0 && (maxDamage - damage) <= maxDamage * WARN_SHARE;
    }

    /** Edge restored to at most {@code to} by a hone; a hone never dulls a sharper edge. */
    public static double hone(double edge, double to) { return Math.max(edge, to); }
}

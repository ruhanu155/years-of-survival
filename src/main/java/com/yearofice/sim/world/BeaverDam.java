package com.yearofice.sim.world;

/**
 * Beaver colonies shape the river (The open world › Beaver dams). Each summer the dam grows one stage (0–4) while 30 or
 * more willow, aspen or birch stand within 100 m; each stage raises the pond 0.5 m. Without the trees the colony holds
 * at its stage; an abandoned colony's pond drains one stage a year.
 */
public final class BeaverDam {
    private BeaverDam() {}

    public static final int MAX_STAGE = 4, TREES_NEEDED = 30;
    /** The summer day the dam grows: day of year 92 (August 1). */
    public static final int GROW_DOY = 92;

    public static int nextStage(int stage, int treesInReach, boolean abandoned) {
        if (abandoned) return Math.max(0, stage - 1);
        return treesInReach >= TREES_NEEDED ? Math.min(MAX_STAGE, stage + 1) : stage;
    }

    /** Pond rise in metres at a stage (0.5 m a stage). */
    public static double riseM(int stage) { return 0.5 * stage; }

    /** Whole blocks the pond has risen: 2 stages make a block. */
    public static int riseBlocks(int stage) { return stage / 2; }

    /** Absolute days of the growth summers falling in (from, to]. */
    public static java.util.List<Long> growDays(long fromDay, long toDay) {
        java.util.List<Long> out = new java.util.ArrayList<>();
        for (long d = fromDay + 1; d <= toDay; d++) if (Math.floorMod(d, 365L) == GROW_DOY) out.add(d);
        return out;
    }
}

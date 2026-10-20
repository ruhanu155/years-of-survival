package com.yearofice.sim.fishing;

/** The knife cuts a landed fish into bait-sized chunks (Waters and fish › Small items: fish chunk). Pure Java. */
public final class FishCut {
    private FishCut() {}

    /** Grams of fish a chunk is. */
    public static final int CHUNK_G = 150;

    /** Chunks a landed fish of this weight gives: one a 150 g, at least two. */
    public static int chunks(int grams) { return Math.max(2, grams / CHUNK_G); }
}

package com.yearofice.sim.wood;

/**
 * One felled or topped tree in a chunk's {@code yearofice:trees} attachment, packed into a long (8 bytes a tree,
 * so 1,000 felled trees are about 8 KB of mod data before the chunk overhead; Ready for development › M2 budget
 * 20 KB). The standing trees themselves are the deterministic TreePlan and need no storing; a mark only says what
 * the player changed, so a felled tree's plan entry never stands again.
 *
 * <p>Bits: x in the chunk 4, z 4, base y + 2048 12, state 2, height 8, absolute day 32.
 */
public final class TreeMarks {
    private TreeMarks() {}

    /** What became of the tree. */
    public enum State {
        /** Felled: a stump stands at the base. */
        FELLED,
        /** Cut down from the top with Sneak; {@code height} logs still stand. */
        TOPPED
    }

    public static long pack(int lx, int lz, int baseY, State state, int height, long day) {
        if (lx < 0 || lx > 15 || lz < 0 || lz > 15) throw new IllegalArgumentException("local x, z 0-15");
        int y = baseY + 2048;
        if (y < 0 || y > 4095) throw new IllegalArgumentException("base y " + baseY);
        int h = Math.max(0, Math.min(255, height));
        long d = day & 0xFFFFFFFFL;
        return (long) lx | (long) lz << 4 | (long) y << 8 | (long) state.ordinal() << 20 | (long) h << 22 | d << 30;
    }

    public static int lx(long m) { return (int) (m & 15); }

    public static int lz(long m) { return (int) ((m >>> 4) & 15); }

    public static int baseY(long m) { return (int) ((m >>> 8) & 4095) - 2048; }

    public static State state(long m) { return State.values()[(int) ((m >>> 20) & 3)]; }

    public static int height(long m) { return (int) ((m >>> 22) & 255); }

    public static long day(long m) { return (m >>> 30) & 0xFFFFFFFFL; }

    /** The key a mark is filed under: its base block in the chunk. */
    public static int key(long m) { return (int) (m & 0xFFFFF); }

    public static int key(int lx, int lz, int baseY) { return lx | lz << 4 | (baseY + 2048) << 8; }
}

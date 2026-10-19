package com.yearofice.sim.body.part;

/**
 * Lost fingers and toes ({@code lost_digits}: [0] fingers, [1] toes; bits 0–4 the left side, 5–9 the right,
 * little first and thumb or big toe last). Deep frostbite takes the next digit on the part it froze.
 */
public final class Digits {
    private Digits() {}

    public static final int FINGERS = 0, TOES = 1;

    /** Takes the next digit on {@code part}'s hand or foot; other parts are returned unchanged. */
    public static int[] loseNext(int[] lost, Part part) {
        int which;
        if (part.group == Part.Group.HANDS) which = FINGERS;
        else if (part.group == Part.Group.FEET) which = TOES;
        else return lost.clone();
        int base = part.side == Side.RIGHT ? 5 : 0;
        int[] out = lost.clone();
        for (int i = 0; i < 5; i++) {
            int bit = 1 << (base + i);
            if ((out[which] & bit) == 0) {
                out[which] |= bit;
                break;
            }
        }
        return out;
    }

    public static int count(int mask) { return Integer.bitCount(mask & 0x3FF); }

    /** Crafting time ×: 5% slower per finger, counting at most two. */
    public static double craftingFactor(int[] lost) { return 1 + 0.05 * Math.min(2, count(lost[FINGERS])); }

    /** Walking speed ×: 3% slower per toe, counting at most two. */
    public static double walkFactor(int[] lost) { return 1 - 0.03 * Math.min(2, count(lost[TOES])); }

    public static boolean lost(int[] lost, Part part, int index) {
        int which = part.group == Part.Group.HANDS ? FINGERS : TOES;
        int base = part.side == Side.RIGHT ? 5 : 0;
        return (lost[which] & (1 << (base + index))) != 0;
    }
}

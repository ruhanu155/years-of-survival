package com.yearofice.sim.fire;

/** The layers of a campfire after the ring, in the order they must go in (Blueprint › Building a campfire). */
public enum LayPart {
    TINDER,
    KINDLING,
    FUEL;

    /**
     * True when {@code part} may go on a lay whose top layer is {@code top} (null for a bare ring) without breaking
     * the order: tinder first, kindling on tinder, fuel on kindling. More of the same layer is always fine.
     */
    public static boolean inOrder(LayPart top, LayPart part) {
        if (top == null) return part == TINDER;
        return part.ordinal() == top.ordinal() || part.ordinal() == top.ordinal() + 1;
    }
}

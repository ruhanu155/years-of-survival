package com.yearofice.sim.body;

/** Each zone has an inner slot (traps warmth) and an outer slot (stops wind and water). */
public enum Layer {
    INNER,
    OUTER;

    /** Slot index 0–9: zone × 2 + layer. */
    public static int slot(Zone z, Layer l) { return z.ordinal() * 2 + l.ordinal(); }

    public static final int SLOTS = 10;
}

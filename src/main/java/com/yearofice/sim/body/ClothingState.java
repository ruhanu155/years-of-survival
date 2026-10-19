package com.yearofice.sim.body;

import java.util.Arrays;

/**
 * What is worn in the ten slots and how wet each slot is. Immutable; every change returns a copy.
 *
 * <p>Wetness lives per slot, 0–1. For a worn slot it is that garment's {@code yearofice:wetness}; for an
 * empty slot it is whatever is there instead (bare skin for an empty inner slot), so bare wet hands still
 * read as wet.
 */
public final class ClothingState {
    private final WornGarment[] slots;
    private final double[] wet;
    private final boolean coatOpen;

    private ClothingState(WornGarment[] slots, double[] wet, boolean coatOpen) {
        this.slots = slots;
        this.wet = wet;
        this.coatOpen = coatOpen;
    }

    public static ClothingState naked() {
        return new ClothingState(new WornGarment[Layer.SLOTS], new double[Layer.SLOTS], false);
    }

    public static ClothingState of(Garment... garments) {
        ClothingState s = naked();
        for (Garment g : garments) s = s.wear(WornGarment.fresh(g));
        return s;
    }

    public WornGarment at(int slot) { return slots[slot]; }

    public WornGarment at(Zone z, Layer l) { return slots[Layer.slot(z, l)]; }

    public double wet(int slot) { return wet[slot]; }

    public double wet(Zone z, Layer l) { return wet[Layer.slot(z, l)]; }

    public boolean coatOpen() { return coatOpen; }

    public ClothingState wear(WornGarment w) {
        WornGarment[] s = slots.clone();
        s[w.garment().slot()] = w;
        return new ClothingState(s, wet, coatOpen);
    }

    public ClothingState remove(Zone z, Layer l) {
        WornGarment[] s = slots.clone();
        s[Layer.slot(z, l)] = null;
        double[] w = wet.clone();
        w[Layer.slot(z, l)] = 0; // a removed garment takes its water with it
        return new ClothingState(s, w, coatOpen);
    }

    public ClothingState withWet(int slot, double m) {
        double[] w = wet.clone();
        w[slot] = clamp01(m);
        return new ClothingState(slots, w, coatOpen);
    }

    public ClothingState withAllWet(double m) {
        double[] w = new double[Layer.SLOTS];
        Arrays.fill(w, clamp01(m));
        return new ClothingState(slots, w, coatOpen);
    }

    public ClothingState withWet(double[] m) {
        if (m.length != Layer.SLOTS) throw new IllegalArgumentException("10 slots");
        double[] w = new double[Layer.SLOTS];
        for (int i = 0; i < w.length; i++) w[i] = clamp01(m[i]);
        return new ClothingState(slots, w, coatOpen);
    }

    public ClothingState withCoatOpen(boolean open) { return new ClothingState(slots, wet, open); }

    public double[] wetCopy() { return wet.clone(); }

    static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
}

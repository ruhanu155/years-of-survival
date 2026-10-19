package com.yearofice.sim.body;

/** A garment as worn, with its condition from 0 to 1 (vanilla durability fraction). */
public record WornGarment(Garment garment, double condition) {

    public WornGarment {
        if (garment == null) throw new IllegalArgumentException("garment");
        condition = Math.max(0, Math.min(1, condition));
    }

    public static WornGarment fresh(Garment g) { return new WornGarment(g, 1.0); }

    /** "c is 1 above 30% condition and 0.5 below it." */
    public double conditionFactor() { return condition >= 0.3 ? 1.0 : 0.5; }

    /** Below 30% a garment "leaks wind"; read as half its wind block. */
    public double effectiveWindBlock() { return garment.windBlock() * (condition >= 0.3 ? 1.0 : 0.5); }

    /** Sheds water only while waterproof and at 50% condition or more. */
    public boolean sheds() { return garment.waterproof() && condition >= 0.5; }
}

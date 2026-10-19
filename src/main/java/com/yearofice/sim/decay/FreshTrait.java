/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Adapted for Year of Ice from TerraFirmaCraft/TerraFirmaCraft@c3b4d9536b82e5a9746dd793c586720a74108057,
 * src/main/java/net/dries007/tfc/common/component/food/FoodTraits.java and FoodTrait.java
 * (the idea of named traits whose rate multipliers multiply together; reimplemented, no lines copied).
 */
package com.yearofice.sim.decay;

/**
 * Rate multipliers carried on a food stack, in the style of TerraFirmaCraft's food traits: each trait scales
 * the decay rate, the stack's rate is the product, and applying or removing a trait re-dates the stack so its
 * decay progress p is unchanged ({@link Fresh#withTrait}).
 *
 * <p>The numbers are Year of Ice's, not TFC's (Blueprint › Spoilage model, place factor m; Decay and time ›
 * Seven factors, protection). Place factors are traits too, because they belong to the holder, not the clock:
 * a meat pole and a closed cache in the same air share a clock but not a rate. Flies are not a trait: they
 * apply only above 10 °C, so they are a curve ({@link ColdCurve#withFlies()}) to keep the closed form.
 */
public enum FreshTrait {
    /** Salt 0.5 on food; salt doubles every preserved shelf life. */
    SALTED(0.5),
    /** As TFC's brined: no rate change on its own, a flag other steps require. */
    BRINED(1.0),
    /** Hung in shade with moving air: meat pole, raised cache. */
    HUNG(0.8),
    /** On the ground. */
    GROUND(1.3),
    /** In sun. */
    SUN(1.5),
    /** Preserved food stored damp: "Damp halves it". Fixed-life curves only. */
    DAMP_STORE(2.0);

    private final double rate;

    FreshTrait(double rate) { this.rate = rate; }

    public double rate() { return rate; }

    public int bit() { return 1 << ordinal(); }

    public static int mask(FreshTrait... traits) {
        int m = 0;
        for (FreshTrait t : traits) m |= t.bit();
        return m;
    }

    public static boolean has(int mask, FreshTrait t) { return (mask & t.bit()) != 0; }

    /** The product of every trait's multiplier in the mask, in ordinal order so the bits are reproducible. */
    public static double product(int mask) {
        double p = 1.0;
        for (FreshTrait t : values()) {
            if (has(mask, t)) p *= t.rate;
        }
        return p;
    }
}

package com.yearofice.sim.travel;

/**
 * When an open canoe goes over (Water power, crossings and craft › Canoe). Three stresses each add to the chance: wind
 * past the limit on open water, rapids (fast flowing water), and a load past what it carries. The chance per second is
 * the dial times the stresses present, wind counted by how far past the limit it blows (up to 3x). Pure Java; original
 * Year of Ice code.
 */
public final class CanoeMath {
    private CanoeMath() {}

    /** Flow-vector length (0 still, about 1 steep) from which running water counts as rapids. */
    public static final double RAPIDS_FLOW = 0.8;

    public static double capsizeChancePerS(double windMs, double windLimitMs, boolean exposed, boolean rapids,
                                           double loadKg, double loadCapKg, double basePerS) {
        double stress = 0;
        if (exposed && windMs > windLimitMs) stress += Math.min(3.0, windMs / windLimitMs);
        if (rapids) stress += 1;
        if (loadKg > loadCapKg) stress += 1;
        return Math.min(1.0, basePerS * stress);
    }
}

package com.yearofice.sim.build;

/**
 * B8, the cabin tells (Structures and buildables › Warm rooms; Proposed): what a warm room shows the eye and ear
 * without a gauge. Frost forms on a leaky seam when the inside is warm and the outside well below freezing; an icicle
 * grows under a roof edge while heat leaks through snow-covered roof; meltwater drips in between. Pure.
 */
public final class CabinTells {
    private CabinTells() {}

    /** Frost on a seam, 0 to 1: nothing under a 15 degree gap, full at a 50 degree gap, wind pushes it harder. */
    public static double frost(double insideC, double outsideC, double windMs) {
        if (outsideC >= 0) return 0;
        double gap = insideC - outsideC;
        double base = Math.max(0, Math.min(1, (gap - 15) / 35));
        return Math.min(1, base * (1 + Math.min(windMs, 10) / 20));
    }

    /** Icicle length in cm after {@code days} under a snow-covered roof edge over a warm room. */
    public static double icicleCm(double roofSnowCm, double insideC, double outsideC, double days) {
        if (outsideC >= 0 || insideC < 5 || roofSnowCm <= 0) return 0;
        double meltPerDay = Math.min(1.0, roofSnowCm / 20.0) * (insideC - 5) / 15.0 * 6.0;
        double freeze = Math.min(1.0, -outsideC / 5.0);
        return Math.min(80, days * meltPerDay * freeze);
    }

    /** Drips per game minute from a warm roof edge when the outside is near freezing. */
    public static double dripsPerMin(double insideC, double outsideC) {
        if (insideC < 5 || outsideC > 3 || outsideC < -15) return 0;
        return Math.min(6, (insideC - 5) / 5.0) * (outsideC < 0 ? 0.5 : 1.0);
    }
}

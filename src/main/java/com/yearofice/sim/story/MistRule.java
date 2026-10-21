package com.yearofice.sim.story;

/** Misty mornings (Every concept from the video › Misty mornings): open water, calm, warm water, a dry night. */
public final class MistRule {
    private MistRule() {}

    public static boolean mist(boolean openWater, double windMs, double waterC, double airC0500, boolean wetNight) {
        return openWater && windMs < 2 && waterC - airC0500 >= 5 && !wetNight;
    }
}

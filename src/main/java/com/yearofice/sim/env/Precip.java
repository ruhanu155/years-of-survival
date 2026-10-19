package com.yearofice.sim.env;

/** Precipitation state of an {@link EnvSample}. Pure Java mirror of the weather state the env layer samples. */
public enum Precip {
    NONE,
    DRIZZLE,
    RAIN,
    SNOW,
    /** Rain and snow together, or wet snow. */
    MIXED,
    FREEZING_RAIN,
    HAIL;

    /** True for liquid water reaching the body: rain, drizzle, freezing rain and the liquid half of mixed. */
    public boolean liquid() {
        return this == DRIZZLE || this == RAIN || this == FREEZING_RAIN || this == MIXED;
    }
}

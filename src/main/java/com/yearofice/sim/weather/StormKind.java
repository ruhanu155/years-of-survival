package com.yearofice.sim.weather;

import java.util.Locale;

/** What the sky is doing besides the day chain (Weather and storms › Storm catalogue); CLEAR is "no storm". */
public enum StormKind {
    CLEAR, SNOWSTORM, BLIZZARD, FREEZING_RAIN, HAIL, THUNDERSTORM, LONG_RAIN;

    /** What falls: the four the api names, plus NONE. */
    public enum PrecipKind {
        NONE, RAIN, SNOW, GLAZE, HAIL;

        public String id() { return name().toLowerCase(Locale.ROOT); }
    }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static StormKind of(String id) { return valueOf(id.toUpperCase(Locale.ROOT)); }
}

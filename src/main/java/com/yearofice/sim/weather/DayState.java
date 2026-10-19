package com.yearofice.sim.weather;

/** The four-state day chain (Blueprint › Calendar, daylight and weather): Wet is rain or snow, Severe a storm or blizzard. */
public enum DayState {
    CLEAR, CLOUDY, WET, SEVERE;

    public String id() { return name().toLowerCase(java.util.Locale.ROOT); }
}

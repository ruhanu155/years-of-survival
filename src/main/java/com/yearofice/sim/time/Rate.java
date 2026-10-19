package com.yearofice.sim.time;

import java.util.Locale;

/**
 * The clock rates (Minecraft development › Layer 1), the six of the doc plus Day 1. Declaration order is the
 * per-player priority the {@link RateArbiter} applies: close, then work, then until done, then sleep, then travel,
 * then Day 1, then world. Across
 * players the world runs at the slowest requested rate, the one with the fewest game deciseconds a tick.
 */
public enum Rate {
    /** Minigame, drawn bow, alerted predator, bleeding, in water, soaked below 0 °C: 6 game s a real second. */
    CLOSE,
    /** A hold-to-work ring is filling: 5 game min a real second. */
    WORK,
    /** A queued job runs (Z): 20 game min a real second. */
    UNTIL_DONE,
    /** Sleeping or waiting by the fire: 60 game min a real second. */
    SLEEP,
    /** Moving with no job: 12 game s a real second. */
    TRAVEL,
    /**
     * Day 1 (Blueprint › Start site › Day 1 rules): a guided-night player with no fire lit yet, before 22:00 on the
     * first day, asks for the World rate at {@code first_fire_world_pct} (half), so the first afternoon gives about
     * 20 real minutes of daylight work on the 24-minute day. Any other request of the player's wins over it.
     */
    DAY_ONE,
    /** The default: 1 game min a real second on the 24-minute day; the only rate the day length scales. */
    WORLD;

    public String id() { return name().toLowerCase(Locale.ROOT); }

    /** The rate named {@code id} ({@code close}, {@code until_done} ...), or null. */
    public static Rate byId(String id) {
        for (Rate r : values()) if (r.id().equals(id)) return r;
        return null;
    }
}

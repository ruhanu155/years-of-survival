package com.yearofice.body.clock;

import net.minecraft.server.MinecraftServer;

/**
 * What the body tick needs from Layer 1, Clock and calendar: the absolute game minute and a way to jump it for
 * debugging. The body package reads time only through this interface; {@link ServiceBodyClock} answers it from
 * {@code api.clock.ClockService}.
 *
 * <p>Game minute 0 is 00:00 of day 1. Every body rate is per game minute (RECONCILE 117), so the clock's real-time
 * rate never changes what a day costs the body.
 */
public interface BodyClock {

    /** The absolute game minute now. */
    long minute(MinecraftServer server);

    /** Jumps the calendar to day {@code day} (1-based) at the same time of day. Bodies are not stepped. */
    void setDay(MinecraftServer server, int day);

    /** Moves the clock forward {@code minutes}; the caller steps the bodies itself. */
    void advance(MinecraftServer server, long minutes);

    static int day(long minute) { return (int) Math.floorDiv(minute, 1440) + 1; }

    static int minuteOfDay(long minute) { return (int) Math.floorMod(minute, 1440); }

    static String hhmm(long minute) {
        int m = minuteOfDay(minute);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", m / 60, m % 60);
    }
}

package com.yearofice.decay;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * STAND-IN for ClockService (Layer 1), which another department is building: the game minute the decay clock reads.
 * Game time is the overworld's day time, 24,000 ticks a day, day time 0 at 06:00, game minutes counted from day 0's
 * midnight; the same reading the ecosystem's and the body's stand-ins use, so a carcass, its meat and a meal agree.
 * When ClockService lands this class reads it and nothing else changes.
 */
public final class DecayClock {
    private DecayClock() {}

    public static final long TICKS_PER_DAY = 24_000;
    public static final long OFFSET_TICKS = 6_000;
    public static final double TICKS_PER_MINUTE = TICKS_PER_DAY / 1440.0;

    /** Fractional game minutes now, on either side. */
    public static double minuteD(Level level) {
        Level clock = level instanceof ServerLevel sl ? sl.getServer().overworld() : level;
        return (clock.getDayTime() + OFFSET_TICKS) / TICKS_PER_MINUTE;
    }
}

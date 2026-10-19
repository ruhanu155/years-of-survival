package com.yearofice.decay;

import com.yearofice.api.clock.ClientTime;
import com.yearofice.api.clock.ClockService;
import net.minecraft.world.level.Level;

/**
 * The game minute the decay clock reads, from Layer 1 (M2 lane B): {@link ClockService} on the server and the synced
 * {@link ClientTime} on the client. Game minutes count from day 0's midnight, as the body's and the ecosystem's
 * clocks do, so a carcass, its meat and a meal agree.
 */
public final class DecayClock {
    private DecayClock() {}

    public static final long TICKS_PER_DAY = 24_000;
    public static final long OFFSET_TICKS = 6_000;
    public static final double TICKS_PER_MINUTE = TICKS_PER_DAY / 1440.0;

    /** Fractional game minutes now, on either side. */
    public static double minuteD(Level level) {
        if (level.isClientSide) return ClientTime.minuteD();
        return ClockService.installed() ? ClockService.get().nowDs() / 600.0 : 0;
    }
}

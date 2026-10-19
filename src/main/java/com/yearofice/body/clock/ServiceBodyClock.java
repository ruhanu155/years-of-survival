package com.yearofice.body.clock;

import com.yearofice.api.clock.ClockService;
import net.minecraft.server.MinecraftServer;

/**
 * The one-class adapter {@link BodyClock} was written for: the body reads Layer 1 through {@link ClockService}.
 * Game minute 0 is 00:00 of May 1, year 1, in both. (Landed with the clock in M2 lane B, because the clock now
 * drives vanilla's day time from the true sun and {@link VanillaDayClock}'s linear reading no longer holds.)
 */
public final class ServiceBodyClock implements BodyClock {

    @Override
    public long minute(MinecraftServer server) { return ClockService.get().nowMinute(); }

    @Override
    public void setDay(MinecraftServer server, int day) { ClockService.get().setDay(day); }

    @Override
    public void advance(MinecraftServer server, long minutes) { ClockService.get().advance(minutes); }
}

package com.yearofice.body.clock;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * The stand-in clock until Layer 1 lands: game time is the overworld's day time, 24,000 ticks a day, with tick 0
 * at 06:00 as vanilla draws the sun. One game minute is 16⅔ ticks at vanilla's rate; the World rate and the
 * world-creation day length (RECONCILE 117, 145) belong to the Clock layer and arrive with it. Vanilla's night
 * skip moves the day time by hours in one tick, which the body ticker steps in closed form as sleep.
 */
public final class VanillaDayClock implements BodyClock {
    public static final long TICKS_PER_DAY = 24_000;
    /** Day time 0 is 06:00. */
    public static final long OFFSET_TICKS = 6_000;

    @Override
    public long minute(MinecraftServer server) {
        return toMinute(server.overworld().getDayTime());
    }

    public static long toMinute(long dayTime) {
        return Math.floorDiv((dayTime + OFFSET_TICKS) * 1440, TICKS_PER_DAY);
    }

    public static long toDayTime(long minute) {
        // Ceiling, so toMinute(toDayTime(m)) == m.
        return -Math.floorDiv(-minute * TICKS_PER_DAY, 1440) - OFFSET_TICKS;
    }

    @Override
    public void setDay(MinecraftServer server, int day) {
        long now = minute(server);
        long target = (long) (day - 1) * 1440 + BodyClock.minuteOfDay(now);
        set(server, toDayTime(target));
    }

    @Override
    public void advance(MinecraftServer server, long minutes) {
        set(server, toDayTime(minute(server) + minutes));
    }

    private static void set(MinecraftServer server, long dayTime) {
        for (ServerLevel level : server.getAllLevels()) level.setDayTime(dayTime);
    }
}

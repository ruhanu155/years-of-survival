package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.CloseClock;
import com.yearofice.sim.fishing.SliceBite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

/**
 * Time as fishing reads it until Layer 1's {@code clock.ClockService} lands, and the fight's close clock.
 *
 * <p><b>Game minutes.</b> The same stand-in the body uses: the overworld's day time, 24,000 ticks a day, tick 0 at
 * 06:00, game minute 0 at 00:00 of day 1 (May 1). Bites roll once per game minute.
 *
 * <p><b>Sun.</b> The vanilla sky's sun: up at 06:00, down at 18:00, below −6° 24 minutes either side (the sun moves
 * 15° an hour). {@code sim.time.Sun} at 64°N replaces this when the Time and sky layer lands.
 *
 * <p><b>Close clock.</b> From the strike until the fish is landed or lost the world clock runs at a tenth of its
 * speed (Fishing methods › The fight, as built). The Blueprint keeps it per player for co-op after 1.0; with one
 * world clock in Minecraft, this stand-in slows it only while every player online is in a fight, which is the solo
 * case exactly and the "slowest player" rule in co-op.
 */
public final class FishingClock {
    private FishingClock() {}

    public static final long TICKS_PER_DAY = 24_000, OFFSET_TICKS = 6_000;
    public static final int SUNRISE_MIN = 6 * 60, SUNSET_MIN = 18 * 60, TWILIGHT_MIN = 24;

    private static CloseClock carry = CloseClock.ZERO;

    public static long minute(MinecraftServer server) { return toMinute(server.overworld().getDayTime()); }

    public static long toMinute(long dayTime) { return Math.floorDiv((dayTime + OFFSET_TICKS) * 1440, TICKS_PER_DAY); }

    /** Absolute day, 1 on the first May 1. */
    public static long day(long minute) { return Math.floorDiv(minute, 1440) + 1; }

    public static int minuteOfDay(long minute) { return (int) Math.floorMod(minute, 1440L); }

    public static BiteModel.TimeBand band(long minute) {
        return SliceBite.timeBand(minuteOfDay(minute), SUNRISE_MIN, SUNSET_MIN, TWILIGHT_MIN);
    }

    /** Every non-spectator player online is in a fight, and at least one fight is on. */
    static boolean closeClockOn(MinecraftServer server) {
        if (FishingService.activeFights() == 0) return false;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!p.isSpectator() && !FishingService.inFight(p)) return false;
        }
        return true;
    }

    /** After the overworld's tick: give back the share of this tick's minute the close clock does not spend. */
    public static void afterLevelTick(Level level) {
        if (!(level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
        if (!sl.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT) || !closeClockOn(sl.getServer())) {
            carry = CloseClock.ZERO;
            return;
        }
        CloseClock.Step s = carry.tick(FishingConfig.tuning().closeClockRate());
        carry = s.next();
        sl.setDayTime(sl.getDayTime() - 1 + s.ticks());
    }

    static void clear() { carry = CloseClock.ZERO; }
}

package com.yearofice.ecosystem.clock;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The ecosystem's reading of the game clock until Layer 1 lands, on the same footing as the body's stand-in: game
 * time is the overworld's day time, 24,000 ticks a day, day time 0 at 06:00, so dawn and dusk match the sky the
 * player sees. Game minutes count from midnight of day 0 ({@link com.yearofice.sim.ecosystem.SliceCalendar}).
 *
 * <p>{@link #lapse} is the Work time-lapse (Minecraft engine › The four rates) for one player's hold-to-work job:
 * it moves the day time on, as {@code TimeService.requestRate(player, WORK)} will. In single player the world
 * follows the request; with more than one player online the world keeps its rate (co-op rule, RECONCILE 115).
 */
public final class SliceClock {
    private SliceClock() {}

    public static final long TICKS_PER_DAY = 24_000;
    public static final long OFFSET_TICKS = 6_000;
    public static final double TICKS_PER_MINUTE = TICKS_PER_DAY / 1440.0;

    /** Fractional game minutes now. */
    public static double minuteD(Level level) {
        Level clock = level instanceof ServerLevel sl ? sl.getServer().overworld() : level;
        return (clock.getDayTime() + OFFSET_TICKS) / TICKS_PER_MINUTE;
    }

    public static long minute(Level level) { return (long) Math.floor(minuteD(level)); }

    private static final Map<UUID, Double> CARRY = new ConcurrentHashMap<>();

    /**
     * Adds the time-lapse's extra game minutes for one tick of work at {@code minPerS}, beyond the World rate the
     * day time already runs at (1,440 game minutes in 20 real minutes). Only a player in the server's list counts.
     */
    public static void lapse(ServerPlayer p, double minPerS) {
        MinecraftServer server = p.getServer();
        if (server == null || server.getPlayerList().getPlayer(p.getUUID()) != p) return;
        if (server.getPlayerList().getPlayerCount() > 1) return;
        double worldMinPerTick = 1.0 / TICKS_PER_MINUTE;
        double extraMin = Math.max(0, minPerS / 20.0 - worldMinPerTick);
        double ticks = CARRY.getOrDefault(p.getUUID(), 0.0) + extraMin * TICKS_PER_MINUTE;
        long whole = (long) Math.floor(ticks);
        CARRY.put(p.getUUID(), ticks - whole);
        if (whole <= 0) return;
        for (ServerLevel level : server.getAllLevels()) level.setDayTime(level.getDayTime() + whole);
    }

    /** Moves the day time on by whole game minutes (setting a snare, a debug advance). */
    public static void advance(ServerPlayer p, int minutes) {
        MinecraftServer server = p.getServer();
        if (server == null || server.getPlayerList().getPlayer(p.getUUID()) != p) return;
        if (server.getPlayerList().getPlayerCount() > 1) return;
        long ticks = Math.round(minutes * TICKS_PER_MINUTE);
        for (ServerLevel level : server.getAllLevels()) level.setDayTime(level.getDayTime() + ticks);
    }
}

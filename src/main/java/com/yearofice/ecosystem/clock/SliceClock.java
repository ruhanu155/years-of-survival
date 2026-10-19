package com.yearofice.ecosystem.clock;

import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.time.Rate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The ecosystem's reading of the game clock, through Layer 1's {@link ClockService} (M2 lane B: the clock now
 * drives vanilla's day time from the true sun, so reading the day time linearly no longer gives game minutes).
 * Game minutes count from midnight of day 0 ({@link com.yearofice.sim.time.Calendar}), as the clock's do.
 *
 * <p>{@link #lapse} asks the Work rate for one player's hold-to-work job (Minecraft development › Layer 1); the
 * clock runs the world at the slowest player's rate, so in single player the world follows the request and in
 * co-op it does not (RECONCILE 115).
 */
public final class SliceClock {
    private SliceClock() {}

    /** Real ticks per game minute at the World rate on the vanilla-length day the slice was tuned on. */
    public static final long TICKS_PER_DAY = 24_000;
    public static final long OFFSET_TICKS = 6_000;
    public static final double TICKS_PER_MINUTE = TICKS_PER_DAY / 1440.0;

    /** Fractional game minutes now. */
    public static double minuteD(Level level) {
        return ClockService.installed() ? ClockService.get().nowDs() / 600.0 : 0;
    }

    public static long minute(Level level) { return (long) Math.floor(minuteD(level)); }

    /** One tick of a hold-to-work job: ask the Work rate for this player this tick. */
    public static void lapse(ServerPlayer p, double minPerS) {
        ClockService.get().requestRate(p, Rate.WORK, "butchering");
    }

    /** Moves the clock on by whole game minutes (setting a snare, a debug advance); single player only. */
    public static void advance(ServerPlayer p, int minutes) {
        MinecraftServer server = p.getServer();
        if (server == null || server.getPlayerList().getPlayer(p.getUUID()) != p) return;
        if (server.getPlayerList().getPlayerCount() > 1) return;
        ClockService.get().advance(minutes);
    }
}

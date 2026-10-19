package com.yearofice.hunt.fishing;

import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.SliceBite;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.time.Rate;
import com.yearofice.sim.time.Sun;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Time as fishing reads it, through Layer 1's {@link ClockService} (M2 lane B moved it off the vanilla day time).
 *
 * <p><b>Game minutes.</b> Minute 0 is 00:00 on May 1 of year 1; bites roll once per game minute.
 *
 * <p><b>Sun.</b> The daylight table at 64°N: the time band's sunrise and sunset are that day's, and its twilight
 * runs from dawn (the sun through −6°) to sunrise and from sunset to dusk.
 *
 * <p><b>Close clock.</b> From the strike until the fish is landed or lost, each fighting player asks the Close rate
 * every tick (Fishing methods › The fight, as built). The clock runs the world at the slowest player's rate, so in
 * single player the fight runs at 3 game ds a tick, a tenth of the 24-minute day's World rate.
 */
public final class FishingClock {
    private FishingClock() {}

    public static long minute(MinecraftServer server) { return ClockService.get().nowMinute(); }

    /** Absolute day, 1 on the first May 1. */
    public static long day(long minute) { return Math.floorDiv(minute, 1440) + 1; }

    public static int minuteOfDay(long minute) { return (int) Math.floorMod(minute, 1440L); }

    /** The time band at {@code minute} from that day's sunrise, sunset and twilight. */
    public static BiteModel.TimeBand band(long minute) {
        long absDay = Calendar.absDayOfMinute(minute);
        Sun sun = ClockService.get().sun();
        int rise = sun.sunriseMinute(absDay), set = Math.min(sun.sunsetMinute(absDay), 1439);
        if (rise < 0) return BiteModel.TimeBand.DAY;
        int twilight = Math.max(1, rise - Math.max(0, sun.dawnMinute(absDay)));
        return SliceBite.timeBand(minuteOfDay(minute), rise, set, twilight);
    }

    /** Each tick, every player in a fight asks for the close clock. */
    public static void afterLevelTick(Level level) {
        if (level.isClientSide || level.getServer() == null || level != level.getServer().overworld()) return;
        for (ServerPlayer p : FishingService.fighters()) ClockService.get().requestRate(p, Rate.CLOSE, "fishing fight");
    }

    static void clear() { }
}

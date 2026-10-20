package com.yearofice.fire;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.sim.fire.FireDanger;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.WeatherModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Today's fire danger level (Every concept from the video › Fire danger), worked once a game day from the
 * Environment field's drawn days: days since 5 mm of rain, today's high (the day's mean plus its swing) and wind.
 * {@code /yoi fire danger <level>} pins a level for testing. Green-up and thaw dates are not modelled yet, so the
 * window is the game year's May 1 to August 31 with no snow on the ground.
 */
public final class FireDangerIndex {
    private FireDangerIndex() {}

    private record Cached(long day, FireDanger.Level level) {}

    private static final Map<ResourceKey<Level>, Cached> CACHE = new HashMap<>();
    private static final Map<ResourceKey<Level>, FireDanger.Level> PIN = new HashMap<>();

    public static void pin(Level level, FireDanger.Level l) {
        if (l == null) PIN.remove(level.dimension());
        else PIN.put(level.dimension(), l);
    }

    static void clear() {
        CACHE.clear();
        PIN.clear();
    }

    public static FireDanger.Level level(Level level) {
        FireDanger.Level pinned = PIN.get(level.dimension());
        if (pinned != null) return pinned;
        if (!(level instanceof ServerLevel sl) || !EnvService.installed() || !ClockService.installed()) return FireDanger.Level.LOW;
        long day = Calendar.absDayOfMinute(ClockService.get().nowMinute());
        Cached c = CACHE.get(level.dimension());
        if (c != null && c.day() == day) return c.level();
        FireDanger.Level l = compute(sl, day);
        CACHE.put(level.dimension(), new Cached(day, l));
        return l;
    }

    static FireDanger.Level compute(ServerLevel level, long day) {
        EnvService env = EnvService.get();
        WeatherModel.Day today = env.day(level, day);
        List<FireDanger.Day> past = new ArrayList<>();
        for (int i = 1; i <= FireDanger.DRY_CAP_DAYS; i++) past.add(new FireDanger.Day(env.day(level, day - i).precipMm()));
        double snow = env.valley(level).snowDepthCm();
        return FireDanger.today(Calendar.dayOfYearOfDay(day), snow, past, today.meanC() + today.swingC(), today.windMs());
    }
}

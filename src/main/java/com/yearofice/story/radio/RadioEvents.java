package com.yearofice.story.radio;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.RadioMath;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.story.Journal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Day and noon hooks: the panel charges loaded desks once a day, and a player whose radio works gets the season's drop on
 * the first flyable day of its first month, slipping a day at a time (up to the data's {@code max_slip_days}).
 * ponytail: no log points or orders yet, so every season with a working radio earns its drop; desks in unloaded chunks
 * miss the day's charge — upgrade: a catch-up from a last-charged day on load.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class RadioEvents {
    private RadioEvents() {}

    static final Set<RadioDeskBlockEntity> LIVE = ConcurrentHashMap.newKeySet();

    @SubscribeEvent
    public static void day(ClockEvent.Day e) {
        int month = Calendar.month(Calendar.dayOfYearOfDay(e.absDay()));
        for (RadioDeskBlockEntity be : LIVE) {
            if (!(be.getLevel() instanceof ServerLevel l)) continue;
            boolean sunny = !WeatherService.installed() || WeatherService.get().at(l, be.getBlockPos(), e.absDay() * 1440L + 720).kind() == StormKind.CLEAR;
            be.charge(RadioMath.dailyGainPct(month, sunny, RadioConfig.get("panel_factor")));
        }
    }

    /** Season number from May 1 of year 1: May–Jul 0, Aug–Oct 1, Nov–Jan 2, Feb–Apr 3, then on. */
    static int season(long absDay) {
        int doy = Calendar.dayOfYearOfDay(absDay);
        return (int) (Math.floorDiv(absDay, 365L) * 4 + Math.min(3, Math.floorDiv(doy, 92)));
    }

    @SubscribeEvent
    public static void hour(ClockEvent.Hour e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || e.hourOfDay() != RadioData.skeds().getAsJsonObject("drop").get("at_h").getAsInt()) return;
        long absDay = Calendar.absDayOfMinute(e.minute());
        int season = season(absDay);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            CompoundTag r = Radio.state(p);
            if (!r.getBoolean("working")) continue;
            if (r.contains("last_drop_season") && r.getInt("last_drop_season") >= season) continue;
            if (season == 0) continue; // the first season has no drop: the crate itself is the first kit
            ServerLevel l = p.serverLevel();
            var zone = SupplyDrop.zoneFor(l, p);
            int slips = r.getInt("slips");
            if (!SupplyDrop.flyable(l, zone, e.minute()) && slips < RadioData.skeds().getAsJsonObject("drop").get("max_slip_days").getAsInt()) {
                r.putInt("slips", slips + 1);
                Radio.save(p, r);
                continue;
            }
            SupplyDrop.fly(l, zone);
            r.putInt("last_drop_season", season);
            r.putInt("slips", 0);
            Radio.save(p, r);
            Journal.record(p, "supply_drop", "season_" + season, "A supply drop came down by parachute.", e.minute(), (int) absDay);
        }
    }
}

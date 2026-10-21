package com.yearofice.story.radio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.sim.story.RadioMath;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.story.Journal;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** The sked: Dana's program outlook (read from {@link WeatherService}), one news line, and the drop notice. */
public final class Radio {
    private Radio() {}

    static final String KEY = "yearofice:radio";

    /** The text of a sked read at {@code minute} for a station at {@code pos}. */
    public static List<String> skedLines(ServerLevel level, BlockPos pos, long minute) {
        JsonObject d = RadioData.skeds();
        List<String> out = new ArrayList<>();
        out.add(d.get("open").getAsString());
        out.add(d.get("forecast_intro").getAsString());
        long today = Calendar.absDayOfMinute(minute);
        for (int lead = 1; lead <= 5; lead++) out.add(forecastLine(level, pos, today, lead));
        JsonArray news = d.getAsJsonArray("news");
        out.add(news.get(RadioMath.pick(today, 7, news.size())).getAsString());
        return out;
    }

    /** One outlook line; days 3–5 are right 70% of the time and a miss reads as fair. */
    public static String forecastLine(ServerLevel level, BlockPos pos, long today, int lead) {
        StormKind kind = StormKind.CLEAR;
        if (WeatherService.installed()) kind = WeatherService.get().at(level, pos, (today + lead) * 1440L + 720).kind();
        if (!RadioMath.outlookRight(today, lead)) kind = StormKind.CLEAR;
        String when = lead == 1 ? "Tomorrow" : "In " + lead + " days";
        return when + ": " + RadioData.skeds().getAsJsonObject("forecast").get(kind.name().toLowerCase(java.util.Locale.ROOT)).getAsString();
    }

    /** A powered desk used by a player: the sked is read to them and the radio counts as working. */
    public static void tuneIn(ServerLevel level, ServerPlayer p, BlockPos pos) {
        long minute = ClockService.installed() ? ClockService.get().nowMinute() : 0;
        for (String line : skedLines(level, pos, minute)) com.yearofice.api.hud.HudBus.get().post(p, com.yearofice.api.hud.HudLine.of(com.yearofice.api.hud.HudLineType.EVENT, "radio_sked", "story.radio", com.yearofice.api.hud.HudArg.text(line)));
        CompoundTag r = state(p);
        if (!r.getBoolean("working")) {
            r.putBoolean("working", true);
            Journal.record(p, "radio", "first_sked", "The radio works. Dana answered the first call.", minute, (int) Calendar.absDayOfMinute(minute));
        }
        save(p, r);
    }

    static CompoundTag state(ServerPlayer p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(KEY);
    }

    static void save(ServerPlayer p, CompoundTag r) {
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        root.put(KEY, r);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
    }

    public static boolean working(ServerPlayer p) { return state(p).getBoolean("working"); }
}

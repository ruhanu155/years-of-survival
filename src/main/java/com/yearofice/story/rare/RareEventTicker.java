package com.yearofice.story.rare;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.event.RareEventFiredEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.MistRule;
import com.yearofice.sim.story.RareEvents;
import com.yearofice.sim.time.Calendar;
import com.yearofice.story.Journal;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Wires the M7 story sims into the clock: each day a rare event's window opens, {@link RareEvents#rolls} decides (a pure
 * function of seed, id and year, so the answer is the same after a restart) and a {@link RareEventFiredEvent} is posted
 * with a journal line; at 05:00 {@link MistRule} decides the morning's mist ({@link #mistToday}).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class RareEventTicker {
    private RareEventTicker() {}

    record Row(RareEvents.Def def, int startDoy) {}

    static final List<String> IDS = List.of("aurora_storm", "big_berry_year", "decade_storm", "earthquake", "hard_winter", "spring_flood", "wildfire", "winter_killed_moose");
    private static volatile List<Row> rows;
    private static volatile boolean mist;

    public static boolean mistToday() { return mist; }

    static synchronized List<Row> rows() {
        if (rows != null) return rows;
        List<Row> l = new ArrayList<>();
        for (String id : IDS) {
            try (var in = RareEventTicker.class.getResourceAsStream("/data/yearofice/event/" + id + ".json")) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                String[] from = (o.has("window") ? o.getAsJsonArray("window").get(0).getAsString() : "05-01").split("-");
                int doy = Calendar.dayOfYear(Integer.parseInt(from[0]), Integer.parseInt(from[1]));
                l.add(new Row(new RareEvents.Def(id, o.get("chance_per_year").getAsDouble(), o.get("from_year").getAsInt()), doy));
            } catch (java.io.IOException | RuntimeException ex) {
                throw new IllegalStateException("bad rare event " + id, ex);
            }
        }
        return rows = List.copyOf(l);
    }

    /** The events whose window opens on {@code absDay} and that roll true this year. */
    public static List<String> dayPass(long seed, long absDay) {
        List<String> out = new ArrayList<>();
        int doy = Calendar.dayOfYearOfDay(absDay), year = Calendar.yearOfDay(absDay);
        for (Row r : rows()) if (r.startDoy() == doy && RareEvents.rolls(r.def(), year, seed)) out.add(r.def().id());
        return out;
    }

    /** Posts and journals today's rare events; returns their ids. */
    public static List<String> fireDay(ServerLevel level, long absDay) {
        List<String> ids = dayPass(level.getSeed(), absDay);
        for (String id : ids) {
            NeoForge.EVENT_BUS.post(new RareEventFiredEvent(id, Calendar.yearOfDay(absDay), absDay));
            for (ServerPlayer p : level.players())
                Journal.record(p, "rare_event", id, "A rare year: " + id.replace('_', ' ') + ".", absDay * 1440L, (int) absDay);
        }
        return ids;
    }

    /** The mist at 05:00 of {@code absDay} (valley air, river water, a dry night before). */
    public static boolean morningMist(ServerLevel level, long absDay) {
        EnvService env = EnvService.get();
        long minute = absDay * 1440 + 5 * 60;
        var hr = env.valleyAt(level, minute);
        double water = env.waterCAt(level, EnvService.WaterKind.RIVER, minute);
        boolean wet = env.day(level, absDay - 1).precipMm() >= 0.5;
        return mist = MistRule.mist(water >= 1, hr.windMs(), water, hr.airC(), wet);
    }

    @SubscribeEvent
    public static void day(ClockEvent.Day e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) fireDay(server.overworld(), e.absDay());
    }

    @SubscribeEvent
    public static void hour(ClockEvent.Hour e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && e.hourOfDay() == 5) morningMist(server.overworld(), Calendar.absDayOfMinute(e.minute()));
    }
}

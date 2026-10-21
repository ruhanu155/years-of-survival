package com.yearofice.story.ambition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.api.story.AmbitionTracker;
import com.yearofice.sim.time.Calendar;
import com.yearofice.story.Journal;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * The ambition catalogue ({@code data/yearofice/ambition/<id>.json}, Open world in depth › Ambitions) on top of
 * {@link AmbitionTracker}. A system calls {@link #fire} with a trigger key; a def's {@code opens_on} opens it and its
 * {@code closes_on} completes it with one journal line (recognition only: reward is none). Defs with no {@code opens_on}
 * open with their group: on its date, or after 4 ambitions of the group before it are done ({@link #refresh}).
 */
public final class Ambitions {
    private Ambitions() {}

    public record Def(String id, String group, String title, String condition, String opensOn, String closesOn, String journalLine, boolean hidden) {}

    record Group(String id, int openDay, String after) {}

    private static volatile List<Def> defs;
    private static volatile Map<String, Group> groups;
    private static volatile List<String> groupOrder;

    public static List<Def> all() {
        load();
        return defs;
    }

    private static synchronized void load() {
        if (defs != null) return;
        JsonObject g = read("groups");
        Map<String, Group> gm = new HashMap<>();
        List<String> order = new ArrayList<>();
        g.getAsJsonArray("groups").forEach(e -> {
            JsonObject o = e.getAsJsonObject();
            gm.put(o.get("id").getAsString(), new Group(o.get("id").getAsString(), o.get("open_days").getAsInt(), o.get("after").isJsonNull() ? "" : o.get("after").getAsString()));
            order.add(o.get("id").getAsString());
        });
        List<Def> l = new ArrayList<>();
        g.getAsJsonArray("ids").forEach(e -> {
            JsonObject o = read(e.getAsString());
            l.add(new Def(o.get("id").getAsString(), o.get("group").getAsString(), o.get("title").getAsString(), o.get("condition").getAsString(),
                    o.get("opens_on").getAsString(), o.get("closes_on").getAsString(), o.get("journal_line").getAsString(), o.get("hidden").getAsBoolean()));
        });
        groups = gm;
        groupOrder = order;
        defs = List.copyOf(l);
    }

    private static JsonObject read(String name) {
        try (var in = Ambitions.class.getResourceAsStream("/data/yearofice/ambition/" + name + ".json")) {
            if (in == null) throw new IllegalArgumentException("missing ambition data " + name);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Def byId(String id) {
        for (Def d : all()) if (d.id().equals(id)) return d;
        return null;
    }

    /** A system reports a counted event: opens what listens for it, completes what it closes. */
    public static void fire(ServerPlayer p, String trigger, long minute) {
        for (Def d : all()) {
            if (d.opensOn().equals(trigger)) AmbitionTracker.open(p, d.id());
            if (d.closesOn().equals(trigger)) {
                AmbitionTracker.open(p, d.id()); // a completion is never lost to a group that has not opened yet
                if (AmbitionTracker.complete(p, d.id())) Journal.record(p, "ambition", d.id(), d.journalLine(), minute, (int) Math.floorDiv(minute, 1440L));
            }
        }
    }

    /** Opens every def with no {@code opens_on} whose group is open on {@code absDay}. */
    public static void refresh(ServerPlayer p, long absDay) {
        load();
        for (Def d : defs) if (d.opensOn().isEmpty() && groupOpen(p, d.group(), absDay)) AmbitionTracker.open(p, d.id());
    }

    static boolean groupOpen(ServerPlayer p, String group, long absDay) {
        Group g = groups.get(group);
        if (g == null) return false;
        if (absDay >= g.openDay()) return true;
        if (g.after().isEmpty()) return false;
        long done = defs.stream().filter(d -> d.group().equals(g.after()) && AmbitionTracker.isDone(p, d.id())).count();
        return done >= 4;
    }

    /** The journal page: done, open and not-yet-open lines; hidden ones read "???" until done. */
    public static List<String> page(ServerPlayer p) {
        List<String> out = new ArrayList<>();
        load();
        for (String g : groupOrder) {
            out.add("== " + g.replace('_', ' ') + " ==");
            for (Def d : defs) {
                if (!d.group().equals(g)) continue;
                boolean done = AmbitionTracker.isDone(p, d.id()), open = AmbitionTracker.isOpen(p, d.id());
                if (!done && !open) continue;
                String name = d.hidden() && !done ? "???" : d.title();
                out.add((done ? "[x] " : "[ ] ") + name + (d.hidden() && !done ? "" : " - " + d.condition()));
            }
        }
        return out;
    }
}

package com.yearofice.data.migrate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The step registry of the versioned codecs (Saves across versions › Why our own steps). A migration id such as
 * {@code yearofice:fresh} has steps keyed by the version they start from. Simple steps are data in
 * {@code data/yearofice/retired/migrations.json}:
 * <pre>{ "migration": "yearofice:fresh", "from": 1, "to": 2,
 *   "add": { "curve": "meat" }, "rename": { "due": "due_dh" } }</pre>
 * Also accepted: {@code scale} ({@code {"field": factor}}), {@code map_values} ({@code {"field": {"old": "new"}}})
 * and {@code remove} (a list of fields). They apply in the order rename, scale, map_values, remove, add; {@code add}
 * never overwrites a field already there. Anything else is a Java {@link Step} registered with
 * {@link #register(String, int, int, Step)}.
 */
public final class Migrations {
    public static final String RESOURCE = "/data/yearofice/retired/migrations.json";
    public static final String DEFAULT_KEY = "v";

    private record Entry(int to, Step step) {}

    private final Map<String, Map<Integer, Entry>> steps = new HashMap<>();
    private static volatile Migrations shared;

    /** The migrations shipped in the jar. */
    public static Migrations shared() {
        Migrations m = shared;
        if (m == null) {
            m = new Migrations();
            try (InputStream in = Migrations.class.getResourceAsStream(RESOURCE)) {
                if (in != null) m.addJson(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (IOException e) {
                throw new IllegalStateException("cannot read " + RESOURCE, e);
            }
            shared = m;
        }
        return m;
    }

    public Migrations register(String id, int from, int to, Step step) {
        if (to <= from) throw new IllegalArgumentException(id + ": step must go up, not " + from + " -> " + to);
        if (steps.computeIfAbsent(id, k -> new HashMap<>()).putIfAbsent(from, new Entry(to, step)) != null)
            throw new IllegalArgumentException(id + ": two steps from v" + from);
        return this;
    }

    public boolean has(String id) { return steps.containsKey(id); }

    public int count() { return steps.values().stream().mapToInt(Map::size).sum(); }

    /** Loads the {@code steps} array of a migrations file. */
    public Migrations addJson(JsonObject root) {
        JsonArray arr = root.has("steps") ? root.getAsJsonArray("steps") : new JsonArray();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            register(o.get("migration").getAsString(), o.get("from").getAsInt(), o.get("to").getAsInt(), fromJson(o));
        }
        return this;
    }

    static Step fromJson(JsonObject o) {
        return rec -> {
            Dynamic<?> d = rec;
            if (o.has("rename")) for (Map.Entry<String, JsonElement> r : o.getAsJsonObject("rename").entrySet()) d = rename(d, r.getKey(), r.getValue().getAsString());
            if (o.has("scale")) for (Map.Entry<String, JsonElement> r : o.getAsJsonObject("scale").entrySet()) d = scale(d, r.getKey(), r.getValue().getAsDouble());
            if (o.has("map_values")) for (Map.Entry<String, JsonElement> r : o.getAsJsonObject("map_values").entrySet()) d = mapValues(d, r.getKey(), r.getValue().getAsJsonObject());
            if (o.has("remove")) for (JsonElement r : o.getAsJsonArray("remove")) d = d.remove(r.getAsString());
            if (o.has("add")) for (Map.Entry<String, JsonElement> r : o.getAsJsonObject("add").entrySet())
                if (d.get(r.getKey()).result().isEmpty()) d = d.set(r.getKey(), convert(r.getValue(), d));
            return d;
        };
    }

    private static Dynamic<?> convert(JsonElement json, Dynamic<?> like) {
        return new Dynamic<>(JsonOps.INSTANCE, json).convert(like.getOps());
    }

    private static Dynamic<?> rename(Dynamic<?> d, String from, String to) {
        var v = d.get(from).result();
        if (v.isEmpty()) return d;
        return d.remove(from).set(to, v.get());
    }

    private static Dynamic<?> scale(Dynamic<?> d, String field, double factor) {
        var v = d.get(field).asNumber().result();
        if (v.isEmpty()) return d;
        double r = v.get().doubleValue() * factor;
        boolean integral = (v.get() instanceof Integer || v.get() instanceof Long || v.get() instanceof Short || v.get() instanceof Byte)
                && factor == Math.rint(factor);
        return d.set(field, integral ? d.createLong((long) r) : d.createDouble(r));
    }

    private static Dynamic<?> mapValues(Dynamic<?> d, String field, JsonObject table) {
        var v = d.get(field).asString().result();
        if (v.isEmpty() || !table.has(v.get())) return d;
        return d.set(field, d.createString(table.get(v.get()).getAsString()));
    }

    /**
     * Runs the steps from the record's stored version (the {@code key} field; v1 when it has none) up to
     * {@code current}. A record already at {@code current} comes back unchanged.
     */
    @SuppressWarnings("unchecked")
    public <U> Dynamic<U> upgrade(String id, Dynamic<U> record, int current, String key) {
        Dynamic<?> d = record;
        int v = d.get(key).asInt(1);
        if (v > current) throw new IllegalStateException(id + " was saved by a newer Year of Ice (record v" + v + "); update the mod to open this world");
        while (v < current) {
            Entry e = steps.getOrDefault(id, Map.of()).get(v);
            if (e == null) throw new IllegalStateException(id + ": no migration step from v" + v + " (jar is at v" + current + ")");
            if (e.to() > current) throw new IllegalStateException(id + ": step v" + v + " -> v" + e.to() + " overshoots v" + current);
            d = e.step().apply(d);
            v = e.to();
            d = d.set(key, d.createInt(v));
        }
        return (Dynamic<U>) d;
    }
}

package com.yearofice.story.keepsake;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** {@code data/yearofice/keepsake/<id>.json}: name, where it is found, its journal clip and the lines it reads. */
public final class Keepsakes {
    private Keepsakes() {}

    public record Def(String id, String name, String description, String where, String clip, List<String> read) {
        public String path() { return id.substring(id.indexOf(':') + 1); }
    }

    private static final Map<String, Def> CACHE = new ConcurrentHashMap<>();

    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        read("index").getAsJsonArray("ids").forEach(e -> out.add(e.getAsString()));
        return out;
    }

    /** By path ({@code fur_ledger}) or full id; null when there is no such keepsake. */
    public static Def get(String id) {
        String p = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (p.isEmpty()) return null;
        try {
            return CACHE.computeIfAbsent(p, k -> {
                JsonObject o = read(k);
                List<String> lines = new ArrayList<>();
                o.getAsJsonArray("read").forEach(e -> lines.add(e.getAsString()));
                return new Def(o.get("id").getAsString(), o.get("name").getAsString(), o.get("description").getAsString(), o.get("where").getAsString(),
                        o.get("journal_clip").getAsString(), lines);
            });
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static JsonObject read(String name) {
        try (var in = Keepsakes.class.getResourceAsStream("/data/yearofice/keepsake/" + name + ".json")) {
            if (in == null) throw new IllegalArgumentException("no keepsake " + name);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}

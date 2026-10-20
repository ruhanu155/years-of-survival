package com.yearofice.sim.hunt;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Loads {@code weapon/}, {@code arrowhead/}, {@code call/} and {@code hitzone/} data from the mod's own resources, once each. */
public final class HuntData {
    private HuntData() {}

    private static final Map<String, Weapon> WEAPONS = new ConcurrentHashMap<>();
    private static final Map<String, ArrowHead> HEADS = new ConcurrentHashMap<>();
    private static final Map<String, HitZones> ZONES = new ConcurrentHashMap<>();
    private static final Map<String, JsonObject> CALLS = new ConcurrentHashMap<>();

    static JsonObject read(String kind, String id) {
        String path = "/data/yearofice/" + kind + "/" + id + ".json";
        try (InputStream in = HuntData.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalArgumentException("no " + path);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    public static Weapon weapon(String id) { return WEAPONS.computeIfAbsent(id, k -> Weapon.parse(read("weapon", k))); }

    public static ArrowHead head(String id) { return HEADS.computeIfAbsent(id, k -> ArrowHead.parse(read("arrowhead", k))); }

    public static JsonObject call(String id) { return CALLS.computeIfAbsent(id, k -> read("call", k)); }

    /** Zones for a species id with or without the {@code yearofice:} prefix. */
    public static HitZones zones(String species) {
        String id = species.startsWith("yearofice:") ? species.substring(10) : species;
        return ZONES.computeIfAbsent(id, k -> HitZones.parse(read("hitzone", k)));
    }
}

package com.yearofice.ecosystem.species;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.sim.ecosystem.Drives;
import com.yearofice.sim.ecosystem.Predation;
import com.yearofice.sim.ecosystem.PreyRules;
import com.yearofice.sim.ecosystem.Senses;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One {@code data/yearofice/species/<id>.json} (Minecraft engine › Species entry; Land animals in depth › Spawn
 * rules), the fields the slice reads. Gaits are m/s; senses metres and degrees.
 *
 * @param fleeGait  the gait a flee runs at: "run" (hare's zigzag), "trot" (the moose walks off, then trots) or
 *                  "fly" (a flush)
 * @param huntable  false for an animal the player cannot take yet; the moose also needs
 *                  {@code [ecosystem] moose_huntable}
 * @param steps     dressing steps in game minutes at practice 0 (Bestiary › Dressing by size class)
 */
public record SpeciesDef(String id, String rig, List<String> habitat, double homeRangeM, double walkMps,
                         double trotMps, double runMps, double flyMps, Senses senses, PreyRules.Behavior behavior,
                         String fleeGait, boolean zigzag, boolean huntable, double respawnDays, double wholeKg,
                         String carcassClass, Map<String, Double> steps, String butcherTable, List<Part> parts,
                         double gutsKg, Spawn spawn, String entityType, Drives.Rates driveRates, Drives.Weights driveWeights,
                         Predation.Rule predator, boolean denning, String contentFlag) {

    /**
     * The {@code "spawn"} block of a species file: the fixed-slot spawner's cell size in metres, group size and
     * density per km² of habitat at the Standard preset (scaled by {@code small_game_density_mult}).
     */
    public record Spawn(int cellM, int groupMin, int groupMax, double densityPerKm2, String densityGroup) {}

    /** The species has drive rates and a daily schedule (M6: moose, caribou, bears, predators). */
    public boolean hasDrives() { return driveRates != null && driveWeights != null; }

    /** True when the spawner places this species by data. */
    public boolean spawns() { return spawn != null; }

    /** One part from {@code item_family/<species>.json}. */
    public record Part(String item, String kind) {}

    public double dressMinutes() {
        double t = 0;
        for (double v : steps.values()) t += v;
        return t;
    }

    public String meatItem() {
        for (Part p : parts) if (p.kind().equals("meat")) return p.item();
        return null;
    }

    public SpeciesDef withParts(List<Part> parts) {
        return new SpeciesDef(id, rig, habitat, homeRangeM, walkMps, trotMps, runMps, flyMps, senses, behavior, fleeGait,
                zigzag, huntable, respawnDays, wholeKg, carcassClass, steps, butcherTable, List.copyOf(parts), gutsKg, spawn, entityType,
                driveRates, driveWeights, predator, denning, contentFlag);
    }

    public static SpeciesDef parse(JsonObject o) {
        String id = o.get("entity").getAsString();
        JsonObject g = obj(o, "gaits");
        JsonObject s = obj(o, "senses");
        JsonObject b = obj(o, "behavior");
        JsonObject c = obj(o, "carcass");
        List<String> habitat = new ArrayList<>();
        if (o.has("habitat")) for (JsonElement e : o.getAsJsonArray("habitat")) habitat.add(e.getAsString());
        double[] flee = range(b, "flee_m", 20, 40);
        double[] hide = range(b, "hide_min", 30, 60);
        PreyRules.Behavior beh = new PreyRules.Behavior(num(b, "alert", 30), num(b, "response", 70), num(b, "calm", 20),
                b.has("freezes") && b.get("freezes").getAsBoolean(), num(b, "flush_m", 0), num(b, "flee_trigger_m", 0),
                flee[0], flee[1], (int) hide[0], (int) hide[1]);
        Map<String, Double> steps = new LinkedHashMap<>();
        if (c.has("steps")) for (Map.Entry<String, JsonElement> e : c.getAsJsonObject("steps").entrySet()) steps.put(e.getKey(), e.getValue().getAsDouble());
        return new SpeciesDef(id, str(o, "rig", "lagomorph"), List.copyOf(habitat), num(o, "home_range_m", 150),
                num(g, "walk", 1), num(g, "trot", 3), num(g, "run", 10), num(g, "fly", 0),
                new Senses(num(s, "sight", 20), num(s, "hearing", 15), num(s, "smell", 20), num(s, "fov", 300)), beh,
                str(b, "flee_gait", "run"), b.has("zigzag") && b.get("zigzag").getAsBoolean(),
                !o.has("huntable") || o.get("huntable").getAsBoolean(), num(o, "respawn_days", 6),
                num(c, "whole_kg", 1.5), str(c, "class", "small"), steps, str(o, "butcher_table", ""), List.of(),
                c.has("parts") && c.getAsJsonObject("parts").has("guts_kg") ? c.getAsJsonObject("parts").get("guts_kg").getAsDouble() : 0,
                spawn(o), str(o, "entity_type", id), driveRates(o), driveWeights(o), predator(o),
                o.has("denning") && o.get("denning").getAsBoolean(), str(o, "content_flag", ""));
    }

    static Drives.Rates driveRates(JsonObject o) {
        if (!o.has("drives")) return null;
        JsonObject d = o.getAsJsonObject("drives");
        return new Drives.Rates(num(d, "hunger_up", 0.08), num(d, "hunger_down", 0.25), num(d, "thirst_up", 0.04),
                num(d, "thirst_down", 1.0), num(d, "rest_up", 0.06), num(d, "rest_down", 0.20));
    }

    static Drives.Weights driveWeights(JsonObject o) {
        if (!o.has("drives") || !o.getAsJsonObject("drives").has("weights")) return null;
        JsonObject w = o.getAsJsonObject("drives").getAsJsonObject("weights");
        return new Drives.Weights(triple(w, "dawn"), triple(w, "midday"), triple(w, "dusk"), triple(w, "night"));
    }

    static double[] triple(JsonObject w, String k) {
        JsonArray a = w.getAsJsonArray(k);
        return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    static Predation.Rule predator(JsonObject o) {
        if (!o.has("predator")) return null;
        JsonObject p = o.getAsJsonObject("predator");
        List<String> prey = new ArrayList<>();
        for (JsonElement e : p.getAsJsonArray("prey")) prey.add(e.getAsString());
        return new Predation.Rule(List.copyOf(prey), num(p, "chase_cap_m", 100), num(p, "detect_m", 60), num(p, "chase_mps", 10),
                (int) num(p, "hunt_from_h", 17), (int) num(p, "hunt_to_h", 6), num(p, "kill_radius_m", 1.5), num(p, "cooldown_min", 60));
    }

    static Spawn spawn(JsonObject o) {
        if (!o.has("spawn")) return null;
        JsonObject s = o.getAsJsonObject("spawn");
        double[] g = range(s, "group", 1, 1);
        return new Spawn((int) num(s, "cell_m", 128), (int) g[0], (int) g[1], num(s, "density_per_km2", 1), str(s, "density_group", "small_game"));
    }

    static JsonObject obj(JsonObject o, String k) { return o.has(k) ? o.getAsJsonObject(k) : new JsonObject(); }

    static double num(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }

    static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    static double[] range(JsonObject o, String k, double lo, double hi) {
        if (!o.has(k)) return new double[] {lo, hi};
        JsonArray a = o.getAsJsonArray(k);
        return new double[] {a.get(0).getAsDouble(), a.get(a.size() - 1).getAsDouble()};
    }
}

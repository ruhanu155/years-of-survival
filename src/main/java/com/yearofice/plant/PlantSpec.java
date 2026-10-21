package com.yearofice.plant;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.sim.plant.PlantPhase;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** One {@code data/yearofice/plant/<id>.json} row, read with Gson so JUnit can load it without Minecraft. */
public final class PlantSpec {
    /** A harvest verb's numbers: the part it gives, grams a unit, minutes an action, units a clump holds. */
    public record Verb(String part, double unitG, double timeMin, double unitsPerClump, Set<PlantPhase> phases,
                       double strippedWeight, boolean allowed, String reason, int emptyYears) {}

    public record Toxic(String grade, double conditionLoss, int onsetLoMin, int onsetHiMin) {}

    public final String id, name, form, lookalikeGroup, look, smell, cut;
    public final List<String> tags, notes;
    public final Map<PlantPhase, Double> phases = new EnumMap<>(PlantPhase.class);
    public final Map<String, Verb> verbs = new LinkedHashMap<>();
    public final int regrowLoDays, regrowHiDays, clumpsLo, clumpsHi;
    public final boolean pocketGuide;
    public final Toxic toxic;
    public final String foodPart;

    public PlantSpec(JsonObject o) {
        id = o.get("id").getAsString();
        name = o.get("name").getAsString();
        form = o.get("form").getAsString();
        lookalikeGroup = o.has("lookalike_group") ? o.get("lookalike_group").getAsString() : "";
        List<String> t = new ArrayList<>();
        for (JsonElement e : o.getAsJsonArray("tags")) t.add(e.getAsString());
        tags = List.copyOf(t);
        JsonObject ph = o.getAsJsonObject("phases_dd");
        for (Map.Entry<String, JsonElement> e : ph.entrySet()) {
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isNumber()) phases.put(PlantPhase.of(e.getKey()), e.getValue().getAsDouble());
        }
        for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("verbs").entrySet()) {
            JsonObject v = e.getValue().getAsJsonObject();
            Set<PlantPhase> ps = EnumSet.noneOf(PlantPhase.class);
            if (v.has("phases")) for (JsonElement p : v.getAsJsonArray("phases")) ps.add(PlantPhase.of(p.getAsString()));
            verbs.put(e.getKey(), new Verb(str(v, "part", ""), num(v, "unit_g", 0), num(v, "time_min", 0), num(v, "units_per_clump", 0), ps,
                    num(v, "stripped_weight", 1.0), !v.has("allowed") || v.get("allowed").getAsBoolean(), str(v, "reason", ""),
                    (int) num(v, "empty_years", 0)));
        }
        JsonObject patch = o.getAsJsonObject("patch");
        regrowLoDays = patch.getAsJsonArray("regrow_days").get(0).getAsInt();
        regrowHiDays = patch.getAsJsonArray("regrow_days").get(1).getAsInt();
        clumpsLo = patch.getAsJsonArray("clumps").get(0).getAsInt();
        clumpsHi = patch.getAsJsonArray("clumps").get(1).getAsInt();
        JsonObject idn = o.getAsJsonObject("identify");
        pocketGuide = idn.get("pocket_guide").getAsBoolean();
        List<String> n = new ArrayList<>();
        for (JsonElement e : (JsonArray) idn.get("notes")) n.add(e.getAsString());
        notes = List.copyOf(n);
        JsonObject in = o.getAsJsonObject("inspect");
        look = in.get("look").getAsString();
        smell = in.get("smell").getAsString();
        cut = in.get("cut").getAsString();
        if (o.has("toxic")) {
            JsonObject x = o.getAsJsonObject("toxic");
            toxic = new Toxic(x.get("grade").getAsString(), x.get("condition_loss").getAsDouble(),
                    x.getAsJsonArray("onset_min").get(0).getAsInt(), x.getAsJsonArray("onset_min").get(1).getAsInt());
        } else toxic = null;
        foodPart = o.has("food") ? o.getAsJsonObject("food").get("part").getAsString() : null;
    }

    public boolean hasTag(String tag) { return tags.contains(tag); }

    public boolean tasteable() { return !hasTag("yearofice:no_taste_test"); }

    private static String str(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }

    private static double num(JsonObject o, String k, double d) { return o.has(k) ? o.get(k).getAsDouble() : d; }
}

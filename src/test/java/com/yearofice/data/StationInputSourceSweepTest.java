package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * craft/station_input_sources (M6 lane G, RECONCILE 312 review rule): every ingredient of every recipe/station/*.json can be
 * obtained: it is another recipe's result, named by a loot table (block drop, butchering, start kit), dropped by a plant verb
 * or crop, or is on {@link #CODE} (a Java giver, whose source file must still name the item's constant). A tag ingredient needs
 * one member that qualifies.
 */
class StationInputSourceSweepTest {
    static final Path DATA = Path.of("src/main/resources/data/yearofice");
    /** Items given by Java (a work job): item path to the source file that must name it. */
    static final Map<String, String> CODE = Map.of(
        "spruce_round", "src/main/java/com/yearofice/build/wood/WoodJobs.java",
        "birch_round", "src/main/java/com/yearofice/build/wood/WoodJobs.java",
        "birch_split", "src/main/java/com/yearofice/build/wood/WoodJobs.java",
        "iron_bar", "src/main/java/com/yearofice/build/metal/ForgeJob.java");

    static String path(String id) { return id.substring(id.indexOf(':') + 1); }

    static TreeSet<String> strings(Path root) throws Exception {
        TreeSet<String> out = new TreeSet<>();
        try (Stream<Path> s = Files.walk(root)) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json"))::iterator) walk(JsonParser.parseString(Files.readString(p)), out);
        }
        return out;
    }

    static void walk(JsonElement e, TreeSet<String> out) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) out.add(e.getAsString());
        else if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> walk(x, out));
        else if (e.isJsonObject()) e.getAsJsonObject().asMap().values().forEach(x -> walk(x, out));
    }

    @Test
    void everyStationInputHasASource() throws Exception {
        TreeSet<String> sources = new TreeSet<>();
        sources.addAll(strings(DATA.resolve("loot_table")));
        sources.addAll(strings(Path.of("src/main/resources/data/minecraft/loot_table"))); // overrides of vanilla block drops
        sources.addAll(strings(DATA.resolve("plant")));
        sources.addAll(strings(DATA.resolve("crop")));
        List<JsonObject> recipes = new ArrayList<>();
        try (Stream<Path> s = Files.list(DATA.resolve("recipe/station"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".json"))::iterator)
                recipes.add(JsonParser.parseString(Files.readString(p)).getAsJsonObject());
        }
        for (JsonObject r : recipes) {
            if (r.has("result")) sources.add(r.getAsJsonObject("result").get("id").getAsString());
            if (r.has("results")) r.getAsJsonArray("results").forEach(x -> sources.add(x.getAsJsonObject().get("id").getAsString()));
        }
        List<String> gaps = new ArrayList<>();
        for (JsonObject r : recipes) {
            for (JsonElement ie : r.getAsJsonArray("ingredients")) {
                JsonObject i = ie.getAsJsonObject();
                if (i.has("item")) {
                    String id = i.get("item").getAsString();
                    if (!sourced(id, sources)) gaps.add(id);
                } else {
                    String tag = i.get("tag").getAsString();
                    JsonObject t = JsonParser.parseString(Files.readString(DATA.resolve("tags/item/" + path(tag) + ".json"))).getAsJsonObject();
                    boolean any = false;
                    for (JsonElement v : t.getAsJsonArray("values")) any |= sourced(v.getAsString(), sources);
                    if (!any) gaps.add(tag);
                }
            }
        }
        assertTrue(gaps.isEmpty(), "station recipe inputs with no source: " + new TreeSet<>(gaps));
    }

    static boolean sourced(String id, TreeSet<String> sources) throws Exception {
        if (sources.contains(id)) return true;
        String file = CODE.get(path(id));
        if (file == null) return false;
        String src = Files.readString(Path.of(file));
        return src.contains(path(id)) || src.contains(path(id).toUpperCase()) || src.contains("WoodItems." + path(id).substring(path(id).indexOf('_') + 1) + "(");
    }
}

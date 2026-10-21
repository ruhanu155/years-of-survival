package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * M9 J (RECONCILE 330, registered is not obtainable): every yearofice item, block and entity registered by the homestead,
 * radio, garden, livestock and companion packages has an in-play source: a station recipe result, a loot table other than
 * the block's own drop, a supply-drop manifest or livestock entry, a crop file, or a Java giver ({@link #CODE}) whose source
 * file must still name the constant.
 */
class FeatureSourceSweepTest {
    static final Path JAVA = Path.of("src/main/java/com/yearofice");
    static final Path DATA = Path.of("src/main/resources/data/yearofice");
    static final List<String> REGISTRIES = List.of("crop/CropRegistry.java", "story/radio/RadioRegistry.java", "ecosystem/livestock/LivestockRegistry.java",
            "ecosystem/companion/CompanionRegistry.java", "ecosystem/EcosystemRegistry.java");
    /** Registered names outside the five packages' own files that the sweep still owns (the tamed wolf). */
    static final TreeSet<String> ONLY = new TreeSet<>(List.of("wolf"));
    /** Name to the Java file that gives, places or spawns it, and the text that file must contain. */
    static final Map<String, String[]> CODE = Map.ofEntries(
        Map.entry("straw", new String[] {"crop/Grain.java", "STRAW"}),
        Map.entry("garden_bed", new String[] {"crop/TillItem.java", "GARDEN_BED"}),
        Map.entry("supply_parachute", new String[] {"story/radio/SupplyDrop.java", "SUPPLY_PARACHUTE"}),
        Map.entry("wolf", new String[] {"ecosystem/companion/DenPups.java", "makeTameable"}),
        Map.entry("sled", new String[] {"ecosystem/companion/DogSledItem.java", "SLED"}));

    static TreeSet<String> registered() throws Exception {
        Pattern p = Pattern.compile("(?:ITEMS|BLOCKS|ENTITIES)\\.register\\w*\\(\"([a-z_]+)\"");
        TreeSet<String> out = new TreeSet<>();
        for (String f : REGISTRIES) {
            Matcher m = p.matcher(Files.readString(JAVA.resolve(f)));
            while (m.find()) if (!f.endsWith("EcosystemRegistry.java") || ONLY.contains(m.group(1))) out.add(m.group(1));
        }
        return out;
    }

    /** Every string in a JSON file. */
    static void strings(JsonElement e, TreeSet<String> out) {
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) out.add(e.getAsString());
        else if (e.isJsonArray()) e.getAsJsonArray().forEach(x -> strings(x, out));
        else if (e.isJsonObject()) e.getAsJsonObject().asMap().values().forEach(x -> strings(x, out));
    }

    /** Source id to the files that name it; a loot table named like the item (the block's own drop) is not a source. */
    static Map<String, List<String>> sources() throws Exception {
        Map<String, List<String>> out = new TreeMap<>();
        for (String dir : List.of("recipe/station", "loot_table", "radio", "crop")) {
            try (Stream<Path> s = Files.walk(DATA.resolve(dir))) {
                for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json"))::iterator) {
                    TreeSet<String> ids = new TreeSet<>();
                    if (dir.equals("recipe/station")) {
                        var r = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                        if (r.has("result")) ids.add(r.getAsJsonObject("result").get("id").getAsString());
                        if (r.has("results")) r.getAsJsonArray("results").forEach(x -> ids.add(x.getAsJsonObject().get("id").getAsString()));
                    } else strings(JsonParser.parseString(Files.readString(f)), ids);
                    String self = f.getFileName().toString().replace(".json", "");
                    for (String id : ids) {
                        String name = id.substring(id.indexOf(':') + 1);
                        if (dir.equals("loot_table") && f.toString().contains("/blocks/") && name.equals(self)) continue;
                        out.computeIfAbsent(name, k -> new ArrayList<>()).add(dir + "/" + self);
                    }
                }
            }
        }
        return out;
    }

    @Test
    void everyFeatureHasASource() throws Exception {
        var src = sources();
        List<String> gaps = new ArrayList<>();
        var reg = registered();
        assertTrue(reg.size() > 40, "the sweep reads the registries: " + reg.size());
        for (String n : reg) {
            if (src.containsKey(n)) continue;
            if (n.endsWith("_crop") && Files.exists(DATA.resolve("crop/" + n.substring(0, n.length() - 5) + ".json"))) continue; // placed by sowing
            String[] c = CODE.get(n);
            if (c != null && Files.readString(JAVA.resolve(c[0])).contains(c[1])) continue;
            gaps.add(n);
        }
        assertTrue(gaps.isEmpty(), "registered but no in-play source (recipe, loot, drop, spawn, trade or taming): " + gaps);
    }

    @Test
    void livestockComesByDrop() throws Exception {
        var drop = JsonParser.parseString(Files.readString(DATA.resolve("radio/supply_drop.json"))).getAsJsonObject().getAsJsonArray("livestock");
        TreeSet<String> got = new TreeSet<>();
        drop.forEach(x -> got.add(x.getAsJsonObject().get("entity").getAsString()));
        assertTrue(got.containsAll(List.of("yearofice:goat", "yearofice:hen", "yearofice:reindeer")), "goats, hens and reindeer arrive by drop: " + got);
    }
}

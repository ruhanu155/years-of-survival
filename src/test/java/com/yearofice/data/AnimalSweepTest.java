package com.yearofice.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * M5 animal sweep (RECONCILE 303): every file in {@code data/yearofice/species} names an entity type that is registered,
 * can spawn (a habitat, a population and a respawn time), has a hunger and feed path (the drive rates, defaulted by
 * SpeciesDef or overridden with positive numbers) and a carcass class with a weight. The data-only probe, the yellowjacket
 * (class "none": nothing to dress) and the three livestock (kept, not spawned) are the declared exceptions.
 */
class AnimalSweepTest {
    private static final Pattern ENTITY_REG = Pattern.compile("ENTITIES\\.register\\(\\s*\"([a-z0-9_]+)\"");
    private static final Set<String> CLASSES = Set.of("tiny", "small", "medium", "large", "huge");

    @Test
    void everySpeciesSpawnsFeedsAndLeavesACarcass() throws Exception {
        TreeSet<String> registered = new TreeSet<>();
        try (Stream<Path> s = Files.walk(Path.of("src/main/java"))) {
            for (Path p : (Iterable<Path>) s.filter(f -> f.toString().endsWith(".java"))::iterator) {
                Matcher m = ENTITY_REG.matcher(Files.readString(p));
                while (m.find()) registered.add(m.group(1));
            }
        }
        String birds = Files.readString(Path.of("src/main/java/com/yearofice/ecosystem/bird/BirdRegistry.java")); // birds register from its IDS list
        TreeSet<String> bad = new TreeSet<>();
        int n = 0;
        try (Stream<Path> s = Files.list(Path.of("src/main/resources/data/yearofice/species"))) {
            for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json")).sorted()::iterator) {
                String name = f.getFileName().toString().replace(".json", "");
                if (name.equals("data_only_probe")) continue;
                n++;
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                String entity = o.get("entity").getAsString().replace("yearofice:", "");
                if (!registered.contains(entity) && !birds.contains("\"" + entity + "\"")) bad.add(name + ": entity " + entity + " is not registered");
                boolean kept = Set.of("goat", "hen", "reindeer").contains(name); // livestock are kept, not spawned wild
                var pop = o.getAsJsonArray("population");
                if (!kept && (pop == null || pop.size() != 2 || pop.get(1).getAsInt() < 1)) bad.add(name + ": no population");
                if (!kept && (o.getAsJsonArray("habitat") == null || o.getAsJsonArray("habitat").isEmpty())) bad.add(name + ": no habitat");
                if (!kept && (!o.has("respawn_days") || o.get("respawn_days").getAsDouble() <= 0)) bad.add(name + ": no respawn_days");
                if (o.has("drives")) {
                    JsonObject d = o.getAsJsonObject("drives");
                    for (String k : new String[] {"hunger_up", "hunger_down"})
                        if (d.has(k) && d.get(k).getAsDouble() <= 0) bad.add(name + ": drives." + k + " must be positive");
                }
                JsonObject c = o.getAsJsonObject("carcass");
                String cls = c == null ? "" : c.get("class").getAsString();
                boolean none = cls.equals("none") && name.equals("yellowjacket");
                if (!none && !CLASSES.contains(cls)) bad.add(name + ": carcass class '" + cls + "'");
                if (!none && c != null && c.get("whole_kg").getAsDouble() <= 0) bad.add(name + ": carcass whole_kg");
            }
        }
        assertTrue(n >= 40, "the sweep found only " + n + " species files");
        assertTrue(bad.isEmpty(), "species problems: " + bad);
    }

    /** Ready for development M5: 27 fungi and aquatic plant blocks, and nine music entries in sounds.json. */
    @Test
    void fungiAquaticPlantsAndMusicMeetTheDocCounts() throws Exception {
        int rows = 0;
        try (Stream<Path> s = Files.list(Path.of("src/main/resources/data/yearofice/plant"))) {
            for (Path f : (Iterable<Path>) s.filter(x -> x.toString().endsWith(".json"))::iterator) {
                String form = JsonParser.parseString(Files.readString(f)).getAsJsonObject().has("form")
                        ? JsonParser.parseString(Files.readString(f)).getAsJsonObject().get("form").getAsString() : "";
                if (form.equals("mushroom") || form.equals("aquatic")) rows++;
            }
        }
        assertTrue(rows >= 27, "fungus and aquatic plant rows: " + rows + " (want 27)");
        JsonObject sounds = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/yearofice/sounds.json"))).getAsJsonObject();
        long music = sounds.keySet().stream().filter(k -> k.startsWith("music.")).count();
        assertTrue(music >= 9, "music entries in sounds.json: " + music + " (want 9)");
    }
}

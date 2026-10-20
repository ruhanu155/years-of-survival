package com.yearofice.sim.body;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The loaded illness register, by short id. The jar's twenty files load on first use from the classpath, so the
 * pure model and JUnit read the same data the game ships; a datapack reload replaces the map through
 * {@link #replace}.
 */
public final class IllnessDefs {
    private IllnessDefs() {}

    /** The register's twenty files (Body tab › The register). */
    public static final List<String> IDS = List.of("gut_illness", "tapeworm", "trichinella", "botulism", "scurvy",
            "infection", "blood_poisoning", "fever", "nausea", "lean_meat", "hypothermia", "frostbite",
            "snow_blindness", "cold_cough", "dehydration", "exhaustion", "coal_air", "poison_plant", "taste_sick",
            "overuse_remedy");

    private static volatile Map<String, IllnessDef> defs;

    public static Map<String, IllnessDef> all() {
        Map<String, IllnessDef> d = defs;
        if (d == null) {
            d = loadClasspath();
            defs = d;
        }
        return d;
    }

    public static IllnessDef get(String shortId) { return all().get(shortId); }

    public static void replace(Map<String, IllnessDef> next) { defs = Map.copyOf(next); }

    static Map<String, IllnessDef> loadClasspath() {
        Map<String, IllnessDef> m = new LinkedHashMap<>();
        for (String id : IDS) {
            try (InputStream in = IllnessDefs.class.getResourceAsStream("/data/yearofice/illness/" + id + ".json")) {
                if (in == null) continue;
                IllnessDef def = IllnessDef.parse(JsonParser.parseReader(
                        new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
                m.put(def.shortId(), def);
            } catch (IOException e) {
                throw new IllegalStateException("cannot read illness/" + id + ".json", e);
            }
        }
        return Map.copyOf(m);
    }
}

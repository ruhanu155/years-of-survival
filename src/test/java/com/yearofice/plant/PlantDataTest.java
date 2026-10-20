package com.yearofice.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** The generated catalogue equals the data folder, and every plant file reads and keeps its promises. */
class PlantDataTest {
    static final Path DIR = Path.of("src/main/resources/data/yearofice/plant");

    static Set<String> files() throws IOException {
        try (Stream<Path> s = Files.list(DIR)) {
            Set<String> out = new TreeSet<>();
            s.forEach(p -> out.add(p.getFileName().toString().replace(".json", "")));
            return out;
        }
    }

    @Test
    void catalogueEqualsDataFolder() throws IOException {
        assertEquals(files(), new TreeSet<>(Arrays.asList(PlantCatalog.SPECIES)));
        assertTrue(PlantCatalog.SPECIES.length >= 72, "the lane's target is about 72 plant blocks");
    }

    @Test
    void everyFileReadsAndItsPartsAreRegistered() throws IOException {
        Set<String> parts = new HashSet<>(Arrays.asList(PlantCatalog.PARTS));
        int toxic = 0;
        for (String id : PlantCatalog.SPECIES) {
            PlantSpec s = new PlantSpec(JsonParser.parseString(Files.readString(DIR.resolve(id + ".json"))).getAsJsonObject());
            assertEquals("yearofice:" + id, s.id);
            assertFalse(s.verbs.isEmpty(), id + " has no verbs");
            assertFalse(s.phases.isEmpty(), id + " has no phases");
            for (PlantSpec.Verb v : s.verbs.values()) {
                if (v.part().isEmpty() || v.part().equals("yearofice:plant_material")) continue;
                assertTrue(parts.contains(v.part().substring("yearofice:".length())), id + ": unregistered part " + v.part());
            }
            if (s.toxic != null) toxic++;
            if (s.hasTag("yearofice:berry_bushes")) assertFalse(s.verbs.containsKey("dig"), id + ": a berry bush has no root to dig");
            if (s.form.equals("mushroom")) assertFalse(s.tasteable(), id + ": no fungus can be taste-tested");
        }
        assertTrue(toxic >= 15, "the blueprint's 15 toxic plants and fungi, found " + toxic);
        assertNotNull(PlantCatalog.FOOD_PARTS);
    }
}

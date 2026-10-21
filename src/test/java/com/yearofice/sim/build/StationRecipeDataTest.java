package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** Every station recipe is well formed: known tier and effort, and kcal = effort x work_min / 60 (within 1). */
class StationRecipeDataTest {
    @Test
    void recipesAreConsistent() throws IOException {
        Path dir = Path.of("src/main/resources/data/yearofice/recipe/station");
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) { files = s.filter(f -> f.toString().endsWith(".json")).toList(); }
        int seen = 0;
        for (Path f : files) {
            JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
            // crop's yearofice:station recipes share this folder until M6-INT folds the two types
            if (!"yearofice:craft_station".equals(o.get("type").getAsString())) continue;
            seen++;
            String tier = o.get("tier").getAsString();
            assertTrue(Tier.byId(tier).isPresent(), f + " tier " + tier);
            Effort e = Effort.valueOf(o.get("effort").getAsString().toUpperCase());
            double expect = e.kcalPerHour * o.get("work_min").getAsDouble() / 60.0;
            assertEquals(expect, o.get("kcal").getAsDouble(), 1.0, f + " kcal");
            assertTrue(o.getAsJsonObject("result").get("id").getAsString().startsWith("yearofice:"), f + " result id");
        }
        assertTrue(seen >= 4, "the first bushcraft set");
    }
}

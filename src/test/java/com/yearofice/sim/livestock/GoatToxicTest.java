package com.yearofice.sim.livestock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** {@code yearofice:forage/goat_toxic}: goats suffer the eight listed plants and refuse every other toxic one. */
class GoatToxicTest {
    static Set<String> tag(String path) throws IOException {
        try (Reader r = new InputStreamReader(GoatToxicTest.class.getResourceAsStream(path))) {
            Set<String> out = new HashSet<>();
            for (JsonElement e : JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("values")) out.add(e.getAsString());
            return out;
        }
    }

    @Test
    void the_tag_holds_exactly_the_eight_plants_and_each_has_an_effect() throws IOException {
        Set<String> goat = tag("/data/yearofice/tags/block/goat_toxic.json");
        assertEquals(8, goat.size());
        assertEquals(goat, Browse.TOXIC.keySet());
        for (String id : goat) assertTrue(Browse.eats(id) && Browse.effect(id) != null, id);
    }

    @Test
    void the_effects_follow_the_table() {
        for (String p : new String[] {"monkshood", "tall_larkspur", "water_hemlock"}) assertEquals(Browse.Effect.DIES, Browse.effect("yearofice:" + p));
        for (String p : new String[] {"false_hellebore", "wild_calla", "bog_rosemary"}) assertEquals(Browse.Effect.SICK, Browse.effect("yearofice:" + p));
        for (String p : new String[] {"leatherleaf", "arctic_lupine"}) assertEquals(Browse.Effect.DROOL, Browse.effect("yearofice:" + p));
        assertEquals(6, Browse.Effect.DIES.dieAfterHours);
        assertEquals(3, Browse.Effect.SICK.milkOffDays);
        assertEquals(0.30, Browse.Effect.SICK.conditionLoss, 1e-9);
        assertEquals(0.5, Browse.Effect.DROOL.milkFactor(), 1e-9);
        assertEquals(2, Browse.Effect.DROOL.milkOffDays);
    }

    @Test
    void other_toxic_plants_are_refused() throws IOException {
        Set<String> goat = tag("/data/yearofice/tags/block/goat_toxic.json");
        Set<String> toxic = tag("/data/yearofice/tags/block/toxic_plants.json");
        int refused = 0;
        for (String id : toxic) if (!goat.contains(id)) { assertFalse(Browse.eats(id), id); assertNull(Browse.effect(id)); refused++; }
        assertTrue(refused >= 5, "baneberry, death camas and the mushrooms are not on the goat list");
    }

    @Test
    void a_penned_goat_eats_a_clump_on_ten_percent_of_days_and_always_toxic_hay() {
        SplittableRandom r = new SplittableRandom(11);
        int ate = 0, n = 20000;
        for (int i = 0; i < n; i++) if (Browse.eatsToday(true, false, r)) ate++;
        assertEquals(0.10, ate / (double) n, 0.01);
        for (int i = 0; i < 100; i++) {
            assertFalse(Browse.eatsToday(false, false, r), "no clump, no poisoning");
            assertTrue(Browse.eatsToday(false, true, r), "toxic hay always");
        }
        int bundles = 0;
        for (int i = 0; i < n; i++) if (Browse.hayBundleToxic(false, r)) bundles++;
        assertEquals(0.05, bundles / (double) n, 0.01);
        for (int i = 0; i < 1000; i++) assertFalse(Browse.hayBundleToxic(true, r));
    }
}

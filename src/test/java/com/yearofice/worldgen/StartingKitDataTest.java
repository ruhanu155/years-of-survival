package com.yearofice.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * {@code loot_table/player/starting_kit.json} (Blueprint › Start site › Starting kit; Items › master table, "Crate"
 * rows): every kit item that exists as an item today, with its count. The crate rolls this table and the Endless SOS
 * return reads it; the trail messenger is not in it because {@code MessengerKit} hands it over on the first join.
 */
class StartingKitDataTest {
    @Test
    void theTableHoldsTheCrateItemsThatExist() throws IOException {
        String json;
        try (InputStream in = StartingKitDataTest.class.getClassLoader().getResourceAsStream("data/yearofice/loot_table/player/starting_kit.json")) {
            assertNotNull(in);
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        GenJson j = GenJson.parse(json, "starting_kit");
        assertEquals("minecraft:chest", j.str("type"));
        Map<String, Integer> kit = new LinkedHashMap<>();
        for (GenJson pool : j.objects("pools")) {
            assertEquals(1, (int) pool.num("rolls"), "each kit row is rolled exactly once");
            for (GenJson e : pool.objects("entries")) {
                int count = 1;
                if (e.has("functions")) for (GenJson f : e.objects("functions")) count = (int) f.num("count");
                kit.put(e.str("name"), count);
            }
        }
        Map<String, Integer> want = new java.util.HashMap<>();
        String[] ids = {"crate_backpack", "hatchet", "match", "fixed_blade_knife", "steel_bottle", "cooking_pot", "pilot_bread", "medkit",
                "bear_spray", "cord", "seed_packet", "compass", "headlamp", "plant_guide", "solar_panel_small"};
        int[] counts = {1, 1, 3, 1, 1, 1, 38, 1, 1, 10, 1, 1, 1, 1, 1};
        for (int i = 0; i < ids.length; i++) want.put("yearofice:" + ids[i], counts[i]);
        assertEquals(want, kit);
    }
}

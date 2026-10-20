package com.yearofice.gametest;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.core.YearOfIce;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Data-only checks that need the live registries. */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DataGameTests {
    private DataGameTests() {}

    /**
     * Every key of every {@code data_maps/item} file (ours and vanilla-keyed alike) names a registered item; an
     * unknown key makes NeoForge log "specified in data map ... doesn't exist" on every boot. Entries guarded by
     * {@code neoforge:conditions} are skipped; an item a lane has not merged yet stays out of the maps until it registers.
     */
    @GameTest(template = "gametest/empty3", batch = "m0")
    public static void data_datamap_keys_registered(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        Map<ResourceLocation, Resource> found = helper.getLevel().getServer().getResourceManager()
                .listResources("data_maps/item", rl -> rl.getPath().endsWith(".json"));
        helper.assertTrue(!found.isEmpty(), "no item data maps found");
        for (Map.Entry<ResourceLocation, Resource> e : found.entrySet()) {
            try (Reader r = e.getValue().openAsReader()) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                if (!o.has("values")) continue;
                for (Map.Entry<String, JsonElement> v : o.getAsJsonObject("values").entrySet()) {
                    String key = v.getKey();
                    if (key.startsWith("#") || (v.getValue().isJsonObject() && v.getValue().getAsJsonObject().has("neoforge:conditions"))) continue;
                    ResourceLocation id = ResourceLocation.parse(key);
                    if (!BuiltInRegistries.ITEM.containsKey(id)) missing.add(e.getKey().getPath() + ": " + key);
                }
            } catch (Exception ex) {
                helper.fail("cannot read " + e.getKey() + ": " + ex);
            }
        }
        helper.assertTrue(missing.isEmpty(), "data map keys that are not registered items: " + missing);
        helper.succeed();
    }
}

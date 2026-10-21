package com.yearofice.build.outpost;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An outpost layout ({@code data/yearofice/outpost/<id>.json}): boxes of blocks with a role, relative to the
 * south-west floor corner. Read from the jar (ponytail: a fixed id list, no reload listener, since the layouts are
 * not datapack-tunable yet; upgrade: SimpleJsonResourceReloadListener).
 */
public record OutpostDef(String id, String name, int spacingM, String fit, int[] size, int minAge, int maxAge, List<Fill> fills) {
    public record Fill(int[] from, int[] to, BlockState state, String role) {}

    public static final List<String> IDS = List.of("trapline_cabin", "fish_camp", "hunting_blind_camp", "rock_shelter");
    private static final Map<String, OutpostDef> CACHE = new ConcurrentHashMap<>();

    public static OutpostDef get(String path) {
        return CACHE.computeIfAbsent(path.contains(":") ? path.substring(path.indexOf(':') + 1) : path, OutpostDef::read);
    }

    private static OutpostDef read(String path) {
        try (var in = OutpostDef.class.getResourceAsStream("/data/yearofice/outpost/" + path + ".json")) {
            if (in == null) throw new IllegalArgumentException("no outpost " + path);
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Fill> fills = new ArrayList<>();
            for (JsonElement e : o.getAsJsonArray("fills")) {
                JsonObject f = e.getAsJsonObject();
                BlockState st = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(f.get("block").getAsString())).defaultBlockState();
                fills.add(new Fill(ints(f.getAsJsonArray("from")), ints(f.getAsJsonArray("to")), st, f.get("role").getAsString()));
            }
            return new OutpostDef(o.get("id").getAsString(), o.get("name").getAsString(), o.get("spacing_m").getAsInt(), o.get("fit").getAsString(),
                    ints(o.getAsJsonArray("size_m")), o.get("min_age_years").getAsInt(), o.get("max_age_years").getAsInt(), fills);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int[] ints(JsonArray a) { return new int[] {a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()}; }
}

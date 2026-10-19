package com.yearofice.ecosystem.species;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * The reload listeners for {@code data/yearofice/species/} and {@code data/yearofice/item_family/} (Minecraft
 * engine › Saves and data: reload listeners, not datapack registries). {@link #get} is server-side truth; a
 * missing file is logged once and falls back to the built-in snowshoe-hare numbers so nothing crashes mid-game.
 */
public final class SpeciesBook {
    private SpeciesBook() {}

    private static final Map<String, SpeciesDef> SPECIES = new ConcurrentHashMap<>();
    private static final Map<String, List<SpeciesDef.Part>> FAMILIES = new ConcurrentHashMap<>();
    private static final java.util.Set<String> WARNED = ConcurrentHashMap.newKeySet();

    public static SpeciesDef get(String id) {
        SpeciesDef d = SPECIES.get(id);
        if (d != null) return d.parts().isEmpty() && FAMILIES.containsKey(id) ? d.withParts(FAMILIES.get(id)) : d;
        if (WARNED.add(id)) YearOfIce.LOG.warn("SpeciesBook: no species file for {}; using defaults", id);
        JsonObject o = new JsonObject();
        o.addProperty("entity", id);
        return SpeciesDef.parse(o);
    }

    public static boolean has(String id) { return SPECIES.containsKey(id); }

    public static void onAddReloadListeners(AddReloadListenerEvent e) {
        e.addListener(new Families());
        e.addListener(new Species());
    }

    static final class Species extends SimpleJsonResourceReloadListener {
        Species() { super(new com.google.gson.Gson(), "species"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            Map<String, SpeciesDef> next = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
                try {
                    SpeciesDef d = SpeciesDef.parse(e.getValue().getAsJsonObject());
                    next.put(d.id(), d);
                } catch (RuntimeException ex) {
                    YearOfIce.LOG.error("species/{}: {}", e.getKey().getPath(), ex.toString());
                }
            }
            SPECIES.clear();
            SPECIES.putAll(next);
        }
    }

    static final class Families extends SimpleJsonResourceReloadListener {
        Families() { super(new com.google.gson.Gson(), "item_family"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            Map<String, List<SpeciesDef.Part>> next = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> e : files.entrySet()) {
                if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                List<SpeciesDef.Part> parts = new ArrayList<>();
                for (JsonElement p : o.getAsJsonArray("parts")) {
                    JsonObject po = p.getAsJsonObject();
                    parts.add(new SpeciesDef.Part(po.get("item").getAsString(), po.get("kind").getAsString()));
                }
                next.put(o.get("species").getAsString(), parts);
            }
            FAMILIES.clear();
            FAMILIES.putAll(next);
        }
    }
}

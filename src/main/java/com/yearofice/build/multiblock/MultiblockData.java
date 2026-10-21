package com.yearofice.build.multiblock;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * {@code data/yearofice/multiblock/<id>.json}: the numbers of every multiblock (stoves, cellars, smokehouse, dugouts).
 * Code reads them by key and never carries a fallback, so a missing number fails loudly. Reloads with datapacks.
 */
public final class MultiblockData extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, JsonObject> rows = Map.of();

    private MultiblockData() { super(new Gson(), "multiblock"); }

    public static void init() {
        NeoForge.EVENT_BUS.addListener((AddReloadListenerEvent e) -> e.addListener(new MultiblockData()));
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller p) {
        Map<String, JsonObject> out = new HashMap<>();
        in.forEach((k, v) -> { if (v.isJsonObject()) out.put(k.getPath(), v.getAsJsonObject()); });
        rows = out;
    }

    public static double num(String id, String key) {
        JsonObject o = rows.get(id);
        if (o == null || !o.has(key)) throw new IllegalStateException("multiblock/" + id + ".json has no " + key);
        return o.get(key).getAsDouble();
    }

    public static boolean loaded(String id) { return rows.containsKey(id); }
}

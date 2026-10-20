package com.yearofice.crop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.yearofice.core.YearOfIce;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

/** Loads {@code data/yearofice/crop/<id>.json}. */
public final class CropData extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, CropSpec> specs = Map.of();

    private CropData() { super(new Gson(), "crop"); }

    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new CropData()); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        Map<String, CropSpec> next = new HashMap<>();
        for (var e : in.entrySet()) {
            if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
            try {
                CropSpec s = new CropSpec(e.getValue().getAsJsonObject());
                next.put(s.id, s);
            } catch (RuntimeException ex) {
                YearOfIce.LOG.warn("Bad crop {}: {}", e.getKey(), ex.toString());
            }
        }
        specs = next;
    }

    public static @Nullable CropSpec get(String id) { return specs.get(id); }

    public static @Nullable CropSpec byBlock(String blockId) {
        for (CropSpec s : specs.values()) if (s.block.equals(blockId)) return s;
        return null;
    }

    public static @Nullable CropSpec bySeed(String itemId) {
        for (CropSpec s : specs.values()) if (s.seedItem.equals(itemId)) return s;
        return null;
    }
}

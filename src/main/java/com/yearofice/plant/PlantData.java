package com.yearofice.plant;

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

/** Loads {@code data/yearofice/plant/<id>.json} into {@link PlantSpec}s, keyed by the species id ({@code yearofice:soapberry_bush}). */
public final class PlantData extends SimpleJsonResourceReloadListener {
    private static volatile Map<String, PlantSpec> specs = Map.of();
    private static volatile Map<String, PlantSpec> byPart = Map.of();

    private PlantData() { super(new Gson(), "plant"); }

    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new PlantData()); }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> in, ResourceManager rm, ProfilerFiller pf) {
        Map<String, PlantSpec> next = new HashMap<>();
        Map<String, PlantSpec> parts = new HashMap<>();
        for (var e : in.entrySet()) {
            if (!e.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
            try {
                PlantSpec s = new PlantSpec(e.getValue().getAsJsonObject());
                next.put(s.id, s);
                if (s.foodPart != null) parts.put(s.foodPart, s);
            } catch (RuntimeException ex) {
                YearOfIce.LOG.warn("Bad plant {}: {}", e.getKey(), ex.toString());
            }
        }
        specs = next;
        byPart = parts;
    }

    public static @Nullable PlantSpec get(String id) { return specs.get(id); }

    public static @Nullable PlantSpec ofPart(String itemId) { return byPart.get(itemId); }

    public static int count() { return specs.size(); }
}

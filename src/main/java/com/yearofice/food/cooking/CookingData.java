package com.yearofice.food.cooking;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.food.EatingLadder;
import com.yearofice.sim.food.Roasting;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * {@code data/yearofice/cooking/foods.json}: per food, which spit rule it roasts by ({@code whole_hare},
 * {@code per_kg}, {@code whole_fish}) and which eating-ladder family it is ({@code game_meat}, {@code fish}). The
 * minutes are tuning ({@code [cooking]}); this file only says which rule an item uses (Items › Food values, the
 * "Made or cooked at" column). It stands in for the cooking-method recipes under {@code recipe/} that M5 brings.
 * Server data: the client never reads it.
 */
public final class CookingData {
    private CookingData() {}

    public record Food(Roasting.Rule spit, EatingLadder.Family family) {}

    private static volatile Map<Item, Food> foods = Map.of();

    public static @Nullable Food of(Item item) { return foods.get(item); }

    public static void onAddReloadListeners(AddReloadListenerEvent e) { e.addListener(new Loader()); }

    static final class Loader extends SimpleJsonResourceReloadListener {
        Loader() { super(new com.google.gson.Gson(), "cooking"); }

        @Override
        protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager rm, ProfilerFiller prof) {
            Map<Item, Food> next = new HashMap<>();
            for (Map.Entry<ResourceLocation, JsonElement> f : files.entrySet()) {
                if (!f.getKey().getNamespace().equals(YearOfIce.MOD_ID)) continue;
                try {
                    JsonObject foodsObj = f.getValue().getAsJsonObject().getAsJsonObject("foods");
                    for (Map.Entry<String, JsonElement> e : foodsObj.entrySet()) {
                        ResourceLocation id = ResourceLocation.parse(e.getKey());
                        if (!BuiltInRegistries.ITEM.containsKey(id)) continue;
                        JsonObject o = e.getValue().getAsJsonObject();
                        next.put(BuiltInRegistries.ITEM.get(id), new Food(
                                Roasting.Rule.valueOf(o.get("spit").getAsString().toUpperCase(Locale.ROOT)),
                                EatingLadder.Family.valueOf(o.get("family").getAsString().toUpperCase(Locale.ROOT))));
                    }
                } catch (RuntimeException ex) {
                    YearOfIce.LOG.error("cooking/{}: {}", f.getKey().getPath(), ex.toString());
                }
            }
            foods = Map.copyOf(next);
        }
    }
}

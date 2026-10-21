package com.yearofice.food.sourdough;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.food.DishMaker;
import com.yearofice.food.FoodItems;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.food.Dishes;
import com.yearofice.sim.food.Starter;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Baking with a starter: pancakes, bread and biscuits, each a {@code dish} with an explicit {@code weight_g} (a dish
 * with none would weigh the 100 g default, the bread weight rule). An Active starter gives the full rise, a Hungry one
 * half the weight, a Sleeping or Dead one nothing: that is a refusal ({@code null}).
 */
public final class Sourdough {
    private Sourdough() {}

    public record Recipe(String id, double flourG, double waterG, int weightG, String method) {}

    private static Map<String, Recipe> recipes;

    public static synchronized Map<String, Recipe> recipes() {
        if (recipes == null) {
            Map<String, Recipe> m = new LinkedHashMap<>();
            try (var in = Sourdough.class.getResourceAsStream("/data/yearofice/cooking/sourdough.json")) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("dishes");
                for (var e : o.entrySet()) {
                    JsonObject r = e.getValue().getAsJsonObject();
                    m.put(e.getKey(), new Recipe(e.getKey(), r.get("flour_g").getAsDouble(), r.get("water_g").getAsDouble(),
                            r.get("weight_g").getAsInt(), r.get("method").getAsString()));
                }
            } catch (java.io.IOException | RuntimeException ex) {
                throw new IllegalStateException("sourdough.json", ex);
            }
            recipes = m;
        }
        return recipes;
    }

    /** The dish, or null when the starter cannot raise it. */
    public static @Nullable ItemStack bake(ServerLevel level, StarterComp starter, ItemStack flour, String dishId) {
        Recipe r = recipes().get(dishId);
        double rise = Starter.riseFactor(starter.data().state());
        if (r == null || rise <= 0) return null;
        Dishes.Method method = Dishes.Method.valueOf(r.method().toUpperCase(java.util.Locale.ROOT));
        ItemStack out = DishMaker.make(level, List.of(flour.copyWithCount(Math.max(1, flour.getCount()))), method, "sourdough_" + dishId, r.waterG());
        out.set(YoiComponents.WEIGHT_G.get(), (int) Math.round(r.weightG() * (rise < 1 ? 0.5 + 0.5 * rise : 1)));
        return out;
    }
}

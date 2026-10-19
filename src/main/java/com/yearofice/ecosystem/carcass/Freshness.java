package com.yearofice.ecosystem.carcass;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.yearofice.api.decay.DecayService;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.food.FoodValues;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.RegistryManager;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * {@code yearofice:fresh} for carcasses and their meat, on the slice's one decay clock (Minecraft development ›
 * Layer 5), read through {@link DecayService}: one clock, handle 0, {@code air:slice}, whose decay-hours are game
 * hours times f_T at {@code [decay] stand_in_air_c} (5 °C, f_T = 1) until EnvService lands. Food values come from the
 * {@code yearofice:food} data map ({@code data_maps/item/food.json}), read by its id so the ecosystem needs no
 * other package's classes.
 *
 * <p>Whole carcasses spoil as an unopened body: k = 0.6 and size factor s = 1.0 for small game (Bestiary › From
 * warm to bones). Dressed meat keeps the carcass's progress on the plain meat clock (the {@code copy_freshness}
 * rule).
 */
public final class Freshness {
    private Freshness() {}

    public static final int CLOCK_AIR_SLICE = 0;
    public static final double K_UNOPENED = 0.6;
    public static final double SIZE_SMALL = 1.0;
    /** Lean meat, Items › Food values: when a meat item has no food row yet, its raw life is 6 days at 5 °C. */
    static final FoodValues LEAN_MEAT = new FoodValues("lean_meat", 115, 27, 0.8, 0, FoodValues.LIVING_ROT,
            Map.of(FoodState.RAW, 6.0, FoodState.COOKED, 9.0));

    /** The component's codec, owned by {@code item.component.YoiComponents} (one definition, RECONCILE 169). */
    public static final Codec<Fresh> CODEC = YoiComponents.FRESH_CODEC;

    /** Decay-hours on the slice clock at a game minute. */
    public static double dAt(double minute) { return DecayService.get().dAt(minute); }

    public static double dNow(Level level) { return DecayService.get().dNow(level); }

    /** A whole small carcass, stamped at its death minute. */
    public static Fresh wholeCarcass(Item meat, double deathMinute) {
        double life = values(meat).refLifeDh(FoodState.RAW) * K_UNOPENED * SIZE_SMALL;
        return Fresh.stamp(FoodState.RAW, CLOCK_AIR_SLICE, dAt(deathMinute), life);
    }

    /** Dressed meat: the carcass's progress carried onto the meat's own raw life. */
    public static Fresh dressed(Fresh carcass, Item meat, double dNow) {
        double p = carcass.progress(dNow);
        double life = values(meat).refLifeDh(FoodState.RAW);
        double due = dNow + (1 - p) * life;
        return new Fresh(FoodState.RAW, CLOCK_AIR_SLICE, Fresh.quantise(due, life, Fresh.DEFAULT_BANDS), life, 0, true);
    }

    /** The meat's food row, or lean meat when the data map has none. */
    public static FoodValues values(Item item) {
        ResourceLocation id = item.builtInRegistryHolder().key().location();
        try {
            @SuppressWarnings("unchecked")
            DataMapType<Item, Object> type = (DataMapType<Item, Object>) RegistryManager.getDataMap(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "food"));
            if (type == null) return LEAN_MEAT;
            Object row = item.builtInRegistryHolder().getData(type);
            if (row == null) return LEAN_MEAT;
            JsonElement json = type.codec().encodeStart(JsonOps.INSTANCE, row).getOrThrow();
            JsonObject o = json.getAsJsonObject();
            EnumMap<FoodState, Double> life = new EnumMap<>(FoodState.class);
            if (o.has("spoil_days_at_5c")) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("spoil_days_at_5c").entrySet()) {
                    life.put(FoodState.valueOf(e.getKey().toUpperCase(Locale.ROOT)), e.getValue().getAsDouble());
                }
            }
            if (!life.containsKey(FoodState.RAW)) return LEAN_MEAT;
            return new FoodValues(id.toString(), num(o, "kcal_per_100g"), num(o, "protein_g_per_100g"), num(o, "fat_g_per_100g"),
                    num(o, "vit_c_mg_per_100g"), o.has("curve") ? o.get("curve").getAsString() : FoodValues.LIVING_ROT, life);
        } catch (RuntimeException e) {
            return LEAN_MEAT;
        }
    }

    private static double num(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsDouble() : 0; }
}

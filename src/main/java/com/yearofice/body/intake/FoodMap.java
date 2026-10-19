package com.yearofice.body.intake;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.food.FoodValues;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code yearofice:food} item data map, {@code data_maps/item/food.json} (Items › Food values; Minecraft
 * development › Layer 6). One row per food, per 100 g, read into {@link FoodValues}. A row may carry
 * {@code weight_g}: the grams one item of that stack is (single pieces and the vanilla foods); without it one item
 * is a 100 g unit (RECONCILE 43).
 *
 * <p>Registered here for the body slice, because eating is the first reader; the Food and hunting department's
 * {@code food} package takes the registration over with the same id when {@code food.Eating} lands. Rows whose
 * item is not registered yet carry a {@code neoforge:item_exists} condition, so they load silently.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class FoodMap {
    private FoodMap() {}

    public record Row(double kcalPer100g, double proteinGPer100g, double fatGPer100g, double vitCMgPer100g,
                      String curve, Map<String, Double> spoilDaysAt5c, double weightG, double waterMlPer100g) {

        public FoodValues values(String id) {
            EnumMap<FoodState, Double> life = new EnumMap<>(FoodState.class);
            spoilDaysAt5c.forEach((k, v) -> life.put(FoodState.valueOf(k.toUpperCase(Locale.ROOT)), v));
            return new FoodValues(id, kcalPer100g, proteinGPer100g, fatGPer100g, vitCMgPer100g, curve, life);
        }
    }

    public static final Codec<Row> ROW = RecordCodecBuilder.create(i -> i.group(
            Codec.DOUBLE.fieldOf("kcal_per_100g").forGetter(Row::kcalPer100g),
            Codec.DOUBLE.fieldOf("protein_g_per_100g").forGetter(Row::proteinGPer100g),
            Codec.DOUBLE.fieldOf("fat_g_per_100g").forGetter(Row::fatGPer100g),
            Codec.DOUBLE.optionalFieldOf("vit_c_mg_per_100g", 0.0).forGetter(Row::vitCMgPer100g),
            Codec.STRING.optionalFieldOf("curve", FoodValues.LIVING_ROT).forGetter(Row::curve),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("spoil_days_at_5c", Map.of())
                    .forGetter(Row::spoilDaysAt5c),
            Codec.DOUBLE.optionalFieldOf("weight_g", 100.0).forGetter(Row::weightG),
            Codec.DOUBLE.optionalFieldOf("water_ml_per_100g", 0.0).forGetter(Row::waterMlPer100g)
    ).apply(i, Row::new));

    public static final DataMapType<Item, Row> FOOD = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "food"), Registries.ITEM, ROW)
            .synced(ROW, false)
            .build();

    @SubscribeEvent
    public static void register(RegisterDataMapTypesEvent e) {
        e.register(FOOD);
    }

    public static @Nullable Row row(ItemStack stack) {
        Holder<Item> h = stack.getItemHolder();
        return h.getData(FOOD);
    }

    public static @Nullable FoodValues values(ItemStack stack) {
        Row r = row(stack);
        return r == null ? null : r.values(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }
}

package com.yearofice.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The Item model's static numbers (Layer 6, "Static numbers are data maps"), synced to clients by NeoForge so the
 * load meter and the pack rows read the same numbers on both sides:
 *
 * <ul>
 *   <li>{@code yearofice:weight} ({@code data_maps/item/weight.json}): {@code weight_g} of one item.</li>
 *   <li>{@code yearofice:props} ({@code data_maps/item/props.json}): category, stack class and the other fields of
 *       the Layer 6 table; every field but {@code category} and {@code stack_class} is optional.</li>
 *   <li>{@code yearofice:pack} ({@code data_maps/item/pack.json}): a pack's {@code rows}, {@code max_kg},
 *       {@code lash_points} and {@code sheds_rain} (Player systems and client › Inventory and slots).</li>
 * </ul>
 */
public final class YoiDataMaps {
    private YoiDataMaps() {}

    public record Weight(int weightG) {
        public static final Codec<Weight> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 10_000_000).fieldOf("weight_g").forGetter(Weight::weightG)).apply(i, Weight::new));
    }

    public record Props(String category, String stackClass, boolean floats, boolean shiny, int scentLevel,
                        double frostKg, String material, int maxCondition, String salvage, String theft) {
        public static final Codec<Props> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("category").forGetter(Props::category),
                Codec.STRING.fieldOf("stack_class").forGetter(Props::stackClass),
                Codec.BOOL.optionalFieldOf("floats", false).forGetter(Props::floats),
                Codec.BOOL.optionalFieldOf("shiny", false).forGetter(Props::shiny),
                Codec.INT.optionalFieldOf("scent_level", 0).forGetter(Props::scentLevel),
                Codec.DOUBLE.optionalFieldOf("frost_kg", 0.0).forGetter(Props::frostKg),
                Codec.STRING.optionalFieldOf("material", "").forGetter(Props::material),
                Codec.INT.optionalFieldOf("max_condition", 0).forGetter(Props::maxCondition),
                Codec.STRING.optionalFieldOf("salvage", "").forGetter(Props::salvage),
                Codec.STRING.optionalFieldOf("theft", "").forGetter(Props::theft)
        ).apply(i, Props::new));
    }

    public record Pack(int rows, double maxKg, int lashPoints, boolean shedsRain) {
        public static final Codec<Pack> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(0, 3).fieldOf("rows").forGetter(Pack::rows),
                Codec.DOUBLE.fieldOf("max_kg").forGetter(Pack::maxKg),
                Codec.INT.optionalFieldOf("lash_points", 0).forGetter(Pack::lashPoints),
                Codec.BOOL.optionalFieldOf("sheds_rain", false).forGetter(Pack::shedsRain)
        ).apply(i, Pack::new));
    }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    public static final DataMapType<Item, Weight> WEIGHT = DataMapType.builder(id("weight"), Registries.ITEM, Weight.CODEC)
            .synced(Weight.CODEC, false).build();
    public static final DataMapType<Item, Props> PROPS = DataMapType.builder(id("props"), Registries.ITEM, Props.CODEC)
            .synced(Props.CODEC, false).build();
    public static final DataMapType<Item, Pack> PACK = DataMapType.builder(id("pack"), Registries.ITEM, Pack.CODEC)
            .synced(Pack.CODEC, false).build();

    public static void register(RegisterDataMapTypesEvent e) {
        e.register(WEIGHT);
        e.register(PROPS);
        e.register(PACK);
    }

    /** The map's grams for one {@code stack} item, or empty. */
    public static Optional<Integer> mapWeightG(ItemStack stack) {
        @Nullable Weight w = stack.getItemHolder().getData(WEIGHT);
        return w == null ? Optional.empty() : Optional.of(w.weightG());
    }

    public static @Nullable Pack pack(ItemStack stack) { return stack.isEmpty() ? null : stack.getItemHolder().getData(PACK); }
}

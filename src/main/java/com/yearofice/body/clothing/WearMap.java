package com.yearofice.body.clothing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.Garment;
import com.yearofice.sim.body.Layer;
import com.yearofice.sim.body.Zone;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/** {@code yearofice:wear} ({@code data_maps/item/wear.json}): the Blueprint › Clothing garment table, one row per worn item. */
public final class WearMap {
    private WearMap() {}

    public record Wear(String zone, String layer, double warmthC, double windBlock, double keptWhenWet, double safeToC,
                       boolean waterproof, double weightKg, double waterUptake, boolean fur) {
        public static final Codec<Wear> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("zone").forGetter(Wear::zone),
                Codec.STRING.fieldOf("layer").forGetter(Wear::layer),
                Codec.DOUBLE.fieldOf("warmth_c").forGetter(Wear::warmthC),
                Codec.DOUBLE.fieldOf("wind_block").forGetter(Wear::windBlock),
                Codec.DOUBLE.optionalFieldOf("kept_when_wet", Double.NaN).forGetter(Wear::keptWhenWet),
                Codec.DOUBLE.optionalFieldOf("safe_to_c", Double.NaN).forGetter(Wear::safeToC),
                Codec.BOOL.optionalFieldOf("waterproof", false).forGetter(Wear::waterproof),
                Codec.DOUBLE.fieldOf("weight_kg").forGetter(Wear::weightKg),
                Codec.DOUBLE.optionalFieldOf("water_uptake", 1.0).forGetter(Wear::waterUptake),
                Codec.BOOL.optionalFieldOf("fur", false).forGetter(Wear::fur)).apply(i, Wear::new));

        public Garment garment(String id) {
            return new Garment(id, Zone.valueOf(zone.toUpperCase(Locale.ROOT)), Layer.valueOf(layer.toUpperCase(Locale.ROOT)),
                    warmthC, windBlock, keptWhenWet, safeToC, waterproof, weightKg, waterUptake, fur);
        }
    }

    public static final DataMapType<Item, Wear> WEAR = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "wear"), Registries.ITEM, Wear.CODEC)
            .synced(Wear.CODEC, false).build();
}

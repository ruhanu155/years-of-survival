package com.yearofice.api.build;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * One row of the block data map {@code yearofice:bed} ({@code data_maps/block/bed.json}): a block a player can
 * sleep on, its bed quality factor (Blueprint › Sleep: bough bed 0.95) and its insulation while asleep (Blueprint ›
 * Making camp: bough bed +5 °C). The building package registers the map and owns the file; the body reads it to
 * step a sleeper.
 *
 * @param quality       the bed factor of q, 0–1
 * @param sleepWarmthC  °C added to feels-like while asleep on it
 */
public record BedData(double quality, double sleepWarmthC) {

    public static final Codec<BedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 1).fieldOf("quality").forGetter(BedData::quality),
            Codec.doubleRange(-20, 40).optionalFieldOf("sleep_warmth_c", 0.0).forGetter(BedData::sleepWarmthC)
    ).apply(i, BedData::new));

    public static final DataMapType<Block, BedData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "bed"), Registries.BLOCK, CODEC).synced(CODEC, false).build();
}

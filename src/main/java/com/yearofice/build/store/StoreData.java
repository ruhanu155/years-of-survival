package com.yearofice.build.store;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * One row of the block data map {@code yearofice:store} ({@code data_maps/block/store.json}): what a storage block holds
 * and what it does to the air round its contents. {@code buffer_pct} is how much of the day's swing it smooths (a
 * sealed barrel more than a rail); {@code dryness_pct} how well the contents stay out of rain.
 */
public record StoreData(double capacityKg, double massKg, double bufferPct, double drynessPct) {
    public static final Codec<StoreData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0.1, 1000).fieldOf("capacity_kg").forGetter(StoreData::capacityKg),
            Codec.doubleRange(0, 1000).fieldOf("mass_kg").forGetter(StoreData::massKg),
            Codec.doubleRange(0, 100).fieldOf("buffer_pct").forGetter(StoreData::bufferPct),
            Codec.doubleRange(0, 100).fieldOf("dryness_pct").forGetter(StoreData::drynessPct)
    ).apply(i, StoreData::new));

    public static final DataMapType<Block, StoreData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "store"), Registries.BLOCK, CODEC).synced(CODEC, false).build();

    public static StoreData of(net.minecraft.world.level.block.state.BlockState s) { return s.getBlockHolder().getData(MAP); }
}

package com.yearofice.api.build;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/** Block data map {@code yearofice:heat} ({@code data_maps/block/heat.json}): the U value of a wall, roof or floor block, W/m²·°C, and the metres of seam its exposed face holds (a log wall block: 3.3). */
public record HeatData(double uWPerM2K, double seamM) {
    public HeatData(double uWPerM2K) { this(uWPerM2K, 0); }

    public static final Codec<HeatData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0.05, 40).fieldOf("u_w_m2k").forGetter(HeatData::uWPerM2K),
            Codec.doubleRange(0, 20).optionalFieldOf("seam_m", 0.0).forGetter(HeatData::seamM)).apply(i, HeatData::new));

    public static final DataMapType<Block, HeatData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "heat"), Registries.BLOCK, CODEC).synced(CODEC, false).build();

    /** U for blocks the map does not list (vanilla solids). */
    public static final double DEFAULT_SOLID_U = 2.0;
}

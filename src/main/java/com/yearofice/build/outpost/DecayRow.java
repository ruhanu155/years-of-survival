package com.yearofice.build.outpost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/**
 * One row of the block data map {@code yearofice:decay} ({@code data_maps/block/decay.json}): the yearly rot of a
 * block that stands under a roof and one under open sky (Decay and time › Wood), and what is left when it goes.
 */
public record DecayRow(double rotPctSheltered, double rotPctExposed, ResourceLocation becomes) {
    public static final Codec<DecayRow> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 100).fieldOf("rot_pct_per_year_sheltered").forGetter(DecayRow::rotPctSheltered),
            Codec.doubleRange(0, 100).fieldOf("rot_pct_per_year_exposed").forGetter(DecayRow::rotPctExposed),
            ResourceLocation.CODEC.optionalFieldOf("becomes", ResourceLocation.withDefaultNamespace("air")).forGetter(DecayRow::becomes)
    ).apply(i, DecayRow::new));

    public static final DataMapType<Block, DecayRow> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "decay"), Registries.BLOCK, CODEC).build();

    public static DecayRow of(BlockState st) { return st.getBlockHolder().getData(MAP); }

    public BlockState leftover() { return BuiltInRegistries.BLOCK.get(becomes).defaultBlockState(); }
}

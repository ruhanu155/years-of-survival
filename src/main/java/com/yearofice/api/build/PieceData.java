package com.yearofice.api.build;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.core.YearOfIce;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.datamaps.DataMapType;

/** Block data map {@code yearofice:piece} ({@code data_maps/block/piece.json}): mass, labour and materials of a building piece. */
public record PieceData(double massKg, double labourMin, List<Material> materials) {
    public record Material(ResourceLocation item, int count) {
        public static final Codec<Material> CODEC = RecordCodecBuilder.create(i -> i.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(Material::item),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Material::count)).apply(i, Material::new));
    }

    public static final Codec<PieceData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.doubleRange(0, 5000).fieldOf("mass_kg").forGetter(PieceData::massKg),
            Codec.doubleRange(0, 2000).fieldOf("labour_min").forGetter(PieceData::labourMin),
            Material.CODEC.listOf().optionalFieldOf("materials", List.of()).forGetter(PieceData::materials)).apply(i, PieceData::new));

    public static final DataMapType<Block, PieceData> MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "piece"), Registries.BLOCK, CODEC).synced(CODEC, false).build();
}

package com.yearofice.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.yearofice.worldgen.Noise;
import com.yearofice.worldgen.gen.WorldgenRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * {@code yearofice:loot_seed}: every container in a template that names a loot table gets a seed from its world
 * position (a hash, never the level's random), so the same world opens the same chests whoever opens them first.
 */
public final class LootSeedProcessor extends StructureProcessor {
    public static final LootSeedProcessor INSTANCE = new LootSeedProcessor();
    public static final MapCodec<LootSeedProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private LootSeedProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() { return WorldgenRegistry.LOOT_SEED.get(); }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
            StructureTemplate.StructureBlockInfo raw, StructureTemplate.StructureBlockInfo info, StructurePlaceSettings settings) {
        CompoundTag nbt = info.nbt();
        if (nbt == null || !nbt.contains("LootTable")) return info;
        CompoundTag copy = nbt.copy();
        BlockPos p = info.pos();
        long seed = ((long) Noise.hash(p.getX(), p.getY(), p.getZ()) << 32) ^ (Noise.hash(p.getZ(), p.getX(), 0x5EED) & 0xFFFFFFFFL);
        copy.putLong("LootTableSeed", seed);
        return new StructureTemplate.StructureBlockInfo(p, info.state(), copy);
    }
}

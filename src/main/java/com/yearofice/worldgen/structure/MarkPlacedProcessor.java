package com.yearofice.worldgen.structure;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * {@code yearofice:mark_placed}: sets the {@link PlacedMask} bit of every block a template places. It runs last
 * in {@code finalizeProcessing}, after every other processor has had its say, so the mask holds exactly what lands.
 *
 * <p>At M1 it is attached in code by {@link SetPieceTemplates}, so its type is not registered: a processor's type
 * is only read to serialise processor lists. It joins {@code Registries.STRUCTURE_PROCESSOR} with the set-piece
 * structure type and the other four processors in M2, once {@code studio/inventory.json} plans the ids.
 */
public final class MarkPlacedProcessor extends StructureProcessor {
    public static final MarkPlacedProcessor INSTANCE = new MarkPlacedProcessor();
    public static final MapCodec<MarkPlacedProcessor> CODEC = MapCodec.unit(() -> INSTANCE);
    public static final StructureProcessorType<MarkPlacedProcessor> TYPE = () -> CODEC;

    private MarkPlacedProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() { return TYPE; }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset,
            BlockPos pos, List<StructureTemplate.StructureBlockInfo> original,
            List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        PlacedMask mask = PlacedMask.get(level.getLevel());
        for (StructureTemplate.StructureBlockInfo b : processed) {
            if (b != null && !b.state().isAir()) mask.mark(b.pos());
        }
        return processed;
    }
}

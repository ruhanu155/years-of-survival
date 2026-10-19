package com.yearofice.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.yearofice.worldgen.gen.WorldgenRegistry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * {@code yearofice:mark_placed}: sets the {@link PlacedMask} bit of every block a template places. It runs last
 * in {@code finalizeProcessing}, after every other processor has had its say, so the mask holds exactly what lands.
 * During generation a piece is placed one chunk box at a time, so only blocks inside the settings' box are marked
 * (the rest are marked when their own chunk is decorated).
 */
public final class MarkPlacedProcessor extends StructureProcessor {
    public static final MarkPlacedProcessor INSTANCE = new MarkPlacedProcessor();
    public static final MapCodec<MarkPlacedProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private MarkPlacedProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() { return WorldgenRegistry.MARK_PLACED.get(); }

    @Override
    public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level, BlockPos offset,
            BlockPos pos, List<StructureTemplate.StructureBlockInfo> original,
            List<StructureTemplate.StructureBlockInfo> processed, StructurePlaceSettings settings) {
        PlacedMask mask = PlacedMask.get(level);
        BoundingBox box = settings.getBoundingBox();
        for (StructureTemplate.StructureBlockInfo b : processed) {
            if (b == null || b.state().isAir()) continue;
            if (box != null && !box.isInside(b.pos())) continue;
            mask.mark(b.pos());
        }
        return processed;
    }
}

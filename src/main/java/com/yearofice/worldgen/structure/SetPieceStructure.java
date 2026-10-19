package com.yearofice.worldgen.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.gen.WorldgenRegistry;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * {@code yearofice:setpiece} (Set pieces and Interior structures as templates): one structure per set piece
 * ({@code worldgen/structure/setpiece/<id>.json}). Stage 5 has already chosen every position, so the structure
 * asks the bound places layer whether one of its pieces starts in this chunk (the chunk holding its footprint's
 * north-west corner) and, if so, adds one {@link TemplatePiece} standing on the flattened pad. No vanilla spacing,
 * salt or jigsaw.
 */
public final class SetPieceStructure extends Structure {
    public static final MapCodec<SetPieceStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i), Codec.STRING.fieldOf("setpiece").forGetter(SetPieceStructure::setpiece)).apply(i, SetPieceStructure::new));

    private final String setpiece;

    public SetPieceStructure(StructureSettings settings, String setpiece) {
        super(settings);
        this.setpiece = setpiece;
    }

    public String setpiece() { return setpiece; }

    @Override
    public StructureType<?> type() { return WorldgenRegistry.SETPIECE.get(); }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (!(ctx.chunkGenerator() instanceof YoiChunkGenerator gen) || gen.world() == null) return Optional.empty();
        ValleyWorld w = gen.world();
        for (SetPieces.Placement q : w.placements) {
            if (!q.id().equals(setpiece)) continue;
            int[] r = ValleyWorld.rect(q);
            if ((r[0] >> 4) != ctx.chunkPos().x || (r[1] >> 4) != ctx.chunkPos().z) continue;
            BlockPos min = new BlockPos(r[0], ValleyWorld.padY(q) + 1, r[1]);
            return Optional.of(new GenerationStub(min, b -> b.addPiece(TemplatePiece.at(ctx.structureTemplateManager(),
                    ctx.registryAccess(), q.template(), min, q.rotation()))));
        }
        return Optional.empty();
    }
}

package com.yearofice.worldgen.structure;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.gen.ValleyWorlds;
import com.yearofice.worldgen.gen.WorldgenRegistry;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

/**
 * {@code yearofice:places_layer}: a structure set is offered a chunk only when stage 5 starts a set piece there.
 * The placement sees only the level seed, so it reads the layers the generator bound for that seed; with none
 * bound (another generator) it offers nothing, and with a seed bound twice the structure itself still checks.
 */
public final class PlacesLayerPlacement extends StructurePlacement {
    public static final MapCodec<PlacesLayerPlacement> CODEC = RecordCodecBuilder.mapCodec(i -> placementCodec(i).apply(i, PlacesLayerPlacement::new));

    public PlacesLayerPlacement(net.minecraft.core.Vec3i locateOffset, FrequencyReductionMethod method, float frequency, int salt,
                                java.util.Optional<ExclusionZone> exclusion) {
        super(locateOffset, method, frequency, salt, exclusion);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int cx, int cz) {
        ValleyWorld w = ValleyWorlds.forLevelSeed(state.getLevelSeed());
        if (w == null) return false;
        for (SetPieces.Placement q : w.placements) {
            int[] r = ValleyWorld.rect(q);
            if ((r[0] >> 4) == cx && (r[1] >> 4) == cz) return true;
        }
        return false;
    }

    @Override
    public StructurePlacementType<?> type() { return WorldgenRegistry.PLACES_LAYER.get(); }
}

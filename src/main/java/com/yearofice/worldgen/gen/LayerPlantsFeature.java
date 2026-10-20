package com.yearofice.worldgen.gen;

import com.yearofice.plant.ForageConfig;
import com.yearofice.plant.PlantRegistry;
import com.yearofice.worldgen.PlantPlan;
import com.yearofice.worldgen.TreePlacer;
import com.yearofice.worldgen.ValleyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code yearofice:layer_plants}: the vegetation pass. Places the plant clumps of {@link PlantPlan} whose column lies
 * in this chunk, one block on dry soil ground, only into air, after {@code layer_trees} in the same step. Reads the
 * layers, never the terrain it must not change.
 */
public final class LayerPlantsFeature extends Feature<NoneFeatureConfiguration> {
    public LayerPlantsFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        if (!(ctx.chunkGenerator() instanceof YoiChunkGenerator gen) || gen.world() == null) return false;
        ValleyWorld w = gen.world();
        WorldGenLevel level = ctx.level();
        int x0 = ctx.origin().getX() & ~15, z0 = ctx.origin().getZ() & ~15;
        ValleyWorld.Columns cols = TreePlacer.columnsFor(w, x0, z0);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        boolean any = false;
        for (PlantPlan.Clump c : PlantPlan.clumpsIn(w, x0, z0, x0 + 16, z0 + 16, ForageConfig.tuning().patchDensity())) {
            int y = TreePlacer.baseY(cols, c.x(), c.z());
            if (y == ValleyWorld.NONE) continue;
            if (!LayerTreesFeature.stands(level.getBlockState(mp.set(c.x(), y - 1, c.z())))) continue;
            if (!level.getBlockState(mp.set(c.x(), y, c.z())).isAir()) continue;
            String id = c.species().substring(c.species().indexOf(':') + 1);
            Block b = PlantRegistry.block(id);
            level.setBlock(mp, b.defaultBlockState(), Block.UPDATE_CLIENTS);
            any = true;
        }
        return any;
    }
}

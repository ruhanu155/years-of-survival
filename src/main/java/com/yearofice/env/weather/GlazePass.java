package com.yearofice.env.weather;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.sim.weather.StormKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Sets and melts {@code glaze_layer} and {@code hail_layer} (Weather and storms › Blocks, ops and passes): a sparse sample of
 * columns around each player per game minute. Derived state: the layer count comes from the storm's accumulated glaze or
 * hail, never from who watched. ponytail: columns nobody is near are not visited — upgrade: settle on chunk load with the
 * catch-up path.
 */
public final class GlazePass {
    private GlazePass() {}

    /** Places {@code layers} (1..3) of {@code block} on {@code samples} random open columns within {@code radius} of {@code c}. Returns how many were set. */
    public static int place(ServerLevel level, BlockPos c, int radius, int samples, long salt, Block block, int layers, boolean canopyThins) {
        int set = 0;
        for (int i = 0; i < samples; i++) {
            Mulberry32 r = new Mulberry32(StageHash.of(level.getSeed() + ":gz:" + salt + ":" + i));
            int x = c.getX() + r.nextInt(radius * 2 + 1) - radius, z = c.getZ() + r.nextInt(radius * 2 + 1) - radius;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos p = new BlockPos(x, y, z);
            boolean canopy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > y;
            int n = canopy && canopyThins ? layers / 3 : layers; // under canopy e = 0.3
            if (n < 1) continue;
            BlockState below = level.getBlockState(p.below());
            if (!below.isFaceSturdy(level, p.below(), Direction.UP) || !level.getFluidState(p).isEmpty()) continue;
            BlockState here = level.getBlockState(p);
            if (!(here.isAir() || here.is(block) || here.canBeReplaced())) continue;
            if (here.is(block) && here.getValue(SheetBlock.LAYERS) >= n) continue;
            level.setBlock(p, block.defaultBlockState().setValue(SheetBlock.LAYERS, n), 3);
            set++;
        }
        return set;
    }

    /** Melts: each sampled sheet loses a layer with chance {@code pLose}. Returns layers removed. */
    public static int melt(ServerLevel level, BlockPos c, int radius, int samples, long salt, Block block, double pLose) {
        int gone = 0;
        for (int i = 0; i < samples; i++) {
            Mulberry32 r = new Mulberry32(StageHash.of(level.getSeed() + ":gm:" + salt + ":" + i));
            int x = c.getX() + r.nextInt(radius * 2 + 1) - radius, z = c.getZ() + r.nextInt(radius * 2 + 1) - radius;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
            BlockState s = level.getBlockState(p);
            if (!s.is(block) || r.nextDouble() >= pLose) continue;
            int l = s.getValue(SheetBlock.LAYERS);
            level.setBlock(p, l <= 1 ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : s.setValue(SheetBlock.LAYERS, l - 1), 3);
            gone++;
        }
        return gone;
    }

    static boolean precipSheet(StormKind k) { return k == StormKind.FREEZING_RAIN || k == StormKind.HAIL; }
}

package com.yearofice.env.weather;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.core.hash.StageHash;
import com.yearofice.net.YoiNet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Strikes (Weather and storms › Thunderstorms): the highest point in a 32 x 32 cell takes the bolt, the payload tells players
 * within 12 km, and a struck tree drops a {@code fallen_limb} beside it. No vanilla LightningBolt: thunder is ours.
 */
public final class Lightning {
    private Lightning() {}

    static final double FAR_M = 12_000;
    private static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1}, DZ = {0, 1, 1, 1, 0, -1, -1, -1};

    /** The highest motion-blocking top in the 32 x 32 cell holding (x, z), sampled every 4 blocks; null if nothing is loaded. */
    public static BlockPos highest(ServerLevel level, int x, int z) {
        int cx = Math.floorDiv(x, 32) * 32, cz = Math.floorDiv(z, 32) * 32;
        BlockPos best = null;
        for (int dx = 0; dx < 32; dx += 4)
            for (int dz = 0; dz < 32; dz += 4) {
                if (!level.hasChunkAt(new BlockPos(cx + dx, 64, cz + dz))) continue;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx + dx, cz + dz) - 1;
                if (best == null || y > best.getY()) best = new BlockPos(cx + dx, y, cz + dz);
            }
        return best;
    }

    /** Strikes at {@code top}; returns the limb block it dropped, or null. */
    public static BlockPos strike(ServerLevel level, BlockPos top, long salt) {
        int seed = StageHash.of(level.getSeed() + ":bolt:" + salt + ":" + top.asLong());
        for (ServerPlayer p : level.players())
            if (p.blockPosition().distSqr(top) <= FAR_M * FAR_M) YoiNet.send(p, new StrikePayload(top, seed, 1f));
        com.yearofice.story.rare.StrikeHarm.apply(level, top, seed);
        return limb(level, top, seed);
    }

    static boolean tree(BlockState s) { return s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES); }

    /** A struck tree sets a fallen_limb on free ground within 3 blocks of its foot. */
    static BlockPos limb(ServerLevel level, BlockPos top, int seed) {
        if (!tree(level.getBlockState(top))) return null;
        BlockPos g = top;
        while (g.getY() > level.getMinBuildHeight() && tree(level.getBlockState(g))) g = g.below();
        Mulberry32 r = new Mulberry32(seed);
        int start = r.nextInt(8);
        for (int ring = 1; ring <= 3; ring++)
            for (int i = 0; i < 8; i++) {
                int k = (start + i) % 8;
                int dx = DX[k] * ring, dz = DZ[k] * ring;
                BlockPos p = g.offset(dx, 1, dz);
                BlockState here = level.getBlockState(p);
                if ((here.isAir() || here.canBeReplaced()) && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                    level.setBlock(p, WeatherRegistry.FALLEN_LIMB.get().defaultBlockState()
                            .setValue(FallenLimbBlock.FACING, Direction.from2DDataValue(r.nextInt(4))), 3);
                    return p;
                }
            }
        return null;
    }

    /** One storm minute: maybe a strike within 2 km of {@code player}, at the highest point of the cell it lands in. */
    static void minute(ServerLevel level, ServerPlayer player, double everyMin, long minute) {
        Mulberry32 r = new Mulberry32(StageHash.of(level.getSeed() + ":strike:" + minute + ":" + player.getUUID()));
        if (r.nextDouble() >= 1.0 / Math.max(1, everyMin)) return;
        double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * 2000;
        BlockPos top = highest(level, (int) (player.getX() + Math.cos(a) * d), (int) (player.getZ() + Math.sin(a) * d));
        if (top != null) strike(level, top, minute);
    }
}

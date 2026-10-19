package com.yearofice.ecosystem.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The habitat names a species file lists (Minecraft engine › Species entry: {@code "habitat"}), as tests on the
 * blocks around a surface column. They read our brush and willow blocks once the World department ships them
 * (any block whose id names willow, alder, dwarf birch or brush) and vanilla leaves, logs and bushes until then.
 *
 * <ul>
 *   <li>{@code willow_edge}, {@code spruce_edge}: open ground with cover within 3 blocks: where hares run
 *       ("willow and spruce edges").</li>
 *   <li>{@code open_shrub}: open sky over low cover within 2 blocks: willow flats and dwarf birch, for
 *       ptarmigan.</li>
 *   <li>{@code riparian}: water within 12 blocks and cover within 6: the moose's willow flats.</li>
 * </ul>
 */
public final class Habitats {
    private Habitats() {}

    public static boolean isCover(BlockState s) {
        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.AZALEA) || s.is(Blocks.FLOWERING_AZALEA)) return true;
        String path = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        return path.contains("willow") || path.contains("alder") || path.contains("dwarf_birch") || path.contains("brush");
    }

    static boolean openGround(ServerLevel level, BlockPos ground) {
        BlockState below = level.getBlockState(ground.below());
        if (!below.isFaceSturdy(level, ground.below(), net.minecraft.core.Direction.UP)) return false;
        if (!level.getFluidState(ground.below()).isEmpty()) return false;
        BlockState at = level.getBlockState(ground);
        return at.isAir() || at.canBeReplaced();
    }

    /** The first open block above the ground in a column, ignoring leaves. */
    public static BlockPos surface(ServerLevel level, int x, int z) {
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    static boolean coverWithin(ServerLevel level, BlockPos at, int r, int up) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                for (int dy = 0; dy <= up; dy++) {
                    m.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (isCover(level.getBlockState(m))) return true;
                }
        return false;
    }

    static boolean waterWithin(ServerLevel level, BlockPos at, int r) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx += 2)
            for (int dz = -r; dz <= r; dz += 2)
                for (int dy = -2; dy <= 1; dy++) {
                    m.set(at.getX() + dx, at.getY() + dy, at.getZ() + dz);
                    if (level.getFluidState(m).is(FluidTags.WATER)) return true;
                }
        return false;
    }

    /** True when the column at this surface position is the named habitat. */
    public static boolean is(ServerLevel level, BlockPos ground, String habitat) {
        if (!openGround(level, ground)) return false;
        return switch (habitat) {
            case "willow_edge", "spruce_edge", "riparian_edge" -> coverWithin(level, ground, 3, 3);
            case "open_shrub" -> level.canSeeSky(ground) && coverWithin(level, ground, 2, 1);
            case "riparian" -> waterWithin(level, ground, 12) && coverWithin(level, ground, 6, 4);
            default -> false;
        };
    }

    public static boolean any(ServerLevel level, BlockPos ground, Iterable<String> habitats) {
        for (String h : habitats) if (is(level, ground, h)) return true;
        return false;
    }

    /** Share of {@code samples} hashed columns within {@code radius} of a point that are habitat, 0–1. */
    public static double share(ServerLevel level, BlockPos center, int radius, Iterable<String> habitats, int samples, long seed) {
        var r = com.yearofice.sim.decay.Thresholds.stream(seed, "habitat", center.getX(), center.getZ(), radius);
        int hit = 0, n = 0;
        for (int i = 0; i < samples; i++) {
            int x = center.getX() + (int) Math.round((r.nextDouble() * 2 - 1) * radius);
            int z = center.getZ() + (int) Math.round((r.nextDouble() * 2 - 1) * radius);
            if (!level.hasChunkAt(new BlockPos(x, center.getY(), z))) continue;
            n++;
            if (any(level, surface(level, x, z), habitats)) hit++;
        }
        return n == 0 ? 0 : hit / (double) n;
    }
}

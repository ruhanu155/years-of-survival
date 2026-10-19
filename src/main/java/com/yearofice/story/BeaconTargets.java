package com.yearofice.story;

import com.yearofice.sim.story.FirstNightStep;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Where the objective beacon stands for a step (Blueprint › Start site, The steps: "Beacon" column), found by the
 * server from loaded blocks only, so a search never loads a chunk:
 *
 * <ul>
 *   <li>Gather: the nearest birch trunk (its bark is the tinder that burns wet), else the nearest tree.</li>
 *   <li>Fire spot: the ring once set, else the outpost's fire spot, 6 m out of the door (Blueprint › Start site,
 *       The outpost layout: "Firepit, 6 m from the door").</li>
 *   <li>Fire: the player's ring.</li>
 *   <li>Food: the nearest open water at the surface, the creek where the rod and the game trails are.</li>
 * </ul>
 */
final class BeaconTargets {
    private BeaconTargets() {}

    /** Columns are sampled every 2 blocks; trunks are looked for this far under the surface. */
    static final int STEP = 2, TRUNK_DEPTH = 12;

    static @Nullable BlockPos find(ServerPlayer p, OnboardingRecord r, FirstNightStep step, int radius) {
        return switch (step.beacon) {
            case GATHER -> {
                BlockPos birch = nearestTrunk(p, radius, true);
                yield birch != null ? birch : nearestTrunk(p, radius, false);
            }
            case FIRE_SPOT -> r.ring != null ? r.ring : fireSpot(p, r);
            case FIRE -> r.ring;
            case FOOD -> nearestOpenWater(p, radius);
        };
    }

    /** Metres from the door to the fire spot. */
    static final int FIRE_SPOT_M = 6;

    /** The start site's fire spot: {@link #FIRE_SPOT_M} blocks out of the door along the spawn's facing, on the ground. */
    static @Nullable BlockPos fireSpot(ServerPlayer p, OnboardingRecord r) {
        if (r.startSite == null) return null;
        ServerLevel level = p.serverLevel();
        double yaw = Math.toRadians(level.getSharedSpawnAngle());
        int x = r.startSite.getX() + (int) Math.round(-Math.sin(yaw) * FIRE_SPOT_M);
        int z = r.startSite.getZ() + (int) Math.round(Math.cos(yaw) * FIRE_SPOT_M);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, r.startSite.getY(), z);
        if (!level.isLoaded(m)) return r.startSite;
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** The ground under the nearest trunk of a birch (or any log), or null. */
    static @Nullable BlockPos nearestTrunk(ServerPlayer p, int radius, boolean birch) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += STEP) {
            for (int dz = -radius; dz <= radius; dz += STEP) {
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!level.isLoaded(m.set(x, c.getY(), z))) continue;
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int y = top; y > top - TRUNK_DEPTH; y--) {
                    BlockState s = level.getBlockState(m.set(x, y, z));
                    if (birch ? s.is(BlockTags.BIRCH_LOGS) : s.is(BlockTags.LOGS)) {
                        // Walk to the foot of the trunk so the column rises from the ground.
                        int foot = y;
                        while (foot > level.getMinBuildHeight() && level.getBlockState(m.set(x, foot - 1, z)).is(BlockTags.LOGS)) foot--;
                        double d = (double) dx * dx + (double) dz * dz;
                        if (d < bd) {
                            bd = d;
                            best = new BlockPos(x, foot, z);
                        }
                        break;
                    }
                }
            }
        }
        return best;
    }

    /** The nearest water block open to the sky, or null. */
    static @Nullable BlockPos nearestOpenWater(ServerPlayer p, int radius) {
        ServerLevel level = p.serverLevel();
        BlockPos c = p.blockPosition();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx += STEP) {
            for (int dz = -radius; dz <= radius; dz += STEP) {
                int x = c.getX() + dx, z = c.getZ() + dz;
                if (!level.isLoaded(m.set(x, c.getY(), z))) continue;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                if (!level.getFluidState(m.set(x, y, z)).is(FluidTags.WATER)) continue;
                double d = (double) dx * dx + (double) dz * dz;
                if (d < bd) {
                    bd = d;
                    best = new BlockPos(x, y, z);
                }
            }
        }
        return best;
    }
}

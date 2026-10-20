package com.yearofice.ecosystem.raid;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.encounter.WorldToggles;
import com.yearofice.ecosystem.theft.TheftService;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.ecosystem.Theft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Rack raids (video 2, A3; RECONCILE 170): the world edit that follows a theft decided by {@link TheftService}. A fox
 * drags the hide or strip 20-60 m and hides it as a {@code theft_cache}; 70% of caches can be recovered within 2 game
 * days. Birds and martens eat on the spot. Behind the {@code [world] rack_thieves} option.
 */
public final class RackRaid {
    private RackRaid() {}

    public static final double RECOVERABLE = 0.70;

    /** Writes the cache for the theft just made at {@code store}, if the thief cached it. Returns the cache position or null. */
    public static BlockPos afterTheft(ServerLevel level, BlockPos store, TheftService.Taken t) {
        if (t == null || !WorldToggles.rackThieves() || t.thief() != Theft.Thief.RED_FOX) return null;
        return cache(level, store, t.taken(), t.hourIndex());
    }

    /** Hides {@code stolen} 20-60 m from the store, on the surface of a loaded chunk. */
    public static BlockPos cache(ServerLevel level, BlockPos store, ItemStack stolen, long hourIndex) {
        if (!WorldToggles.rackThieves() || stolen.isEmpty()) return null;
        Mulberry32 r = Thresholds.stream(level.getSeed(), "theft_cache", store.getX(), store.getY(), store.getZ(), hourIndex);
        double ang = r.nextDouble() * Math.PI * 2, dist = 20 + r.nextDouble() * 40;
        boolean recoverable = r.nextDouble() < RECOVERABLE;
        int x = store.getX() + (int) Math.round(Math.cos(ang) * dist), z = store.getZ() + (int) Math.round(Math.sin(ang) * dist);
        if (!level.hasChunkAt(new BlockPos(x, store.getY(), z))) return null;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos at = new BlockPos(x, y, z);
        if (!level.getBlockState(at).isAir()) return null;
        level.setBlock(at, RaidRegistry.THEFT_CACHE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(at) instanceof TheftCacheBlockEntity be) be.fill(stolen, SliceClock.minute(level), recoverable);
        return at;
    }
}

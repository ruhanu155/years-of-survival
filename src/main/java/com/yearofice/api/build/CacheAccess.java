package com.yearofice.api.build;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Whether an animal can get onto a raised cache (Every concept › Raised cache legs). The theft and bear lanes call
 * {@link #reach} before an animal takes from a cache platform. Owner {@code build.store}.
 */
public interface CacheAccess {
    /** Chance 0-1 the species reaches the platform at {@code pos}; 1 for a position that is not a cache platform. */
    double reach(Level level, BlockPos pos, String species);

    Holder INSTANCE = new Holder();

    final class Holder implements CacheAccess {
        private volatile CacheAccess delegate;

        public void set(CacheAccess d) { delegate = d; }

        @Override
        public double reach(Level level, BlockPos pos, String species) { CacheAccess d = delegate; return d == null ? 1 : d.reach(level, pos, species); }
    }
}

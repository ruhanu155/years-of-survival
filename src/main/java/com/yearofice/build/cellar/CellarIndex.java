package com.yearofice.build.cellar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The loaded cellar hatches per level, so a store lookup finds its room without scanning chunks. Server thread only. */
public final class CellarIndex {
    private CellarIndex() {}

    private static final Map<ResourceKey<Level>, Set<BlockPos>> POS = new HashMap<>();

    static void add(Level level, BlockPos pos) { POS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable()); }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> s = POS.get(level.dimension());
        if (s != null) s.remove(pos);
    }

    public static void clear() { POS.clear(); }

    /** The valid cellar whose room holds {@code pos}, or null. Hatches farther than 8 blocks are skipped. */
    public static CellarBlockEntity at(Level level, BlockPos pos) {
        for (BlockPos h : POS.getOrDefault(level.dimension(), Set.of())) {
            if (h.distManhattan(pos) > 12) continue;
            if (level.getBlockEntity(h) instanceof CellarBlockEntity be && be.valid() && be.contains(pos)) return be;
        }
        return null;
    }
}

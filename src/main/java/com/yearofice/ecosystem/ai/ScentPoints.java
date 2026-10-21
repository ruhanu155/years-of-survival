package com.yearofice.ecosystem.ai;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Where carrion scent comes from besides carcass entities: the gut piles and uncleared cleaning spots a hunter or
 * fisher leaves (Blueprint › Field care: "a carcass or gut pile is a bear bait station"). The gut-pile block
 * registers and unregisters its position here; scavengers read the points within their smell range.
 */
public final class ScentPoints {
    private ScentPoints() {}

    private static final Map<ResourceKey<Level>, Set<BlockPos>> POINTS = new ConcurrentHashMap<>();

    public static void add(ServerLevel level, BlockPos pos) {
        POINTS.computeIfAbsent(level.dimension(), k -> ConcurrentHashMap.newKeySet()).add(pos.immutable());
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        Set<BlockPos> s = POINTS.get(level.dimension());
        if (s != null) s.remove(pos);
    }

    public static Set<BlockPos> in(ServerLevel level) {
        Set<BlockPos> s = POINTS.get(level.dimension());
        return s == null ? Set.of() : s;
    }

    public static void clear() { POINTS.clear(); }
}

package com.yearofice.food.pan;

import com.yearofice.sim.food.Placer;
import com.yearofice.worldgen.PlacerParams;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Placer reaches on the live world: the {@code placer} entry of valley.json, the world seed and the start site. */
public final class PlacerSites {
    private PlacerSites() {}

    private static volatile Placer params;

    public static Placer params() {
        Placer p = params;
        if (p == null) params = p = PlacerParams.load();
        return p;
    }

    public static boolean reachAt(ServerLevel level, BlockPos pos) {
        BlockPos start = level.getSharedSpawnPos();
        return params().reachAt(level.getSeed(), pos.getX(), pos.getZ(), start.getX(), start.getZ());
    }
}

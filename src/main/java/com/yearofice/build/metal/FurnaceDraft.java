package com.yearofice.build.metal;

import com.yearofice.api.fire.HeatSources;
import com.yearofice.fire.stove.StoveRegistry;
import com.yearofice.sim.fire.Stove;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * A bloomery, forge or furnace under work heats its room through {@link HeatSources}: {@code furnace_kw} times the draft
 * of the pipe stacked above it (the stove draft curve of M8 lane B: no pipe 0.3, ideal 4 m 1.0). The heat is posted while
 * the player works and withdrawn when the job ends or is let go.
 */
final class FurnaceDraft {
    private FurnaceDraft() {}

    private static final Stove.Kind KIND = new Stove.Kind("furnace", 0, 0, 0, 2, 4);

    static double draft(ServerLevel level, BlockPos pos) {
        int n = 0;
        BlockPos p = pos.above();
        while (n < 8 && level.getBlockState(p).is(StoveRegistry.CHIMNEY_PIPE.get())) { n++; p = p.above(); }
        return Stove.draft(KIND, n);
    }

    static void post(ServerLevel level, BlockPos pos, boolean on) {
        HeatSources.post(level, pos, on ? MetalConfig.get("furnace_kw") * 1000 * draft(level, pos) : 0);
    }
}

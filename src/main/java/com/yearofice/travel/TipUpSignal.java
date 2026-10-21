package com.yearofice.travel;

import com.yearofice.hunt.fishing.PassiveGearService;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** A tip-up with a fish on signals through snow: new snow over the flag never hides it (fishing/tipup_buried). */
public final class TipUpSignal {
    private TipUpSignal() {}

    public static boolean signals(ServerLevel level, BlockPos pos) {
        var g = PassiveGearService.at(level, pos);
        return g != null && !g.held.isEmpty();
    }

    public static boolean buried(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(WorldBlocks.SNOW_COVER.get());
    }
}

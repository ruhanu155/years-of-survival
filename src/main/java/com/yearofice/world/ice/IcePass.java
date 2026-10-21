package com.yearofice.world.ice;

import com.yearofice.sim.world.IceModel;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.world.WorldHooks;
import com.yearofice.world.season.PassContext;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.WaterStage;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ice pass (Ice on blocks): sets the top water block of each column to the body's ice state (skim, river ice
 * thin, safe or rotten, or water). Columns flagged thin, spring or lead take the next weaker state. Ice is one block
 * thick on screen whatever its true thickness. An open ice hole stays a hole while there is ice; when the ice goes,
 * the snow that lay on it goes too.
 */
public final class IcePass {
    private IcePass() {}

    /** Body class and thin flag of a water column: the valley's water layer, or a lake anywhere else. */
    public static int[] classOf(ValleyWorld w, int x, int z) {
        if (w == null) return new int[] {BodyClass.LAKE.ordinal(), 0};
        int t = w.waterTypeAt(x, z);
        int f = w.waterFlagsAt(x, z);
        BodyClass c = t == WaterStage.MAIN || t == WaterStage.CREEK ? BodyClass.RIVER
                : t == WaterStage.LAKE ? BodyClass.LAKE : t == WaterStage.DRY ? BodyClass.LAKE : BodyClass.POND;
        boolean thin = (f & (WaterStage.F_THIN | WaterStage.F_SPRING | WaterStage.F_LEAD)) != 0;
        return new int[] {c.ordinal(), thin ? 1 : 0};
    }

    public static void apply(PassContext c) {
        ValleyWorld w = WorldHooks.world(c.level);
        int x0 = c.chunk.getPos().getMinBlockX(), z0 = c.chunk.getPos().getMinBlockZ();
        var snow = WorldBlocks.SNOW_COVER.get();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int y = c.waterBase[PassContext.idx(lx, lz)];
                if (y < c.chunk.getMinBuildHeight()) continue;
                BlockPos p = new BlockPos(x0 + lx, y, z0 + lz);
                if (c.floodFlags.contains(p.asLong())) continue;
                BlockState cur = c.get(p);
                boolean water = cur.is(Blocks.WATER) && cur.getFluidState().isSource();
                boolean hole = cur.is(IceBlocks.ICE_HOLE.get());
                if (!water && !hole && !IceBlocks.isIce(cur)) continue;
                int[] k = classOf(w, p.getX(), p.getZ());
                IceModel.Stage st = IceModel.stage(c.core.ice(BodyClass.values()[k[0]]), c.ice);
                if (k[1] == 1) st = st.weaker();
                if (hole && st != IceModel.Stage.OPEN) continue;
                BlockState want = IceBlocks.stateFor(st);
                if (c.set(p, want) && st == IceModel.Stage.OPEN) {
                    for (int dy = 1; dy <= 3; dy++) {
                        BlockPos s = p.above(dy);
                        if (c.get(s).is(snow)) c.set(s, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }
}

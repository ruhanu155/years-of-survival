package com.yearofice.hunt.fishing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * The fish weir as a multiblock: a {@code weir_basket} in the gap of a line of {@code weir_stake} blocks. The stakes
 * count in an unbroken run either side of the basket along x or z (the longer axis counts); a stake that is
 * broken or a gap in the line ends the count there. {@link com.yearofice.sim.fishing.WeirFence} turns the count
 * into the basket's multiplier.
 */
public final class WeirFences {
    private WeirFences() {}

    static final int MAX_SCAN = 24;

    /** Stakes in the fence through the basket at {@code basket}: the better of the x and z lines. */
    public static int stakes(Level level, BlockPos basket) {
        int best = 0;
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            Direction d = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            best = Math.max(best, run(level, basket, d) + run(level, basket, d.getOpposite()));
        }
        return best;
    }

    private static int run(Level level, BlockPos from, Direction d) {
        int n = 0;
        BlockPos p = from.relative(d);
        while (n < MAX_SCAN && level.getBlockState(p).is(FishingRegistry.WEIR_STAKE.get())) {
            n++;
            p = p.relative(d);
        }
        return n;
    }
}

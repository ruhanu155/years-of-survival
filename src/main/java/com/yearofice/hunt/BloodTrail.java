package com.yearofice.hunt;

import com.yearofice.sim.hunt.Shot.Channel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Places and reads blood patches. */
public final class BloodTrail {
    private BloodTrail() {}

    public static BloodTrailBlock.Kind kindOf(Channel c) {
        return switch (c) {
            case VITALS -> BloodTrailBlock.Kind.BRIGHT;
            case GUT -> BloodTrailBlock.Kind.GUT;
            default -> BloodTrailBlock.Kind.DARK;
        };
    }

    /** Drops one patch at the ground under {@code at}; a second drop on the same cell thickens it (to 3). Returns the cell used, or null when there is no ground. */
    @Nullable
    public static BlockPos drop(ServerLevel level, BlockPos at, BloodTrailBlock.Kind kind) {
        BlockPos p = at;
        for (int i = 0; i < 3; i++) {
            BlockState here = level.getBlockState(p);
            if (here.is(HuntRegistry.BLOOD_TRAIL.get())) {
                int a = here.getValue(BloodTrailBlock.AMOUNT);
                if (a < 3) level.setBlock(p, here.setValue(BloodTrailBlock.AMOUNT, a + 1), 2);
                return p;
            }
            if (here.isAir() || here.canBeReplaced()) {
                BlockState put = HuntRegistry.BLOOD_TRAIL.get().defaultBlockState().setValue(BloodTrailBlock.KIND, kind);
                if (!put.canSurvive(level, p)) { p = p.below(); continue; }
                level.setBlock(p, put, 2);
                level.scheduleTick(p, HuntRegistry.BLOOD_TRAIL.get(), BloodTrailBlock.lifeTicks(level, p, HuntConfig.tuning()));
                return p;
            }
            p = p.above();
        }
        return null;
    }

    /** The patch nearest {@code from} within {@code r} blocks that lies farther from the start than {@code from}: the way the trail runs. */
    @Nullable
    public static BlockPos next(ServerLevel level, BlockPos from, int r) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(from.offset(-r, -2, -r), from.offset(r, 2, r))) {
            if (p.equals(from) || !level.getBlockState(p).is(HuntRegistry.BLOOD_TRAIL.get())) continue;
            double d = p.distSqr(from);
            if (d < bd) { bd = d; best = p.immutable(); }
        }
        return best;
    }

    public static String compass(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(dx, -dz));
        if (deg < 0) deg += 360;
        String[] n = {"north", "northeast", "east", "southeast", "south", "southwest", "west", "northwest"};
        return n[(int) Math.round(deg / 45.0) % 8];
    }

    /** Number of patches of any amount within {@code r} blocks of {@code c}, and the total drops they hold. */
    public static int[] count(ServerLevel level, BlockPos c, int r) {
        int cells = 0, drops = 0;
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-r, -2, -r), c.offset(r, 3, r))) {
            BlockState s = level.getBlockState(p);
            if (s.is(HuntRegistry.BLOOD_TRAIL.get())) { cells++; drops += s.getValue(BloodTrailBlock.AMOUNT); }
        }
        return new int[] {cells, drops};
    }
}

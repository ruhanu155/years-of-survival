package com.yearofice.build.dugout;

import com.yearofice.sim.build.Dugout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * {@code /yoi dugout template}: a debug dugout laid whole, the way a player's staged dig would leave it. The interior's
 * front-left-floor corner is {@code origin}; {@code front} is the side the log wall faces, the room runs away from it.
 * Earth (or stone for a rock shelter) packs round and 2 blocks over the roof; the front is a spruce plank wall.
 */
public final class DugoutTemplate {
    private DugoutTemplate() {}

    /** Lays the template and returns a cell inside it. Dimensions are wide x high x deep and must fit the kind. */
    public static BlockPos build(ServerLevel l, BlockPos origin, Direction front, Dugout.Kind kind, int wide, int high, int deep) {
        Direction back = front.getOpposite(), side = front.getClockWise();
        Block fill = kind == Dugout.Kind.ROCK_SHELTER ? Blocks.STONE : Blocks.DIRT;
        for (int u = -1; u <= wide; u++) {
            for (int v = -1; v <= high + 2; v++) {
                for (int d = -1; d <= deep; d++) {
                    BlockPos p = origin.relative(side, u).above(v).relative(back, d);
                    boolean inside = u >= 0 && u < wide && v >= 0 && v < high && d >= 0 && d < deep;
                    if (inside) l.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                    else if (d == -1) l.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
                    else l.setBlock(p, fill.defaultBlockState(), 3);
                }
            }
        }
        return origin;
    }
}

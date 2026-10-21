package com.yearofice.build.outpost;

import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.block.WorldBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

/**
 * Where the cabin tells show (B8): leaky wall seams and warm roof edges, found from blocks alone so the client can ask
 * on its own level. Seams come from {@code #yearofice:leak_seams} until M8 lane A's RoomService.leaks lands; swap
 * {@link #SEAMS} for it then (HOOKS).
 */
public final class Tells {
    private Tells() {}

    public static final TagKey<Block> LEAK_SEAMS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "leak_seams"));

    /** A seam: a tagged wall block with roofed indoor air on one side and open outdoor air on the other. */
    public static List<BlockPos> leakySeams(LevelReader l, BlockPos center, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (!l.getBlockState(p).is(LEAK_SEAMS)) continue;
            boolean in = false, outside = false;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (!l.getBlockState(n).isAir()) continue;
                if (roofed(l, n)) in = true; else outside = true;
            }
            if (in && outside) out.add(p.immutable());
        }
        return out;
    }

    /** A roof edge: a block with snow on top, air under it, and open sky beside it (where an icicle hangs). */
    public static List<BlockPos> roofEdges(LevelReader l, BlockPos center, int r) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (!l.getBlockState(p.above()).is(WorldBlocks.SNOW_COVER.get()) || !l.getBlockState(p.below()).isAir()) continue;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.below().relative(d);
                if (l.getBlockState(n).isAir() && !roofed(l, n)) { out.add(p.immutable()); break; }
            }
        }
        return out;
    }

    /** Something solid within six blocks above. */
    public static boolean roofed(LevelReader l, BlockPos p) {
        for (int i = 1; i <= 6; i++) if (!l.getBlockState(p.above(i)).isAir()) return true;
        return false;
    }

    /** Snow lies on the ground beside the position: a cheap client-side "it is cold out" until a temperature sync exists. */
    public static boolean coldOutside(LevelReader l, BlockPos p) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-3, -2, -3), p.offset(3, 2, 3))) if (l.getBlockState(q).is(WorldBlocks.SNOW_COVER.get())) return true;
        return false;
    }
}

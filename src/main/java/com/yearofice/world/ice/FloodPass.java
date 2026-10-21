package com.yearofice.world.ice;

import com.yearofice.sim.world.SeasonCore;
import com.yearofice.world.WorldHooks;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.world.season.PassContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The spring flood pulse on blocks (Floods use the flood flags): raises water above a river's top block, a block at
 * a time, only into cells whose four neighbours (inside this chunk) are not air, so the placed water can never flow
 * out. The chunk keeps the positions it placed ({@code flood_flags}); the fall removes exactly those.
 * ponytail: a neighbour in another chunk is judged from the valley layers, not the blocks (load state must not change
 * the result) — upgrade: a world-gen flood mask.
 */
public final class FloodPass {
    private FloodPass() {}

    /** The block as the terrain has it, with the flood water this chunk placed read as air. */
    private static BlockState view(PassContext c, BlockPos p) {
        return c.floodFlags.contains(p.asLong()) ? Blocks.AIR.defaultBlockState() : c.get(p);
    }

    /**
     * A neighbour in another chunk, which must not be read (its load state would change the result): fine if the
     * valley's own layers say it is river or ground at least as high. Outside a valley level it counts as open.
     */
    private static boolean outsideOk(ValleyWorld valley, BlockPos n) {
        if (valley == null) return false;
        ValleyWorld.Columns col = valley.columns(n.getX(), n.getZ(), 1, 1);
        return col.kind[0] == ValleyWorld.WATER || col.ground[0] >= n.getY();
    }

    /** Returns the new flag list for the chunk. The wanted set is a pure function of the terrain, not of old flags. */
    public static List<Long> apply(PassContext c) {
        int blocks = SeasonCore.floodBlocks(c.core.floodM);
        ValleyWorld valley = WorldHooks.world(c.level);
        java.util.Map<Long, Boolean> outside = new java.util.HashMap<>();
        Set<Long> cand = new HashSet<>();
        int x0 = c.chunk.getPos().getMinBlockX(), z0 = c.chunk.getPos().getMinBlockZ();
        if (blocks > 0) {
            for (int lz = 0; lz < 16; lz++) {
                for (int lx = 0; lx < 16; lx++) {
                    int y = c.waterBase[PassContext.idx(lx, lz)];
                    if (y < c.chunk.getMinBuildHeight()) continue;
                    BlockState top = c.get(new BlockPos(x0 + lx, y, z0 + lz));
                    if (!top.is(Blocks.WATER) || !top.getFluidState().isSource()) continue;
                    for (int k = 1; k <= blocks; k++) {
                        BlockPos p = new BlockPos(x0 + lx, y + k, z0 + lz);
                        if (!c.inside(p) || c.masked(p) || !view(c, p).isAir()) break;
                        cand.add(p.asLong());
                    }
                }
            }
            // Drop every cell that has an open side (air that is not itself flood water): water must not flow out.
            boolean changed = true;
            while (changed) {
                changed = false;
                for (Long l : new ArrayList<>(cand)) {
                    BlockPos p = BlockPos.of(l);
                    boolean ok = true;
                    for (Direction d : Direction.Plane.HORIZONTAL) {
                        BlockPos n = p.relative(d);
                        if (cand.contains(n.asLong())) continue;
                        if (c.chunk.getPos().x == (n.getX() >> 4) && c.chunk.getPos().z == (n.getZ() >> 4)) {
                            if (view(c, n).isAir()) { ok = false; break; }
                        } else if (!outside.computeIfAbsent(n.asLong(), k -> outsideOk(valley, n))) { ok = false; break; }
                    }
                    BlockPos below = p.below();
                    if (ok && !cand.contains(below.asLong()) && !view(c, below).is(Blocks.WATER)) ok = false;
                    if (!ok) { cand.remove(l); changed = true; }
                }
            }
        }
        List<Long> next = new ArrayList<>(cand);
        java.util.Collections.sort(next);
        BlockState water = Blocks.WATER.defaultBlockState(), air = Blocks.AIR.defaultBlockState();
        for (long f : c.floodFlags) if (!cand.contains(f)) {
            BlockPos p = BlockPos.of(f);
            if (c.get(p).is(Blocks.WATER)) c.set(p, air);
        }
        for (long f : next) c.set(BlockPos.of(f), water);
        c.floodFlags.clear();
        c.floodFlags.addAll(next);
        return next;
    }
}

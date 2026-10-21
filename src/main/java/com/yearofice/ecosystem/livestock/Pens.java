package com.yearofice.ecosystem.livestock;

import com.yearofice.sim.livestock.PenRisk;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pen detection: a bounded flood fill over walkable ground from the animal. If it closes inside {@link #MAX_CELLS}
 * cells the pen is penned, and its fence kind is the weakest piece on the boundary. Reads blocks only.
 */
public final class Pens {
    private Pens() {}

    public static final int MAX_CELLS = 400;

    public record Pen(PenRisk.Fence fence, int cells, boolean doorOpen) {
        public static final Pen OPEN = new Pen(PenRisk.Fence.NONE, 0, false);

        public boolean penned() { return fence != PenRisk.Fence.NONE; }
    }

    public static PenRisk.Fence kindOf(BlockState s) {
        String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
        if (id.contains("willow_brush") || id.contains("shrub_stem") || id.contains("brush")) return PenRisk.Fence.BRUSH;
        if (id.contains("wall_log") || id.contains("stockade")) return PenRisk.Fence.STOCKADE;
        if (s.is(BlockTags.FENCES) || s.is(BlockTags.WALLS) || s.is(BlockTags.FENCE_GATES)) return PenRisk.Fence.POLE;
        return null;
    }

    public static Pen scan(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(start);
        seen.add(start);
        PenRisk.Fence weakest = PenRisk.Fence.STOCKADE;
        boolean sawFence = false, doorOpen = false;
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (seen.size() > MAX_CELLS) return Pen.OPEN;
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.contains(n)) continue;
                BlockState s = level.getBlockState(n);
                PenRisk.Fence k = kindOf(s);
                if (k != null) {
                    sawFence = true;
                    if (s.is(BlockTags.FENCE_GATES) && s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                            && s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)) {
                        doorOpen = true;
                        q.add(n); seen.add(n);
                    } else if (k.ordinal() < weakest.ordinal()) weakest = k;
                    continue;
                }
                if (!s.getCollisionShape(level, n).isEmpty()) {
                    // A plain solid block is a wall as strong as a pole fence; a step up is walkable ground.
                    if (level.getBlockState(n.above()).getCollisionShape(level, n.above()).isEmpty()
                            && level.getBlockState(n.above(2)).getCollisionShape(level, n.above(2)).isEmpty()) { seen.add(n); q.add(n.above()); }
                    continue;
                }
                seen.add(n);
                q.add(n);
            }
        }
        if (!sawFence) return Pen.OPEN;
        return new Pen(weakest, seen.size(), doorOpen);
    }
}

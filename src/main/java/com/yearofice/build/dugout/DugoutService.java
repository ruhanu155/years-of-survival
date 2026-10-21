package com.yearofice.build.dugout;

import com.yearofice.build.multiblock.MultiblockData;
import com.yearofice.fire.FireRingBlock;
import com.yearofice.fire.stove.StoveBlock;
import com.yearofice.fire.stove.StoveBlockEntity;
import com.yearofice.fire.stove.ChimneyPipeBlock;
import com.yearofice.sim.build.Dugout;
import com.yearofice.worldgen.structure.PlacedMask;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Validates a dugout from a cell inside it and seals its shell (Every concept › Dugouts and rock shelters). Nothing is
 * dug here: the room is the air the player (or a template) left. A validated shell is written to the placed mask, so
 * terrain ops (frost heave, slumps, rockfall, drifts) never change a wall: that is "a dugout never yields".
 */
public final class DugoutService {
    private DugoutService() {}

    /** The measured room. {@code fault} is null for a valid dugout. */
    public record Result(Dugout.Kind kind, Set<BlockPos> cells, Set<BlockPos> shell, int wide, int high, int deep, int cover,
                         int earthFaces, int woodFaces, boolean hearth, boolean flue, String fault) {
        public boolean valid() { return fault == null; }
    }

    private static final int CAP = 61;

    public static Result validate(ServerLevel level, BlockPos start, Dugout.Kind kind) {
        Set<BlockPos> cells = new HashSet<>();
        ArrayDeque<BlockPos> work = new ArrayDeque<>();
        if (!open(level, start)) return fail(kind, cells, "no_room");
        cells.add(start.immutable());
        work.add(start.immutable());
        boolean leaks = false;
        while (!work.isEmpty() && !leaks) {
            BlockPos c = work.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (cells.contains(n) || !open(level, n)) continue;
                if (cells.size() >= CAP) { leaks = true; break; }
                cells.add(n);
                work.add(n);
            }
        }
        if (leaks) return fail(kind, cells, "open_shell");
        Set<BlockPos> shell = new HashSet<>();
        java.util.Map<BlockPos, Integer> faces = new java.util.HashMap<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos c : cells) {
            minX = Math.min(minX, c.getX()); maxX = Math.max(maxX, c.getX());
            minY = Math.min(minY, c.getY()); maxY = Math.max(maxY, c.getY());
            minZ = Math.min(minZ, c.getZ()); maxZ = Math.max(maxZ, c.getZ());
            for (Direction d : Direction.values()) {
                BlockPos n = c.relative(d);
                if (cells.contains(n)) continue;
                shell.add(n.immutable());
                faces.merge(n.immutable(), 1, Integer::sum);
            }
        }
        int dx = maxX - minX + 1, dz = maxZ - minZ + 1, high = maxY - minY + 1;
        int wide = Math.min(dx, dz), deep = Math.max(dx, dz);
        boolean hearth = false, flue = false, weak = false;
        int earth = 0, wood = 0;
        for (BlockPos p : shell) {
            BlockState s = level.getBlockState(p);
            int f = faces.get(p);
            if (s.getBlock() instanceof StoveBlock) {
                hearth = hearth || level.getBlockEntity(p) instanceof StoveBlockEntity be && be.lit();
                flue = flue || level.getBlockEntity(p) instanceof StoveBlockEntity be && be.lit() && be.flued();
                wood += f;
                continue;
            }
            if (s.getBlock() instanceof FireRingBlock) {
                hearth = hearth || s.getValue(FireRingBlock.BURN).light > 0;
                continue;
            }
            if (s.getBlock() instanceof ChimneyPipeBlock) { wood += f; continue; }
            if (!s.isCollisionShapeFullBlock(level, p)) { weak = true; continue; }
            if (isEarth(s)) earth += f; else wood += f;
        }
        BlockPos top = null;
        for (BlockPos c : cells) if (top == null || c.getY() > top.getY()) top = c;
        int cover = 0;
        for (int i = 2; i <= 4 && top != null; i++) if (level.getBlockState(top.above(i)).isCollisionShapeFullBlock(level, top.above(i))) cover++;
        String id = id(kind);
        if (wide > MultiblockData.num(id, "wide_max_m") || high > MultiblockData.num(id, "high_max_m") || deep > MultiblockData.num(id, "deep_max_m"))
            return new Result(kind, cells, shell, wide, high, deep, cover, earth, wood, hearth, flue, "too_big");
        String fault = weak ? "weak_shell" : Dugout.verdict(kind, wide, high, deep, cover >= MultiblockData.num(id, "cover_min_m") ? cover : 0, true, hearth, flue);
        return new Result(kind, cells, shell, wide, high, deep, cover, earth, wood, hearth, flue, fault);
    }

    /** Marks every shell block as placed, so no terrain op can change it. Returns the blocks marked. */
    public static int seal(ServerLevel level, Result r) {
        if (!r.valid()) return 0;
        PlacedMask mask = PlacedMask.get(level);
        for (BlockPos p : r.shell()) mask.mark(p);
        return r.shell().size();
    }

    static String id(Dugout.Kind k) {
        return switch (k) { case HILLSIDE -> "dugout_hillside"; case CUT_BANK -> "dugout_cut_bank"; case ROCK_SHELTER -> "dugout_rock_shelter"; };
    }

    private static Result fail(Dugout.Kind k, Set<BlockPos> cells, String why) {
        return new Result(k, cells, Set.of(), 0, 0, 0, 0, 0, 0, false, false, why);
    }

    private static boolean open(ServerLevel l, BlockPos p) {
        BlockState s = l.getBlockState(p);
        return s.getCollisionShape(l, p).isEmpty() && s.getFluidState().isEmpty();
    }

    private static boolean isEarth(BlockState s) {
        return s.is(net.minecraft.tags.BlockTags.DIRT) || s.is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD)
                || s.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL) || s.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE);
    }

    /** Interior temperature for the measured room, or the air when it is not a dugout. */
    public static double interiorC(Result r, double airC, double groundC, double stoveW) {
        return r.valid() ? Dugout.interiorC(airC, groundC, stoveW, r.earthFaces(), r.woodFaces()) : airC;
    }

}

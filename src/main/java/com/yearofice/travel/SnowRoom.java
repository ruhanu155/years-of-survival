package com.yearofice.travel;

import com.yearofice.sim.travel.Travel;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A closed snow room and its {@code air} field (research B10, B-quinzhee). Rooms land as a system in M8; until then the
 * room is a flood fill of air cells around a player's head (at most {@link #LIMIT} cells) whose walls are mostly
 * {@code #yearofice:snow_walls}. A cell open to the sky is a vent.
 */
public final class SnowRoom {
    private SnowRoom() {}

    public static final int LIMIT = 120;
    public static final double WALL_SHARE = 0.8;
    public static final TagKey<net.minecraft.world.level.block.Block> SNOW_WALLS = TagKey.create(
            net.minecraft.core.registries.Registries.BLOCK, snow("snow_walls"));

    private static net.minecraft.resources.ResourceLocation snow(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.yearofice.core.YearOfIce.MOD_ID, path);
    }

    public record Scan(boolean closed, int volume, boolean vented, double snowShare, int flames) {
        public boolean snowRoom() { return closed && snowShare >= WALL_SHARE; }
    }

    /** Open air straight up for 24 blocks (the light engine lags block edits; a heightmap read would too). */
    static boolean openAbove(ServerLevel level, BlockPos c) {
        for (int i = 1; i <= 24; i++) if (!level.getBlockState(c.above(i)).isAir()) return false;
        return true;
    }

    public static Scan scan(ServerLevel level, BlockPos head) {
        // an enclosed room the building system knows answers directly; open or oversized ground falls through to the local fill
        var room = com.yearofice.api.build.RoomService.get().roomAt(level, head);
        if (room != null) return new Scan(true, room.volumeM3(), !room.leaks().isEmpty() || openAbove(level, head), room.snowShare(), room.flames());
        if (!level.getBlockState(head).isAir()) return new Scan(false, 0, false, 0, 0);
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        q.add(head); seen.add(head);
        int walls = 0, snowWalls = 0, flames = 0;
        boolean vent = false;
        while (!q.isEmpty()) {
            BlockPos c = q.poll();
            if (seen.size() > LIMIT) return new Scan(false, seen.size(), false, 0, 0);
            boolean open = openAbove(level, c);
            if (open) vent = true;
            for (Direction d : Direction.values()) {
                if (open && d == Direction.UP) continue; // the sky is not part of the room
                BlockPos n = c.relative(d);
                BlockState s = level.getBlockState(n);
                if (s.isAir()) { if (seen.add(n)) q.add(n); continue; }
                if (s.is(BlockTags.FIRE) || (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT))) flames++;
                walls++;
                if (s.is(SNOW_WALLS)) snowWalls++;
            }
        }
        return new Scan(true, seen.size(), vent, walls == 0 ? 0 : snowWalls / (double) walls, flames);
    }

    /** The air after {@code minutes} for {@code people} breathing in the room around {@code head}; open rooms stay fresh. */
    public static double step(ServerLevel level, BlockPos head, double air, int minutes, int people) {
        Scan s = scan(level, head);
        if (!s.closed()) return 1.0;
        return Travel.air(air, minutes, people, s.flames(), s.vented(), s.volume());
    }

    /** The quinzhee verdict (a dug snow shelter): needs a snow room of 2 to 40 cells; a vent keeps it breathable. */
    public enum Quinzhee { NOT_A_SHELTER, STALE_UNTIL_VENTED, SAFE }

    public static Quinzhee quinzhee(ServerLevel level, BlockPos head) {
        Scan s = scan(level, head);
        if (!s.snowRoom() || s.volume() < 2 || s.volume() > 40) return Quinzhee.NOT_A_SHELTER;
        return s.vented() ? Quinzhee.SAFE : Quinzhee.STALE_UNTIL_VENTED;
    }
}

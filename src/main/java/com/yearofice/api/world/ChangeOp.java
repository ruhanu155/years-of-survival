package com.yearofice.api.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/**
 * One permanent terrain edit in the queue (Minecraft development › The terrain-change queue in full). An op never
 * touches another chunk: {@link #pos} decides the chunk. {@code block} is the registry id placed ({@code
 * minecraft:air} removes); {@code airOnly} limits it to air and snow. Canonical order is (day, kind, priority, x, z,
 * y, seq), so catch-up and live apply the same ops the same way.
 */
public record ChangeOp(long day, Kind kind, int priority, BlockPos pos, String block, boolean airOnly, int seq)
        implements Comparable<ChangeOp> {

    /** The producers' op kinds (a subset of the blueprint's list; more join as their lanes land). */
    public enum Kind { BURN, REGROW, TREE_DIE, FALL, DAM, POND, BREACH, STRIKE, WINDTHROW, ROCKFALL, AVALANCHE, EXPOSE, NODE,
        DRIFT, THERMOKARST, HEAVE, DECAY }

    public ChunkPos chunk() { return new ChunkPos(pos); }

    @Override
    public int compareTo(ChangeOp o) {
        int c = Long.compare(day, o.day);
        if (c == 0) c = kind.compareTo(o.kind);
        if (c == 0) c = Integer.compare(priority, o.priority);
        if (c == 0) c = Integer.compare(pos.getX(), o.pos.getX());
        if (c == 0) c = Integer.compare(pos.getZ(), o.pos.getZ());
        if (c == 0) c = Integer.compare(pos.getY(), o.pos.getY());
        return c != 0 ? c : Integer.compare(seq, o.seq);
    }
}

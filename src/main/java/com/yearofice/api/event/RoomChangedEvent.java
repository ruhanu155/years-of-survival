package com.yearofice.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/** Posted by {@code build.rooms} after a block change near a room has been rescanned; {@code seed} is any block of the affected area. */
public final class RoomChangedEvent extends Event {
    private final ServerLevel level;
    private final BlockPos seed;
    private final boolean enclosed;

    public RoomChangedEvent(ServerLevel level, BlockPos seed, boolean enclosed) { this.level = level; this.seed = seed; this.enclosed = enclosed; }

    public ServerLevel level() { return level; }
    public BlockPos seed() { return seed; }
    /** Whether the rescan found a closed room. */
    public boolean enclosed() { return enclosed; }
}

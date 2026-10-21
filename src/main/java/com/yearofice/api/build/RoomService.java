package com.yearofice.api.build;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/** Rooms (build.rooms): the enclosure at a point and its heat balance. Set by the building system at start-up. */
public interface RoomService {
    /** The room whose enclosed air contains {@code pos}, or null in the open. */
    @Nullable Room roomAt(Level level, BlockPos pos);

    /** True when {@code pos} is enclosed air (SnowRoom.scan's {@code air}). */
    default boolean airAt(Level level, BlockPos pos) { return roomAt(level, pos) != null; }

    Holder INSTANCE = new Holder();

    static RoomService get() { return INSTANCE; }

    final class Holder implements RoomService {
        private volatile RoomService delegate;
        public void set(RoomService d) { delegate = d; }
        @Override
        public @Nullable Room roomAt(Level level, BlockPos pos) { RoomService d = delegate; return d == null ? null : d.roomAt(level, pos); }
    }
}

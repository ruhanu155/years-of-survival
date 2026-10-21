package com.yearofice.api.build;

import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * An enclosed room as {@link RoomService} reports it. {@code uWPerK} is the total conductance (W/°C); {@code leaks}
 * are the openings (open doors, vents) on its boundary.
 */
public record Room(BlockPos seed, int volumeM3, double uWPerK, double feelsLikeC, double outsideC, double sourceW, List<BlockPos> leaks) {
    /** Lane D's SnowRoom.scan meaning: the room's enclosed air. */
    public boolean air() { return true; }
}

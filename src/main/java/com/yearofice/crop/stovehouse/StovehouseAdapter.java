package com.yearofice.crop.stovehouse;

import com.yearofice.api.build.Room;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.crop.Stovehouse;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The stovehouse as the garden sees it (Crop farming › Structures and buildables › Greenhouse): a crop that stands in
 * an enclosed room grows on the room's air, not the outside's, and lights through whatever roofs it. Heat comes from
 * the room service, so a stove, the walls and an open door all count. Only the live day is read: a day long past has no
 * room to ask, so a catch-up runs on the open air.
 */
public final class StovehouseAdapter {
    private StovehouseAdapter() {}

    public record Inside(double roomC, double units, double lowC, Stovehouse.Glazing glazing) {}

    /** What a crop at {@code cropPos} gets from its room for the day that ended, or null in the open (or for a day long past). */
    public static @Nullable Inside inside(ServerLevel level, BlockPos cropPos, long endedDay, double outsideLowC) {
        if (ClockService.get().absDay() - endedDay > 1) return null;
        Room room = RoomService.get().roomAt(level, cropPos);
        if (room == null) return null;
        double roomC = room.feelsLikeC();
        Stovehouse.Glazing g = glazing(level, cropPos);
        double daylight = ClockService.get().daylightHours(endedDay);
        return new Inside(roomC, Stovehouse.growthUnits(roomC, daylight, g), Stovehouse.nightLowC(roomC, outsideLowC), g);
    }

    /** The first thing above the crop that is not air: glass lights fully, a window frame as rawhide, anything else dimly. */
    public static Stovehouse.Glazing glazing(ServerLevel level, BlockPos cropPos) {
        for (int i = 1; i < 12; i++) {
            BlockState s = level.getBlockState(cropPos.above(i));
            if (s.isAir()) continue;
            String id = BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath();
            if (id.contains("glass") || id.contains("plexi")) return id.contains("plexi") ? Stovehouse.Glazing.PLEXIGLASS : Stovehouse.Glazing.GLASS;
            if (id.contains("window_frame")) return Stovehouse.Glazing.RAWHIDE;
            return Stovehouse.Glazing.DIM;
        }
        return Stovehouse.Glazing.GLASS;
    }
}

package com.yearofice.api.world;

import com.yearofice.sim.world.IceModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Ice on the water, per body class (Owner: {@code world.ice}). Thickness lives in {@code yearofice_ice}; blocks only
 * show it. Fishing digs holes through {@link #digHole}; a hole skins over by {@link IceModel#holeSkinMinutes}.
 */
public interface IceService {
    /** Thickness in cm of the ice at {@code pos} (0 when open water or not a water column). */
    double thicknessCm(ServerLevel level, BlockPos pos);

    /** What the ice at {@code pos} shows. */
    IceModel.Stage stage(ServerLevel level, BlockPos pos);

    /** Chisels an ice hole at {@code pos} (an ice block of the surface); false when there is no ice to cut. */
    boolean digHole(ServerLevel level, BlockPos pos);

    /** True while {@code pos} is an open ice hole. */
    boolean isOpenHole(ServerLevel level, BlockPos pos);

    /** Skins over every hole whose time is up at the current game minute; returns how many closed. */
    int tickHoles(ServerLevel level);

    static IceService get() {
        IceService s = Holder.service;
        if (s == null) throw new IllegalStateException("IceService not installed yet");
        return s;
    }

    static void install(IceService s) { Holder.service = s; }

    final class Holder {
        private static volatile IceService service;

        private Holder() {}
    }
}

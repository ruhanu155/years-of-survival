package com.yearofice.api.world;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Read access to stage 4–5's fishing spots (Waters and fish › Water bodies as places; RECONCILE 168): their type,
 * centre, radius, quality q (0–1), depth and starting stock per species. World generation implements it and
 * installs itself at mod construction; Food and hunting reads it through {@link #get()}. A level whose generator
 * is not ours has no spots.
 */
public interface FishSpotService {

    /** One spot. {@code species} and {@code stock} are parallel lists. */
    record Spot(String type, BlockPos centre, double radiusM, double quality, double depthM, int waterBodyId,
                List<String> species, List<Integer> stock, boolean major) {
        public boolean contains(BlockPos p) {
            double dx = p.getX() + 0.5 - (centre.getX() + 0.5), dz = p.getZ() + 0.5 - (centre.getZ() + 0.5);
            return dx * dx + dz * dz <= radiusM * radiusM;
        }
    }

    /** Spots whose area comes within {@code radius} blocks of {@code pos}, nearest first. */
    List<Spot> spotsNear(ServerLevel level, BlockPos pos, double radius);

    /** The spot whose area holds {@code pos}, if any. */
    default java.util.Optional<Spot> spotAt(ServerLevel level, BlockPos pos) {
        for (Spot s : spotsNear(level, pos, 0)) if (s.contains(pos)) return java.util.Optional.of(s);
        return java.util.Optional.empty();
    }

    /** The slice valley's guaranteed spot (a pool at least 2 × 2 blocks and 2 blocks deep), if this level has one. */
    java.util.Optional<Spot> sliceSpot(ServerLevel level);

    static FishSpotService get() { return Holder.INSTANCE; }

    static void install(FishSpotService s) { Holder.INSTANCE = s; }

    final class Holder {
        static volatile FishSpotService INSTANCE = new FishSpotService() {
            @Override
            public List<Spot> spotsNear(ServerLevel level, BlockPos pos, double radius) { return List.of(); }

            @Override
            public java.util.Optional<Spot> sliceSpot(ServerLevel level) { return java.util.Optional.empty(); }
        };

        private Holder() {}
    }
}

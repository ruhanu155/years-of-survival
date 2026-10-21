package com.yearofice.api.fire;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Stoves as heat sources for the room heat model (M8 lane A reads this; lane B's {@code fire.stove} writes it). One
 * entry per lit stove: its block, the watts it gives the room right now (by fuel, moisture and draft), and whether its
 * smoke leaves by a pipe. Rooms with an unflued stove fill with smoke.
 */
public interface StoveHeatLookup {
    record StoveHeat(BlockPos pos, double watts, boolean flued) {}

    /** Every lit stove within {@code radiusM} of the point. */
    List<StoveHeat> near(Level level, BlockPos pos, double radiusM);

    Holder INSTANCE = new Holder();

    final class Holder implements StoveHeatLookup {
        private volatile StoveHeatLookup delegate;

        public void set(StoveHeatLookup d) { delegate = d; }

        @Override
        public List<StoveHeat> near(Level level, BlockPos pos, double radiusM) {
            StoveHeatLookup d = delegate;
            return d == null ? List.of() : d.near(level, pos, radiusM);
        }
    }
}

package com.yearofice.api.build;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * What a stored thing sits in: the temperature at a container, cellar or cache and the decay rate it implies. The food
 * and decay lanes ask here instead of reading outside air; owner {@code build.store}. A position in a validated
 * cellar answers with the cellar's temperature; a store block answers with the air damped by its buffer; any other
 * position answers with the open air.
 */
public interface StoreService {
    /** Temperature the contents at {@code pos} feel, degrees C. */
    double tempC(Level level, BlockPos pos);

    /** Living-rot rate there relative to 5 C (the decay curve's reference): 0 frozen, 1 at 5 C. */
    double rate(Level level, BlockPos pos);

    /** 0 (rain-soaked rail) to 1 (sealed lid): how dry the contents stay. */
    double dryness(Level level, BlockPos pos);

    /** The live service; until the store system starts it answers with 5 C, rate 1, dry 0. */
    Holder INSTANCE = new Holder();

    final class Holder implements StoreService {
        private volatile StoreService delegate;

        public void set(StoreService d) { delegate = d; }

        @Override
        public double tempC(Level level, BlockPos pos) { StoreService d = delegate; return d == null ? 5 : d.tempC(level, pos); }

        @Override
        public double rate(Level level, BlockPos pos) { StoreService d = delegate; return d == null ? 1 : d.rate(level, pos); }

        @Override
        public double dryness(Level level, BlockPos pos) { StoreService d = delegate; return d == null ? 0 : d.dryness(level, pos); }
    }
}

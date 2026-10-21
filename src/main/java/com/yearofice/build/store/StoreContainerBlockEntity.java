package com.yearofice.build.store;

import com.yearofice.food.preserve.StoreBlockEntity;
import com.yearofice.sim.build.CacheLegs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A crate, barrel, shelf, hanging bar or cache-leg platform: holds food by weight up to its data row's capacity
 * ({@link StoreData}); temperature and dryness are asked through {@code api.build.StoreService}. On a cache platform it
 * also answers who can climb to it.
 */
public class StoreContainerBlockEntity extends StoreBlockEntity {
    private double heightPin = Double.NaN;
    private CacheLegs.Leg legPin;

    public StoreContainerBlockEntity(BlockPos pos, BlockState state) { super(StoreRegistry.CONTAINER_ENTITY.get(), pos, state); }

    @Override
    public double capacityKg() {
        StoreData d = StoreData.of(getBlockState());
        if (d == null) throw new IllegalStateException(getBlockState().getBlock() + " has no yearofice:store row");
        return d.capacityKg();
    }

    /** GameTests pin the platform height and leg dressing; real play measures the poles below. */
    public void pinLegs(double heightM, CacheLegs.Leg leg) { heightPin = heightM; legPin = leg; }

    /** Metres from the ground to this platform: the count of cache poles standing under it. */
    public double heightM() {
        if (!Double.isNaN(heightPin)) return heightPin;
        if (level == null) return 0;
        int n = 0;
        BlockPos p = worldPosition.below();
        while (level.getBlockState(p).is(StoreRegistry.CACHE_POLE.get()) && n < 16) { n++; p = p.below(); }
        return n;
    }

    public CacheLegs.Leg leg() {
        if (legPin != null) return legPin;
        if (level == null) return CacheLegs.Leg.BARKED;
        BlockPos p = worldPosition.below();
        BlockState s = level.getBlockState(p);
        if (!s.is(StoreRegistry.CACHE_POLE.get())) return CacheLegs.Leg.BARKED;
        return s.getValue(CachePoleBlock.LEG).leg;
    }

    /** Chance 0-1 the species gets onto this platform; a non-cache container is reachable by all. */
    public double reach(String species) {
        if (!getBlockState().is(StoreRegistry.CACHE_LEGS.get())) return 1;
        return CacheLegs.reach(species, leg(), heightM());
    }
}

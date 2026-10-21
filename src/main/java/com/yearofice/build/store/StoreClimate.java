package com.yearofice.build.store;

import com.yearofice.api.build.CacheAccess;
import com.yearofice.api.build.StoreService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.build.cellar.CellarBlockEntity;
import com.yearofice.build.cellar.CellarIndex;
import com.yearofice.sim.build.Cellars;
import com.yearofice.sim.decay.ColdCurve;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * {@link StoreService} and {@link CacheAccess}: a cellar answers with its room's temperature, a store block with the
 * air smoothed by its buffer, anything else with the open air. The food lane multiplies its spoil rate by {@code rate}.
 */
public final class StoreClimate implements StoreService, CacheAccess {
    public static final StoreClimate INSTANCE = new StoreClimate();

    private StoreClimate() {}

    @Override
    public double tempC(Level level, BlockPos pos) {
        CellarBlockEntity cellar = CellarIndex.at(level, pos);
        if (cellar != null) return cellar.tempC();
        if (!(level instanceof ServerLevel sl)) return 5;
        double air = EnvService.get().airC(sl, pos);
        StoreData d = StoreData.of(level.getBlockState(pos));
        if (d == null) return air;
        double mean = EnvService.get().day(sl, ClockService.get().absDay()).meanC();
        return Cellars.containerC(air, mean, d.bufferPct() / 100.0);
    }

    @Override
    public double rate(Level level, BlockPos pos) { return ColdCurve.STANDARD.rate(tempC(level, pos)); }

    @Override
    public double dryness(Level level, BlockPos pos) {
        StoreData d = StoreData.of(level.getBlockState(pos));
        if (CellarIndex.at(level, pos) != null) return 1;
        return d == null ? 0 : d.drynessPct() / 100.0;
    }

    @Override
    public double reach(Level level, BlockPos pos, String species) {
        return level.getBlockEntity(pos) instanceof StoreContainerBlockEntity be ? be.reach(species) : 1;
    }
}

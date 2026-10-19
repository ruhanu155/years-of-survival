package com.yearofice.fire;

import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.FireWeather;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * The weather at a fire, until {@code EnvService.sample} lands: vanilla rain and thunder, the biome's
 * precipitation type, sky access for cover, and the wind placeholders in {@code [fire]}. {@code /yoi fire weather}
 * sets a per-level override for testing; a ring can carry its own override (GameTests).
 */
public final class FireEnv {
    private FireEnv() {}

    private static final Map<ResourceKey<Level>, FireWeather> LEVEL_OVERRIDE = new HashMap<>();

    public static void setOverride(Level level, FireWeather w) {
        if (w == null) LEVEL_OVERRIDE.remove(level.dimension());
        else LEVEL_OVERRIDE.put(level.dimension(), w);
    }

    public static FireWeather override(Level level) { return LEVEL_OVERRIDE.get(level.dimension()); }

    static void clear() { LEVEL_OVERRIDE.clear(); }

    /** True when something solid is overhead: a roof, a lean-to or a tarp cancels rain on the fire. */
    public static boolean covered(Level level, BlockPos pos) { return !level.canSeeSky(pos.above()); }

    public static FireWeather sample(Level level, BlockPos pos, FireWeather ringOverride) {
        if (ringOverride != null) return ringOverride;
        FireWeather lo = LEVEL_OVERRIDE.get(level.dimension());
        if (lo != null) return new FireWeather(lo.precip(), lo.heavy(), lo.windMs(), lo.covered() || covered(level, pos));
        boolean covered = covered(level, pos);
        Precip precip = Precip.NONE;
        if (level.isRaining()) {
            Biome.Precipitation p = level.getBiome(pos).value().getPrecipitationAt(pos);
            precip = switch (p) {
                case RAIN -> Precip.RAIN;
                case SNOW -> Precip.SNOW;
                case NONE -> Precip.NONE;
            };
        }
        boolean heavy = level.isThundering();
        double wind = heavy ? FireConfig.STORM_WIND_MS.get() : level.isRaining() ? FireConfig.RAIN_WIND_MS.get() : FireConfig.CALM_WIND_MS.get();
        return new FireWeather(precip, heavy, wind, covered);
    }
}

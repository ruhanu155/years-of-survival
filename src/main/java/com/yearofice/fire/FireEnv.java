package com.yearofice.fire;

import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.FireWeather;
import com.yearofice.api.env.EnvService;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

/**
 * The weather at a fire, from {@code EnvService} (M2): precipitation, heaviness and wind, plus sky access for
 * cover. The {@code [fire]} wind placeholders are no longer read. {@code /yoi fire weather}
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

    /**
     * The weather at a fire from the Environment field: the valley hour's precipitation and whether it is heavy,
     * and the wind at the ring (ridge exposure and canopy included). A roof over the ring covers it.
     */
    public static FireWeather sample(Level level, BlockPos pos, FireWeather ringOverride) {
        if (ringOverride != null) return ringOverride;
        FireWeather lo = LEVEL_OVERRIDE.get(level.dimension());
        if (lo != null) return new FireWeather(lo.precip(), lo.heavy(), lo.windMs(), lo.covered() || covered(level, pos));
        boolean covered = covered(level, pos);
        if (!(level instanceof ServerLevel sl) || !EnvService.installed()) return new FireWeather(Precip.NONE, false, 0, covered);
        EnvService env = EnvService.get();
        return new FireWeather(env.precip(sl, pos), env.heavy(sl), env.wind(sl, pos).ms(), covered);
    }
}

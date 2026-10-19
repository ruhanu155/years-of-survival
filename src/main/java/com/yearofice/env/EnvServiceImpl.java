package com.yearofice.env;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.Climate;
import com.yearofice.sim.weather.WeatherModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * {@code env}: the Environment field for M2 (Layer 2). The valley floor's hour comes from {@link WeatherModel}
 * (normals plus the seeded daily draw); a sample then applies the place rules that need no other system:
 *
 * <ul>
 *   <li>Air: the lapse from the valley floor (y 150), or the winter inversion on clear calm nights.</li>
 *   <li>Wind: ridge exposure by height, and ×0.3 under a canopy (leaves above the position in the column).</li>
 *   <li>Water: creek, river or lake from the 5-day mean air.</li>
 *   <li>Light: vanilla sky and block light at the position (held light is {@code env.light}, M3).</li>
 * </ul>
 *
 * Nothing is ticked: a sample is a few lookups into the cached year table, so it costs well under a microsecond
 * once the year is drawn. A per-level {@link EnvService.WeatherOverride} (debug and tests) replaces the valley's air, wind
 * or precipitation before the place rules.
 */
public final class EnvServiceImpl implements EnvService {
    private final Map<Long, WeatherModel> models = new ConcurrentHashMap<>();
    private final Map<ResourceKey<Level>, WeatherOverride> overrides = new ConcurrentHashMap<>();
    /** Fixed first days by world seed, kept across a climate reload. */
    private final Map<Long, WeatherModel.FixedDay> firstDays = new ConcurrentHashMap<>();

    WeatherModel model(ServerLevel level) {
        Climate c = EnvResources.current;
        WeatherModel m = models.get(level.getSeed());
        if (m == null || m.climate() != c) {
            m = new WeatherModel(c, level.getSeed());
            m.fixFirstDay(firstDays.get(level.getSeed()));
            models.put(level.getSeed(), m);
        }
        return m;
    }

    void clear() {
        models.clear();
        overrides.clear();
        firstDays.clear();
    }

    private static long now() { return ClockService.installed() ? ClockService.get().nowMinute() : 0; }

    /** The valley hour at {@code minute} with this level's override applied. */
    WeatherModel.Hour hourAt(ServerLevel level, long minute) {
        WeatherModel.Hour h = model(level).hour(minute);
        WeatherOverride o = overrides.get(level.dimension());
        if (o == null) return h;
        double air = Double.isNaN(o.airC()) ? h.airC() : o.airC();
        double wind = Double.isNaN(o.windMs()) ? h.windMs() : o.windMs();
        double gust = Double.isNaN(o.windMs()) ? h.gustMs() : o.windMs() * 1.4;
        Precip p = o.precip() == null ? h.precip() : o.precip();
        double mm = o.precip() == null ? h.precipMmH() : Double.isNaN(o.precipMmH()) ? (p == Precip.NONE ? 0 : 2.5) : o.precipMmH();
        return new WeatherModel.Hour(air, wind, gust, h.windDirDeg(), p, mm, h.cloudPct(), p == Precip.NONE ? h.humidityPct() : 100,
                h.visibilityM(), h.pressureHpa(), h.state(), h.snowDepthCm());
    }

    private EnvSample at(ServerLevel level, double x, double y, double z, long minute, BlockPos blocks) {
        WeatherModel m = model(level);
        WeatherModel.Hour h = hourAt(level, minute);
        long day = Calendar.absDayOfMinute(minute);
        int doy = Calendar.dayOfYearOfDay(day);
        int mod = (int) Math.floorMod(minute, (long) Calendar.MINUTES_PER_DAY);
        double sun = ClockService.installed() ? ClockService.get().sun().elevation(day, mod) : 0;
        boolean overrideAir = overrides.containsKey(level.dimension()) && !Double.isNaN(overrides.get(level.dimension()).airC());
        double air = overrideAir ? h.airC() : m.airAtHeight(h.airC(), y, m.inversion(h, doy, sun));
        boolean canopy = blocks != null && canopy(level, blocks);
        double factor = m.windAtPlace(1, y, canopy);
        boolean overrideWind = overrides.containsKey(level.dimension()) && !Double.isNaN(overrides.get(level.dimension()).windMs());
        double wind = overrideWind ? h.windMs() : h.windMs() * factor;
        double gust = overrideWind ? h.gustMs() : h.gustMs() * factor;
        int light = blocks != null && level.isLoaded(blocks) ? level.getMaxLocalRawBrightness(blocks) : sun > 0 ? 15 : 0;
        return new EnvSample(air, EnvSample.envFeelsLike(air, wind), m.groundC(doy), m.creekC(day), 30, h.windDirDeg(),
                wind, gust, h.snowDepthCm(), false, 0, h.precip(), h.precipMmH(), h.visibilityM(), light, 0, 0, 0,
                h.cloudPct(), h.pressureHpa(), 0, h.humidityPct(), "", 0, 0, 0);
    }

    /** Leaves above the position in its column: the motion-blocking height counts leaves, the other does not. */
    static boolean canopy(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        int withLeaves = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        int without = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return withLeaves > without && withLeaves > pos.getY() + 1;
    }

    // ---- the service ------------------------------------------------------------------------------------

    @Override
    public EnvSample sample(ServerLevel level, BlockPos pos) {
        return at(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, now(), pos);
    }

    @Override
    public EnvSample sampleAbstract(ServerLevel level, double x, double y, double z, long minute) {
        return at(level, x, y, z, minute, null);
    }

    @Override public double airC(ServerLevel level, BlockPos pos) { return sample(level, pos).airC(); }

    @Override public double feelsLikeC(ServerLevel level, BlockPos pos) { return sample(level, pos).feelsLikeC(); }

    @Override
    public double groundC(ServerLevel level, BlockPos pos, double depthM) {
        return model(level).groundC(Calendar.dayOfYearOfDay(Calendar.absDayOfMinute(now())));
    }

    @Override
    public double waterC(ServerLevel level, BlockPos pos, WaterKind kind) { return waterCAt(level, kind, now()); }

    @Override
    public double waterCAt(ServerLevel level, WaterKind kind, long minute) {
        WeatherModel m = model(level);
        long day = Calendar.absDayOfMinute(minute);
        return switch (kind) {
            case RIVER -> m.riverC(day);
            case CREEK -> m.creekC(day);
            case LAKE -> m.lakeC(day);
        };
    }

    @Override
    public Wind wind(ServerLevel level, BlockPos pos) {
        EnvSample s = sample(level, pos);
        return new Wind(s.windDirDeg(), s.windMs(), s.gustMs());
    }

    @Override public Precip precip(ServerLevel level, BlockPos pos) { return hourAt(level, now()).precip(); }

    @Override public double snowCm(ServerLevel level, BlockPos pos) { return hourAt(level, now()).snowDepthCm(); }

    @Override public double visibilityM(ServerLevel level, BlockPos pos) { return hourAt(level, now()).visibilityM(); }

    @Override public int light(ServerLevel level, BlockPos pos) { return sample(level, pos).light(); }

    @Override public WeatherModel.Hour valley(ServerLevel level) { return hourAt(level, now()); }

    @Override public WeatherModel.Day day(ServerLevel level, long absDay) { return model(level).day(absDay); }

    @Override
    public List<HourAir> hourlySeries(ServerLevel level, BlockPos pos, long fromDs, long toDs) {
        List<HourAir> out = new ArrayList<>();
        long from = Math.floorDiv(Calendar.minute(fromDs), 60L) * 60, to = Calendar.minute(toDs);
        for (long m = from; m <= to; m += 60) {
            EnvSample s = sampleAbstract(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, m);
            out.add(new HourAir(m, s.airC(), s.feelsLikeC(), s.windMs()));
        }
        return out;
    }

    @Override
    public void setOverride(ServerLevel level, WeatherOverride override) {
        if (override == null) overrides.remove(level.dimension());
        else overrides.put(level.dimension(), override);
    }

    @Override public WeatherOverride override(ServerLevel level) { return overrides.get(level.dimension()); }

    @Override
    public void fixFirstDay(ServerLevel level, WeatherModel.FixedDay day) {
        if (day == null) firstDays.remove(level.getSeed());
        else firstDays.put(level.getSeed(), day);
        model(level).fixFirstDay(day);
    }

    @Override
    public boolean heavy(ServerLevel level) { return model(level).heavy(valley(level)); }
}

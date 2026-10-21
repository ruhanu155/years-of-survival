package com.yearofice.env.weather;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.event.WeatherChangedEvent;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.api.weather.WeatherState;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.DayState;
import com.yearofice.sim.weather.DecadeStorm;
import com.yearofice.sim.weather.StormDef;
import com.yearofice.sim.weather.StormJson;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.sim.weather.StormMath;
import com.yearofice.sim.weather.StormTable;
import com.yearofice.sim.weather.WeatherModel;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Storm weather on top of the env layer (Weather and storms): the year's storm table (a Markov chain from {@code weather/storm},
 * {@code weather/ring}, the decade storm and any forced storms), the {@link WeatherService}, the overlay that
 * {@code EnvServiceImpl} applies to the valley's hour (so wind chill, visibility, precipitation and the hourly closed forms
 * all see the storm), and the per-minute jobs: the changed event, strikes, glaze and hail.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class WeatherSystem {
    private WeatherSystem() {}

    /** Everything loaded from data. */
    public record Loaded(Map<String, StormDef> defs, StormJson.Ring ring, StormJson.Forecast forecast, DecadeStorm decade) {}

    static final String[] STORM_IDS = {"snowstorm", "blizzard", "freezing_rain", "hail", "thunderstorm", "long_rain"};
    static volatile Loaded loaded = fromJar();
    private static final Map<String, StormTable> TABLES = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, WeatherState> LAST = new ConcurrentHashMap<>();
    private static final Map<ResourceKey<Level>, long[]> OPEN = new ConcurrentHashMap<>(); // start, peak(hours) of the storm being logged
    static final Service SERVICE = new Service();

    public static void init(IEventBus modBus) {
        WeatherRegistry.register(modBus);
        WeatherService.install(SERVICE);
        modBus.addListener((RegisterPayloadHandlersEvent e) -> e.registrar("1").playToClient(StrikePayload.TYPE, StrikePayload.CODEC,
                (p, ctx) -> ctx.enqueueWork(() -> com.yearofice.client.weather.StrikeFx.on(p))));
    }

    // ---- data -----------------------------------------------------------------------------------------

    private static ResourceLocation res(String path) { return ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, path); }

    static JsonObject jar(String path) {
        try (var in = WeatherSystem.class.getResourceAsStream("/data/yearofice/" + path)) {
            if (in == null) throw new IllegalStateException("missing " + path);
            return JsonParser.parseReader(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static Loaded fromJar() {
        Map<String, StormDef> defs = new HashMap<>();
        for (String id : STORM_IDS) defs.put(id, StormJson.storm(jar("weather/storm/" + id + ".json")));
        return new Loaded(Map.copyOf(defs), StormJson.ring(jar("weather/ring/valley_floor.json")), StormJson.forecast(jar("weather/forecast.json")),
                DecadeStorm.parse(jar("event/decade_storm.json")));
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent e) {
        e.addListener(new SimplePreparableReloadListener<Loaded>() {
            @Override
            protected Loaded prepare(ResourceManager rm, ProfilerFiller p) {
                try {
                    Map<String, StormDef> defs = new HashMap<>();
                    for (var en : rm.listResources("weather/storm", l -> l.getPath().endsWith(".json")).entrySet())
                        try (Reader r = en.getValue().openAsReader()) {
                            StormDef d = StormJson.storm(JsonParser.parseReader(r).getAsJsonObject());
                            defs.put(d.id(), d);
                        }
                    if (defs.isEmpty()) return loaded;
                    StormJson.Ring ring = loaded.ring();
                    var rr = rm.getResource(res("weather/ring/valley_floor.json"));
                    if (rr.isPresent()) try (Reader r = rr.get().openAsReader()) { ring = StormJson.ring(JsonParser.parseReader(r).getAsJsonObject()); }
                    StormJson.Forecast fc = loaded.forecast();
                    var fr = rm.getResource(res("weather/forecast.json"));
                    if (fr.isPresent()) try (Reader r = fr.get().openAsReader()) { fc = StormJson.forecast(JsonParser.parseReader(r).getAsJsonObject()); }
                    DecadeStorm dec = loaded.decade();
                    var dr = rm.getResource(res("event/decade_storm.json"));
                    if (dr.isPresent()) try (Reader r = dr.get().openAsReader()) { dec = DecadeStorm.parse(JsonParser.parseReader(r).getAsJsonObject()); }
                    return new Loaded(Map.copyOf(defs), ring, fc, dec);
                } catch (Exception ex) {
                    YearOfIce.LOG.error("weather data failed to load; keeping the previous", ex);
                    return loaded;
                }
            }

            @Override
            protected void apply(Loaded l, ResourceManager rm, ProfilerFiller p) {
                loaded = l;
                TABLES.clear();
            }
        });
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        WeatherData.forget();
        TABLES.clear();
        LAST.clear();
        OPEN.clear();
    }

    // ---- the table ------------------------------------------------------------------------------------

    /** The storm table of {@code year} for this level's seed and saved forced storms. */
    public static StormTable table(ServerLevel level, int year) {
        WeatherData d = WeatherData.get(level);
        WeatherConfig.Tuning t = WeatherConfig.tuning();
        Loaded l = loaded;
        String key = level.getSeed() + ":" + year + ":" + d.version() + ":" + System.identityHashCode(l) + ":" + t.stormFrequency() + t.decadeStorm();
        StormTable cached = TABLES.get(key);
        if (cached != null) return cached;
        if (TABLES.size() > 12) TABLES.clear();
        long seed = level.getSeed();
        StormTable built = StormTable.build(seed, year, l.defs().values(), l.ring(), t.stormFrequency(),
                day -> EnvService.get().day(level, day).meanC(), t.decadeStorm() ? l.decade() : null, d.forced());
        TABLES.put(key, built);
        return built;
    }

    public static StormTable.Event eventAt(ServerLevel level, long minute) {
        long hour = Math.floorDiv(minute, 60L);
        return table(level, Calendar.yearOfDay(Math.floorDiv(hour, 24L))).at(hour);
    }

    /** Lays a storm on the table from {@code startHour}; it persists in {@code yearofice_weather}. */
    public static StormTable.Event force(ServerLevel level, String id, long startHour, int hours, double intensity) {
        StormDef def = loaded.defs().get(id);
        if (def == null) throw new IllegalArgumentException("no storm " + id);
        StormTable.Event e = new StormTable.Event(id, def.kind(), startHour, hours, intensity, true);
        WeatherData.get(level).force(e);
        TABLES.clear();
        return e;
    }

    // ---- the overlay on the valley hour ----------------------------------------------------------------

    /** Applies the storm of {@code minute} to the valley's hour: wind, gust, precipitation, visibility, cloud and pressure. */
    public static WeatherModel.Hour overlay(ServerLevel level, long minute, WeatherModel.Hour h) {
        if (level.getServer() == null || !level.dimensionType().hasSkyLight()) return h;
        StormTable.Event e = eventAt(level, minute);
        if (e == null) return h;
        StormDef def = loaded.defs().get(e.id());
        if (def == null) return h;
        WeatherConfig.Tuning t = WeatherConfig.tuning();
        StormMath.Live l = StormMath.live(def, e, minute, t.stormSeverity(), t.whiteoutSightM());
        Precip p = switch (def.precip()) {
            case RAIN -> Precip.RAIN;
            case SNOW -> Precip.SNOW;
            case GLAZE -> Precip.FREEZING_RAIN;
            case HAIL -> Precip.HAIL;
            default -> h.precip();
        };
        double air = def.kind() == StormKind.FREEZING_RAIN ? Math.max(-6, Math.min(-0.5, h.airC())) : h.airC();
        return new WeatherModel.Hour(air, Math.max(h.windMs(), l.windMs()), Math.max(h.gustMs(), l.gustMs()), h.windDirDeg(), p,
                Math.max(h.precipMmH(), l.precipMmH()), 100, 100, Math.min(h.visibilityM(), l.visibilityM()),
                h.pressureHpa() - 20 * l.level(), DayState.SEVERE, h.snowDepthCm());
    }

    // ---- the service ----------------------------------------------------------------------------------

    static StormKind.PrecipKind precipKind(Precip p) {
        return switch (p) {
            case RAIN, DRIZZLE -> StormKind.PrecipKind.RAIN;
            case SNOW, MIXED -> StormKind.PrecipKind.SNOW;
            case FREEZING_RAIN -> StormKind.PrecipKind.GLAZE;
            case HAIL -> StormKind.PrecipKind.HAIL;
            case NONE -> StormKind.PrecipKind.NONE;
        };
    }

    static final class Service implements WeatherService {
        // ponytail: the valley's weather everywhere; benches and rings differ in the table's ring multipliers only — upgrade: per-position ring
        @Override
        public WeatherState current(ServerLevel level, BlockPos pos) { return at(level, pos, ClockService.get().nowMinute()); }

        @Override
        public WeatherState at(ServerLevel level, BlockPos pos, long minute) {
            WeatherModel.Hour h = EnvService.get().valleyAt(level, minute);
            StormTable.Event e = level.dimensionType().hasSkyLight() ? eventAt(level, minute) : null;
            double level01 = 0;
            StormKind kind = StormKind.CLEAR;
            if (e != null && loaded.defs().get(e.id()) != null) {
                WeatherConfig.Tuning t = WeatherConfig.tuning();
                level01 = StormMath.live(loaded.defs().get(e.id()), e, minute, t.stormSeverity(), t.whiteoutSightM()).level();
                kind = e.kind();
            }
            return new WeatherState(kind, level01, h.windMs(), h.windDirDeg(), h.precipMmH(), precipKind(h.precip()), h.visibilityM());
        }
    }

    // ---- per game minute --------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onMinute(ClockEvent.Minute ev) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !EnvService.installed()) return;
        long now = ClockService.get().nowMinute();
        for (ServerLevel level : server.getAllLevels()) {
            if (!level.dimensionType().hasSkyLight()) continue;
            tickLevel(level, now);
        }
    }

    static void tickLevel(ServerLevel level, long now) {
        WeatherState s = SERVICE.at(level, BlockPos.ZERO, now);
        WeatherState was = LAST.put(level.dimension(), s);
        if (was != null && (was.kind() != s.kind() || Math.round(was.intensity() * 4) != Math.round(s.intensity() * 4)
                || was.precipKind() != s.precipKind())) NeoForge.EVENT_BUS.post(new WeatherChangedEvent(level, was, s));
        logStorm(level, was, s, now);
        StormTable.Event e = eventAt(level, now);
        StormDef def = e == null ? null : loaded.defs().get(e.id());
        for (ServerPlayer p : level.players()) {
            if (s.kind() == StormKind.THUNDERSTORM && def != null) {
                Lightning.minute(level, p, def.strikeEveryMin() / Math.max(0.5, WeatherConfig.tuning().stormSeverity()), now);
            }
            sheets(level, p.blockPosition(), e, def, now, 24, 48);
        }
    }

    /** One minute of glaze or hail near {@code c}: lay what the storm has accumulated, melt what no storm is feeding. */
    static void sheets(ServerLevel level, BlockPos c, StormTable.Event e, StormDef def, long now, int r, int n) {
        WeatherConfig.Tuning t = WeatherConfig.tuning();
        boolean glazing = e != null && e.kind() == StormKind.FREEZING_RAIN && def != null;
        boolean hailing = e != null && e.kind() == StormKind.HAIL && def != null;
        if (glazing) {
            double hoursIn = (now - e.startHour() * 60) / 60.0;
            int layers = StormMath.glazeLayers(StormMath.glazeAfter(def, e, hoursIn, t.stormSeverity(), false));
            if (layers > 0) GlazePass.place(level, c, r, n, now, WeatherRegistry.GLAZE_LAYER.get(), layers, true);
        } else if (EnvService.get().airC(level, c) > 0) {
            GlazePass.melt(level, c, r, n, now, WeatherRegistry.GLAZE_LAYER.get(), 1.0 / 600); // 0.5 mm an hour: a layer in 10 h
        }
        if (hailing) {
            double cm = 3 * StormMath.live(def, e, now, t.stormSeverity(), 0).level();
            int layers = StormMath.hailLayers(cm);
            if (layers > 0) GlazePass.place(level, c, r, n, now + 7, WeatherRegistry.HAIL_LAYER.get(), layers, false);
        } else {
            GlazePass.melt(level, c, r, n, now + 7, WeatherRegistry.HAIL_LAYER.get(), 1.0 / 120); // gone in 1-3 hours
        }
    }

    private static void logStorm(ServerLevel level, WeatherState was, WeatherState s, long now) {
        long[] open = OPEN.get(level.dimension());
        if (s.kind() != StormKind.CLEAR && open == null) OPEN.put(level.dimension(), new long[] {now, now, s.kind().ordinal()});
        else if (s.kind() != StormKind.CLEAR) {
            if (s.intensity() >= was.intensity()) open[1] = now;
        } else if (open != null) {
            OPEN.remove(level.dimension());
            WeatherData.get(level).log(new WeatherData.Log(StormKind.values()[(int) open[2]].id(), open[0], open[1], now));
        }
    }

    /** For tests: forget the cached state. */
    static void reset() {
        TABLES.clear();
        LAST.clear();
        OPEN.clear();
    }
}

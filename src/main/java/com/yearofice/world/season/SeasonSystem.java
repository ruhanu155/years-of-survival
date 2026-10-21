package com.yearofice.world.season;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.world.IceService;
import com.yearofice.api.world.WorldEditService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.WeatherModel;
import com.yearofice.world.WorldHooks;
import com.yearofice.world.ice.FloodPass;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.world.ice.IcePass;
import com.yearofice.world.ice.IceServiceImpl;
import com.yearofice.world.snow.SnowPass;
import com.yearofice.world.terrain.TerrainApply;
import com.yearofice.world.terrain.TerrainChangeQueue;
import com.yearofice.world.terrain.WorldEdits;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Seasons, freeze-up, snow, ice and the terrain-change queue (M7 lane B). One writer of {@link SeasonState}: it
 * steps the {@link com.yearofice.sim.world.SeasonCore} a day at a time from the valley's weather, then settles
 * chunks: loaded ones near players a few a tick (budget), others in {@link #catchUp} when they load. Only levels
 * whose generator is ours are touched (the GameTest server's flat overworld is left alone).
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class SeasonSystem {
    private SeasonSystem() {}

    /** Where the day's weather comes from; tests swap it. */
    public interface DayWeather { WeatherModel.Day day(ServerLevel level, long absDay); }

    private static volatile DayWeather source = (l, d) -> EnvService.get().day(l, d);

    public static void setWeatherSource(DayWeather w) { source = w == null ? (l, d) -> EnvService.get().day(l, d) : w; }

    /** Storm water per day {snowMm, rainMm} from the weather table; tests swap it. */
    private static volatile java.util.function.BiFunction<ServerLevel, Long, double[]> stormSource = com.yearofice.env.weather.WeatherSystem::dayPrecip;

    public static void setStormSource(java.util.function.BiFunction<ServerLevel, Long, double[]> f) {
        stormSource = f == null ? com.yearofice.env.weather.WeatherSystem::dayPrecip : f;
    }

    private static final Map<ServerLevel, ArrayDeque<ChunkPos>> LIVE = new HashMap<>();
    private static final Map<ServerLevel, ArrayDeque<ChunkPos>> DEFERRED = new HashMap<>();
    private static int loadsThisTick;
    /** Longest single settle (ns) seen since the last reset: the budget test reads it. */
    public static volatile long slowestNs;

    public static void init(IEventBus modBus) {
        IceBlocks.BLOCKS.register(modBus);
        IceService.install(new IceServiceImpl());
        WorldEditService.install(new WorldEdits());
    }

    // ---- the day -----------------------------------------------------------------------------------------------

    /** Steps the season state to {@code today}, replaying every missed day in order. Returns days stepped. */
    public static int advanceTo(ServerLevel any, long today) {
        ServerLevel ow = any.getServer().overworld();
        SeasonState st = SeasonState.get(ow);
        var core = st.core;
        long from = core.lastDay == Long.MIN_VALUE ? 0 : core.lastDay + 1;
        int n = 0;
        var snow = SeasonConfig.snow();
        var ice = SeasonConfig.ice();
        for (long d = from; d <= today; d++) {
            WeatherModel.Day w = source.day(ow, d);
            double[] storm = stormSource.apply(ow, d);
            core.step(d, Calendar.dayOfYearOfDay(d), w.meanC(), w.precipMm(), w.windMs(), storm[0], storm[1], snow, ice);
            n++;
        }
        if (n > 0) st.setDirty();
        return n;
    }

    /** Settles one chunk: ice, flood, snow, in that order; idempotent. Returns blocks written. */
    public static int settle(ServerLevel level, LevelChunk chunk, boolean live, long today) {
        return settle(level, chunk, live, today, SeasonState.get(level).core, TerrainChangeQueue.get(level));
    }

    /** As {@link #settle(ServerLevel, LevelChunk, boolean, long)} against explicit state (tests keep their own). */
    public static int settle(ServerLevel level, LevelChunk chunk, boolean live, long today, com.yearofice.sim.world.SeasonCore core,
                             TerrainChangeQueue q) {
        long t0 = System.nanoTime();
        PassContext c = new PassContext(level, chunk, live, core, SeasonConfig.snow(), SeasonConfig.ice(), q.floodFlags(chunk.getPos()));
        IcePass.apply(c);
        List<Long> flags = FloodPass.apply(c);
        SnowPass.apply(c);
        if (!flags.equals(q.floodFlags(chunk.getPos()))) q.setFloodFlags(chunk.getPos(), flags);
        q.setStamp(chunk.getPos(), today);
        if (c.writes > 0) chunk.setUnsaved(true);
        slowestNs = Math.max(slowestNs, System.nanoTime() - t0);
        return c.writes;
    }

    /** Catch-up on chunk load: due ops (oldest first), then the passes, written through the chunk. */
    public static void catchUp(ServerLevel level, LevelChunk chunk) {
        long today = ClockService.get().absDay();
        advanceTo(level, today);
        TerrainChangeQueue q = TerrainChangeQueue.get(level);
        TerrainApply.apply(level, chunk, q, today, false, Integer.MAX_VALUE);
        settle(level, chunk, false, today);
    }

    // ---- events ------------------------------------------------------------------------------------------------

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
        if (WorldHooks.world(level) == null || !EnvService.installed()) return;
        loadOrDefer(level, chunk);
    }

    /**
     * A chunk loaded: caught up now, or, past {@code catchUpChunksPerTick} loads in this tick, left for
     * {@link #drainDeferred} (live writes, a few a tick). Returns true when deferred.
     * ponytail: a deferred chunk is already sent when it settles — upgrade: the blueprint's send gate (LoadGate).
     */
    public static boolean loadOrDefer(ServerLevel level, LevelChunk chunk) {
        if (++loadsThisTick > SeasonConfig.season().catchUpChunksPerTick()) {
            DEFERRED.computeIfAbsent(level, k -> new ArrayDeque<>()).add(chunk.getPos());
            return true;
        }
        catchUp(level, chunk);
        return false;
    }

    public static int deferredCount(ServerLevel level) {
        ArrayDeque<ChunkPos> q = DEFERRED.get(level);
        return q == null ? 0 : q.size();
    }

    /** Settles up to {@code catchUpChunksPerTick} deferred chunks (called once a tick). */
    public static void drainDeferred(ServerLevel level) {
        drain(level, DEFERRED.get(level), ClockService.get().absDay(), SeasonConfig.season().catchUpChunksPerTick());
    }

    /** Test hook: forget the load count of this tick. */
    public static void resetLoadBudget() { loadsThisTick = 0; }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        loadsThisTick = 0;
        MinecraftServer server = e.getServer();
        if (!EnvService.installed()) return;
        long tick = server.getTickCount();
        ServerLevel ow = server.overworld();
        if (ow == null) return;
        long today = ClockService.get().absDay();
        if (tick % 20 == 0) {
            boolean any = false;
            for (ServerLevel l : server.getAllLevels()) any |= WorldHooks.world(l) != null;
            if (any) {
                if (advanceTo(ow, today) > 0) enqueueLive(server);
                IceService.get().tickHoles(ow);
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (WorldHooks.world(level) == null) continue;
            drainDeferred(level);
            drain(level, LIVE.get(level), today, SeasonConfig.season().chunksPerTick());
            applyOps(level, today, SeasonConfig.season().opsPerTick());
        }
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent e) {
        LIVE.clear();
        DEFERRED.clear();
    }

    /** Queues every chunk near a player, nearest first, for the live passes (6 a tick). */
    static void enqueueLive(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            if (WorldHooks.world(level) == null) continue;
            Set<ChunkPos> set = new HashSet<>();
            List<ServerPlayer> ps = level.players();
            for (ServerPlayer p : ps) {
                int cx = p.chunkPosition().x, cz = p.chunkPosition().z;
                for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) set.add(new ChunkPos(cx + dx, cz + dz));
            }
            List<ChunkPos> order = new ArrayList<>(set);
            order.sort(Comparator.comparingDouble(c -> ps.stream().mapToDouble(p -> p.chunkPosition().getChessboardDistance(c)).min().orElse(0)));
            LIVE.put(level, new ArrayDeque<>(order));
        }
    }

    private static void drain(ServerLevel level, ArrayDeque<ChunkPos> q, long today, int budget) {
        if (q == null) return;
        for (int i = 0; i < budget && !q.isEmpty(); i++) {
            ChunkPos p = q.poll();
            if (!(level.getChunkSource().getChunkNow(p.x, p.z) instanceof LevelChunk chunk)) continue;
            TerrainChangeQueue tq = TerrainChangeQueue.get(level);
            if (tq.stamp(p) == today && tq.pending(p) == 0) continue;
            TerrainApply.apply(level, chunk, tq, today, true, Integer.MAX_VALUE);
            settle(level, chunk, true, today);
        }
    }

    /** Applies due ops to loaded chunks: at most {@code budget} block sets a tick, canonical order inside a chunk. */
    static void applyOps(ServerLevel level, long today, int budget) {
        TerrainChangeQueue q = TerrainChangeQueue.get(level);
        if (q.totalPending() == 0) return;
        for (Long key : new ArrayList<>(q.pendingChunks())) {
            if (budget <= 0) return;
            ChunkPos p = new ChunkPos(key);
            if (!(level.getChunkSource().getChunkNow(p.x, p.z) instanceof LevelChunk chunk)) continue;
            budget -= TerrainApply.apply(level, chunk, q, today, true, budget).size();
        }
    }
}

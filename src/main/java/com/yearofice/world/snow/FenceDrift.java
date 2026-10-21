package com.yearofice.world.snow;

import com.yearofice.api.weather.WeatherService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.world.SnowPack;
import com.yearofice.sim.world.SnowTuning;
import com.yearofice.world.WorldHooks;
import com.yearofice.world.season.SeasonConfig;
import com.yearofice.world.season.SeasonState;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * B9, drifts bury fences (Structures and buildables › Snow fence; Proposed): a fence or wall in {@code #yearofice:fences}
 * stops wind-blown snow, so the lee side collects a drift up to {@code leeDepth × boost}, as tall as the fence at most,
 * thinning to nothing {@code reach} blocks downwind. Three storms of lee snow bury a 1.5 m fence and the drift bridges
 * it. Grows only, never shrinks; the melt is the snow pass's. The lee depth is the LEE_DRIFT pack of the season core.
 * ponytail: runs on chunk load and once a game hour near players, after (not inside) the snow pass, so a later snow
 * pass can flatten a drift until the next scan — upgrade: have SnowPass keep columns downwind of tagged fences.
 */
public final class FenceDrift {
    private FenceDrift() {}

    public static final TagKey<Block> FENCES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "fences"));
    static double boost() { return com.yearofice.build.outpost.OutpostConfig.get("fence_drift_boost"); }

    /** Blocks downwind the drift reaches (5-10 in the doc). */
    static int reach() { return (int) com.yearofice.build.outpost.OutpostConfig.get("fence_drift_reach"); }

    /** Layers of drift at {@code d} blocks downwind (1-based): the cap at the fence, thinning linearly to REACH. */
    public static int layersAt(double capLayers, int d) {
        return (int) Math.round(capLayers * (reach() - d + 1) / (double) reach());
    }

    /**
     * Grows a drift downwind of the fence column whose lowest fence block is {@code fenceBase}. Returns the layers
     * added in total.
     */
    public static int build(ServerLevel l, BlockPos fenceBase, Direction downwind, double leeDepthCm, SnowPack.State pack, SnowTuning t) {
        int h = 0;
        while (l.isLoaded(fenceBase.above(h)) && l.getBlockState(fenceBase.above(h)).is(FENCES)) h++;
        if (h == 0) return 0;
        double capLayers = Math.min(leeDepthCm * boost() / t.layerCm(), h * 8.0);
        int added = 0;
        for (int d = 1; d <= reach(); d++) {
            int want = layersAt(capLayers, d);
            BlockPos col = fenceBase.relative(downwind, d);
            if (!l.isLoaded(col)) continue; // never ask for a chunk: a blocking load from a chunk-load handler hangs the server
            if (!l.getBlockState(col.below()).isFaceSturdy(l, col.below(), Direction.UP)) continue;
            for (int k = 0; k * 8 < want; k++) {
                BlockPos p = col.above(k);
                BlockState cur = l.getBlockState(p);
                int have = cur.is(WorldBlocks.SNOW_COVER.get()) ? cur.getValue(SnowCoverBlock.LAYERS) : cur.isAir() ? 0 : 8;
                int layers = Math.min(8, want - 8 * k);
                if (layers <= have) continue;
                if (!cur.isAir() && !cur.is(WorldBlocks.SNOW_COVER.get())) break;
                l.setBlock(p, WorldBlocks.SNOW_COVER.get().defaultBlockState().setValue(SnowCoverBlock.LAYERS, layers)
                        .setValue(SnowCoverBlock.CRUST, pack.crust).setValue(SnowCoverBlock.WET, pack.wet), 2);
                added += layers - have;
            }
        }
        return added;
    }

    /** Downwind of a wind from {@code fromDeg} (compass: 270 is a west wind, so snow goes east). */
    public static Direction downwind(double fromDeg) {
        int idx = (int) Math.floorMod(Math.round(((fromDeg + 180) % 360) / 90.0), 4L);
        return new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}[idx];
    }

    /** Drifts every tagged fence column in the chunk. */
    public static int settle(ServerLevel l, LevelChunk chunk) {
        var pack = SeasonState.get(l).core.snow(SnowPack.SnowClass.LEE_DRIFT);
        if (pack.depthCm() <= 0) return 0;
        int added = 0;
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++) {
                int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, lx, lz);
                while (y > chunk.getMinBuildHeight() && chunk.getBlockState(m.set(x0 + lx, y, z0 + lz)).is(WorldBlocks.SNOW_COVER.get())) y--;
                if (!chunk.getBlockState(m.set(x0 + lx, y, z0 + lz)).is(FENCES)) continue;
                while (y > chunk.getMinBuildHeight() && chunk.getBlockState(m.set(x0 + lx, y - 1, z0 + lz)).is(FENCES)) y--;
                BlockPos base = new BlockPos(x0 + lx, y, z0 + lz);
                double from = WeatherService.installed() ? WeatherService.get().current(l, base).fromDeg() : 270;
                added += build(l, base, downwind(from), pack.depthCm(), pack, SeasonConfig.snow());
            }
        if (added > 0) chunk.setUnsaved(true);
        return added;
    }

    /** Chunks that loaded and still wait to settle: chunk to the tick it queued. Drained on the server thread, never inside the load. */
    private static final java.util.Map<Long, Object[]> PENDING = new java.util.LinkedHashMap<>();

    /** Queues a loaded chunk for the next tick; settle runs once its neighbours are in memory (or after 100 ticks). */
    public static void queue(ServerLevel l, LevelChunk c) {
        PENDING.putIfAbsent(c.getPos().toLong(), new Object[] {(long) l.getServer().getTickCount(), l.dimension()});
    }

    public static int pending() { return PENDING.size(); }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel l && e.getChunk() instanceof LevelChunk c && WorldHooks.world(l) != null && !e.isNewChunk()) queue(l, c);
    }

    @SubscribeEvent
    public static void drain(ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        var it = PENDING.entrySet().iterator();
        int budget = 4;
        while (it.hasNext() && budget > 0) {
            var en = it.next();
            ChunkPos cp = new ChunkPos(en.getKey());
            ServerLevel l = e.getServer().getLevel((net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level>) en.getValue()[1]);
            if (l == null) { it.remove(); continue; }
            if (!(l.getChunkSource().getChunkNow(cp.x, cp.z) instanceof LevelChunk c)) { it.remove(); continue; }
            boolean ready = l.hasChunk(cp.x - 1, cp.z) && l.hasChunk(cp.x + 1, cp.z) && l.hasChunk(cp.x, cp.z - 1) && l.hasChunk(cp.x, cp.z + 1);
            if (!ready && e.getServer().getTickCount() - (long) en.getValue()[0] < 100) continue;
            it.remove();
            budget--;
            if (WorldHooks.world(l) != null) settle(l, c);
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 1200 != 0) return;
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            ServerLevel l = p.serverLevel();
            if (WorldHooks.world(l) == null) continue;
            ChunkPos c = p.chunkPosition();
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    if (l.getChunkSource().getChunkNow(c.x + dx, c.z + dz) instanceof LevelChunk lc) settle(l, lc);
        }
    }
}

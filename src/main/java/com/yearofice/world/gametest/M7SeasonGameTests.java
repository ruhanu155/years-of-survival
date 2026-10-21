package com.yearofice.world.gametest;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.world.ChangeOp;
import com.yearofice.api.world.IceService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.weather.DayState;
import com.yearofice.sim.weather.WeatherModel;
import com.yearofice.sim.world.IceModel;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.sim.world.SeasonCore;
import com.yearofice.sim.world.SeasonPreset;
import com.yearofice.sim.world.SnowPack;
import com.yearofice.sim.world.SnowPack.SnowClass;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.world.season.SeasonConfig;
import com.yearofice.world.season.SeasonState;
import com.yearofice.world.season.SeasonSystem;
import com.yearofice.world.terrain.TerrainApply;
import com.yearofice.world.terrain.TerrainChangeQueue;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M7 lane B GameTests: catch-up equals live, catch-up is order free, the catch-up budget, the snow classes, the flood
 * returns, an ice hole skins over. They build sky platforms (stone at y 100) in far chunks of the flat GameTest
 * overworld, so nothing depends on the terrain below and no other test sees them. All on Standard.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class M7SeasonGameTests {
    private M7SeasonGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int Y0 = 100;

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:world/catchup_equals_live", 600, M7SeasonGameTests::catchupEqualsLive));
        out.add(test("yearofice:world/catchup_order_free", 600, M7SeasonGameTests::catchupOrderFree));
        out.add(test("yearofice:world/catchup_budget", 800, M7SeasonGameTests::catchupBudget));
        out.add(test("yearofice:world/snow_pass_classes", 200, M7SeasonGameTests::snowPassClasses));
        out.add(test("yearofice:world/flood_returns", 200, M7SeasonGameTests::floodReturns));
        out.add(test("yearofice:ice/hole_skins_over", 200, M7SeasonGameTests::holeSkinsOver));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m7b", name, TEMPLATE, maxTicks, 0, true, body);
    }

    // ---- terrain ----------------------------------------------------------------------------------------------

    static void set(LevelChunk ch, int x, int y, int z, BlockState s) {
        ch.setBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, y, ch.getPos().getMinBlockZ() + z), s, false);
    }

    /** A stone platform at y 100 with a closed pond (x 5-6, z 2-13), a west ridge (x 0-1) and a floating canopy leaf. */
    static void build(LevelChunk ch, int variant) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            set(ch, x, Y0, z, Blocks.STONE.defaultBlockState());
            for (int y = Y0 + 1; y <= Y0 + 16; y++) set(ch, x, y, z, Blocks.AIR.defaultBlockState());
        }
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int y = Y0 + 1; y <= Y0 + 3; y++) {
            for (int z = 1; z <= 14; z++) { set(ch, 4, y, z, stone); set(ch, 7, y, z, stone); }
            for (int x = 4; x <= 7; x++) { set(ch, x, y, 1, stone); set(ch, x, y, 14, stone); }
            if (variant % 3 != 0) for (int z = 0; z < 16; z++) { set(ch, 0, y, z, stone); set(ch, 1, y, z, stone); }
        }
        for (int z = 2; z <= 13; z++) { set(ch, 5, Y0 + 1, z, Blocks.WATER.defaultBlockState()); set(ch, 6, Y0 + 1, z, Blocks.WATER.defaultBlockState()); }
        set(ch, 11, Y0 + 3, 11, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
    }

    static List<LevelChunk> chunks(ServerLevel l, int cx0, int cz0, int w, int d) {
        List<LevelChunk> out = new ArrayList<>();
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) out.add(l.getChunk(cx0 + x, cz0 + z));
        return out;
    }

    static long hash(List<LevelChunk> chs) {
        long h = 1125899906842597L;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (LevelChunk c : chs) {
            for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) for (int y = Y0 - 1; y <= Y0 + 16; y++) {
                h = h * 31 + Block.getId(c.getBlockState(m.set(c.getPos().getMinBlockX() + x, y, c.getPos().getMinBlockZ() + z)));
            }
        }
        return h;
    }

    /** A year of synthetic weather: warm July, cold December, a snowfall every few days. */
    static void day(SeasonCore core, long d) {
        int doy = (int) (d % 365);
        double t = 4 + 20 * Math.cos(2 * Math.PI * (doy - 80) / 365.0);
        double precip = (d * 7919 % 13 == 0 ? 6 : 0) + (d % 5 == 0 ? 2 : 0);
        double wind = d % 9 == 0 ? 9 : 3;
        core.step(d, doy, t, precip, wind, SeasonConfig.snow(), SeasonConfig.ice());
    }

    static WeatherModel.Day synthDay(long d) {
        int doy = (int) (d % 365);
        double t = 4 + 20 * Math.cos(2 * Math.PI * (doy - 80) / 365.0);
        return new WeatherModel.Day(d, DayState.CLOUDY, 0, t, t, 3, d % 9 == 0 ? 9 : 3, 0, (d * 7919 % 13 == 0 ? 6 : 0) + (d % 5 == 0 ? 2 : 0), 0, 6);
    }

    // ---- world/catchup_equals_live -------------------------------------------------------------------------------

    static void catchupEqualsLive(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        List<LevelChunk> a = chunks(l, 3000, 3000, 4, 4), b = chunks(l, 3010, 3000, 4, 4);
        for (int i = 0; i < a.size(); i++) { build(a.get(i), i); build(b.get(i), i); }
        SeasonCore coreA = new SeasonCore(), coreB = new SeasonCore();
        TerrainChangeQueue qa = new TerrainChangeQueue(), qb = new TerrainChangeQueue();
        long[] opDays = {50, 120, 250};
        for (int i = 0; i < a.size(); i++) {
            for (int k = 0; k < opDays.length; k++) {
                for (int side = 0; side < 2; side++) {
                    LevelChunk c = side == 0 ? a.get(i) : b.get(i);
                    BlockPos p = new BlockPos(c.getPos().getMinBlockX() + 9 + k, Y0 + 1, c.getPos().getMinBlockZ() + 3 + k);
                    (side == 0 ? qa : qb).submit(new ChangeOp(opDays[k], ChangeOp.Kind.HEAVE, 0, p, "yearofice:till", true, k));
                }
            }
        }
        int days = 300;
        for (long d = 0; d < days; d++) {
            day(coreA, d);
            day(coreB, d);
            for (LevelChunk c : a) {
                TerrainApply.apply(l, c, qa, d, true, Integer.MAX_VALUE);
                SeasonSystem.settle(l, c, true, d, coreA, qa);
            }
        }
        long today = days - 1;
        for (LevelChunk c : b) {
            TerrainApply.apply(l, c, qb, today, false, Integer.MAX_VALUE);
            SeasonSystem.settle(l, c, false, today, coreB, qb);
        }
        h.assertTrue(Math.abs(coreA.snow(SnowClass.OPEN).sweMm - coreB.snow(SnowClass.OPEN).sweMm) < 1e-9, "cores agree");
        h.assertTrue(coreA.snow(SnowClass.OPEN).depthCm() > 5 || coreA.ice(BodyClass.LAKE).cm > 5, "the year produced snow or ice to compare");
        long ha = hash(a), hb = hash(b);
        h.assertTrue(ha == hb, "block hash live " + ha + " vs catch-up " + hb);
        for (int i = 0; i < a.size(); i++) {
            h.assertTrue(qa.floodFlags(a.get(i).getPos()).size() == qb.floodFlags(b.get(i).getPos()).size(), "flood flags agree in chunk " + i);
        }
        h.assertTrue(qa.totalPending() == 0 && qb.totalPending() == 0, "every op applied");
        h.succeed();
    }

    // ---- world/catchup_order_free ---------------------------------------------------------------------------------

    static void catchupOrderFree(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        List<LevelChunk> chs = chunks(l, 3100, 3100, 8, 8);
        SeasonCore core = new SeasonCore();
        for (long d = 0; d < 270; d++) day(core, d);
        long today = 269;
        Long first = null;
        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < chs.size(); i++) build(chs.get(i), i);
            TerrainChangeQueue q = new TerrainChangeQueue();
            for (int i = 0; i < chs.size(); i += 3) {
                LevelChunk c = chs.get(i);
                q.submit(new ChangeOp(100 + i, ChangeOp.Kind.HEAVE, i % 4, new BlockPos(c.getPos().getMinBlockX() + 9, Y0 + 1, c.getPos().getMinBlockZ() + 9),
                        "yearofice:till", true, i));
                q.submit(new ChangeOp(100 + i, ChangeOp.Kind.HEAVE, 0, new BlockPos(c.getPos().getMinBlockX() + 9, Y0 + 1, c.getPos().getMinBlockZ() + 9),
                        "minecraft:gravel", true, i + 1));
            }
            List<LevelChunk> order = new ArrayList<>(chs);
            Collections.shuffle(order, new Random(round * 7919L + 1));
            for (LevelChunk c : order) {
                TerrainApply.apply(l, c, q, today, false, Integer.MAX_VALUE);
                SeasonSystem.settle(l, c, false, today, core, q);
            }
            long hh = hash(chs);
            if (first == null) first = hh;
            h.assertTrue(first == hh, "load order " + round + " changed the hash");
        }
        h.assertTrue(core.snow(SnowClass.OPEN).depthCm() > 5 || core.ice(BodyClass.LAKE).cm > 5, "there was winter to settle");
        h.succeed();
    }

    // ---- world/catchup_budget -------------------------------------------------------------------------------------

    static void catchupBudget(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        List<LevelChunk> chs = chunks(l, 3200, 3200, 21, 21);
        for (int i = 0; i < chs.size(); i++) build(chs.get(i), i);
        for (LevelChunk c : chs) l.setChunkForced(c.getPos().x, c.getPos().z, true);
        SeasonState st = SeasonState.get(l);
        double[] saved = st.core.encode();
        long savedDay = st.core.lastDay;
        TerrainChangeQueue q = TerrainChangeQueue.get(l);
        long clockDay = ClockService.get().absDay();
        long away = 365 * 3;
        ClockService.get().setDay(clockDay + away);
        long farDay = ClockService.get().absDay();
        SeasonSystem.setWeatherSource((lv, d) -> synthDay(d));
        st.core.decode(new SeasonCore().encode());
        st.core.lastDay = Long.MIN_VALUE;
        SeasonSystem.slowestNs = 0;
        long[] worst = {0};
        int[] state = {0, 0}; // 0 = not started, 1 = loads issued; second = ticks drained
        h.onEachTick(() -> {
            long t0 = System.nanoTime();
            if (state[0] == 0) {
                SeasonSystem.resetLoadBudget();
                int deferred = 0;
                for (LevelChunk c : chs) if (SeasonSystem.loadOrDefer(l, c)) deferred++;
                h.assertTrue(deferred == chs.size() - SeasonConfig.season().catchUpChunksPerTick(),
                        "a 441-chunk load storm defers all but the per-tick budget, deferred " + deferred);
                state[0] = 1;
            } else {
                SeasonSystem.drainDeferred(l);
                state[1]++;
            }
            worst[0] = Math.max(worst[0], System.nanoTime() - t0);
        });
        h.succeedWhen(() -> {
            h.assertTrue(state[0] == 1 && SeasonSystem.deferredCount(l) == 0, "deferred chunks left: " + SeasonSystem.deferredCount(l));
            h.assertTrue(worst[0] < 50_000_000L, "a tick of catch-up took " + worst[0] / 1_000_000 + " ms");
            h.assertTrue(state[1] >= 441 / SeasonConfig.season().catchUpChunksPerTick() - 1, "the drain was spread over ticks: " + state[1]);
            for (LevelChunk c : chs) h.assertTrue(q.stamp(c.getPos()) == farDay, "chunk settled: stamp " + q.stamp(c.getPos()) + " vs day " + farDay);
            ClockService.get().setDay(clockDay);
            for (LevelChunk c : chs) l.setChunkForced(c.getPos().x, c.getPos().z, false);
            SeasonSystem.setWeatherSource(null);
            st.core.decode(saved);
            st.core.lastDay = savedDay;
        });
    }

    // ---- world/snow_pass_classes --------------------------------------------------------------------------------------

    static void snowPassClasses(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(3300, 3300);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            set(ch, x, Y0, z, Blocks.STONE.defaultBlockState());
            for (int y = Y0 + 1; y <= Y0 + 16; y++) set(ch, x, y, z, Blocks.AIR.defaultBlockState());
        }
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int y = Y0 + 1; y <= Y0 + 3; y++) {
            for (int z = 0; z < 16; z++) { set(ch, 0, y, z, stone); set(ch, 1, y, z, stone); }      // west ridge: lee at x 3-4
            for (int x = 8; x < 16; x++) { set(ch, x, y, 2, stone); set(ch, x, y, 3, stone); }      // north ridge: south slope at z 4-5
        }
        set(ch, 12, Y0 + 3, 12, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true));
        SeasonCore core = new SeasonCore();
        var snow = SeasonConfig.snow();
        var ice = SeasonConfig.ice();
        for (int d = 200; d < 260; d++) core.step(d, d, -15, 4, 8, snow, ice);        // two months of snow, windy
        for (int d = 310; d < 322; d++) core.step(d, d, 4, 0, 3, snow, ice);          // a March thaw (south slopes melt faster)
        TerrainChangeQueue q = new TerrainChangeQueue();
        SeasonSystem.settle(l, ch, false, 321, core, q);
        int open = layers(ch, 10, 10), canopy = layers(ch, 12, 12), lee = layers(ch, 4, 8), south = layers(ch, 10, 4);
        int[] expect = {SnowPack.layers(core.snow(SnowClass.OPEN).depthCm(), snow), SnowPack.layers(core.snow(SnowClass.UNDER_SPRUCE).depthCm(), snow),
                SnowPack.layers(core.snow(SnowClass.LEE_DRIFT).depthCm(), snow), SnowPack.layers(core.snow(SnowClass.SOUTH_SLOPE).depthCm(), snow)};
        h.assertTrue(open == expect[0] && canopy == expect[1] && lee == expect[2] && south == expect[3],
                "layers open/canopy/lee/south " + open + "/" + canopy + "/" + lee + "/" + south + " expected " + java.util.Arrays.toString(expect));
        h.assertTrue(canopy < open && open < lee && south < open, "classes order: canopy " + canopy + " < open " + open + " < lee " + lee + ", south " + south + " < open");
        h.assertTrue(lee > 8 && h.getLevel().getBlockState(new BlockPos(ch.getPos().getMinBlockX() + 4, Y0 + 2, ch.getPos().getMinBlockZ() + 8)).is(WorldBlocks.SNOW_COVER.get()),
                "a deep drift stacks a second snow block");
        h.assertTrue(SeasonSystem.settle(l, ch, false, 321, core, q) == 0, "a second pass changes nothing");
        h.succeed();
    }

    static int layers(LevelChunk ch, int x, int z) {
        int n = 0;
        for (int k = 1; k <= 3; k++) {
            BlockState s = ch.getBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, Y0 + k, ch.getPos().getMinBlockZ() + z));
            if (s.is(WorldBlocks.SNOW_COVER.get())) n += s.getValue(SnowCoverBlock.LAYERS); else break;
        }
        return n;
    }

    // ---- world/flood_returns -------------------------------------------------------------------------------------------

    static void floodReturns(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(3310, 3310);
        build(ch, 1);
        List<LevelChunk> one = List.of(ch);
        long before = hash(one);
        SeasonCore core = new SeasonCore();
        TerrainChangeQueue q = new TerrainChangeQueue();
        core.floodM = 1.2;
        SeasonSystem.settle(l, ch, false, 10, core, q);
        BlockPos flooded = new BlockPos(ch.getPos().getMinBlockX() + 5, Y0 + 2, ch.getPos().getMinBlockZ() + 5);
        h.assertTrue(ch.getBlockState(flooded).is(Blocks.WATER), "the pulse put water above the pond");
        h.assertTrue(!q.floodFlags(ch.getPos()).isEmpty(), "flood flags record what was placed");
        int water = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = Y0 + 1; y <= Y0 + 6; y++)
            if (ch.getBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, y, ch.getPos().getMinBlockZ() + z)).is(Blocks.WATER)) water++;
        h.assertTrue(water == 2 * 12 * 2, "water only over the pond's 24 columns (two blocks deep with the flood): " + water);
        int day = 10;
        while (core.floodM > 0 && day < 100) {
            core.step(++day, day, 5, 0, 3, SeasonConfig.snow(), SeasonConfig.ice());
        }
        h.assertTrue(core.floodM == 0 && day < 60, "the pulse decays to nothing in " + (day - 10) + " days");
        SeasonSystem.settle(l, ch, false, day, core, q);
        h.assertTrue(q.floodFlags(ch.getPos()).isEmpty(), "flags cleared");
        h.assertTrue(hash(one) == before, "the terrain is exactly as before the flood");
        h.succeed();
    }

    // ---- ice/hole_skins_over ------------------------------------------------------------------------------------------------

    static void holeSkinsOver(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(3320, 3320);
        build(ch, 1);
        SeasonState st = SeasonState.get(l);
        double[] saved = st.core.encode();
        long savedDay = st.core.lastDay;
        var lake = st.core.ice(BodyClass.LAKE);
        lake.cm = 40; lake.peakCm = 40; lake.rotten = false;
        TerrainChangeQueue q = new TerrainChangeQueue();
        SeasonSystem.settle(l, ch, false, 5, st.core, q);
        BlockPos p = new BlockPos(ch.getPos().getMinBlockX() + 5, Y0 + 1, ch.getPos().getMinBlockZ() + 5);
        h.assertTrue(l.getBlockState(p).is(IceBlocks.RIVER_ICE.get()), "the pond is iced over");
        h.assertTrue(IceService.get().thicknessCm(l, p) == 40 && IceService.get().stage(l, p) == IceModel.Stage.SAFE, "40 cm reads as safe ice");
        BlockPos q2 = p.offset(0, 0, 3);
        h.assertTrue(IceService.get().digHole(l, p) && IceService.get().digHole(l, q2), "two holes cut");
        h.assertTrue(IceService.get().isOpenHole(l, p), "the hole is open");
        // 61 minutes at -10 C: not yet. Then 61 minutes at -25 C: skinned over.
        EnvService env = EnvService.get();
        long nowDs = ClockService.get().nowDs();
        env.setOverride(l, new EnvService.WeatherOverride(-10, Double.NaN, null, Double.NaN));
        ClockService.get().advance(30);
        IceService.get().tickHoles(l);
        h.assertTrue(IceService.get().isOpenHole(l, p), "at -10 C the hole is still open after half an hour");
        env.setOverride(l, new EnvService.WeatherOverride(-25, Double.NaN, null, Double.NaN));
        ClockService.get().advance(61);
        int closed = IceService.get().tickHoles(l);
        env.setOverride(l, null);
        ClockService.get().setNow(nowDs);
        BlockState after = l.getBlockState(p);
        st.core.decode(saved);
        st.core.lastDay = savedDay;
        st.holes.clear();
        h.assertTrue(closed == 2 && !after.is(IceBlocks.ICE_HOLE.get()) && IceBlocks.isIce(after), "below -20 C the hole skinned over within the hour: " + after);
        h.succeed();
    }
}

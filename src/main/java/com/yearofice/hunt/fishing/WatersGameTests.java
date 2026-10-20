package com.yearofice.hunt.fishing;

import com.yearofice.sim.fishing.FishStock;
import com.yearofice.sim.fishing.SalmonRun;
import com.yearofice.sim.fishing.WeirFence;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M6 lane C waters: spot migration, salmon runs by date, the fish_weir fence. Batch {@code m6c}. */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class WatersGameTests {
    private WatersGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> watersTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(FishingGameTests.test("yearofice:fish/spot_recovers_by_migration", 100, WatersGameTests::migration));
        out.add(FishingGameTests.test("yearofice:fish/salmon_run_by_date", 100, WatersGameTests::runByDate));
        out.add(FishingGameTests.test("yearofice:fish/weir_multiblock", 100, WatersGameTests::weir));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    /** A fished-down river spot refills from neighbouring water in days; a tarn does not. */
    static void migration(GameTestHelper h) {
        FishGameTests.with(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(BlockPos.ZERO);
            SpotFish.Entry pike = FishGameTests.entry(SpotFish.at(level, pos, 40, FishingConfig.tuning()), "northern_pike");
            long cell = SliceSpots.key(pos) ^ 0x6d1L;
            long key = SliceSpots.speciesKey(cell, pike.id());
            SliceSpots.Stocks stocks = SliceSpots.stocks(level);
            double mig = FishingConfig.tuning().spotMigrationPerDay();
            long day = 40;
            for (int i = 0; i < 12; i++) stocks.harvest(key, day, pike.k(), pike.r(), 1, day * 1440);
            double k = pike.k();
            double d0 = stocks.get(key, day, k, pike.r(), mig).n();
            double d3 = stocks.get(key, day + 3, k, pike.r(), mig).n();
            double d7 = stocks.get(key, day + 7, k, pike.r(), mig).n();
            double d14 = stocks.get(key, day + 14, k, pike.r(), mig).n();
            h.assertTrue(d0 < k - 11.9, "fished down: " + d0);
            h.assertTrue(d3 > d0 && d7 > d3 && d14 > d7, "refills steadily: " + d0 + " " + d3 + " " + d7 + " " + d14);
            h.assertTrue(d7 >= 0.35 * k && d14 >= 0.6 * k, "a week and a fortnight: " + d7 + ", " + d14 + " of " + k);
            long key2 = SliceSpots.speciesKey(cell ^ 0x77L, pike.id());
            for (int i = 0; i < 12; i++) stocks.harvest(key2, day, k, pike.r(), 1, day * 1440);
            double tarn = SpotFish.stock(level, cell ^ 0x77L, day, pike, "tarn_shoal").n();
            double tarn3 = SpotFish.stock(level, cell ^ 0x77L, day + 3, pike, "tarn_shoal").n();
            h.assertTrue(tarn3 - tarn < 0.4 * (d3 - d0) + 1e-9, "a tarn gets no migration: +" + (tarn3 - tarn) + " vs river +" + (d3 - d0));
            h.succeed();
        });
    }

    /** Run strength by date, the year factor's bounds, the echo and the fish a run puts on the water. */
    static void runByDate(GameTestHelper h) {
        FishGameTests.with(h, () -> {
            ServerLevel level = h.getLevel();
            SalmonRun.Run c = SalmonRun.RUNS.get("chinook_salmon");
            h.assertTrue(SalmonRun.strength("chinook_salmon", c.peak()) == 1.0, "peak is full");
            h.assertTrue(SalmonRun.strength("chinook_salmon", c.from() - 1) == 0 && SalmonRun.strength("chinook_salmon", c.to() + 1) == 0, "none outside");
            h.assertTrue(SalmonRun.strength("chinook_salmon", c.from() + 2) < 1 && SalmonRun.strength("chinook_salmon", c.from() + 2) > 0, "ramps in");
            double lo = 9, hi = 0, sum = 0;
            for (int i = 0; i < 400; i++) {
                double y = SalmonRun.yearFactor(i / 400.0 + 0.00125);
                lo = Math.min(lo, y);
                hi = Math.max(hi, y);
                sum += y;
            }
            h.assertTrue(lo >= 0.5 && hi <= 1.6 && Math.abs(sum / 400 - 1.0) < 0.03, "y in 0.5..1.6 around 1: " + lo + " " + hi + " " + sum / 400);
            RunLedger led = RunLedger.get(level);
            long seed = level.getSeed();
            int yr = 50;
            double before = c.rMean() * SalmonRun.yearFactor(seed, "chinook_salmon", yr + 5);
            led.record("chinook_salmon", yr, (int) (0.6 * led.runSize(seed, "chinook_salmon", yr)) + 1);
            double after = led.runSize(seed, "chinook_salmon", yr + 5);
            h.assertTrue(after < before * 0.8, "the echo: a heavy harvest thins the run five years on: " + before + " -> " + after);
            FishStock full = FishStock.full(100);
            int peak = FishSpawner.wantedOn("chinook_salmon", full, c.peak());
            int mid = FishSpawner.wantedOn("chinook_salmon", full, (c.from() + c.peak()) / 2);
            int edge = FishSpawner.wantedOn("chinook_salmon", full, c.from());
            int out = FishSpawner.wantedOn("chinook_salmon", full, c.to() + 10);
            h.assertTrue(peak == 10 && mid > edge && peak > mid && edge >= 1, "fish by date: " + peak + " " + mid + " " + edge);
            h.assertTrue(out == 0, "no salmon outside the window: " + out);
            h.succeed();
        });
    }

    /** A basket with weir stakes on both sides counts the whole line; breaking one shortens it. */
    static void weir(GameTestHelper h) {
        BlockState stake = FishingRegistry.WEIR_STAKE.get().defaultBlockState();
        BlockPos base = new BlockPos(2, 2, 2);
        for (int dx = -4; dx <= 4; dx++) if (dx != 0) h.setBlock(base.offset(dx, 0, 0), stake);
        BlockPos basket = h.absolutePos(base);
        int n = WeirFences.stakes(h.getLevel(), basket);
        h.assertTrue(n == 8, "eight stakes: " + n);
        h.setBlock(base.offset(2, 0, 0), net.minecraft.world.level.block.Blocks.AIR);
        int cut = WeirFences.stakes(h.getLevel(), basket);
        h.assertTrue(cut == 5, "a broken stake leaves the near run and the far side: " + cut);
        double m8 = WeirFence.multiplier(8, 12), m5 = WeirFence.multiplier(5, 12), m0 = WeirFence.multiplier(0, 12);
        h.assertTrue(m0 == 1.0 && m5 > 1.0 && m8 > m5 && m8 <= 1.0 + WeirFence.MAX_BONUS, "bonus grows with the fence: " + m0 + " " + m5 + " " + m8);
        h.succeed();
    }
}

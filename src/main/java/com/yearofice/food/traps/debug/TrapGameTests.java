package com.yearofice.food.traps.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.food.traps.SnareBlockEntity;
import com.yearofice.food.traps.TrapConfig;
import com.yearofice.food.traps.TrapRegistry;
import com.yearofice.sim.ecosystem.Density;
import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.ecosystem.Snare;
import com.yearofice.sim.ecosystem.TrapTuning;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, traps: the trail snare on a real block entity. The snare's clock is pinned per test (its own
 * override, so the world's day time and other tests never leak in); hares in range are pinned to a hare run in a
 * middle year of the cycle at Standard density.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class TrapGameTests {
    private TrapGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos AT = new BlockPos(1, 1, 1);
    static final Snare.SetQuality HARE_RUN = new Snare.SetQuality(true, false, false, false, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> trapTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:food/slice_snare_hare", 100, TrapGameTests::sliceSnareHare));
        out.add(test("yearofice:food/snare_catchup_equals_live", 400, TrapGameTests::catchUpEqualsLive));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static SnareBlockEntity snare(GameTestHelper h) {
        h.setBlock(AT.below(), Blocks.STONE);
        h.setBlock(AT, TrapRegistry.SNARE.get());
        SnareBlockEntity be = h.getBlockEntity(AT);
        be.setSilent(true);
        return be;
    }

    /** GameTests run on Standard: the hare run's density is Standard's, whatever the run's config file holds. */
    static double hareRun() { return Density.coveringPoint(com.yearofice.sim.ecosystem.EcoTuning.standard().hareDensityPerKm2(), 150, 1.0, 1.0); }

    /**
     * Ready for development M2.5 test 2, {@code food/slice_snare_hare}: a snare on a hare run catches 2–5 hares
     * over 10 game days, rolled at dawn and dusk only. Checked each morning; 32 ten-day runs (set 20 days apart,
     * so each draws its own rolls) must average 2–5, and every catch must come from a roll at the clock's dawn or
     * dusk of its day ({@code ClockService.dawnMinute} and {@code duskMinute}, the minutes the clock's events carry).
     */
    static void sliceSnareHare(GameTestHelper h) {
        SnareBlockEntity be = snare(h);
        TrapTuning t = TrapConfig.tuning();
        int total = 0;
        int runs = 32;
        for (int run = 0; run < runs; run++) {
            long start = run * 20L * 1440 + 7 * 60;
            be.setForTest(HARE_RUN, 1.0, start, hareRun());
            for (int day = 1; day <= 10; day++) {
                long check = start + day * 1440L;
                be.setClock(check);
                be.settle(true);
                Snare.Catch c = be.state().visible(check);
                if (c != null) {
                    long rollDay = Math.floorDiv(c.roll(), 2);
                    long at = be.env().rolls().minute(c.roll());
                    ClockService clock = ClockService.get();
                    h.assertTrue(at == clock.dawnMinute(rollDay) || at == clock.duskMinute(rollDay), "a catch rolled at minute " + at
                            + ", not day " + rollDay + "'s dawn " + clock.dawnMinute(rollDay) + " or dusk " + clock.duskMinute(rollDay));
                    h.assertTrue(be.collect() != null, "the check took nothing");
                    total++;
                } else {
                    be.collect();
                }
            }
        }
        double mean = total / (double) runs;
        h.assertTrue(mean >= 2 && mean <= 5, "a hare run caught " + mean + " hares in 10 days on average");
        h.succeed();
    }

    /**
     * Blueprint › Traps: "a trapline left alone for 3 days returns exactly the same catches as one watched the whole
     * time" (here 10 days). Live: the snare's own clock runs an hour a tick for 10 game days, and at each dawn and
     * dusk minute it crosses the snare settles as the clock's {@code Dawn} and {@code Dusk} events settle it, while
     * its tick watches for the catch. Catch-up: the same snare reset to the same start, then loaded once at the end,
     * settles every missed dawn and dusk in one call. The two states must be equal.
     */
    static void catchUpEqualsLive(GameTestHelper h) {
        SnareBlockEntity be = snare(h);
        long start = 3 * 1440 + 9 * 60;
        long end = start + 10 * 1440L;
        double hares = hareRun() * 3;
        be.setForTest(HARE_RUN, 1.0, start, hares);
        long[] clock = {start};
        h.onEachTick(() -> {
            if (clock[0] < end) {
                long next = Math.min(end, clock[0] + 60);
                Snare.Rolls rolls = be.env().rolls();
                for (long k = rolls.atOrBefore(clock[0]) + 1; rolls.minute(k) <= next; k++) {
                    be.setClock(rolls.minute(k));
                    be.settle(true);
                }
                clock[0] = next;
                be.setClock(next);
                return;
            }
            Snare.State live = be.state();
            be.setForTest(HARE_RUN, 1.0, start, hares);
            be.setClock(end);
            be.onLoad();
            Snare.State once = be.state();
            h.assertTrue(live.equals(once), "live " + live + " but catch-up " + once);
            h.assertTrue(live.lastRoll() == be.env().rolls().atOrBefore(end), "rolls not settled");
            h.assertTrue(live.catches() >= 1, "the run should catch something to compare");
            h.succeed();
        });
    }
}

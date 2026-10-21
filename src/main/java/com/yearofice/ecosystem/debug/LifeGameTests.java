package com.yearofice.ecosystem.debug;

import com.yearofice.ecosystem.small.*;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.world.FishSpotService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.chorus.ChorusService;
import com.yearofice.ecosystem.migration.MigrationService;
import com.yearofice.sim.life.Bugs;
import com.yearofice.sim.life.Chorus;
import com.yearofice.sim.life.LemmingCycle;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** Batch m7c, life of the year: {@code life/lemming_cycle}, {@code life/insects_bite}, {@code life/salmon_dieoff}, {@code life/chorus_by_date}. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class LifeGameTests {
    private LifeGameTests() {}

    static final BlockPos AT = EcoGameTests.AT;

    @GameTestGenerator
    public static Collection<TestFunction> lifeTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:life/lemming_cycle", 80, LifeGameTests::lemmingCycle));
        out.add(test("yearofice:life/insects_bite", 100, LifeGameTests::insectsBite));
        out.add(test("yearofice:life/salmon_dieoff", 80, LifeGameTests::salmonDieoff));
        out.add(test("yearofice:life/chorus_by_date", 60, LifeGameTests::chorusByDate));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m7c", name, EcoGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    /** The pinned phase moves the index, the brown lemming's boldness and the spawner's multiplier together. */
    static void lemmingCycle(GameTestHelper h) {
        var l = h.getLevel();
        try {
            LemmingIndex.pin(LemmingCycle.Phase.LOW);
            double low = LemmingIndex.multiplier("yearofice:brown_lemming", l);
            h.assertTrue(!LemmingIndex.bold(l), "bold in a low year");
            LemmingIndex.pin(LemmingCycle.Phase.PEAK);
            double peak = LemmingIndex.multiplier("yearofice:brown_lemming", l);
            h.assertTrue(LemmingIndex.bold(l), "not bold at the peak");
            h.assertTrue(peak > 5 * low, "peak " + peak + " vs low " + low);
            LemmingIndex.pin(LemmingCycle.Phase.CRASH);
            h.assertTrue(LemmingIndex.multiplier("yearofice:brown_lemming", l) < peak / 4, "crash did not follow the peak down");
            h.assertTrue(LemmingIndex.multiplier("yearofice:snowshoe_hare", l) == 1.0, "another species was scaled");
        } finally {
            LemmingIndex.clear();
        }
        h.succeed();
    }

    /** A swarm bites a player's exposed skin through BodyService; a head net stops the mosquito's bite. */
    static void insectsBite(GameTestHelper h) {
        ServerPlayer p = EcoGameTests.player(h);
        var l = h.getLevel();
        InsectService.mode = InsectService.Mode.ON;
        InsectSwarmEntity s = InsectService.place(l, p, Bugs.Kind.MOSQUITO);
        h.assertTrue(s != null, "no swarm placed");
        double before = BodyService.get().condition(p);
        s.bite(p);
        double after = BodyService.get().condition(p);
        InsectService.mode = InsectService.Mode.AUTO;
        h.assertTrue(after < before, "condition " + before + " -> " + after);
        h.assertTrue(s.bites == 1, "bites " + s.bites);
        s.discard();
        h.succeed();
    }

    /** After the run's peak, spent bodies float at a spot with a stocked run species, and the count stays under the cap. */
    static void salmonDieoff(GameTestHelper h) {
        var l = h.getLevel();
        BlockPos c = h.absolutePos(AT);
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            l.setBlockAndUpdate(c.offset(x, -2, z), Blocks.STONE.defaultBlockState());
            l.setBlockAndUpdate(c.offset(x, -1, z), Blocks.WATER.defaultBlockState());
            l.setBlockAndUpdate(c.offset(x, 0, z), Blocks.WATER.defaultBlockState());
        }
        var spot = new FishSpotService.Spot("river_pool", c, 4, 1.0, 2.0, 1, List.of("chinook"), List.of(40), true);
        int placed = MigrationService.dieOff(l, spot, "chinook");
        h.assertTrue(placed > 0, "no bodies placed");
        int have = MigrationService.bodies(l, spot, "chinook").size();
        h.assertTrue(have == placed && have <= LifeConfig.tuning().salmonCarcassMax(), "bodies " + have + " placed " + placed);
        h.assertTrue(MigrationService.dieOff(l, spot, "chinook") == 0, "a second pass added more");
        MigrationService.bodies(l, spot, "chinook").forEach(b -> b.discard());
        h.succeed();
    }

    /** The chorus peaks in June at 03:30 in the white nights, near silent at noon and in October. */
    static void chorusByDate(GameTestHelper h) {
        int rise = 5 * 60;
        int may = Calendar.dayOfYear(6, 10), oct = Calendar.dayOfYear(10, 10);
        double dawnMay = Chorus.volume(may, 210, rise, 1.0), noonMay = Chorus.volume(may, 13 * 60, rise, 1.0), dawnOct = Chorus.volume(oct, rise, rise, 1.0);
        h.assertTrue(dawnMay > 0.5, "May dawn " + dawnMay);
        h.assertTrue(noonMay < dawnMay / 2, "noon " + noonMay);
        h.assertTrue(dawnOct < dawnMay / 4, "October dawn " + dawnOct);
        ServerPlayer p = EcoGameTests.player(h);
        ChorusService.pinnedVolume = 0.8;
        try {
            h.assertTrue(ChorusService.volume(h.getLevel(), p) == 0.8, "pin ignored");
        } finally {
            ChorusService.pinnedVolume = Double.NaN;
        }
        h.succeed();
    }
}

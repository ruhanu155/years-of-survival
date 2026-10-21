package com.yearofice.env.weather;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.core.YearOfIce;
import com.yearofice.gametest.TestFilter;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.StormDef;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.sim.weather.StormTable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** Batch m7a: {@code weather/table_replay}, {@code weather/glaze_forms}, {@code weather/strike_limb}. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class WeatherGameTests {
    private WeatherGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> m7aTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:weather/table_replay", 100, WeatherGameTests::tableReplay));
        out.add(test("yearofice:weather/glaze_forms", 100, WeatherGameTests::glazeForms));
        out.add(test("yearofice:weather/strike_limb", 100, WeatherGameTests::strikeLimb));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m7a", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static long nowHour() { return Math.floorDiv(ClockService.get().nowMinute(), 60L); }

    /** Same seed, same 30-day table: built twice from nothing, and again after a forced storm is saved and cleared. */
    static void tableReplay(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        WeatherSystem.reset();
        int year = Calendar.yearOfDay(Math.floorDiv(ClockService.get().nowMinute(), 1440L));
        StormTable a = WeatherSystem.table(level, year);
        List<StormTable.Event> first30 = window(a, 30);
        WeatherSystem.reset();
        StormTable b = WeatherSystem.table(level, year);
        h.assertTrue(a.hash() == b.hash(), "year table hash differs between two builds");
        h.assertTrue(first30.equals(window(b, 30)), "the 30-day table differs between two builds");
        h.assertTrue(a != b, "the second build was served from a stale cache");
        WeatherSystem.reset();
        h.assertTrue(WeatherSystem.table(level, year).hash() == a.hash(), "a reset changed the table");
        h.succeed();
    }

    static List<StormTable.Event> window(StormTable t, int days) {
        long from = (long) (t.year() - 1) * Calendar.DAYS_PER_YEAR * 24, to = from + days * 24L;
        return t.events().stream().filter(e -> e.startHour() >= from && e.startHour() < to).toList();
    }

    /** A freezing-rain storm in progress lays {@code glaze_layer} on the stone, and the env layer reports glaze. */
    static void glazeForms(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        WeatherSystem.reset();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        // the template has a ceiling; the sky must be open for the heightmap
        for (int y = 2; y <= 8; y++) for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
        long now = ClockService.get().nowMinute();
        WeatherSystem.force(level, "freezing_rain", nowHour() - 5, 12, 1.0);
        BlockPos center = h.absolutePos(new BlockPos(1, 2, 1));
        StormTable.Event e = WeatherSystem.eventAt(level, now);
        h.assertTrue(e != null && e.kind() == StormKind.FREEZING_RAIN, "the forced storm is not on the table");
        h.assertTrue(EnvService.get().precip(level, center) == Precip.FREEZING_RAIN, "env layer does not report freezing rain");
        var state = WeatherService.get().current(level, center);
        h.assertTrue(state.kind() == StormKind.FREEZING_RAIN && state.precipKind() == StormKind.PrecipKind.GLAZE, "WeatherService: " + state);
        StormDef def = WeatherSystem.loaded.defs().get("freezing_rain");
        double mm = com.yearofice.sim.weather.StormMath.glazeAfter(def, e, (now - e.startHour() * 60) / 60.0, 1.0, false);
        h.assertTrue(com.yearofice.sim.weather.StormMath.glazeLayers(mm) >= 1, "5 h of freezing rain gave only " + mm + " mm of glaze");
        WeatherSystem.sheets(level, center, e, def, now, 1, 120);
        int found = 0;
        for (int x = 0; x < 3; x++)
            for (int z = 0; z < 3; z++) {
                BlockState s = h.getBlockState(new BlockPos(x, 2, z));
                if (!s.is(WeatherRegistry.GLAZE_LAYER.get())) continue;
                found++;
                int layers = s.getValue(SheetBlock.LAYERS);
                h.assertTrue(layers >= 1 && layers <= 3, "layers " + layers);
                h.assertTrue(h.getBlockState(new BlockPos(x, 1, z)).is(Blocks.STONE), "glaze must sit on the stone");
            }
        h.assertTrue(found >= 6, "only " + found + " of 9 columns glazed; mm " + mm + " top " + level.getBlockState(center) + " below " + level.getBlockState(center.below()) + " hm " + level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ()) + " center " + center);
        WeatherSystem.reset();
        h.succeed();
    }

    /** A strike on a tree drops a fallen_limb on free ground beside it. */
    static void strikeLimb(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        for (int y = 2; y <= 4; y++) h.setBlock(new BlockPos(1, y, 1), Blocks.SPRUCE_LOG);
        h.setBlock(new BlockPos(1, 5, 1), Blocks.SPRUCE_LEAVES);
        BlockPos top = h.absolutePos(new BlockPos(1, 5, 1));
        BlockPos limb = Lightning.strike(level, top, 1);
        h.assertTrue(limb != null, "a struck tree dropped no limb");
        BlockState s = level.getBlockState(limb);
        h.assertTrue(s.is(WeatherRegistry.FALLEN_LIMB.get()), "the limb block is " + s);
        h.assertTrue(level.getBlockState(limb.below()).isSolid(), "the limb floats");
        h.assertTrue(Math.abs(limb.getX() - top.getX()) <= 3 && Math.abs(limb.getZ() - top.getZ()) <= 3, "the limb is far from the tree");
        BlockPos bare = h.absolutePos(new BlockPos(0, 1, 0));
        h.assertTrue(Lightning.strike(level, bare, 2) == null, "a strike on bare stone must not drop a limb");
        h.succeed();
    }
}

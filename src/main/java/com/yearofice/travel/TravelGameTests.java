package com.yearofice.travel;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.travel.Travel.Gear;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.world.season.SeasonState;
import com.yearofice.world.season.SeasonSystem;
import com.yearofice.world.terrain.TerrainChangeQueue;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M7 lane D travel GameTests on far sky platforms (stone at y 100), Standard preset. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class TravelGameTests {
    private TravelGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int Y0 = 100;

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:travel/skate_black_ice", 200, TravelGameTests::skateBlackIce));
        out.add(test("yearofice:travel/ski_vs_snowshoe", 100, TravelGameTests::skiVsSnowshoe));
        out.add(test("yearofice:travel/trail_sets_overnight", 100, TravelGameTests::trailSets));
        out.add(test("yearofice:body/stale_air_vent", 100, TravelGameTests::staleAirVent));
        out.add(test("yearofice:build/quinzhee", 100, TravelGameTests::quinzhee));
        out.add(test("yearofice:fishing/tipup_buried", 100, TravelGameTests::tipupBuried));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m7d", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static LevelChunk platform(ServerLevel l, int cx) {
        LevelChunk ch = l.getChunk(cx, cx);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            at(ch, x, Y0, z, Blocks.STONE.defaultBlockState());
            for (int y = Y0 + 1; y <= Y0 + 6; y++) at(ch, x, y, z, Blocks.AIR.defaultBlockState());
        }
        return ch;
    }

    static void at(LevelChunk ch, int x, int y, int z, BlockState s) {
        ch.setBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, y, ch.getPos().getMinBlockZ() + z), s, false);
    }

    static BlockPos pos(LevelChunk ch, int x, int y, int z) {
        return new BlockPos(ch.getPos().getMinBlockX() + x, y, ch.getPos().getMinBlockZ() + z);
    }

    static void skateBlackIce(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = platform(l, 3430);
        BlockState stone = Blocks.STONE.defaultBlockState();
        for (int z = 1; z <= 14; z++) { at(ch, 4, Y0 + 1, z, stone); at(ch, 7, Y0 + 1, z, stone); }
        for (int x = 4; x <= 7; x++) { at(ch, x, Y0 + 1, 1, stone); at(ch, x, Y0 + 1, 14, stone); }
        for (int z = 2; z <= 13; z++) { at(ch, 5, Y0 + 1, z, Blocks.WATER.defaultBlockState()); at(ch, 6, Y0 + 1, z, Blocks.WATER.defaultBlockState()); }
        SeasonState st = SeasonState.get(l);
        double[] saved = st.core.encode();
        long savedDay = st.core.lastDay;
        var lake = st.core.ice(BodyClass.LAKE);
        lake.cm = 8; lake.peakCm = 8; lake.rotten = false;
        SeasonSystem.settle(l, ch, false, 5, st.core, new TerrainChangeQueue());
        BlockPos feet = pos(ch, 5, Y0 + 2, 5);
        l.setBlock(feet, Blocks.AIR.defaultBlockState(), 2); // black ice: the settle may have laid snow on it, which is not glare ice
        boolean iced = IceBlocks.isIce(l.getBlockState(feet.below()));
        double skate = TravelSystem.multiplier(l, feet, Gear.SKATES, 0), boots = TravelSystem.multiplier(l, feet, Gear.NONE, 0);
        boolean thin = TravelSystem.thinIce(l, feet);
        st.core.decode(saved);
        st.core.lastDay = savedDay;
        st.holes.clear();
        h.assertTrue(iced, "the pond is iced over");
        h.assertTrue(skate >= 1.5 * boots, "skates are fast on ice: " + skate + " vs " + boots);
        h.assertTrue(thin, "8 cm of ice crackles under a skater");
        h.succeed();
    }

    static void skiVsSnowshoe(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = platform(l, 3440);
        at(ch, 5, Y0 + 1, 5, WorldBlocks.SNOW_COVER.get().defaultBlockState().setValue(SnowCoverBlock.LAYERS, 4));
        BlockPos feet = pos(ch, 5, Y0 + 1, 5);
        double skiDown = TravelSystem.multiplier(l, feet, Gear.SKIS, -0.2), shoeDown = TravelSystem.multiplier(l, feet, Gear.SNOWSHOES, -0.2);
        double skiUp = TravelSystem.multiplier(l, feet, Gear.SKIS, 0.2), skinUp = TravelSystem.multiplier(l, feet, Gear.SKIS_SKINS, 0.2);
        h.assertTrue(skiDown > shoeDown, "skis glide past snowshoes downhill: " + skiDown + " vs " + shoeDown);
        h.assertTrue(skinUp > skiUp, "skins grip uphill: " + skinUp + " vs " + skiUp);
        h.succeed();
    }

    static void trailSets(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = platform(l, 3450);
        BlockPos p = pos(ch, 5, Y0 + 1, 5);
        l.setBlock(p, WorldBlocks.SNOW_COVER.get().defaultBlockState().setValue(SnowCoverBlock.LAYERS, 3).setValue(SnowCoverBlock.TRAIL_WEAR, 1), 2);
        int saved = TravelSystem.coldHours;
        TravelSystem.coldHours = 0;
        for (int i = 0; i < 5; i++) TravelSystem.hourStep(l, p, -15);
        h.assertTrue(l.getBlockState(p).getValue(SnowCoverBlock.TRAIL_WEAR) == 1, "five cold hours: still broken");
        TravelSystem.hourStep(l, p, -15);
        int after = l.getBlockState(p).getValue(SnowCoverBlock.TRAIL_WEAR);
        TravelSystem.coldHours = saved;
        h.assertTrue(after == 2, "six cold hours: the trail has set, state " + after);
        h.succeed();
    }

    /** A hollow snow-block box: shell x/z 3..(3+w+1), y Y0+1..Y0+h+2, interior air w x h x w from (4, Y0+2, 4). */
    static void snowBox(LevelChunk ch, int w, int h) {
        BlockState snow = Blocks.SNOW_BLOCK.defaultBlockState();
        for (int x = 3; x <= 4 + w; x++) for (int z = 3; z <= 4 + w; z++) for (int y = Y0 + 1; y <= Y0 + h + 2; y++) at(ch, x, y, z, snow);
        for (int x = 4; x < 4 + w; x++) for (int z = 4; z < 4 + w; z++) for (int y = Y0 + 2; y < Y0 + 2 + h; y++) at(ch, x, y, z, Blocks.AIR.defaultBlockState());
    }

    static void staleAirVent(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = platform(l, 3460);
        snowBox(ch, 3, 2);
        BlockPos head = pos(ch, 5, Y0 + 3, 5);
        var scan = SnowRoom.scan(l, head);
        h.assertTrue(scan.snowRoom() && !scan.vented() && scan.volume() == 18, "a sealed snow room of 18 cells: " + scan);
        double air = 1;
        for (int i = 0; i < 12; i++) air = SnowRoom.step(l, head, air, 60, 2);
        h.assertTrue(com.yearofice.sim.travel.Travel.stale(air), "twelve hours sealed with two people: stale, air " + air);
        l.setBlock(pos(ch, 5, Y0 + 4, 5), Blocks.AIR.defaultBlockState(), 2); // poke a vent through the ceiling
        h.assertTrue(SnowRoom.scan(l, head).vented(), "the hole is a vent");
        for (int i = 0; i < 2; i++) air = SnowRoom.step(l, head, air, 30, 2);
        h.assertTrue(!com.yearofice.sim.travel.Travel.stale(air), "an hour vented: fresh again, air " + air);
        h.succeed();
    }

    static void quinzhee(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = platform(l, 3470);
        snowBox(ch, 2, 1);
        BlockPos head = pos(ch, 4, Y0 + 2, 4);
        h.assertTrue(SnowRoom.quinzhee(l, head) == SnowRoom.Quinzhee.STALE_UNTIL_VENTED, "a dug-out sealed mound: " + SnowRoom.quinzhee(l, head));
        double air = 1;
        for (int i = 0; i < 6; i++) air = SnowRoom.step(l, head, air, 60, 2);
        h.assertTrue(com.yearofice.sim.travel.Travel.stale(air), "six hours sealed turns the air stale: " + air);
        l.setBlock(pos(ch, 4, Y0 + 3, 4), Blocks.AIR.defaultBlockState(), 2);
        h.assertTrue(SnowRoom.quinzhee(l, head) == SnowRoom.Quinzhee.SAFE, "one vent hole makes it safe");
        l.setBlock(pos(ch, 3, Y0 + 2, 4), Blocks.AIR.defaultBlockState(), 2); // a doorway to the open world
        h.assertTrue(SnowRoom.quinzhee(l, head) != SnowRoom.Quinzhee.STALE_UNTIL_VENTED, "an open door is not a sealed room");
        h.succeed();
    }

    static void tipupBuried(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = platform(l, 3480);
        BlockPos p = pos(ch, 5, Y0 + 1, 5);
        l.setBlock(p, com.yearofice.hunt.fishing.FishingRegistry.TIP_UP.get().defaultBlockState(), 3);
        var g = com.yearofice.hunt.fishing.PassiveGearService.at(l, p);
        h.assertTrue(g != null, "the tip-up registered its hook");
        g.held.add(new com.yearofice.hunt.fishing.PassiveGearService.Held(com.yearofice.hunt.fishing.FishingSpecies.GRAYLING, 600));
        l.setBlock(p.above(), WorldBlocks.SNOW_COVER.get().defaultBlockState().setValue(SnowCoverBlock.LAYERS, 5), 2);
        h.assertTrue(TipUpSignal.buried(l, p), "new snow lies over the tip-up");
        h.assertTrue(TipUpSignal.signals(l, p), "it still signals the fish");
        h.assertTrue(com.yearofice.hunt.fishing.PassiveGearService.at(l, p) == g, "the snow did not unregister the gear");
        h.succeed();
    }
}

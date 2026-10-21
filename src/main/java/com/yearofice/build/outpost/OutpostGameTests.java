package com.yearofice.build.outpost;

import com.yearofice.build.wood.WoodGameTests;
import com.yearofice.build.work.WorkService;
import com.yearofice.core.YearOfIce;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** {@code build/outpost_repairable} (Ready for development › M8; Open world in depth › Outposts). */
@GameTestHolder(YearOfIce.MOD_ID)
public final class OutpostGameTests {
    private OutpostGameTests() {}

    @net.minecraft.gametest.framework.GameTestGenerator
    public static Collection<TestFunction> outpostTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(new TestFunction("m8_outposts", "yearofice:build/outpost_repairable", WoodGameTests.PLOT, 400, 0, true, OutpostGameTests::repairable));
        out.add(new TestFunction("m8_outposts", "yearofice:world/drift_bridges_fence", WoodGameTests.PLOT, 100, 0, true, OutpostGameTests::driftBridgesFence));
        out.add(new TestFunction("m8_outposts", "yearofice:build/frost_marks_leaky_seam", WoodGameTests.PLOT, 100, 0, true, OutpostGameTests::frostMarks));
        out.add(new TestFunction("m8_outposts", "yearofice:build/hot_tent_warms", WoodGameTests.EMPTY, 20, 0, true, OutpostGameTests::hotTentWarms));
        out.add(new TestFunction("m8_outposts", "yearofice:build/raft_drifts_and_breaks", WoodGameTests.EMPTY, 60, 0, true, OutpostGameTests::raft));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static void repairable(GameTestHelper h) {
        OutpostDef def = OutpostDef.get("trapline_cabin");
        BlockPos origin = h.absolutePos(new BlockPos(4, 2, 4));
        var l = h.getLevel();
        Outposts.stamp(l, origin, def);
        h.assertTrue(Outposts.missing(l, origin, def).isEmpty(), "a stamped cabin is whole");
        int lost = Outposts.age(l, origin, def, 60, 12345);
        var miss = Outposts.missing(l, origin, def);
        h.assertTrue(lost > 5 && miss.size() == lost, "sixty years takes pieces (" + lost + " lost, " + miss.size() + " missing)");
        h.assertTrue(miss.stream().anyMatch(p -> p.role().equals("roof")), "the roof sags first");
        var p = WoodGameTests.player(h, "outposter", new Vec3(1, 2, 1));
        double min = WorkService.runFor(p, new RepairJob(l, miss), 5000);
        h.assertTrue(min > 0, "the repair job finished");
        h.assertTrue(Outposts.missing(l, origin, def).isEmpty(), "every piece is back");
        h.succeed();
    }

    static int layers(GameTestHelper h, BlockPos rel) {
        int n = 0;
        for (int k = 0; k < 4; k++) {
            var st = h.getBlockState(rel.above(k));
            if (st.is(com.yearofice.worldgen.block.WorldBlocks.SNOW_COVER.get())) n += st.getValue(com.yearofice.worldgen.block.SnowCoverBlock.LAYERS);
        }
        return n;
    }

    static void driftBridgesFence(GameTestHelper h) {
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        for (int x = 3; x <= 12; x++) { h.setBlock(new BlockPos(x, 1, 4), stone); for (int y = 2; y <= 5; y++) h.setBlock(new BlockPos(x, y, 4), net.minecraft.world.level.block.Blocks.AIR); }
        h.setBlock(new BlockPos(3, 2, 4), net.minecraft.world.level.block.Blocks.OAK_FENCE);
        h.setBlock(new BlockPos(3, 3, 4), net.minecraft.world.level.block.Blocks.OAK_FENCE);
        var pack = new com.yearofice.sim.world.SnowPack.State();
        pack.sweMm = 150; pack.densityKgM3 = 100;
        var t = com.yearofice.world.season.SeasonConfig.snow();
        BlockPos base = h.absolutePos(new BlockPos(3, 2, 4));
        var dir = com.yearofice.world.snow.FenceDrift.downwind(270);
        h.assertTrue(dir == net.minecraft.core.Direction.EAST, "a west wind drifts east");
        int thin = com.yearofice.world.snow.FenceDrift.build(h.getLevel(), base, dir, 20, pack, t);
        int near1 = layers(h, new BlockPos(4, 2, 4));
        h.assertTrue(thin > 0 && near1 < 16, "a light pack leaves a low drift (" + near1 + " layers)");
        com.yearofice.world.snow.FenceDrift.build(h.getLevel(), base, dir, 150, pack, t);
        int near = layers(h, new BlockPos(4, 2, 4)), far = layers(h, new BlockPos(9, 2, 4)), behind = layers(h, new BlockPos(2, 2, 4));
        h.assertTrue(near == 16, "the drift against the fence reaches its top (" + near + " layers of 16)");
        h.assertTrue(far > 0 && far < near, "it thins downwind (" + far + ")");
        h.assertTrue(behind == 0, "none builds on the windward side");
        int again = com.yearofice.world.snow.FenceDrift.build(h.getLevel(), base, dir, 20, pack, t);
        h.assertTrue(again == 0 && layers(h, new BlockPos(4, 2, 4)) == 16, "a drift never shrinks");
        h.succeed();
    }

    static void frostMarks(GameTestHelper h) {
        var air = net.minecraft.world.level.block.Blocks.AIR;
        var log = net.minecraft.world.level.block.Blocks.STRIPPED_SPRUCE_LOG;
        for (int x = 0; x <= 10; x++) for (int z = 2; z <= 9; z++) for (int y = 2; y <= 6; y++) h.setBlock(new BlockPos(x, y, z), air);
        for (int x = 3; x <= 7; x++) for (int z = 3; z <= 7; z++) {
            boolean edge = x == 3 || x == 7 || z == 3 || z == 7;
            for (int y = 2; y <= 3; y++) if (edge) h.setBlock(new BlockPos(x, y, z), z == 3 ? net.minecraft.world.level.block.Blocks.SPRUCE_PLANKS : log);
        }
        for (int x = 3; x <= 8; x++) for (int z = 3; z <= 7; z++) h.setBlock(new BlockPos(x, 4, z), net.minecraft.world.level.block.Blocks.SPRUCE_SLAB);
        h.setBlock(new BlockPos(8, 5, 5), com.yearofice.worldgen.block.WorldBlocks.SNOW_COVER.get().defaultBlockState());
        var seams = Tells.leakySeams(h.getLevel(), h.absolutePos(new BlockPos(5, 3, 5)), 6);
        h.assertTrue(seams.contains(h.absolutePos(new BlockPos(3, 2, 5))), "a log wall with warm air inside and open air outside is a seam");
        h.assertTrue(!seams.contains(h.absolutePos(new BlockPos(5, 2, 3))), "a planked wall is not");
        h.assertTrue(!Tells.roofEdges(h.getLevel(), h.absolutePos(new BlockPos(5, 3, 5)), 6).isEmpty(), "a snow-capped roof edge over open air is found");
        var cold = com.yearofice.sim.build.CabinTells.frost(15, -25, 2);
        h.assertTrue(cold > 0.5 && com.yearofice.sim.build.CabinTells.frost(15, 5, 2) == 0 && com.yearofice.sim.build.CabinTells.frost(15, -10, 2) < cold,
                "frost grows with the gap and never forms above freezing (" + cold + ")");
        h.assertTrue(com.yearofice.sim.build.CabinTells.icicleCm(30, 18, -10, 5) > 10 && com.yearofice.sim.build.CabinTells.icicleCm(30, 0, -10, 5) == 0,
                "icicles grow under a warm roof only");
        h.assertTrue(com.yearofice.sim.build.CabinTells.dripsPerMin(18, -1) > 0 && com.yearofice.sim.build.CabinTells.dripsPerMin(18, -30) == 0, "drips need a thaw edge");
        h.succeed();
    }

    static void hotTentWarms(GameTestHelper h) {
        double loss = com.yearofice.sim.build.TentHeat.lossWpc(OutpostConfig.get("tent_loss_canvas_wpc"), OutpostConfig.get("tent_floor_boughs_wpc"), 0, 0);
        double kw = OutpostConfig.get("tent_stove_kw"), kept = OutpostConfig.get("tent_heat_kept"), cap = OutpostConfig.get("tent_heat_capacity_whc");
        double in = -30;
        for (int i = 0; i < 24; i++) in = com.yearofice.sim.build.TentHeat.step(in, -30, kw * 1000 * kept, loss, cap, 0.25);
        h.assertTrue(in >= 12, "fed at Steady the tent holds +12 or warmer at -30 (" + in + ")");
        double dead = com.yearofice.sim.build.TentHeat.step(in, -30, 0, loss, cap, 0.5);
        h.assertTrue(dead <= -20, "thirty minutes after the fire dies it is within 10 of the outside (" + dead + ")");
        double holed = com.yearofice.sim.build.TentHeat.steadyC(-30, kw, kept, com.yearofice.sim.build.TentHeat.lossWpc(OutpostConfig.get("tent_loss_canvas_wpc"),
                OutpostConfig.get("tent_floor_boughs_wpc"), 1, OutpostConfig.get("tent_spark_hole_wpc")));
        h.assertTrue(holed < in, "a spark hole cools the tent");
        double hot = OutpostConfig.get("tent_spark_hole_hot");
        h.assertTrue(com.yearofice.sim.build.TentHeat.sparkHoleChance(hot, false, true) == 0 && com.yearofice.sim.build.TentHeat.sparkHoleChance(hot, true, false) < hot, "a screen stops sparks, hide is safer");
        h.assertTrue(OutpostRegistry.TENT_STOVE.get() != null && OutpostRegistry.SPARK_SCREEN.get() != null, "the kit is registered");
        h.succeed();
    }

    static void raft(GameTestHelper h) {
        var m = com.yearofice.sim.build.RaftModel.class;
        h.assertTrue(com.yearofice.sim.build.RaftModel.speedMs(1.2, false) == 1.2 && com.yearofice.sim.build.RaftModel.speedMs(0.3, true) < 0.1
                && com.yearofice.sim.build.RaftModel.speedMs(1.2, true) == 1.2, "it drifts at the current and is poled upstream in slow water only");
        h.assertTrue(com.yearofice.sim.build.RaftModel.breakPerS(1.5, 150) == 0 && com.yearofice.sim.build.RaftModel.breakPerS(2.5, 150) > 0.1
                && com.yearofice.sim.build.RaftModel.breakPerS(1.0, 260) > 0, "calm water spares a fair load, rapids and overloads do not");
        var raft = new com.yearofice.build.raft.RaftEntity(com.yearofice.build.raft.RaftRegistry.RAFT.get(), h.getLevel());
        net.minecraft.world.phys.Vec3 at = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        raft.setPos(at);
        raft.setNoGravity(true);
        raft.noPhysics = true;
        h.getLevel().addFreshEntity(raft);
        var rng = new java.util.SplittableRandom(77);
        for (int i = 0; i < 100; i++) h.assertTrue(raft.step(new net.minecraft.world.phys.Vec3(1, 0, 0), 1.2, false, rng), "calm water never breaks it");
        double dx = raft.getX() - at.x;
        h.assertTrue(dx > 5.0 && dx < 6.5, "100 ticks at 1.2 m/s is about 6 m (" + dx + ")");
        int ticks = 0;
        while (raft.step(new net.minecraft.world.phys.Vec3(1, 0, 0), 2.6, false, rng) && ticks < 1000) ticks++;
        h.assertTrue(ticks < 1000 && raft.isRemoved(), "rapids break it up (" + ticks + " ticks)");
        h.succeed();
    }
}

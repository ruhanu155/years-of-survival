package com.yearofice.build.dugout;

import com.yearofice.api.world.ChangeOp;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.stove.StoveBlockEntity;
import com.yearofice.fire.stove.StoveRegistry;
import com.yearofice.sim.build.Dugout;
import com.yearofice.sim.fire.Wood;
import com.yearofice.world.terrain.TerrainApply;
import com.yearofice.world.terrain.TerrainChangeQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M8 lane B, dugouts: {@code build/dugout_never_yields} and {@code build/dugout_heat}. Each builds a 4 x 2 x 6 hillside
 * dugout on its own platform in far chunks, so no neighbouring test sees it.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class DugoutGameTests {
    private DugoutGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int Y0 = 100;

    @GameTestGenerator
    public static Collection<TestFunction> dugoutTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:build/dugout_never_yields", 400, DugoutGameTests::neverYields));
        out.add(test("yearofice:build/dugout_heat", 400, DugoutGameTests::heat));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m8b_dugout", name, TEMPLATE, maxTicks, 0, true, body);
    }

    /** A flat stone platform over one chunk, clear above, and a hillside dugout at its middle; returns the first interior cell. */
    static BlockPos site(ServerLevel l, int cx, int cz) {
        LevelChunk ch = l.getChunk(cx, cz);
        int x0 = ch.getPos().getMinBlockX(), z0 = ch.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            l.setBlock(new BlockPos(x0 + x, Y0 - 1, z0 + z), Blocks.STONE.defaultBlockState(), 2);
            for (int y = Y0; y <= Y0 + 8; y++) l.setBlock(new BlockPos(x0 + x, y, z0 + z), Blocks.AIR.defaultBlockState(), 2);
        }
        // front faces south; the room runs north from its south wall; width runs west
        return DugoutTemplate.build(l, new BlockPos(x0 + 10, Y0, z0 + 9), Direction.SOUTH, Dugout.Kind.HILLSIDE, 4, 2, 6);
    }

    static void neverYields(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos a = site(l, 3200, 3200), b = site(l, 3201, 3200);
        DugoutService.Result ra = DugoutService.validate(l, a, Dugout.Kind.HILLSIDE), rb = DugoutService.validate(l, b, Dugout.Kind.HILLSIDE);
        h.assertTrue(ra.valid(), "the template is a valid hillside dugout: " + ra.fault() + " " + ra.wide() + "x" + ra.high() + "x" + ra.deep());
        h.assertTrue(rb.valid(), "so is the control: " + rb.fault());
        h.assertTrue(DugoutService.seal(l, ra) == ra.shell().size() && ra.shell().size() > 80, "sealed " + ra.shell().size() + " shell blocks");
        Map<BlockPos, BlockState> before = new HashMap<>();
        for (BlockPos p : ra.shell()) before.put(p, l.getBlockState(p));
        Map<BlockPos, BlockState> beforeB = new HashMap<>();
        for (BlockPos p : rb.shell()) beforeB.put(p, l.getBlockState(p));
        TerrainChangeQueue qa = new TerrainChangeQueue(), qb = new TerrainChangeQueue();
        int seq = 0;
        // every producer's op kind aimed at every wall, roof and floor block
        for (ChangeOp.Kind k : ChangeOp.Kind.values()) {
            for (BlockPos p : ra.shell()) qa.submit(new ChangeOp(1, k, 0, p, "minecraft:air", false, seq++));
            for (BlockPos p : rb.shell()) qb.submit(new ChangeOp(1, k, 0, p, "minecraft:air", false, seq++));
        }
        LevelChunk ca = l.getChunk(3200, 3200), cb = l.getChunk(3201, 3200);
        TerrainApply.apply(l, ca, qa, 10, true, Integer.MAX_VALUE);
        TerrainApply.apply(l, cb, qb, 10, true, Integer.MAX_VALUE);
        int lost = 0, lostB = 0;
        for (BlockPos p : ra.shell()) if (l.getBlockState(p) != before.get(p)) lost++;
        for (BlockPos p : rb.shell()) if (l.getBlockState(p) != beforeB.get(p)) lostB++;
        h.assertTrue(lostB > 60, "the control (unsealed) dugout should yield to the ops, lost only " + lostB);
        h.assertTrue(lost == 0, "a sealed dugout lost " + lost + " shell blocks to terrain ops");
        h.succeed();
    }

    static void heat(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos origin = site(l, 3202, 3200);
        Direction side = Direction.SOUTH.getClockWise(), back = Direction.NORTH;
        BlockPos stovePos = origin.relative(back, 5).relative(side, 0);
        l.setBlock(stovePos, StoveRegistry.MASONRY_STOVE.get().defaultBlockState(), 3);
        for (int i = 1; i <= 4; i++) l.setBlock(stovePos.above(i), StoveRegistry.CHIMNEY_PIPE.get().defaultBlockState(), 3);
        StoveBlockEntity stove = (StoveBlockEntity) l.getBlockEntity(stovePos);
        stove.pause(true);
        stove.loadFuel(Wood.BIRCH, 10, 3.0);
        h.assertTrue(stove.light(), "the hearth lights");
        h.assertTrue(stove.flued() && stove.chimneyM() == 4, "four pipes through the roof: " + stove.chimneyM());
        double w = stove.outputW();
        h.assertTrue(w > 1000, "the hearth gives kilowatts, was " + w);
        DugoutService.Result r = DugoutService.validate(l, origin, Dugout.Kind.HILLSIDE);
        h.assertTrue(r.valid(), "a dugout with a hearth and a flue is valid: " + r.fault());
        h.assertTrue(r.hearth() && r.flue(), "hearth " + r.hearth() + " flue " + r.flue());
        double cold = -30, ground = 1;
        double with = DugoutService.interiorC(r, cold, ground, w), without = DugoutService.interiorC(r, cold, ground, 0);
        double hut = Dugout.interiorC(cold, ground, w, 0, r.earthFaces() + r.woodFaces());
        h.assertTrue(with >= 5, "at -30 C outside the lit dugout stays above 5 C, was " + with);
        h.assertTrue(without > cold + 10, "an unheated dugout still holds 10 K over the air, was " + without + " vs " + cold);
        h.assertTrue(with - hut >= 20, "earth cover beats a plank hut with the same stove by 20 K: " + with + " vs " + hut);
        // knock out the pipe through the room: the hearth has no flue and the dugout fails its check
        l.setBlock(stovePos.above(), Blocks.AIR.defaultBlockState(), 3);
        DugoutService.Result bad = DugoutService.validate(l, origin, Dugout.Kind.HILLSIDE);
        h.assertTrue("hearth_without_flue".equals(bad.fault()), "no fire without a flue, was " + bad.fault());
        h.succeed();
    }
}

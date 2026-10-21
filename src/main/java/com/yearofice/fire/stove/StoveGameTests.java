package com.yearofice.fire.stove;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.Wood;
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

/** M8 lane B, stoves: {@code fire/stove_draft} and {@code fire/creosote_builds}. Each pauses the stove's live clock and steps minutes itself. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class StoveGameTests {
    private StoveGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos S = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> stoveTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:fire/stove_draft", 200, StoveGameTests::stoveDraft));
        out.add(test("yearofice:fire/creosote_builds", 400, StoveGameTests::creosoteBuilds));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m8b_fire", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static StoveBlockEntity stove(GameTestHelper h, net.minecraft.world.level.block.Block block) {
        h.setBlock(S.below(), Blocks.STONE);
        h.setBlock(S, block);
        StoveBlockEntity be = h.getBlockEntity(S);
        be.pause(true);
        return be;
    }

    /** Feeds one armload at a time and burns it out; returns the creosote after {@code n} loads. */
    static double burnLoads(StoveBlockEntity be, Wood w, int moisture, int n, boolean dryStarter) {
        for (int i = 0; i < n; i++) {
            if (dryStarter) be.loadFuel(Wood.BIRCH, 10, 0.1);
            be.loadFuel(w, moisture, 1.0 - (dryStarter ? 0.1 : 0));
            if (!be.light()) throw new IllegalStateException("did not light");
            be.stepMinutes(100_000);
        }
        return be.creosote();
    }

    /** Draft rises with the pipe: no pipe smokes the room, each metre burns faster and heats more, up to a cap. */
    static void stoveDraft(GameTestHelper h) {
        StoveBlockEntity be = stove(h, StoveRegistry.MASONRY_STOVE.get());
        h.setBlock(S.above(), StoveRegistry.CHIMNEY_PIPE.get());
        be.pinChimney(Double.NaN);
        h.assertTrue(be.chimneyM() == 1.0, "one pipe counted, was " + be.chimneyM());
        h.setBlock(S.above(2), Blocks.STONE);
        h.assertTrue(!be.topOpen() && !be.flued(), "a capped pipe is not a flue");
        h.setBlock(S.above(), Blocks.AIR);
        h.setBlock(S.above(2), Blocks.AIR);
        be.loadFuel(Wood.BIRCH, 10, 3.0);
        h.assertTrue(be.light(), "dry birch lights");
        be.setting(FireSetting.STEADY);
        double prevDraft = -1, prevW = -1, prevMin = Double.MAX_VALUE;
        for (double m : new double[] {0, 1, 2, 3, 4, 6}) {
            be.pinChimney(m);
            h.assertTrue(be.flued() == (m >= 1), "flued at " + m + " m is " + be.flued());
            h.assertTrue(be.draft() > prevDraft, "draft rises: " + be.draft() + " at " + m + " m");
            h.assertTrue(be.outputW() > prevW, "output rises: " + be.outputW() + " W at " + m + " m");
            h.assertTrue(be.minutesPerArmload() < prevMin, "an armload burns faster at " + m + " m");
            prevDraft = be.draft();
            prevW = be.outputW();
            prevMin = be.minutesPerArmload();
        }
        be.pinChimney(4);
        h.assertTrue(Math.abs(be.draft() - 1.0) < 1e-9, "ideal pipe is draft 1.0, was " + be.draft());
        h.assertTrue(be.outputW() > 1000 && be.outputW() < 8000, "a masonry stove at Steady gives kilowatts, was " + be.outputW());
        be.pinChimney(0);
        be.stepMinutes(10);
        h.assertTrue(be.smokePct() > 50, "an unflued stove fills the room: smoke " + be.smokePct());
        be.pinChimney(4);
        be.stepMinutes(10);
        h.assertTrue(be.smokePct() < 1, "a chimney clears it: smoke " + be.smokePct());
        BlockPos abs = h.absolutePos(S);
        var heat = com.yearofice.api.fire.StoveHeatLookup.INSTANCE.near(h.getLevel(), abs, 3);
        h.assertTrue(heat.size() == 1 && heat.get(0).flued() && Math.abs(heat.get(0).watts() - be.outputW()) < 1, "the stove posts its output: " + heat);
        boolean asFire = com.yearofice.api.fire.FireHeatLookup.INSTANCE.fires(h.getLevel()).stream()
                .anyMatch(f -> f.x() == abs.getX() && f.y() == abs.getY() && f.z() == abs.getZ() && f.kW() > 1);
        h.assertTrue(asFire, "lane A and the body read it as a fire with kW");
        be.douse();
        h.assertTrue(com.yearofice.api.fire.StoveHeatLookup.INSTANCE.near(h.getLevel(), abs, 3).isEmpty(), "an out stove posts nothing");
        h.succeed();
    }

    /** Creosote per armload by wood and wetness, 3 an hour on Low, a warning at 60, sweeping clears it, Hot over 70 burns the chimney. */
    static void creosoteBuilds(GameTestHelper h) {
        StoveBlockEntity be = stove(h, StoveRegistry.BARREL_STOVE.get());
        be.pinChimney(3.5);
        double birch = burnLoads(be, Wood.BIRCH, 10, 10, false);
        h.assertTrue(Math.abs(birch - 10) < 0.01, "10 seasoned birch armloads add 10, was " + birch);
        be.sweep();
        double spruce = burnLoads(be, Wood.SPRUCE, 15, 10, false);
        h.assertTrue(Math.abs(spruce - 20) < 0.01, "10 seasoned spruce armloads add 20, was " + spruce);
        be.sweep();
        double green = burnLoads(be, Wood.SPRUCE, 60, 10, true);
        h.assertTrue(Math.abs(green - 46) < 0.01, "10 loads of 0.1 dry birch and 0.9 green spruce add 10 x (0.1 + 4.5), was " + green);
        h.assertTrue(green > spruce && spruce > birch, "wet wood is worse");
        burnLoads(be, Wood.SPRUCE, 60, 4, true);
        h.assertTrue(be.creosote() >= 60, "the messenger's warning line is 60, was " + be.creosote());
        be.sweep();
        h.assertTrue(be.creosote() == 0, "sweeping resets creosote");
        be.setting(FireSetting.LOW);
        double low = burnLoads(be, Wood.BIRCH, 10, 1, false);
        h.assertTrue(low > 20, "an armload on Low adds its 1 plus 3 an hour (about 27), was " + low);
        be.sweep();
        // a Hot fire over 70 eventually lights the chimney (rolls are seeded by position, so this is the same every run)
        h.setBlock(S.above(), StoveRegistry.CHIMNEY_PIPE.get());
        be.setting(FireSetting.HOT);
        for (int i = 0; i < 600 && be.chimneyFires() == 0; i++) {
            be.creosote(95);
            be.loadFuel(Wood.BIRCH, 10, 1.0);
            be.light();
            be.stepMinutes(60);
        }
        h.assertTrue(be.chimneyFires() > 0, "no chimney fire in 600 Hot hours at 95 creosote");
        h.assertTrue(!be.lit() && be.creosote() == 0, "the chimney fire puts the stove out and burns the soot off");
        h.assertTrue(h.getBlockState(S.above()).is(com.yearofice.fire.FireRegistry.FLAME.get()), "the pipe burns: " + h.getBlockState(S.above()));
        be.setting(FireSetting.STEADY);
        be.creosote(95);
        h.setBlock(S.above(), StoveRegistry.CHIMNEY_PIPE.get());
        int fires = be.chimneyFires();
        for (int i = 0; i < 300; i++) { be.loadFuel(Wood.BIRCH, 10, 1.0); be.light(); be.stepMinutes(60); be.creosote(95); }
        h.assertTrue(be.chimneyFires() == fires, "a Steady fire never lights the chimney, only Hot");
        h.succeed();
    }
}

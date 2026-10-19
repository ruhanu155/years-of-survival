package com.yearofice.fire.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireBurn;
import com.yearofice.fire.FireConfig;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlock;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.fire.FireWeather;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, fire: a fire made from parts on a real server. Names follow the Blueprint's GameTest ids
 * ({@code yearofice:fire/...}), so the tests are generated rather than one method each. Every test builds its ring on
 * a stone block inside {@code gametest/empty3} and pins the ring's weather, so world weather never leaks in.
 * Long spans run through {@link FireRingBlockEntity#stepMinutes}, the closed-form catch-up; the slice test runs on the
 * real tick at the 24-minute day (20 ticks a game minute).
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class FireGameTests {
    private FireGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos RING = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> fireTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:fire/slice_fire_from_parts", 1800, FireGameTests::sliceFireFromParts));
        out.add(test("yearofice:fire/in_order_lights", 200, FireGameTests::inOrderLights));
        out.add(test("yearofice:fire/out_of_order_does_not_light", 200, FireGameTests::outOfOrderDoesNotLight));
        out.add(test("yearofice:fire/rain_puts_out_low", 200, FireGameTests::rainPutsOutLow));
        out.add(test("yearofice:fire/banked_coals_6h", 200, FireGameTests::bankedCoals6h));
        out.add(test("yearofice:fire/light_by_state", 400, FireGameTests::lightByState));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static FireRingBlockEntity ring(GameTestHelper h) {
        h.setBlock(RING.below(), Blocks.STONE);
        h.setBlock(RING, FireRegistry.FIRE_RING.get());
        FireRingBlockEntity be = h.getBlockEntity(RING);
        be.setWeatherOverride(FireWeather.CALM);
        return be;
    }

    static void add(GameTestHelper h, FireRingBlockEntity be, ItemStack s) {
        h.assertTrue(be.addPart(s, FireConfig.tuning()).ok(), "could not add " + s);
    }

    /** Bark tinder, two handfuls of kindling and 2 armloads (4 spruce logs at half an armload each), in order. */
    static void layInOrder(GameTestHelper h, FireRingBlockEntity be) {
        add(h, be, new ItemStack(FireRegistry.TINDER_BUNDLE.get()));
        add(h, be, new ItemStack(FireRegistry.KINDLING.get()));
        add(h, be, new ItemStack(FireRegistry.KINDLING.get()));
        for (int i = 0; i < 4; i++) add(h, be, new ItemStack(Items.SPRUCE_LOG));
    }

    static void lightWithMatch(GameTestHelper h, FireRingBlockEntity be) {
        h.assertTrue(be.strikeMatch(null, new ItemStack(FireRegistry.MATCH.get()), 0.0).ok(), "the match did not take");
        be.puff();
        be.puff();
    }

    static FireBurn burn(GameTestHelper h) { return h.getBlockState(RING).getValue(FireRingBlock.BURN); }

    /**
     * Ready for development › M2.5 test 1: bark, kindling and 2 armloads reach a fire in under 10 game minutes and
     * coals in 60 ±10, on the block entity's own tick.
     */
    static void sliceFireFromParts(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        layInOrder(h, be);
        h.assertTrue(Math.abs(be.fire().armloads() - 2.0) < 1e-9, "armloads " + be.fire().armloads());
        lightWithMatch(h, be);
        h.assertTrue(be.fire().state() == FireState.TINDER, "tinder flame, was " + be.fire().state());
        long start = h.getTick();
        long[] fireAt = {-1};
        h.onEachTick(() -> {
            long minutes = (h.getTick() - start) / 20;
            FireState s = be.fire().state();
            if (s == FireState.FIRE && fireAt[0] < 0) fireAt[0] = minutes;
            if (s == FireState.OUT) h.fail("the fire went out at minute " + minutes);
            if (s == FireState.COALS) {
                h.assertTrue(fireAt[0] >= 0 && fireAt[0] < 10, "a fire after " + fireAt[0] + " game min");
                h.assertTrue(minutes >= 50 && minutes <= 70, "coals after " + minutes + " game min");
                h.assertTrue(burn(h) == FireBurn.COALS, "block state " + burn(h));
                h.succeed();
            }
        });
    }

    /** Laid in order through the player's right mouse, then a match: it lights and the lay catches. */
    static void inOrderLights(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        for (ItemStack s : List.of(new ItemStack(FireRegistry.TINDER_BUNDLE.get()), new ItemStack(FireRegistry.KINDLING.get(), 2),
                new ItemStack(Items.SPRUCE_LOG, 2))) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            int n = s.getCount();
            for (int i = 0; i < n; i++) h.useBlock(RING, p);
            h.assertTrue(p.getMainHandItem().isEmpty(), "the ring did not take every " + s.getItem());
        }
        h.assertTrue(be.fire().layInOrder(), "the lay should be in order");
        h.assertTrue(h.getBlockState(RING).getValue(com.yearofice.fire.FireRingBlock.LAY) == com.yearofice.fire.FireLay.FUEL, "lay shows fuel");
        lightWithMatch(h, be);
        be.stepMinutes(5);
        h.assertTrue(be.fire().state() == FireState.FIRE, "a fire after 5 min, was " + be.fire().state());
        h.assertTrue(burn(h) == FireBurn.STEADY, "block state " + burn(h));
        h.succeed();
    }

    /** Fuel on bare tinder, then kindling: the match will not take and nothing burns. */
    static void outOfOrderDoesNotLight(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        add(h, be, new ItemStack(FireRegistry.TINDER_BUNDLE.get()));
        add(h, be, new ItemStack(Items.SPRUCE_LOG));
        add(h, be, new ItemStack(FireRegistry.KINDLING.get()));
        add(h, be, new ItemStack(FireRegistry.KINDLING.get()));
        h.assertFalse(be.fire().layInOrder(), "the lay should be out of order");
        ItemStack match = new ItemStack(FireRegistry.MATCH.get(), 3);
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertFalse(be.strikeMatch(p, match, 0.0).ok(), "an out-of-order lay must not light");
        h.assertTrue(match.getCount() == 3, "a lay that cannot light does not spend the match");
        be.stepMinutes(10);
        h.assertTrue(be.fire().state() == FireState.OUT, "state " + be.fire().state());
        h.assertTrue(burn(h) == FireBurn.OUT && h.getBlockState(RING).getLightEmission(h.getLevel(), h.absolutePos(RING)) == 0, "dark and out");
        // Taking the fuel and kindling back off and relaying fixes it.
        for (int i = 0; i < 3; i++) h.assertFalse(be.takeBack().isEmpty(), "take back part " + i);
        add(h, be, new ItemStack(FireRegistry.KINDLING.get()));
        add(h, be, new ItemStack(FireRegistry.KINDLING.get()));
        add(h, be, new ItemStack(Items.SPRUCE_LOG));
        h.assertTrue(be.strikeMatch(p, match, 0.0).ok(), "relaid in order it lights");
        h.succeed();
    }

    /** Blueprint › Rain and snow: heavy rain drops an uncovered fire one state every 10 game minutes; Low is the last. */
    static void rainPutsOutLow(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        be.forceState(FireState.FIRE, FireSetting.LOW);
        be.setFuel(2);
        be.setWeatherOverride(FireWeather.heavyRain());
        be.stepMinutes(9);
        h.assertTrue(be.fire().state() == FireState.FIRE && burn(h) == FireBurn.LOW, "still Low at 9 min: " + burn(h));
        be.stepMinutes(1);
        h.assertTrue(be.fire().state() == FireState.OUT, "out at 10 min, was " + be.fire().state());
        h.assertTrue(burn(h) == FireBurn.OUT, "block state " + burn(h));
        // Under a roof the same rain does nothing.
        be.forceState(FireState.FIRE, FireSetting.LOW);
        be.setFuel(2);
        be.setWeatherOverride(new FireWeather(com.yearofice.sim.env.Precip.RAIN, true, 0, true));
        be.stepMinutes(30);
        h.assertTrue(be.fire().state() == FireState.FIRE, "a covered fire keeps burning, was " + be.fire().state());
        h.succeed();
    }

    /** Blueprint › Fire states: banked coals keep 6 game hours and relight with fresh tinder and one puff; not at 6 h 10 min. */
    static void bankedCoals6h(GameTestHelper h) {
        FireRingBlockEntity be = ring(h);
        be.forceState(FireState.COALS, null);
        h.assertTrue(be.bank().ok(), "bank the coals");
        h.assertTrue(burn(h) == FireBurn.BANKED, "block state " + burn(h));
        be.stepMinutes(360);
        h.assertTrue(be.fire().state() == FireState.BANKED, "embers at 6 h, was " + be.fire().state());
        add(h, be, new ItemStack(FireRegistry.TINDER_BUNDLE.get()));
        h.assertTrue(be.puff().ok(), "one puff relights");
        h.assertTrue(be.fire().state() == FireState.SMALL, "small fire, was " + be.fire().state());

        be.forceState(FireState.COALS, null);
        be.bank();
        be.stepMinutes(370);
        h.assertTrue(be.fire().state() == FireState.OUT, "cold at 6 h 10 min, was " + be.fire().state());
        h.succeed();
    }

    /** Minecraft engine › Fire › Light: the block's light follows the state, and the level's light engine agrees. */
    static void lightByState(GameTestHelper h) {
        // Stone on every side, so light from anything outside the cell (the test framework's markers, other tests)
        // cannot reach the ring: the measured block light is the ring's own.
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 3; y++)
                for (int z = 0; z < 3; z++)
                    if (x != 1 || y != 1 || z != 1) h.setBlock(new BlockPos(x, y, z), Blocks.STONE);
        FireRingBlockEntity be = ring(h);
        BlockPos abs = h.absolutePos(RING);
        GameTestSequence seq = h.startSequence();
        int[][] expect = {
                {FireBurn.SMALL.ordinal(), 9}, {FireBurn.LOW.ordinal(), 9}, {FireBurn.STEADY.ordinal(), 12},
                {FireBurn.HOT.ordinal(), 15}, {FireBurn.COALS.ordinal(), 6}, {FireBurn.BANKED.ordinal(), 0},
                {FireBurn.TINDER.ordinal(), 3}, {FireBurn.EMBER.ordinal(), 0}, {FireBurn.OUT.ordinal(), 0}};
        for (int[] e : expect) {
            FireBurn b = FireBurn.values()[e[0]];
            int level = e[1];
            seq.thenExecute(() -> {
                be.forceState(b.state, b.setting);
                h.assertTrue(burn(h) == b, "block state " + burn(h) + " for " + b);
                h.assertTrue(h.getBlockState(RING).getLightEmission(h.getLevel(), abs) == level, b + " emits " + level);
            }).thenWaitUntil(() -> {
                int got = h.getLevel().getBrightness(LightLayer.BLOCK, abs);
                h.assertTrue(got == level, b + ": light " + got + ", want " + level);
            });
        }
        seq.thenSucceed();
    }
}

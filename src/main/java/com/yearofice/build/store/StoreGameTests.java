package com.yearofice.build.store;

import com.yearofice.api.build.CacheAccess;
import com.yearofice.api.build.StoreService;
import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.build.cellar.CellarBlockEntity;
import com.yearofice.build.cellar.CellarRegistry;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.build.CacheLegs;
import com.yearofice.sim.decay.ColdCurve;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M8 lane B, storage: cache legs, root and permafrost cellars, the smokehouse. */
@GameTestHolder(YearOfIce.MOD_ID)
public final class StoreGameTests {
    private StoreGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos C = new BlockPos(1, 1, 1), HATCH = new BlockPos(1, 1, 0);

    @GameTestGenerator
    public static Collection<TestFunction> storeTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:store/cache_legs_bearproof", 200, StoreGameTests::cacheLegs));
        out.add(test("yearofice:store/root_cellar_temp", 200, StoreGameTests::rootCellar));
        out.add(test("yearofice:store/permafrost_cellar_cold", 200, StoreGameTests::permafrost));
        out.add(test("yearofice:store/smokehouse_cold_smoke", 200, StoreGameTests::smokehouse));
        out.add(test("yearofice:store/shed_dries_faster", 200, StoreGameTests::shed));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m8b_store", name, TEMPLATE, maxTicks, 0, true, body);
    }

    // ---- cache legs ----------------------------------------------------------------------------------------------

    static double reach(GameTestHelper h, String species) { return CacheAccess.INSTANCE.reach(h.getLevel(), h.absolutePos(C), species); }

    /** A grizzly cannot climb but tears a platform under 3.5 m; peeled and banded legs stop squirrels and martens; black bears always climb. */
    static void cacheLegs(GameTestHelper h) {
        h.setBlock(C, StoreRegistry.CACHE_LEGS.get());
        StoreContainerBlockEntity be = h.getBlockEntity(C);
        be.pinLegs(3.5, CacheLegs.Leg.BARKED);
        h.assertTrue(reach(h, "grizzly_bear") == 0, "a grizzly cannot reach a 3.5 m platform on live legs");
        be.pinLegs(3.4, CacheLegs.Leg.BARKED);
        h.assertTrue(reach(h, "grizzly_bear") == 1, "a grizzly tears a 3.4 m platform");
        be.pinLegs(6, CacheLegs.Leg.BANDED);
        h.assertTrue(reach(h, "grizzly_bear") == 0 && reach(h, "black_bear") == 1, "black bears climb anything, a high platform beats a grizzly");
        be.pinLegs(1, CacheLegs.Leg.BANDED);
        h.assertTrue(reach(h, "marten") == 0 && reach(h, "red_squirrel") == 0, "a tin band stops martens and squirrels");
        h.assertTrue(Math.abs(reach(h, "wolverine") - 0.2) < 1e-9, "a banded leg leaves the wolverine 20%");
        be.pinLegs(1, CacheLegs.Leg.PEELED);
        h.assertTrue(Math.abs(reach(h, "marten") - 0.5) < 1e-9 && Math.abs(reach(h, "wolverine") - 0.7) < 1e-9, "peeled legs: marten 50%, wolverine 70%");
        be.pinLegs(1, CacheLegs.Leg.BARKED);
        h.assertTrue(reach(h, "marten") == 1 && reach(h, "wolverine") == 1, "live legs stop nothing small");
        // the real thing: a pole under the platform sets height and dressing
        be.pinLegs(Double.NaN, null);
        h.setBlock(C.below(), StoreRegistry.CACHE_POLE.get().defaultBlockState().setValue(CachePoleBlock.LEG, CachePoleBlock.Dress.BANDED));
        h.assertTrue(be.heightM() == 1 && be.leg() == CacheLegs.Leg.BANDED, "one banded pole: " + be.heightM() + " " + be.leg());
        h.assertTrue(reach(h, "marten") == 0, "a banded pole stops the marten");
        h.assertTrue(CacheAccess.INSTANCE.reach(h.getLevel(), h.absolutePos(C.above()), "marten") == 1, "air is reachable");
        h.succeed();
    }

    // ---- cellars -------------------------------------------------------------------------------------------------

    static CellarBlockEntity cellar(GameTestHelper h, Block hatch, Block wall) {
        for (BlockPos p : List.of(C.east(), C.west(), C.below(), C.above(), C.south())) h.setBlock(p, wall);
        h.setBlock(C, Blocks.AIR);
        h.setBlock(HATCH, hatch.defaultBlockState().setValue(com.yearofice.build.cellar.CellarBlock.FACING, net.minecraft.core.Direction.SOUTH));
        return h.getBlockEntity(HATCH);
    }

    static void rootCellar(GameTestHelper h) {
        CellarBlockEntity be = cellar(h, CellarRegistry.ROOT_CELLAR.get(), Blocks.DIRT);
        be.pinClimate(-30, 2);
        h.assertTrue(be.valid(), "an earth-banked room is a root cellar: " + be.room().fault());
        BlockPos in = h.absolutePos(C);
        double winter = StoreService.INSTANCE.tempC(h.getLevel(), in);
        h.assertTrue(winter > -1.5 && winter < 1.5, "at -30 C outside the cellar holds near the ground, was " + winter);
        be.pinClimate(20, 2);
        double summer = StoreService.INSTANCE.tempC(h.getLevel(), in);
        h.assertTrue(summer > 2 && summer < 4, "at +20 C outside the cellar stays cool, was " + summer);
        double rate = StoreService.INSTANCE.rate(h.getLevel(), in);
        h.assertTrue(rate < 0.5 * ColdCurve.STANDARD.rate(20), "food spoils under half as fast: " + rate + " vs " + ColdCurve.STANDARD.rate(20));
        h.assertTrue(StoreService.INSTANCE.dryness(h.getLevel(), in) == 1, "a cellar is dry");
        // open the wall: no longer a sealed banked room, so the hatch gives nothing
        h.setBlock(C.south(), Blocks.AIR);
        be.pinClimate(-30, 2);
        h.assertTrue(!be.valid(), "a room with a hole in its bank is not a cellar");
        h.assertTrue(Math.abs(StoreService.INSTANCE.tempC(h.getLevel(), in) - com.yearofice.api.env.EnvService.get().airC(h.getLevel(), in)) < 1e-9, "no cellar, the open air");
        h.succeed();
    }

    static void permafrost(GameTestHelper h) {
        CellarBlockEntity be = cellar(h, CellarRegistry.PERMAFROST_CELLAR.get(), Blocks.PACKED_ICE);
        be.pinClimate(20, -2);
        h.assertTrue(be.valid(), "an ice-packed room is a permafrost cellar: " + be.room().fault());
        double t = StoreService.INSTANCE.tempC(h.getLevel(), h.absolutePos(C));
        h.assertTrue(t <= 0, "a packed permafrost cellar holds 0 C or colder in July, was " + t);
        h.assertTrue(StoreService.INSTANCE.rate(h.getLevel(), h.absolutePos(C)) <= ColdCurve.HALF_FROZEN_RATE + 1e-9, "spoil rate no more than half-frozen");
        be.pinClimate(20, -2);
        for (BlockPos p : List.of(C.east(), C.west(), C.above())) h.setBlock(p, Blocks.DIRT);
        be.pinClimate(20, -2);
        h.assertTrue(!be.valid(), "a room mostly lined with earth is not packed: " + be.room().packShare());
        h.succeed();
    }

    // ---- smokehouse ----------------------------------------------------------------------------------------------

    static ItemStack meat(GameTestHelper h, int units) {
        ItemStack s = new ItemStack(EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), units);
        double life = FoodMap.values(s).refLifeDh(FoodState.RAW);
        double d = DecayService.get().dNow(h.getLevel());
        s.set(YoiComponents.FRESH.get(), new Fresh(FoodState.RAW, DecayService.CLOCK_AIR_SLICE, d + life, life, 0, true));
        return s;
    }

    /** Cold smoke needs 48-72 hours of a warm pit; an out pit stalls it; the batch comes out smoked. */
    static void smokehouse(GameTestHelper h) {
        h.setBlock(C, StoreRegistry.SMOKEHOUSE.get());
        SmokehouseBlockEntity be = h.getBlockEntity(C);
        h.assertTrue(be.putOn(meat(h, 50)) == 50, "5 kg fits");
        be.pinPit(false);
        be.stepHours(100);
        h.assertTrue(be.progressH() == 0 && !be.done(), "a cold pit smokes nothing");
        be.pinPit(true);
        be.stepHours(40);
        h.assertTrue(!be.done(), "not smoked at 40 h");
        h.assertTrue(be.neededH() >= 48 && be.neededH() <= 72, "needs " + be.neededH() + " h");
        be.stepHours(33);
        h.assertTrue(be.done(), "smoked by 73 h");
        ItemStack s = be.items().get(0);
        Fresh f = DecayService.get().settle(h.getLevel(), s);
        h.assertTrue(f.state() == FoodState.SMOKED, "state " + f.state());
        h.succeed();
    }

    /** Split wood under the shed's roof dries faster than the same wood on a rack, by the existing seasoning model. */
    static void shed(GameTestHelper h) {
        h.setBlock(C, StoreRegistry.FIREWOOD_SHED.get());
        FirewoodShedBlockEntity be = h.getBlockEntity(C);
        ItemStack split = com.yearofice.build.wood.WoodItems.split(com.yearofice.sim.wood.WoodKind.BIRCH, 4, 50, 30);
        double rack = com.yearofice.build.wood.WoodItems.moistureNow(split.copy(), 60);
        h.assertTrue(be.put(split, 30), "the shed takes split wood");
        ItemStack out = be.takeAll(60).get(0);
        double shed = com.yearofice.build.wood.WoodItems.moisturePct(out);
        h.assertTrue(rack < 50, "the rack dries it: " + rack);
        h.assertTrue(shed < Math.round(rack), "the shed dries it faster: " + shed + " vs rack " + rack);
        h.succeed();
    }
}

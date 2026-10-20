package com.yearofice.food.debug;

import com.yearofice.api.decay.DecayService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.food.preserve.DryingRackBlockEntity;
import com.yearofice.food.preserve.FoodCacheBlockEntity;
import com.yearofice.food.preserve.PreserveRegistry;
import com.yearofice.food.preserve.SmokeRackBlockEntity;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.decay.FreshTrait;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.food.Doneness;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M5 lane D, racks and cache (Structures and buildables › Drying rack and smoke rack; Items › Item decay). They join
 * the cooking batch, which pins the decay clock's air at 5 C, and step their blocks by hand: no test reads the weather
 * or the shared clock.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class PreserveGameTests {
    private PreserveGameTests() {}

    static final BlockPos AT = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> preserveTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(FoodGameTests.test("yearofice:food/drying_rack_two_dry_days", PreserveGameTests::dryingRack));
        out.add(FoodGameTests.test("yearofice:food/hot_smoke_keeps_30_days", PreserveGameTests::hotSmoke));
        out.add(FoodGameTests.test("yearofice:food/cache_mice_loss", PreserveGameTests::cacheMice));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static ItemStack meat(GameTestHelper h, int units) {
        return FoodGameTests.rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), units, null);
    }

    static DryingRackBlockEntity dryRack(GameTestHelper h) {
        h.setBlock(AT.below(), Blocks.STONE);
        h.setBlock(AT, PreserveRegistry.DRYING_RACK.get());
        DryingRackBlockEntity be = h.getBlockEntity(AT);
        be.pause(true);
        be.pinRain(false);
        return be;
    }

    /**
     * Strips dry in 48 dry game hours; one uncovered rain hour adds 6 (54); a full day of rain restarts the batch.
     */
    static void dryingRack(GameTestHelper h) {
        DryingRackBlockEntity be = dryRack(h);
        h.assertTrue(be.putOn(meat(h, 60)) == 60, "a 6 kg load should fit a 15 kg rack");
        be.stepHours(47);
        h.assertTrue(!be.done(), "dry at 47 h");
        be.stepHours(1);
        h.assertTrue(be.done(), "dried at 48 h");
        h.assertTrue(DecayService.get().settle(h.getLevel(), be.items().get(0)).state() == FoodState.DRIED, "strips take the dried state");

        DryingRackBlockEntity wet = dryRack(h);
        wet.takeAll();
        h.assertTrue(wet.putOn(meat(h, 60)) == 60, "reloaded");
        wet.stepHours(9);
        wet.pinRain(true);
        wet.stepHours(1);
        wet.pinRain(false);
        wet.stepHours(43);
        h.assertTrue(!wet.done(), "not dried at 53 h with one rain hour");
        wet.stepHours(1);
        h.assertTrue(wet.done(), "dried at 54 h with one rain hour");

        wet.takeAll();
        h.assertTrue(wet.putOn(meat(h, 60)) == 60, "reloaded again");
        wet.stepHours(40);
        wet.pinRain(true);
        wet.stepHours(24);
        h.assertTrue(wet.progressH() == 0, "a full day of rain restarts the batch, progress " + wet.progressH());
        wet.pinRain(false);
        wet.stepHours(48);
        h.assertTrue(wet.done(), "and a fresh 48 dry hours finish it");

        DryingRackBlockEntity full = dryRack(h);
        full.takeAll();
        h.assertTrue(full.putOn(meat(h, 200)) == 150, "15 kg of 100 g units fills the rack");
        h.succeed();
    }

    /**
     * Over a fire, hot smoke for 8-12 hours makes the strips smoked, and at 5 C they keep 30 days, the fixed life that ends in Stale (60 salted).
     * A dead fire and a gale stall the batch.
     */
    static void hotSmoke(GameTestHelper h) {
        h.setBlock(AT.below().below(), Blocks.STONE);
        FoodGameTests.ring(h, FireSetting.STEADY, 1.0);
        h.setBlock(AT.above(), Blocks.AIR);
        h.setBlock(AT.above(), PreserveRegistry.SMOKE_RACK.get());
        SmokeRackBlockEntity be = h.getBlockEntity(AT.above());
        be.pause(true);
        be.pinConditions(Doneness.Fire.OUT, 0);
        h.assertTrue(be.putOn(meat(h, 50)) == 50, "5 kg fits a 10 kg smoke rack");
        be.stepHours(30);
        h.assertTrue(be.progressH() == 0 && !be.done(), "a dead fire smokes nothing");
        be.pinConditions(Doneness.Fire.STEADY, 12);
        be.stepHours(30);
        h.assertTrue(be.progressH() == 0 && !be.done(), "wind over 8 m/s takes the smoke");
        be.pinConditions(Doneness.Fire.STEADY, 2);
        be.stepHours(7);
        h.assertTrue(!be.done(), "not smoked at 7 h");
        be.stepHours(5);
        h.assertTrue(be.done(), "smoked by 12 h");
        ItemStack s = be.items().get(0);
        Fresh f = DecayService.get().settle(h.getLevel(), s);
        h.assertTrue(f.state() == FoodState.SMOKED, "state " + f.state());
        double days = DecayService.get().daysUntil(h.getLevel(), s, 1.0);
        h.assertTrue(Math.abs(days - 30) <= 1.1, "hot-smoked strips keep " + days + " days at 5 C, want 30 +-1");
        double d = DecayService.get().dNow(h.getLevel());
        s.set(YoiComponents.FRESH.get(), Fresh.stamp(FoodState.SMOKED, DecayService.CLOCK_AIR_SLICE, d,
                com.yearofice.body.intake.FoodMap.values(s).refLifeDh(FoodState.SMOKED), FreshTrait.SALTED.bit(), 40));
        double salted = DecayService.get().daysUntil(h.getLevel(), s, 1.0);
        // a stamp floors the due point to a band (life/40 = 1.5 d at 60 d), the decay model's one-band rule
        h.assertTrue(salted <= 60.1 && salted >= 60 - 1.6, "salted strips keep " + salted + " days, want 60 (one 1.5-day band below)");
        h.assertTrue(Math.abs(salted - 2 * days) <= 1.6, "salt doubles the life: " + days + " -> " + salted);
        h.succeed();
    }

    /** October to April a cache loses 2-3% of its units a week to mice; three deadfalls ring it; May to September none. */
    static void cacheMice(GameTestHelper h) {
        h.setBlock(AT, PreserveRegistry.FOOD_CACHE.get());
        FoodCacheBlockEntity be = h.getBlockEntity(AT);
        for (int i = 0; i < 4; i++) h.assertTrue(be.putOn(meat(h, 64)) == 64, "stack " + i + " should fit");
        int start = 256, weeks = 8;
        be.pinDeadfalls(0);
        int lost = 0;
        for (int w = 0; w < weeks; w++) lost += be.settleWeek(1000 + w, 11);
        double perWeek = lost / (double) start / weeks;
        h.assertTrue(perWeek > 0.018 && perWeek < 0.032, "mice took " + perWeek + " of the cache a week, want 2-3%");
        int held = 0;
        for (ItemStack s : be.items()) held += s.getCount();
        h.assertTrue(held == start - lost, "units held " + held + " after losing " + lost);

        int before = held;
        be.pinDeadfalls(3);
        for (int w = 0; w < weeks; w++) h.assertTrue(be.settleWeek(2000 + w, 11) == 0, "a ringed cache loses nothing");
        be.pinDeadfalls(0);
        for (int w = 0; w < weeks; w++) h.assertTrue(be.settleWeek(3000 + w, 7) == 0, "no mice in July");
        h.assertTrue(be.settleWeek(3007, 7) == 0, "same week twice settles once");
        int after = 0;
        for (ItemStack s : be.items()) after += s.getCount();
        h.assertTrue(after == before, "units unchanged " + after + " vs " + before);
        h.succeed();
    }
}

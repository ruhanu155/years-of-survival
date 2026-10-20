package com.yearofice.hunt.fishing;

import com.yearofice.body.intake.FoodMap;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.sim.fishing.FishSeason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/** M5 lane TUNE (RECONCILE 236): bait in week one, and the pilot bread at the blueprint's 100 kcal. */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class TuneGameTests {
    private TuneGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> tuneTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(FishingGameTests.test("yearofice:food/worm_dig_yields_bait", 100, TuneGameTests::wormDigYieldsBait));
        out.add(FishingGameTests.test("yearofice:fish/offal_as_bait", 200, TuneGameTests::offalAsBait));
        out.add(FishingGameTests.test("yearofice:food/pilot_bread_blueprint_kcal", 60, TuneGameTests::pilotBread));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    /** Soil beside water and soil under trees turn up worms in the thawed months; bare dry soil and frozen ground do not. */
    static void wormDigYieldsBait(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var t = FishingConfig.tuning();
        int month = FishSeason.month(FishSeason.dayOfYear(FishingClock.day(FishingClock.minute(level.getServer()))));
        boolean thawed = month >= 5 && month <= 9;
        var dirt = Blocks.DIRT.defaultBlockState();
        // bare ground, no water, no trees: nothing
        int bare = 0;
        for (int i = 0; i < 40; i++) bare += WormDigging.wormsAt(level, h.absolutePos(new BlockPos(i % 3, 2, i / 3 % 3 + 40 * 0)), dirt, t);
        // under trees: logs three blocks up beside the soil
        for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) h.setBlock(new BlockPos(x, 5, z), Blocks.SPRUCE_LOG);
        int forest = 0;
        for (int i = 0; i < 60; i++) forest += WormDigging.wormsAt(level, h.absolutePos(new BlockPos(i % 3, 2 + i / 30, i / 3 % 3)), dirt, t);
        for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) h.setBlock(new BlockPos(x, 5, z), Blocks.AIR);
        // by the water
        FishingGameTests.pool(h, 2);
        int wet = 0;
        for (int i = 0; i < 60; i++) wet += WormDigging.wormsAt(level, h.absolutePos(new BlockPos(2 + i % 3, 2, i / 3 % 5)), dirt, t);
        FishingGameTests.clearPool(h);
        if (thawed) {
            h.assertTrue(bare == 0, "bare dry soil gave " + bare + " worms");
            h.assertTrue(forest > 0, "forest floor gave no worms in 60 digs");
            h.assertTrue(wet > forest / 2, "riparian soil gave " + wet + " worms against the forest floor's " + forest);
        } else {
            h.assertTrue(forest == 0 && wet == 0, "frozen ground gave worms: " + forest + "/" + wet);
        }
        h.assertTrue(FishBaits.key(new ItemStack(FishingRegistry.WORM.get())).orElse("").equals("worm"), "a worm is bait");
        h.succeed();
    }

    /** Hare guts and a fish are bait: a dressed hare gives guts, the set line and the rod take guts and fish. */
    static void offalAsBait(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        var parts = Butchering.parts(level, CarcassRecord.EMPTY);
        int guts = 0;
        for (ItemStack s : parts) if (s.is(FishingRegistry.FISH_GUTS.get())) guts += s.getCount();
        h.assertTrue(guts == 2, "a 0.2 kg hare gut gives two guts, one per 0.1 kg: " + guts);
        h.assertTrue(FishBaits.key(new ItemStack(FishingRegistry.FISH_GUTS.get())).orElse("").equals("guts"), "guts are bait");
        ItemStack grayling = FishingService.landedFish(300);
        h.assertTrue(FishBaits.key(grayling).orElse("").equals("fish_chunk"), "a whole fish cut up is fish_chunk bait");

        FishingGameTests.pool(h, 2);
        for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) h.setBlock(new BlockPos(x, -1, z), Blocks.STONE);
        ServerPlayer p = FishingGameTests.angler(h);
        FishGearGameTests.use(h, p, new ItemStack(FishingRegistry.SET_LINE_ITEM.get()), new BlockPos(0, -1, 0), Direction.UP);
        BlockPos at = h.absolutePos(new BlockPos(0, 0, 0));
        PassiveGearService.Gear g = PassiveGearService.at(level, at);
        h.assertTrue(g != null, "the set line is placed");
        ItemStack stack = new ItemStack(FishingRegistry.FISH_GUTS.get(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        h.getBlockState(new BlockPos(0, 0, 0)).useItemOn(stack, level, p, InteractionHand.MAIN_HAND, FishGearGameTests.hit(at, Direction.UP));
        h.assertTrue("guts".equals(g.bait) && stack.getCount() == 1, "the set line takes guts and uses one up: " + g.bait);
        PassiveGearService.book(level).all().clear();

        // the rod: a bare hook falls back to guts in the pack (no worm)
        ItemStack rod = new ItemStack(FishingRegistry.ROD.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, rod);
        p.getInventory().add(new ItemStack(FishingRegistry.FISH_GUTS.get(), 1));
        h.assertTrue(FishingService.fitBait(p, rod) && RodItem.bait(rod).bait().equals("guts"), "the rod baits with guts: " + RodItem.bait(rod));
        FishingGameTests.clearPool(h);
        h.setBlock(new BlockPos(0, 0, 0), Blocks.AIR);
        h.succeed();
    }

    /** The pilot-bread cracker is 25 g at 400 kcal per 100 g: 100 kcal, 38 of them are 3,800 kcal (Blueprint › Starting kit; Items › Food values). */
    static void pilotBread(GameTestHelper h) {
        ItemStack cracker = new ItemStack(com.yearofice.item.inventory.KitItems.PILOT_BREAD.get());
        FoodMap.Row row = FoodMap.row(cracker);
        h.assertTrue(row != null, "pilot bread has no food row");
        double grams = com.yearofice.food.Meals.grams(cracker);
        double kcal = row.kcalPer100g() * grams / 100.0;
        h.assertTrue(Math.abs(grams - 25) < 1e-9 && Math.abs(kcal - 100) < 1e-9, "a cracker is " + grams + " g and " + kcal + " kcal, not 25 g and 100 kcal");
        h.assertTrue(kcal * 38 <= 3800 + 1e-9, "the 38-cracker kit is " + kcal * 38 + " kcal");
        h.succeed();
    }
}

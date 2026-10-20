package com.yearofice.hunt.fishing;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.SliceBite;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M5 lane C2 GameTests: gear placed and checked by hand, baits that shift the species draw, and live fish in a spot.
 * The stub spot is {@link FishGameTests#stub}: a weed shelf round the test with pike, burbot and whitefish.
 */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class FishGearGameTests {
    private FishGearGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> gearTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(FishingGameTests.test("yearofice:fish/gill_net_placed_by_hand", 200, FishGearGameTests::gillNetByHand));
        out.add(FishingGameTests.test("yearofice:fish/set_line_catches_overnight", 200, FishGearGameTests::setLineOvernight));
        out.add(FishingGameTests.test("yearofice:fish/bait_shifts_draw", 100, FishGearGameTests::baitShiftsDraw));
        out.add(FishingGameTests.test("yearofice:fish/fish_entity_in_spot", 200, FishGearGameTests::fishEntityInSpot));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static BlockHitResult hit(BlockPos pos, Direction face) { return new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false); }

    static void use(GameTestHelper h, ServerPlayer p, ItemStack stack, BlockPos rel, Direction face) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.useOn(new UseOnContext(h.getLevel(), p, InteractionHand.MAIN_HAND, stack, hit(h.absolutePos(rel), face)));
    }

    /** Four segments placed with the item, one click each: three never fish, the fourth does; RMB hands the fish over. */
    static void gillNetByHand(GameTestHelper h) {
        FishGameTests.with(h, () -> {
            ServerLevel level = h.getLevel();
            FishingGameTests.clearPool(h);
            ServerPlayer p = FishingGameTests.angler(h);
            p.setYRot(-90f); // facing east, so each segment runs along x
            ItemStack segs = new ItemStack(FishingRegistry.GILL_NET_SEGMENT.get(), 4);
            for (int z = 0; z < 3; z++) use(h, p, segs, new BlockPos(0, 0, z), Direction.UP);
            h.assertTrue(h.getBlockState(new BlockPos(2, 0, 2)).is(FishingRegistry.GILL_NET.get()), "a segment is three net blocks in a line");
            long now = FishingClock.minute(level.getServer());
            PassiveGearService.Gear g = PassiveGearService.nearest(level, h.absolutePos(BlockPos.ZERO), PassiveGearService.Kind.GILL_NET, 6);
            h.assertTrue(g != null && g.segments == 3 && segs.getCount() == 1, "three clicks, three joined segments: " + (g == null ? "none" : g.segments));
            g.wingM = 30;
            PassiveGearService.settle(level, g, now + 100L * 1440);
            h.assertTrue(g.held.isEmpty(), "3 segments never catch: " + g.held);
            use(h, p, segs, new BlockPos(0, 2, 0), Direction.UP);
            h.assertTrue(g.segments == 4 && segs.getCount() == 0, "the fourth click joins the same net: " + g.segments);
            ServerPlayer taker = FishingGameTests.angler(h);
            var out = PassiveGearService.check(level, taker, g, now + 300L * 1440);
            h.assertTrue(!out.isEmpty() && taker.getInventory().countItem(out.get(0).getItem()) > 0 && out.get(0).getCount() == 1, "4 segments catch, and checking hands the fish over: " + out);
            h.assertTrue(g.held.isEmpty(), "the net is empty after the check");
            var res = h.getBlockState(new BlockPos(0, 0, 0)).useWithoutItem(level, taker, hit(h.absolutePos(BlockPos.ZERO), Direction.UP));
            h.assertTrue(res.consumesAction(), "right mouse on the net checks it");
            PassiveGearService.book(level).all().clear();
            FishingGameTests.clearPool(h);
            h.succeed();
        });
    }

    /** A set line placed from its item, baited by hand: a bare line catches nothing, a baited one fills over nights. */
    static void setLineOvernight(GameTestHelper h) {
        FishGameTests.with(h, () -> {
            ServerLevel level = h.getLevel();
            FishingGameTests.clearPool(h);
            for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) h.setBlock(new BlockPos(x, -1, z), Blocks.STONE);
            ServerPlayer p = FishingGameTests.angler(h);
            use(h, p, new ItemStack(FishingRegistry.SET_LINE_ITEM.get()), new BlockPos(0, -1, 0), Direction.UP);
            use(h, p, new ItemStack(FishingRegistry.SET_LINE_ITEM.get()), new BlockPos(2, -1, 2), Direction.UP);
            BlockPos bare = h.absolutePos(new BlockPos(2, 0, 2)), baited = h.absolutePos(new BlockPos(0, 0, 0));
            PassiveGearService.Gear a = PassiveGearService.at(level, baited), b = PassiveGearService.at(level, bare);
            h.assertTrue(a != null && b != null && a.hooks == 5, "placing the item sets a five-hook line");
            ItemStack chunk = new ItemStack(FishingRegistry.FISH_CHUNK.get(), 2);
            p.setItemInHand(InteractionHand.MAIN_HAND, chunk);
            h.getBlockState(new BlockPos(0, 0, 0)).useItemOn(chunk, level, p, InteractionHand.MAIN_HAND, hit(baited, Direction.UP));
            h.assertTrue("fish_chunk".equals(a.bait) && chunk.getCount() == 1, "bait goes on the hooks and is used up: " + a.bait);
            long now = a.lastMinute;
            int caught = 0;
            var gotA = PassiveGearService.check(level, p, a, now + 30L * 1440);
            var gotB = PassiveGearService.check(level, p, b, now + 30L * 1440);
            caught = gotA.size();
            h.assertTrue(caught > 0, "a baited set line catches over a month of nights");
            h.assertTrue(gotB.isEmpty(), "a bare line catches nothing");
            h.assertTrue(a.held.isEmpty() && a.bait.isEmpty(), "checking empties the line and takes the bait");
            PassiveGearService.book(level).all().clear();
            h.setBlock(new BlockPos(0, 0, 0), Blocks.AIR);
            h.setBlock(new BlockPos(2, 0, 2), Blocks.AIR);
            h.succeed();
        });
    }

    static double share(List<SliceBite.Inputs> cands, String species) {
        double total = 0, mine = 0;
        for (SliceBite.Inputs c : cands) {
            double p = SliceBite.pBite(c);
            total += p;
            if (c.species().id().equals(species)) mine += p;
        }
        return total == 0 ? 0 : mine / total;
    }

    /** The bait on the hook changes who bites: a spoon favours the pike, and bait in the off hand is fitted to the rod. */
    static void baitShiftsDraw(GameTestHelper h) {
        FishGameTests.with(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos at = h.absolutePos(BlockPos.ZERO);
            long minute = 120L * 1440 + 13 * 60;
            double worm = share(FishingService.candidates(level, at, HookBait.fitted("worm", 2, 30), minute), "northern_pike");
            double spoon = share(FishingService.candidates(level, at, HookBait.fitted("spoon", 99, 999), minute), "northern_pike");
            double none = share(FishingService.candidates(level, at, HookBait.NONE, minute), "northern_pike");
            h.assertTrue(spoon > worm + 0.05, "a spoon shifts the draw to the pike: spoon " + spoon + " worm " + worm);
            h.assertTrue(none == 0, "a bare hook draws nobody");
            ServerPlayer p = FishingGameTests.angler(h);
            ItemStack rod = p.getMainHandItem();
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FishingRegistry.ROE.get(), 3));
            h.assertTrue(FishingService.fitBait(p, rod) && "roe".equals(RodItem.bait(rod).bait()) && p.getOffhandItem().getCount() == 2,
                    "roe in the off hand is fitted and used up: " + RodItem.bait(rod));
            p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FishingRegistry.SPOON.get()));
            h.assertTrue(FishingService.fitBait(p, rod) && "spoon".equals(RodItem.bait(rod).bait()) && p.getOffhandItem().getCount() == 1,
                    "a lure is fitted and kept: " + RodItem.bait(rod));
            h.succeed();
        });
    }

    /** Fish swim in the spot's water, in season, near a player; landing one removes one; out of view they are gone. */
    static void fishEntityInSpot(GameTestHelper h) {
        FishGameTests.with(h, () -> {
            ServerLevel level = h.getLevel();
            FishingGameTests.pool(h, 2);
            BlockPos at = h.absolutePos(BlockPos.ZERO);
            int spawned = FishSpawner.populateAround(level, at);
            FishSpotService.Spot spot = FishSpotService.get().spotsNear(level, at, 8).get(0);
            List<FishEntity> fish = FishSpawner.in(level, spot);
            h.assertTrue(spawned > 0 && fish.size() == spawned, "fish spawn in the spot: " + spawned + " / " + fish.size());
            Set<String> kinds = new HashSet<>();
            for (FishEntity f : fish) {
                kinds.add(f.species());
                h.assertTrue(level.getFluidState(f.blockPosition()).is(net.minecraft.tags.FluidTags.WATER), "each fish is in the water: " + f.blockPosition());
            }
            h.assertTrue(Set.of("northern_pike", "burbot", "humpback_whitefish").containsAll(kinds) && !kinds.isEmpty(), "the spot's species: " + kinds);
            h.assertTrue(FishSpawner.populateAround(level, at) == 0, "a full spot spawns no more");
            String one = fish.get(0).species();
            int before = fish.size();
            h.assertTrue(FishSpawner.removeOne(level, at, one) && FishSpawner.in(level, spot).size() == before - 1, "landing a fish takes one from the water");
            h.assertTrue(FishSpawner.sweep(level) == before - 1 && FishSpawner.in(level, spot).isEmpty(), "with no player in view they are gone");
            FishingGameTests.clearPool(h);
            h.succeed();
        });
    }
}

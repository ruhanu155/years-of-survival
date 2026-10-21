package com.yearofice.gametest;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.encounter.TalkWalk;
import com.yearofice.ecosystem.encounter.WorldToggles;
import com.yearofice.ecosystem.raid.RackRaid;
import com.yearofice.ecosystem.raid.RaidRegistry;
import com.yearofice.ecosystem.raid.ShelterEntry;
import com.yearofice.ecosystem.raid.TheftCacheBlockEntity;
import com.yearofice.ecosystem.theft.TheftService;
import com.yearofice.food.FoodItems;
import com.yearofice.food.grease.BoneGreaseJob;
import com.yearofice.item.component.TrophySize;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.ecosystem.RaidOdds;
import com.yearofice.sim.food.BoneGrease;
import com.yearofice.story.FirstKills;
import com.yearofice.story.Journal;
import com.yearofice.story.collection.RecordBook;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m6d, M6 lane D: door entry and rack raids (A2, A3), bone grease (B5), talk as you walk (B7) and first kills.
 * Ids: {@code ecology/door_entry_rates}, {@code ecology/door_entry_toggle}, {@code encounter/rack_raid_toggle},
 * {@code food/bone_grease_yield}, {@code encounter/talk_prevents_surprise}, {@code story/first_kill_journal}.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class M6DGameTests {
    private M6DGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos DOOR = new BlockPos(1, 2, 1);

    @GameTestGenerator
    public static Collection<TestFunction> m6dTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:ecology/door_entry_rates", 200, M6DGameTests::doorRates));
        out.add(test("yearofice:ecology/door_entry_toggle", 100, M6DGameTests::doorToggle));
        out.add(test("yearofice:encounter/rack_raid_toggle", 100, M6DGameTests::rackToggle));
        out.add(test("yearofice:food/bone_grease_yield", 100, M6DGameTests::boneGrease));
        out.add(test("yearofice:encounter/talk_prevents_surprise", 100, M6DGameTests::talk));
        out.add(test("yearofice:story/first_kill_journal", 100, M6DGameTests::firstKill));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6d", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static SimpleContainer fish() {
        SimpleContainer c = new SimpleContainer(9);
        c.setItem(0, new ItemStack(FoodItems.DRIED_FISH.get(), 20));
        return c;
    }

    static int run(ServerLevel l, BlockPos door, SimpleContainer store, int hours, int hourOfDay, double playerM) {
        int entries = 0;
        for (int i = 0; i < hours; i++) {
            long hourIndex = i * 24L + hourOfDay;
            var ctx0 = ShelterEntry.context(l, door, store, hourIndex * 60);
            var ctx = new RaidOdds.Door(ctx0.doorOpen(), ctx0.night(), false, ctx0.scent(), 0, false, playerM);
            if (ShelterEntry.hour(l, door, store, hourIndex, ctx).isPresent()) {
                entries++;
                store.setItem(0, new ItemStack(FoodItems.DRIED_FISH.get(), 20));
            }
        }
        return entries;
    }

    /** An open cabin door with dried fish inside, the player 50 m away: a fox in 5% +-0.5 of 10,000 night hours, none by day, none shut. */
    static void doorRates(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos door = h.absolutePos(DOOR);
        l.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true), 3);
        WorldToggles.setDoors(null);
        SimpleContainer store = fish();
        int night = run(l, door, store, 10_000, 23, 50);
        h.assertTrue(night >= 450 && night <= 550, "fox entries in 10,000 night hours: " + night + " (want 5% +-0.5, 450-550)");
        int day = run(l, door, store, 10_000, 12, 50);
        h.assertTrue(day == 0, "entries by day: " + day);
        int near = run(l, door, store, 2_000, 23, 10);
        h.assertTrue(near == 0, "entries with the player 10 m away: " + near);
        l.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, false), 3);
        int shut = run(l, door, store, 10_000, 23, 50);
        h.assertTrue(shut == 0, "entries with the door shut: " + shut);
        l.setBlock(door, Blocks.AIR.defaultBlockState(), 3);
        h.succeed();
    }

    /** With the option off, 1,000 nights give no entry. */
    static void doorToggle(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos door = h.absolutePos(DOOR);
        l.setBlock(door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.OPEN, true), 3);
        WorldToggles.setDoors(false);
        try {
            int off = run(l, door, fish(), 1_000, 23, 50);
            h.assertTrue(off == 0, "option off: " + off + " entries in 1,000 nights");
            WorldToggles.setDoors(true);
            int on = run(l, door, fish(), 1_000, 23, 50);
            h.assertTrue(on > 0, "option on: " + on + " entries in 1,000 nights");
        } finally {
            WorldToggles.setDoors(null);
            l.setBlock(door, Blocks.AIR.defaultBlockState(), 3);
        }
        h.succeed();
    }

    /** {@code [world] rack_thieves}: off, the store keeps everything and no cache is written; on, a fox's haul is cached and recoverable. */
    static void rackToggle(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos at = h.absolutePos(DOOR);
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) l.getChunk((at.getX() >> 4) + dx, (at.getZ() >> 4) + dz);
        var ctx = new com.yearofice.sim.ecosystem.Theft.Context(true, false, false, false, false, false, true, 200);
        try {
            WorldToggles.setRacks(false);
            SimpleContainer off = new SimpleContainer(9);
            off.setItem(0, new ItemStack(FoodItems.DRIED_FISH.get(), 40));
            int taken = 0;
            for (int i = 0; i < 2000; i++) if (TheftService.hour(l, at, off, i, ctx).isPresent()) taken++;
            h.assertTrue(taken == 0 && off.getItem(0).getCount() == 40, "toggle off: " + taken + " thefts");
            h.assertTrue(RackRaid.cache(l, at, new ItemStack(FoodItems.DRIED_FISH.get()), 1) == null, "toggle off writes no cache");
            WorldToggles.setRacks(true);
            BlockPos cache = null;
            for (int i = 0; i < 40 && cache == null; i++) cache = RackRaid.cache(l, at, new ItemStack(FoodItems.DRIED_FISH.get(), 2), i);
            h.assertTrue(cache != null, "toggle on: a cache lands 20-60 m away");
            h.assertTrue(l.getBlockState(cache).is(RaidRegistry.THEFT_CACHE.get()), "the block is theft_cache");
            double d = Math.sqrt(cache.distSqr(new BlockPos(at.getX(), cache.getY(), at.getZ())));
            h.assertTrue(d >= 18 && d <= 62, "cache distance " + d);
            var be = (TheftCacheBlockEntity) l.getBlockEntity(cache);
            h.assertTrue(be != null && be.stack().getCount() == 2, "the cache holds the stolen stack");
            l.setBlock(cache, Blocks.AIR.defaultBlockState(), 3);
        } finally {
            WorldToggles.setRacks(null);
        }
        h.succeed();
    }

    /** 8 kg of marrow bone on Low gives 0.40 +-0.02 kg of grease; on Hot, 0.20 +-0.02 kg; the water left is skimmed_broth. */
    static void boneGrease(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        var low = BoneGreaseJob.simmer(l, 8, BoneGrease.Fire.LOW);
        double lowKg = low.grease().getCount() * 0.05;
        h.assertTrue(Math.abs(lowKg - 0.40) <= 0.02, "Low yield " + lowKg + " kg");
        var hot = BoneGreaseJob.simmer(l, 8, BoneGrease.Fire.HOT);
        double hotKg = hot.grease().getCount() * 0.05;
        h.assertTrue(Math.abs(hotKg - 0.20) <= 0.02, "Hot yield " + hotKg + " kg");
        h.assertTrue(!hot.warning().isEmpty() && low.warning().isEmpty(), "a hot pot warns, a low one does not");
        h.assertTrue("skimmed_broth".equals(low.broth().get(YoiComponents.DISH_ID.get())), "the water is skimmed_broth");
        double crushMin = BoneGrease.crushMinutes(8, 20);
        h.assertTrue(Math.abs(crushMin / 60.0 - 2.7) < 0.1, "crushing 8 kg takes " + crushMin / 60.0 + " game hours");
        h.assertTrue(Math.abs(BoneGrease.kcal(lowKg) - 3600) < 200, "kcal " + BoneGrease.kcal(lowKg));
        h.assertTrue(new ItemStack(FoodItems.BONE_GREASE.get()).is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "grease"))), "bone grease counts as grease");
        h.succeed();
    }

    /** 100 runs toward an unaware animal inside 25 m in cover: talking, 5 or fewer surprises and 80 or more move off; not talking, the baseline. */
    static void talk(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        ServerPlayer p = LayerGameTests.player(h, "talker");
        Vec3 pos = h.absoluteVec(new Vec3(1.5, 2, 1.5));
        p.setPos(pos);
        TalkWalk.set(p, true);
        int surprised = 0, off = 0;
        for (int i = 0; i < 100; i++) {
            var o = TalkWalk.meet(p, 20, true, false, false, com.yearofice.sim.decay.Thresholds.stream(l.getSeed(), "talk_test", i));
            if (o == com.yearofice.sim.ecosystem.TalkWalk.Outcome.SURPRISED_DEFENSIVE) surprised++;
            if (o == com.yearofice.sim.ecosystem.TalkWalk.Outcome.MOVES_OFF) off++;
        }
        h.assertTrue(surprised <= 5, "talking: " + surprised + " surprised in 100 runs");
        h.assertTrue(off >= 80, "talking: " + off + " moved off in 100 runs");
        TalkWalk.set(p, false);
        int base = 0;
        for (int i = 0; i < 100; i++)
            if (TalkWalk.meet(p, 20, true, false, false, com.yearofice.sim.decay.Thresholds.stream(l.getSeed(), "talk_test", i)) == com.yearofice.sim.ecosystem.TalkWalk.Outcome.SURPRISED_DEFENSIVE) base++;
        h.assertTrue(base >= 30 && base <= 60, "not talking: baseline " + base + " surprised in 100 runs");
        // The voice reaches an animal inside the radius and adds 40 awareness.
        YoiAnimal moose = EcosystemRegistry.MOOSE.get().create(l);
        moose.setPos(pos.add(30, 0, 0));
        l.addFreshEntity(moose);
        int reached = TalkWalk.emit(p, false);
        h.assertTrue(reached >= 1 && moose.awareness().of(p) >= TalkWalk.AWARENESS_GAIN, "the voice adds " + TalkWalk.AWARENESS_GAIN + " awareness: " + moose.awareness().of(p));
        moose.discard();
        TalkWalk.set(p, false);
        h.succeed();
    }

    /** The first kill of a species writes one journal line, once; a kill with a trophy size reaches the record book. */
    static void firstKill(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        ServerPlayer p = LayerGameTests.player(h, "hunter");
        TrophySize big = new TrophySize("spread_cm", 152, 1.8, "trophy");
        h.assertTrue(FirstKills.onKill(p, "moose", "arrow", big, 100, 3), "first moose is a first");
        h.assertTrue(!FirstKills.onKill(p, "moose", "arrow", null, 200, 4), "second moose is not");
        long n = Journal.entries(p).stream().filter(e -> FirstKills.KIND.equals(e.kind())).count();
        h.assertTrue(n == 1, "one first_kill line after two moose, got " + n);
        h.assertTrue(RecordBook.page(p).get("moose").cls().equals("trophy"), "the record book holds the moose");
        // Through the death event, as the game fires it.
        YoiAnimal hare = EcosystemRegistry.SNOWSHOE_HARE.get().create(l);
        var ev = new LivingDeathEvent(hare, l.damageSources().playerAttack(p));
        FirstKills.onDeath(ev);
        FirstKills.onDeath(ev);
        n = Journal.entries(p).stream().filter(e -> FirstKills.KIND.equals(e.kind())).count();
        h.assertTrue(n == 2, "hare adds one more line, got " + n);
        h.succeed();
    }
}

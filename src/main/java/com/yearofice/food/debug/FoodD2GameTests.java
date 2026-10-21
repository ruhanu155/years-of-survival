package com.yearofice.food.debug;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.decay.DecayService;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.build.work.WorkService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.food.DishMaker;
import com.yearofice.food.FoodConfig;
import com.yearofice.food.FoodItems;
import com.yearofice.food.Meals;
import com.yearofice.food.pan.PanJob;
import com.yearofice.food.pan.PanRegistry;
import com.yearofice.food.pan.PlacerSites;
import com.yearofice.food.preserve.DryingRackBlockEntity;
import com.yearofice.food.preserve.PreserveRegistry;
import com.yearofice.food.preserve.RackWatch;
import com.yearofice.food.traps.DeadfallBlockEntity;
import com.yearofice.food.traps.SnareSite;
import com.yearofice.food.traps.TrapConfig;
import com.yearofice.food.traps.TrapRegistry;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.ecosystem.Snare;
import com.yearofice.sim.food.BlackSand;
import com.yearofice.sim.food.Deadfall;
import com.yearofice.sim.food.DeadfallTuning;
import com.yearofice.sim.food.Dishes;
import com.yearofice.sim.food.Nutrients;
import com.yearofice.sim.food.Placer;
import com.yearofice.worldgen.PlacerParams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M5 lane D2: the named foods and dishes reaching the body, dried weight, rack flies and thieves seam, the deadfalls and
 * the snare funnel, and black sand in the pan with its placer reaches. Food tests join the cooking batch (air pinned at
 * 5 C); the rest step their blocks by hand and pin their own clocks.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class FoodD2GameTests {
    private FoodD2GameTests() {}

    static final BlockPos AT = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> d2Tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(FoodGameTests.test("yearofice:food/dish_nutrients_reach_body", FoodD2GameTests::dishNutrientsReachBody));
        out.add(FoodGameTests.test("yearofice:food/dried_weight_40pct", FoodD2GameTests::driedWeight));
        out.add(FoodGameTests.test("yearofice:food/rack_flies", FoodD2GameTests::rackFlies));
        out.add(FoodGameTests.test("yearofice:food/rack_thief_toggle", FoodD2GameTests::rackThiefToggle));
        out.add(m5("yearofice:trap/deadfall_rolls", FoodD2GameTests::deadfallRolls));
        out.add(m5("yearofice:trap/snare_funnel", FoodD2GameTests::snareFunnel));
        out.add(m5("yearofice:build/black_sand_pan_rates", FoodD2GameTests::panRates));
        out.add(m5("yearofice:worldgen/placer_far_from_start", FoodD2GameTests::placerFarFromStart));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction m5(String name, Consumer<GameTestHelper> body) {
        return new TestFunction("m5", name, FoodGameTests.TEMPLATE, 100, 0, true, body);
    }

    // ---- food/dish_nutrients_reach_body ------------------------------------------------------------------------

    /**
     * A dish's summed nutrients (stew with bones x1.15) reach the body as kcal and macros, and its vitamin C reaches the
     * scurvy clock; the ten named foods all have a food row; rose hips (2 C-days a 100 g) fill the reserve a bite at a time.
     */
    static void dishNutrientsReachBody(GameTestHelper h) {
        for (var item : FoodItems.named()) {
            h.assertTrue(FoodMap.row(new ItemStack(item.get())) != null, "no food row for " + item.getId());
        }
        ServerPlayer p = FoodGameTests.player(h);
        var body = BodyAttachments.of(p);
        ItemStack meat = FoodGameTests.rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 5, null);
        ItemStack hips = new ItemStack(FoodItems.ROSE_HIPS.get(), 2);
        ItemStack dish = DishMaker.make(h.getLevel(), List.of(meat, hips), Dishes.Method.STEW_BONES, "camp_stew", 500);
        Nutrients n = dish.get(YoiComponents.NUTRIENTS.get());
        double raw = 5 * 115 + 2 * 160;
        h.assertTrue(Math.abs(n.kcal() - raw * 1.15) < 1, "dish kcal " + n.kcal() + ", want " + raw * 1.15);
        h.assertTrue(Math.abs(n.vitCMg() - 2 * 20 * 0.5) < 0.01, "stew keeps half the vitamin C: " + n.vitCMg());
        h.assertTrue("camp_stew".equals(dish.get(YoiComponents.DISH_ID.get())), "dish_id");
        h.assertTrue(Meals.grams(dish) == 1200, "dish weight " + Meals.grams(dish));

        body.vitC = 10;
        double pending = body.pendingKcal;
        Meals.Bite bite = Meals.eat(p, dish);
        h.assertTrue(bite.eaten(), "the dish was not eaten");
        h.assertTrue(body.pendingKcal - pending > 0.9 * n.kcal(), "kcal reached the body: " + (body.pendingKcal - pending) + " of " + n.kcal());
        h.assertTrue(Math.abs(body.vitC - (10 + n.cDays())) < 1e-6, "vitamin C days " + body.vitC + ", want " + (10 + n.cDays()));

        body.vitC = 5;
        ItemStack bite2 = new ItemStack(FoodItems.ROSE_HIPS.get(), 1);
        h.assertTrue(Meals.eat(p, bite2).eaten(), "rose hips not eaten");
        h.assertTrue(Math.abs(body.vitC - 7) < 1e-6, "100 g of rose hips is 2 C-days: " + body.vitC);
        h.succeed();
    }

    // ---- food/dried_weight_40pct --------------------------------------------------------------------------------

    /** Dried strips weigh 40% of the fresh weight and keep every calorie (about 2,900 kcal a kg). */
    static void driedWeight(GameTestHelper h) {
        h.setBlock(AT.below(), Blocks.STONE);
        h.setBlock(AT, PreserveRegistry.DRYING_RACK.get());
        DryingRackBlockEntity be = h.getBlockEntity(AT);
        be.pause(true);
        be.pinRain(false);
        be.pinFlies(5.0, false);
        ItemStack fresh = FoodGameTests.rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 20, null);
        h.assertTrue(be.putOn(fresh) == 20, "20 units fit");
        double freshKg = be.heldKg(), freshKcal = 20 * 115;
        be.stepHours(48);
        h.assertTrue(be.done(), "dried after 48 h");
        ItemStack dried = be.items().get(0);
        h.assertTrue(DecayService.get().settle(h.getLevel(), dried).state() == FoodState.DRIED, "state dried");
        double driedKg = be.heldKg();
        h.assertTrue(Math.abs(driedKg - 0.4 * freshKg) < 1e-6, "dried weight " + driedKg + " kg, want " + 0.4 * freshKg);
        Nutrients n = dried.get(YoiComponents.NUTRIENTS.get());
        h.assertTrue(n != null && Math.abs(n.kcal() * 20 - freshKcal) < 1, "every calorie kept: " + (n == null ? -1 : n.kcal() * 20) + " of " + freshKcal);
        double perKg = n.kcal() * 20 / driedKg;
        h.assertTrue(Math.abs(perKg - 2875) < 5, "kcal per kg dried " + perKg + ", want 2,875 (115 / 0.04)");
        // eating a dried unit books the fresh unit's 115 kcal, not 40% of it
        ServerPlayer p = FoodGameTests.player(h);
        var body = BodyAttachments.of(p);
        double before = body.pendingKcal;
        ItemStack one = dried.copyWithCount(1);
        h.assertTrue(Meals.eat(p, one).eaten(), "dried strip not eaten");
        h.assertTrue(Math.abs((body.pendingKcal - before) - 115) < 12, "a dried strip gave " + (body.pendingKcal - before) + " kcal");
        h.succeed();
    }

    // ---- food/rack_flies ----------------------------------------------------------------------------------------

    /** Above 15 C flies lay eggs on drying strips unless a smoky fire burns within 3 m; a fouled batch finishes rotten. */
    static void rackFlies(GameTestHelper h) {
        h.setBlock(AT.below(), Blocks.STONE);
        h.setBlock(AT, PreserveRegistry.DRYING_RACK.get());
        DryingRackBlockEntity be = h.getBlockEntity(AT);
        be.pause(true);
        be.pinRain(false);
        be.pinFlies(15.0, false);
        be.putOn(FoodGameTests.rawExact(h, EcosystemRegistry.SNOWSHOE_HARE_MEAT.get(), 4, null));
        be.stepHours(1);
        h.assertTrue(!be.fouled(), "15.0 C is not above 15");
        be.pinFlies(16.0, true);
        be.stepHours(1);
        h.assertTrue(!be.fouled(), "a smoky fire within 3 m keeps the flies off");
        be.pinFlies(16.0, false);
        be.stepHours(1);
        h.assertTrue(be.fouled(), "flies laid eggs at 16 C with no smoke");
        be.stepHours(47);
        h.assertTrue(be.done(), "the batch still finishes");
        int stage = DecayService.get().stage(h.getLevel(), be.items().get(0));
        h.assertTrue(stage >= com.yearofice.sim.food.EatingLadder.ROTTEN, "a fouled batch finishes rotten, stage " + stage);
        h.succeed();
    }

    // ---- food/rack_thief_toggle ---------------------------------------------------------------------------------

    /** Racks join the thieves' watch only while the player's toggle is on (default on). */
    static void rackThiefToggle(GameTestHelper h) {
        var oldWatcher = RackWatch.watcher;
        var oldToggle = RackWatch.toggle;
        h.assertTrue(RackWatch.enabled() == oldToggle.getAsBoolean(), "toggle reads the player's option");
        List<BlockPos> seen = new ArrayList<>();
        RackWatch.watcher = (level, pos) -> seen.add(pos);
        RackWatch.toggle = () -> false;
        h.setBlock(AT.below(), Blocks.STONE);
        h.setBlock(AT, PreserveRegistry.DRYING_RACK.get());
        BlockPos second = AT.east();
        // both delays are scheduled here: a runAfterDelay inside a delay callback modifies the framework's map mid-iteration
        h.runAfterDelay(3, () -> {
            try {
                h.assertTrue(seen.isEmpty(), "toggle off: the rack is not watched, " + seen);
                RackWatch.toggle = () -> true;
                h.setBlock(second.below(), Blocks.STONE);
                h.setBlock(second, PreserveRegistry.SMOKE_RACK.get());
            } catch (RuntimeException e) {
                RackWatch.watcher = oldWatcher;
                RackWatch.toggle = oldToggle;
                throw e;
            }
        });
        h.runAfterDelay(6, () -> {
            try {
                h.assertTrue(seen.contains(h.absolutePos(second)), "toggle on: the next rack is watched, " + seen);
                h.assertTrue(!seen.contains(h.absolutePos(AT)), "the first rack was not, " + seen);
                h.succeed();
            } finally {
                RackWatch.watcher = oldWatcher;
                RackWatch.toggle = oldToggle;
            }
        });
    }

    // ---- trap/deadfall_rolls ------------------------------------------------------------------------------------

    /** The deadfall's kill and slip rates, the mouse season, sticks that last 10 triggers, and no reroll on reload. */
    static void deadfallRolls(GameTestHelper h) {
        DeadfallTuning t = DeadfallTuning.standard();
        // P(catch) = 1 - (1 - p_s)^n, then 90% kill for the right size, 50% for anything heavier than a tenth of the rock
        int kills = 0, heavy = 0, rolls = 20_000;
        for (int i = 0; i < rolls; i++) {
            if (Deadfall.roll(7, 3, 64, 9, i, t.bigN(), 1.0, false, t.big()) == Deadfall.Outcome.KILL) kills++;
            if (Deadfall.roll(7, 3, 64, 9, i, t.bigN(), 1.0, true, t.big()) == Deadfall.Outcome.KILL) heavy++;
        }
        double pc = Deadfall.pCatch(t.bigP(), 1.0, t.bigN());
        h.assertTrue(Math.abs(kills / (double) rolls - pc * 0.9) < 0.004, "kill rate " + kills / (double) rolls + ", want " + pc * 0.9);
        h.assertTrue(Math.abs(heavy / (double) rolls - pc * 0.5) < 0.004, "heavy kill rate " + heavy / (double) rolls + ", want " + pc * 0.5);
        int slips = 0;
        for (int i = 0; i < 10_000; i++) if (Deadfall.slips(7, 3, 64, 9, i, t.slipP(), 0)) slips++;
        h.assertTrue(Math.abs(slips / 10_000.0 - 0.15) < 0.015, "slip rate " + slips / 10_000.0 + ", want 15%");
        h.assertTrue(Deadfall.slips(7, 3, 64, 9, 1, t.slipP(), 1.0) == false, "full practice never slips");

        // the block entity: mice roll in winter nights only
        h.setBlock(AT.below(), Blocks.STONE);
        h.setBlock(AT, TrapRegistry.MOUSE_DEADFALL.get());
        DeadfallBlockEntity be = h.getBlockEntity(AT);
        be.setSilent(true);
        be.setAnimals(50.0);
        long july = dayInMonth(7) * 1440, december = dayInMonth(12) * 1440;
        be.setClock(july);
        be.set();
        be.setClock(july + 20L * 1440);
        h.assertTrue(be.settle(false) == 0 && be.status() == DeadfallBlockEntity.Status.SET, "no mice in July: " + be.status());
        be.setClock(december);
        be.set();
        be.setClock(december + 10L * 1440);
        be.settle(false);
        h.assertTrue(be.status() != DeadfallBlockEntity.Status.SET, "a December night with 50 mice in range triggers it: " + be.status());
        // settling again rerolls nothing
        var status = be.status();
        int trig = be.triggers();
        be.settle(false);
        h.assertTrue(be.status() == status && be.triggers() == trig, "reload never rerolls");
        // sticks last 10 triggers: 50 mice trigger it every roll; reset until the sticks give
        long clock = december + 10L * 1440;
        for (int i = 0; i < 40 && be.status() != DeadfallBlockEntity.Status.BROKEN; i++) {
            if (be.status() == DeadfallBlockEntity.Status.CATCH) be.takeCatch(null);
            if (be.status() == DeadfallBlockEntity.Status.SPRUNG) be.set();
            clock += 1440;
            be.setClock(clock);
            be.settle(false);
        }
        h.assertTrue(be.status() == DeadfallBlockEntity.Status.BROKEN && be.triggers() == t.sticksLife(),
                "sticks break at " + t.sticksLife() + " triggers: " + be.status() + " after " + be.triggers());
        h.assertTrue(be.mice() >= 1, "mice counted: " + be.mice());
        h.succeed();
    }

    static long dayInMonth(int month) {
        for (long d = 400; d < 800; d++) if (com.yearofice.sim.time.Calendar.month(com.yearofice.sim.time.Calendar.dayOfYearOfDay(d)) == month) return d + 5;
        throw new IllegalStateException("no day in month " + month);
    }

    // ---- trap/snare_funnel --------------------------------------------------------------------------------------

    /** A funnel is a snare_funnel (or log or rock) one block either side across the snare: x1.3, inside the x6 place cap. */
    static void snareFunnel(GameTestHelper h) {
        h.setBlock(AT.below(), Blocks.STONE);
        BlockPos abs = h.absolutePos(AT);
        Direction facing = Direction.NORTH;
        Direction side = facing.getClockWise();
        var level = h.getLevel();
        h.assertTrue(!SnareSite.read(level, abs, facing).quality().funnel(), "no walls, no funnel");
        h.setBlock(AT.relative(side), TrapRegistry.SNARE_FUNNEL.get());
        h.assertTrue(!SnareSite.read(level, abs, facing).quality().funnel(), "one wall is no funnel");
        h.setBlock(AT.relative(side.getOpposite()), TrapRegistry.SNARE_FUNNEL.get());
        Snare.SetQuality q = SnareSite.read(level, abs, facing).quality();
        h.assertTrue(q.funnel(), "two funnel blocks make a funnel");
        var t = TrapConfig.tuning();
        Snare.SetQuality bare = new Snare.SetQuality(false, false, false, false, 1, 1);
        Snare.SetQuality funnelled = new Snare.SetQuality(false, false, true, false, 1, 1);
        h.assertTrue(Math.abs(funnelled.multiplier(t) / bare.multiplier(t) - 1.3) < 1e-9, "a funnel is x1.3");
        Snare.SetQuality full = new Snare.SetQuality(true, true, true, true, 1, 1);
        h.assertTrue(Math.abs(full.place(t) - 6.0) < 1e-9, "trail 4 x cover 1.5 x funnel 1.3 = 7.8, capped at 6: " + full.place(t));
        double p = Snare.pCatch(t.hareP(), full.multiplier(t), 1.0);
        int caught = 0, n = 10_000;
        for (int i = 0; i < n; i++) if (Thresholds.stream(11, "funnel_cap", i).nextDouble() < p) caught++;
        h.assertTrue(Math.abs(caught / (double) n - p) < 0.02, "10,000 rolls at the cap: " + caught / (double) n + " vs " + p);
        h.succeed();
    }

    // ---- build/black_sand_pan_rates -----------------------------------------------------------------------------

    /**
     * Black sand: 0.3 kg a game hour from a gold pan on placer gravel, 0.1 on other gravel, half that from a birch pan,
     * 3x from a sluice, nothing outside May to October; and one real pan load through hold-to-work gives 300 g.
     */
    static void panRates(GameTestHelper h) {
        BlackSand sand = FoodConfig.pan();
        BlockPos any = new BlockPos(5, 64, 5);
        double[][] want = {{0.3, 0.1}, {0.15, 0.05}, {0.9, 0.3}};
        BlackSand.Tool[] tools = {BlackSand.Tool.GOLD_PAN, BlackSand.Tool.BIRCH_PAN, BlackSand.Tool.SLUICE};
        for (int k = 0; k < 3; k++) {
            for (int rich = 0; rich < 2; rich++) {
                double kg = 0;
                int hours = 600;
                for (int i = 0; i < hours; i++) kg += PanJob.units(sand.kgPerHour(tools[k], rich == 0, 7), 99, any, i) * PanJob.UNIT_KG;
                double per = kg / hours;
                h.assertTrue(Math.abs(per - want[k][rich]) < 0.1 * want[k][rich], tools[k] + (rich == 0 ? " placer " : " plain ") + per + " kg/h, want " + want[k][rich]);
            }
        }
        h.assertTrue(sand.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 1) == 0 && sand.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 4) == 0
                && sand.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 11) == 0, "no panning in frozen months");
        h.assertTrue(sand.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 5) > 0 && sand.kgPerHour(BlackSand.Tool.GOLD_PAN, true, 10) > 0, "May to October is open");

        // one real load through the work service
        h.setBlock(AT, PanRegistry.PLACER_GRAVEL.get());
        h.setBlock(AT.above(), Blocks.WATER);
        ServerPlayer p = FoodGameTests.player(h);
        BlockPos abs = h.absolutePos(AT);
        p.setPos(abs.getX() + 0.5, abs.getY() + 1.0, abs.getZ() + 2.5);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PanRegistry.GOLD_PAN.get()));
        PanJob job = PanJob.resolve(p, abs);
        h.assertTrue(job != null, "a gold pan on wet placer gravel starts a job");
        job.pinMonth(6);
        double minutes = WorkService.runFor(p, job, 2000);
        h.assertTrue(minutes > 0, "the pan load finished");
        int got = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(PanRegistry.BLACK_SAND.get())) got += s.getCount();
        h.assertTrue(got == 3, "an hour on placer gravel is 300 g (3 units), got " + got);
        h.assertTrue(Math.abs(minutes - 60) < 3, "a load is one game hour of work: " + minutes + " min");
        h.assertTrue(PanJob.resolve(p, abs.above()) == null, "water is not gravel");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        h.assertTrue(PanJob.resolve(p, abs) == null, "no pan, no job");
        h.succeed();
    }

    // ---- worldgen/placer_far_from_start -------------------------------------------------------------------------

    /** Placer reaches stay 400 m or more from the start (so the iron-distance test holds) and are not rare elsewhere. */
    static void placerFarFromStart(GameTestHelper h) {
        Placer placer = PlacerParams.load();
        h.assertTrue(placer.minStartM() >= 400, "valley.json keeps reaches 400 m out: " + placer.minStartM());
        int sx = 123, sz = -457, hitsNear = 0, hits = 0, cells = 0, nearFree = 0;
        Placer anywhere = new Placer(placer.cellM(), placer.share(), 0, placer.quantityKg());
        for (int x = -3000; x <= 3000; x += 24) {
            for (int z = -3000; z <= 3000; z += 24) {
                double d = Math.hypot(x - sx, z - sz);
                boolean r = placer.reachAt(2024L, x, z, sx, sz);
                if (r && d < 400) hitsNear++;
                if (d >= 400) {
                    cells++;
                    if (r) hits++;
                } else if (anywhere.reachAt(2024L, x, z, sx, sz)) {
                    nearFree++;
                }
            }
        }
        h.assertTrue(hitsNear == 0, "reaches inside 400 m of the start: " + hitsNear);
        h.assertTrue(nearFree > 0, "the test bites: without the rule some reaches fall inside 400 m");
        double share = hits / (double) cells;
        h.assertTrue(share > 0.03 && share < 0.09, "reach share outside 400 m " + share + ", want near " + placer.share());
        h.assertTrue(PlacerSites.params().cellM() == placer.cellM(), "the live world reads the same entry");
        h.succeed();
    }
}

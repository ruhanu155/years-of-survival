package com.yearofice.ecosystem.livestock;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.world.ChangeOp;
import com.yearofice.build.pieces.PieceRegistry;
import com.yearofice.build.rooms.Rooms;
import com.yearofice.core.YearOfIce;
import com.yearofice.crop.CropRegistry;
import com.yearofice.crop.stovehouse.StovehouseAdapter;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.debug.EcoGameTests;
import com.yearofice.fire.FireDangerIndex;
import com.yearofice.food.FoodItems;
import com.yearofice.food.sourdough.Sourdough;
import com.yearofice.food.sourdough.SourdoughRegistry;
import com.yearofice.food.sourdough.StarterComp;
import com.yearofice.food.sourdough.StarterItem;
import com.yearofice.gametest.TestFilter;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.fire.FireDanger;
import com.yearofice.sim.food.Starter;
import com.yearofice.sim.livestock.Growth;
import com.yearofice.sim.livestock.PenRisk;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.world.Regrowth;
import com.yearofice.sim.world.Wildfire;
import com.yearofice.world.longterm.LongTerm;
import com.yearofice.world.terrain.TerrainApply;
import com.yearofice.world.terrain.TerrainChangeQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M9 lane C GameTests: livestock ({@code livestock/}), the long world ({@code world/}, {@code fire/}), the starter
 * ({@code food/}), the forage dials ({@code forage/}) and the stovehouse ({@code crop/}). Each animal test gives its
 * animal its own test group and a floor through {@code EcoGameTests.stone}; the world tests build sky platforms
 * (stone at y 100) in their own far chunks (x and z 4000 and up), so nothing else sees them. All on Standard.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class LivestockGameTests {
    private LivestockGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int Y0 = 100;

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:livestock/goat_hand_feed", 60, LivestockGameTests::goatHandFeed));
        out.add(test("yearofice:livestock/hen_eggs", 60, LivestockGameTests::henEggs));
        out.add(test("yearofice:livestock/pen_scan", 60, LivestockGameTests::penScan));
        out.add(test("yearofice:livestock/predator_night", 60, LivestockGameTests::predatorNight));
        out.add(test("yearofice:livestock/goat_milk", 60, LivestockGameTests::goatMilk));
        out.add(test("yearofice:livestock/kid_raised", 60, LivestockGameTests::kidRaised));
        out.add(test("yearofice:livestock/reindeer_winter_debt", 60, LivestockGameTests::reindeerDebt));
        out.add(test("yearofice:world/regrowth_schedule", 200, LivestockGameTests::regrowthSchedule));
        out.add(test("yearofice:world/beaver_pond_rise", 200, LivestockGameTests::beaverPondRise));
        out.add(test("yearofice:world/old_camp_10y", 200, LivestockGameTests::oldCamp));
        out.add(test("yearofice:world/wildfire_escalation", 200, LivestockGameTests::wildfireEscalation));
        out.add(test("yearofice:fire/controlled_burn", 200, LivestockGameTests::controlledBurn));
        out.add(test("yearofice:food/starter_states", 100, LivestockGameTests::starterStates));
        out.add(test("yearofice:forage/coppice_shoots", 60, LivestockGameTests::coppiceShoots));
        out.add(test("yearofice:forage/browse_exclosure", 60, LivestockGameTests::browseExclosure));
        out.add(test("yearofice:crop/stovehouse", 100, LivestockGameTests::stovehouse));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m9c", name, TEMPLATE, maxTicks, 0, true, body);
    }

    // ---- shared ---------------------------------------------------------------------------------------------------

    static void floor(GameTestHelper h, boolean on) {
        for (int x = 0; x <= 2; x++) for (int z = 0; z <= 2; z++) EcoGameTests.stone(h, new BlockPos(x, 0, z), on);
    }

    @SuppressWarnings("unchecked")
    static <T extends YoiAnimal> T spawn(GameTestHelper h, EntityType<T> type, String group) {
        floor(h, true);
        T e = type.create(h.getLevel());
        BlockPos at = h.absolutePos(new BlockPos(1, 1, 1));
        e.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        e.setTestGroup(group);
        e.setScentEnabled(false);
        e.setNoAi(true);
        h.getLevel().addFreshEntity(e);
        return e;
    }

    static LivestockCore core(YoiAnimal e) { return ((Livestock) e).core(); }

    static final long DAY = 200; // Nov 17: no browse, a month with no rut for hens

    // ---- livestock ------------------------------------------------------------------------------------------------

    static void goatHandFeed(GameTestHelper h) {
        var goat = spawn(h, LivestockRegistry.GOAT.get(), "m9c-goat-feed");
        try {
            LivestockCore c = core(goat);
            ServerPlayer p = EcoGameTests.player(h);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CropRegistry.STRAW.get(), 4));
            InteractionResult r = LivestockSystem.interact(goat, c, p, InteractionHand.MAIN_HAND);
            h.assertTrue(r == InteractionResult.CONSUME, "hay feeds the goat: " + r);
            h.assertTrue(Math.abs(c.feedKg - 0.5) < 1e-9 && p.getMainHandItem().getCount() == 3, "one hay is half a kilo: " + c.feedKg);
            // Twelve winter hours on 5 kg: it eats about a kilo, and owes nothing.
            c.feedKg = 5;
            c.waterL = 20;
            for (int i = 0; i < 12; i++) LivestockSystem.hour(goat, h.getLevel(), c, DAY * 24 + 4 + i, true);
            h.assertTrue(c.feedKg > 3.8 && c.feedKg < 4.2, "ate about 1 kg: " + c.feedKg);
            h.assertTrue(c.feedDebtKg < 0.001, "no debt while the store lasts: " + c.feedDebtKg);
            // An empty store owes the ration.
            c.feedKg = 0;
            for (int i = 0; i < 12; i++) LivestockSystem.hour(goat, h.getLevel(), c, DAY * 24 + 4 + i, true);
            h.assertTrue(c.feedDebtKg > 0.9 && c.feedDebtKg < 1.1, "owes about 1 kg: " + c.feedDebtKg);
            h.assertTrue(c.care.fed() < 99, "hunger shows: " + c.care.fed());
            goat.discard();
            h.succeed();
        } finally {
            floor(h, false);
        }
    }

    static void henEggs(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        EnvService.get().setOverride(l, new EnvService.WeatherOverride(15, 1, Precip.NONE, 0));
        var hen = spawn(h, LivestockRegistry.HEN.get(), "m9c-hen-eggs");
        try {
            LivestockCore c = core(hen);
            c.female = true;
            AABB box = new AABB(hen.blockPosition()).inflate(4);
            for (int d = 0; d < 5; d++) LivestockSystem.daily(hen, l, c, 100 + d, 8, 100 + d, LivestockSystem.tuning());
            long eggs = l.getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(FoodItems.HEN_EGG.get())).stream().mapToInt(i -> i.getItem().getCount()).sum();
            h.assertTrue(eggs >= 3 && eggs <= 5, "a fed hen lays about 4 eggs in 5 days: " + eggs);
            c.care = new com.yearofice.sim.livestock.Care.State(10, 100, 100, 100, 0);
            c.eggAcc = 0;
            for (int d = 0; d < 3; d++) LivestockSystem.daily(hen, l, c, 110 + d, 8, 110 + d, LivestockSystem.tuning());
            long after = l.getEntitiesOfClass(ItemEntity.class, box, i -> i.getItem().is(FoodItems.HEN_EGG.get())).stream().mapToInt(i -> i.getItem().getCount()).sum();
            h.assertTrue(after == eggs, "a hen below 30 fed lays nothing: " + eggs + " -> " + after);
            h.assertTrue(core(hen).adult() && c.stage() == Growth.Stage.ADULT, "a spawned hen is grown");
            hen.discard();
            l.getEntitiesOfClass(ItemEntity.class, box).forEach(ItemEntity::discard);
            h.succeed();
        } finally {
            EnvService.get().setOverride(l, null);
            floor(h, false);
        }
    }

    static void penScan(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        // A far sky platform: the template is walled in by its neighbours, so a gap there would still be closed.
        LevelChunk ch = l.getChunk(4004, 4000);
        platform(ch);
        BlockPos in = new BlockPos(ch.getPos().getMinBlockX() + 8, Y0 + 1, ch.getPos().getMinBlockZ() + 8);
        BlockPos[] ring = {in.west(), in.east(), in.north(), in.south()};
        for (BlockPos r : ring) l.setBlock(r, Blocks.OAK_FENCE.defaultBlockState(), 2);
        var pole = Pens.scan(l, in);
        h.assertTrue(pole.penned() && pole.fence() == PenRisk.Fence.POLE && pole.cells() == 1, "oak fence ring is a pole pen: " + pole);
        for (BlockPos r : ring) l.setBlock(r, PieceRegistry.WALL_LOG.get().defaultBlockState(), 2);
        h.assertTrue(Pens.scan(l, in).fence() == PenRisk.Fence.STOCKADE, "wall logs are a stockade");
        l.setBlock(ring[3], Blocks.OAK_FENCE.defaultBlockState(), 2);
        h.assertTrue(Pens.scan(l, in).fence() == PenRisk.Fence.POLE, "the pen is as weak as its weakest piece");
        l.setBlock(ring[3], Blocks.AIR.defaultBlockState(), 2);
        var gap = Pens.scan(l, in);
        h.assertTrue(!gap.penned(), "a gap leaves the animal in the open: " + gap);
        l.setBlock(ring[3], Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN, true), 2);
        h.assertTrue(!Pens.scan(l, in).penned(), "an open gate is a gap");
        h.succeed();
    }

    static void predatorNight(GameTestHelper h) {
        var goat = spawn(h, LivestockRegistry.GOAT.get(), "m9c-goat-night");
        try {
            LivestockCore c = core(goat);
            int open = 0, pole = 0, stockade = 0;
            for (long d = 0; d < 2000; d++) {
                c.penned = false;
                if (LivestockSystem.raided(goat, c, d, LivestockSystem.tuning())) open++;
                c.penned = true; c.fence = PenRisk.Fence.POLE;
                if (LivestockSystem.raided(goat, c, d, LivestockSystem.tuning())) pole++;
                c.fence = PenRisk.Fence.STOCKADE;
                if (LivestockSystem.raided(goat, c, d, LivestockSystem.tuning())) stockade++;
            }
            // Expected: 12%, 2.4%, 0.6% of 2000 nights (the pole and brush fences also let a goat slip out some days).
            h.assertTrue(open > 150 && open < 330, "open ground is raided about 12% of nights: " + open);
            h.assertTrue(pole < open && stockade < pole, "a stronger fence is raided less: open " + open + ", pole " + pole + ", stockade " + stockade);
            h.assertTrue(stockade < 40, "a stockade is raided about 0.6% of nights: " + stockade);
            // And a raid on the open takes the animal and leaves a carcass.
            c.penned = false;
            long d = 0;
            while (!LivestockSystem.raided(goat, c, d, LivestockSystem.tuning())) d++;
            LivestockSystem.nightRoll(goat, h.getLevel(), c, d, LivestockSystem.tuning());
            h.assertTrue(goat.isRemoved(), "a raid took the goat");
            h.assertTrue(!h.getLevel().getEntitiesOfClass(CarcassEntity.class, new AABB(goat.blockPosition()).inflate(4)).isEmpty(), "every death makes a carcass");
            h.getLevel().getEntitiesOfClass(CarcassEntity.class, new AABB(goat.blockPosition()).inflate(4)).forEach(CarcassEntity::discard);
            h.succeed();
        } finally {
            floor(h, false);
        }
    }

    static void goatMilk(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        EnvService.get().setOverride(l, new EnvService.WeatherOverride(10, 1, Precip.NONE, 0));
        var goat = spawn(h, LivestockRegistry.GOAT.get(), "m9c-goat-milk");
        try {
            LivestockCore c = core(goat);
            c.female = true;
            c.lastBirthDay = 90;
            c.feedKg = 50;
            ServerPlayer p = EcoGameTests.player(h);
            // A goat that drank toxic browse gives nothing for three days.
            c.milkOffUntilDay = 103;
            LivestockSystem.daily(goat, l, c, 100, 8, 100, LivestockSystem.tuning());
            h.assertTrue(c.milkOwedL == 0, "a sick goat gives no milk: " + c.milkOwedL);
            c.milkOffUntilDay = -1;
            LivestockSystem.daily(goat, l, c, 104, 8, 104, LivestockSystem.tuning());
            h.assertTrue(c.milkOwedL > 1.0 && c.milkOwedL < 2.3, "a fresh goat gives about 2 L a day: " + c.milkOwedL);
            p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            InteractionResult r = LivestockSystem.interact(goat, c, p, InteractionHand.MAIN_HAND);
            h.assertTrue(r == InteractionResult.CONSUME, "milking works with an empty hand: " + r);
            h.assertTrue(p.getInventory().contains(new ItemStack(FoodItems.GOAT_MILK.get())) || p.getInventory().countItem(FoodItems.GOAT_MILK.get()) > 0, "the milk is in the inventory");
            // A dry goat gives none.
            c.milkOwedL = 0;
            LivestockSystem.daily(goat, l, c, 90 + 241, 3, 90 + 241, LivestockSystem.tuning());
            h.assertTrue(c.milkOwedL == 0, "a goat 240 days past kidding is dry: " + c.milkOwedL);
            goat.discard();
            h.succeed();
        } finally {
            EnvService.get().setOverride(l, null);
            floor(h, false);
        }
    }

    static void kidRaised(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        EnvService.get().setOverride(l, new EnvService.WeatherOverride(10, 1, Precip.NONE, 0));
        var doe = spawn(h, LivestockRegistry.GOAT.get(), "m9c-goat-kid");
        try {
            LivestockCore c = core(doe);
            c.female = true;
            c.dueDay = 120;
            LivestockSystem.daily(doe, l, c, 120, 5, 120, LivestockSystem.tuning());
            h.assertTrue(c.dueDay < 0 && c.lastBirthDay == 120, "she kidded and is no longer due");
            List<GoatEntity> goats = l.getEntitiesOfClass(GoatEntity.class, new AABB(doe.blockPosition()).inflate(6), g -> "m9c-goat-kid".equals(g.testGroup()));
            h.assertTrue(goats.size() == 2, "a kid was born beside her: " + goats.size());
            GoatEntity kid = goats.stream().filter(g -> g != doe).findFirst().orElseThrow();
            LivestockCore k = core(kid);
            h.assertTrue(k.stage() == Growth.Stage.SUCKLING && k.progress == 0, "newborn is a suckling");
            h.assertTrue(k.massKg() < 5, "newborn weighs a few kilos: " + k.massKg());
            for (int d = 0; d < 300; d++) k.stepDay(LivestockSystem.tuning(), true);
            h.assertTrue(k.stage() == Growth.Stage.ADULT && Math.abs(k.massKg() - k.spec().adultKg()) < 0.5, "raised on milk and feed it is a grown goat: " + k.stage() + " " + k.massKg());
            LivestockCore starved = new LivestockCore(k.spec());
            starved.progress = 0; starved.ageDays = 0;
            for (int d = 0; d < 10; d++) starved.stepDay(LivestockSystem.tuning(), false);
            h.assertTrue(starved.progress == 0, "a kid with no milk does not grow");
            h.assertTrue(starved.starvedKid(), "and after three milkless days it has starved");
            kid.discard();
            doe.discard();
            h.succeed();
        } finally {
            EnvService.get().setOverride(l, null);
            floor(h, false);
        }
    }

    static void reindeerDebt(GameTestHelper h) {
        var r = spawn(h, LivestockRegistry.REINDEER.get(), "m9c-reindeer-debt");
        try {
            LivestockCore c = core(r);
            c.waterL = 100;
            // Winter at -25 C with no feed: the debt builds, hunger falls.
            for (int i = 0; i < 12; i++) LivestockSystem.hour(r, h.getLevel(), c, DAY * 24 + 4 + i, true);
            double perDay = c.spec().feedKgPerDay();
            h.assertTrue(Math.abs(c.feedDebtKg - perDay / 2) < 0.15 * perDay, "12 h of ration owed: " + c.feedDebtKg + " of " + perDay + " a day");
            h.assertTrue(c.care.fed() < 100, "an unfed reindeer gets hungry");
            r.discard();
            h.succeed();
        } finally {
            floor(h, false);
        }
    }

    // ---- the long world -------------------------------------------------------------------------------------------

    static void platform(LevelChunk ch) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            ch.setBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, Y0, ch.getPos().getMinBlockZ() + z), Blocks.STONE.defaultBlockState(), false);
            for (int y = Y0 + 1; y <= Y0 + 12; y++) ch.setBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, y, ch.getPos().getMinBlockZ() + z), Blocks.AIR.defaultBlockState(), false);
        }
    }

    static void tree(ServerLevel l, BlockPos base) {
        for (int y = 0; y < 4; y++) l.setBlock(base.above(y), Blocks.SPRUCE_LOG.defaultBlockState(), 2);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) for (int y = 2; y <= 4; y++)
            if (dx != 0 || dz != 0 || y == 4) l.setBlock(base.offset(dx, y, dz), Blocks.SPRUCE_LEAVES.defaultBlockState(), 2);
    }

    static void regrowthSchedule(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(4000, 4000);
        platform(ch);
        TerrainChangeQueue q = TerrainChangeQueue.get(l);
        BlockPos base = new BlockPos(ch.getPos().getMinBlockX() + 8, Y0 + 1, ch.getPos().getMinBlockZ() + 8);
        tree(l, base);
        long today = ClockService.get().absDay();
        int ops = LongTerm.burn(l, base, 3, today);
        h.assertTrue(ops > 20, "a burn queues ops: " + ops);
        // The REGROW ops carry the sim's dates.
        List<ChangeOp> regrow = q.due(ch.getPos(), today + 40_000).stream().filter(o -> o.kind() == ChangeOp.Kind.REGROW).toList();
        h.assertTrue(!regrow.isEmpty(), "regrowth was scheduled");
        long june = Regrowth.firstJune1(today);
        h.assertTrue(regrow.stream().anyMatch(o -> o.day() == june && o.block().equals("yearofice:fireweed")), "fireweed comes up the first June 1: " + june);
        h.assertTrue(regrow.stream().anyMatch(o -> o.day() == today + 2 * 365 && o.block().equals("yearofice:shrub_stem")), "saplings at 2 years");
        h.assertTrue(regrow.stream().noneMatch(o -> o.day() < june), "nothing regrows before the first June");
        // Today the tree goes; the fireweed is not there yet.
        TerrainApply.apply(l, ch, q, today, false, Integer.MAX_VALUE);
        h.assertTrue(!l.getBlockState(base.above(2)).is(Blocks.SPRUCE_LOG) && !l.getBlockState(base.above(3)).is(Blocks.SPRUCE_LEAVES), "the burn took the tree");
        net.minecraft.world.level.block.Block fireweed = BuiltInRegistries.BLOCK.get(ResourceLocation.parse("yearofice:fireweed"));
        int early = countBlock(l, base, 4, fireweed);
        h.assertTrue(early == 0, "no fireweed on the day of the fire: " + early);
        // After the first June it is up, on about half the cells.
        TerrainApply.apply(l, ch, q, june, false, Integer.MAX_VALUE);
        int up = countBlock(l, base, 4, fireweed);
        h.assertTrue(up >= 8 && up <= 45, "fireweed on about half of the burned cells: " + up);
        h.succeed();
    }

    static int countBlock(ServerLevel l, BlockPos c, int r, net.minecraft.world.level.block.Block b) {
        int n = 0;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) for (int dy = 0; dy <= 6; dy++) if (l.getBlockState(c.offset(dx, dy, dz)).is(b)) n++;
        return n;
    }

    static void controlledBurn(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(4001, 4000);
        platform(ch);
        BlockPos base = new BlockPos(ch.getPos().getMinBlockX() + 8, Y0 + 1, ch.getPos().getMinBlockZ() + 8);
        BlockPos far = base.offset(12, 0, 0);
        tree(l, base);
        tree(l, far);
        long today = ClockService.get().absDay();
        LongTerm.burn(l, base, 3, today); // a small burn: the line holds at 3 blocks
        TerrainApply.apply(l, ch, TerrainChangeQueue.get(l), today, false, Integer.MAX_VALUE);
        h.assertTrue(l.getBlockState(base).is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("yearofice:stump"))), "the base log is left a stump");
        h.assertTrue(!l.getBlockState(base.above(3)).is(Blocks.SPRUCE_LOG), "the trunk is gone");
        h.assertTrue(l.getBlockState(far.above(3)).is(Blocks.SPRUCE_LOG) && l.getBlockState(far.offset(1, 3, 1)).is(Blocks.SPRUCE_LEAVES), "the tree beyond the burn line stands");
        h.succeed();
    }

    static void beaverPondRise(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(4002, 4000);
        platform(ch);
        int ox = ch.getPos().getMinBlockX(), oz = ch.getPos().getMinBlockZ();
        // A basin: stone walls round x 2-13, z 8-14 at y 101-103; the dam crosses it at z 7.
        for (int x = 1; x <= 14; x++) for (int z = 7; z <= 15; z++) for (int y = Y0 + 1; y <= Y0 + 4; y++) {
            boolean wall = x == 1 || x == 14 || z == 15;
            ch.setBlockState(new BlockPos(ox + x, y, oz + z), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), false);
        }
        BlockPos dam = new BlockPos(ox + 7, Y0 + 1, oz + 7);
        long today = ClockService.get().absDay();
        TerrainChangeQueue q = TerrainChangeQueue.get(l);
        LongTerm.dam(l, dam, 0, today);
        TerrainApply.apply(l, ch, q, today, false, Integer.MAX_VALUE);
        h.assertTrue(countBlock(l, dam.offset(0, 0, 3), 7, Blocks.WATER) == 0, "a stage 0 dam holds no pond");
        LongTerm.dam(l, dam, 2, today);
        TerrainApply.apply(l, ch, q, today, false, Integer.MAX_VALUE);
        int water = countBlock(l, dam.offset(0, 0, 4), 8, Blocks.WATER);
        h.assertTrue(water > 20, "a stage 2 dam raises a pond behind it: " + water);
        h.assertTrue(l.getBlockState(dam.offset(-5, 0, 0)).is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse("yearofice:fallen_log"))), "the dam is logs across the stream");
        h.assertTrue(l.getBlockState(dam.offset(0, 0, -2)).isAir(), "downstream stays dry");
        h.succeed();
    }

    static void oldCamp(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        LevelChunk ch = l.getChunk(4003, 4000);
        platform(ch);
        int ox = ch.getPos().getMinBlockX(), oz = ch.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) ch.setBlockState(new BlockPos(ox + x, Y0 + 1, oz + z), PieceRegistry.ROOF_POLE_SOD.get().defaultBlockState(), false);
        long today = ClockService.get().absDay();
        int queued = LongTerm.ageChunk(l, ch, 10, today);
        TerrainApply.apply(l, ch, TerrainChangeQueue.get(l), today, false, Integer.MAX_VALUE);
        int left = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) if (l.getBlockState(new BlockPos(ox + x, Y0 + 1, oz + z)).is(PieceRegistry.ROOF_POLE_SOD.get())) left++;
        // 6.7% a year for 10 years: half are gone.
        h.assertTrue(queued == 256 - left, "every lost block was a queued op: " + queued + " vs " + (256 - left));
        h.assertTrue(left > 90 && left < 165, "about half of a sod roof is left after ten years: " + left + " of 256");
        // Ten years is about the same as ten one-year ages: an unvisited chunk is the same chunk.
        h.assertTrue(LongTerm.ageChunk(l, ch, 0, today) == 0, "no years, no loss");
        h.succeed();
    }

    static void wildfireEscalation(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        long ordinary = -1, wildfire = -1;
        for (int y = 1; y < 80 && (ordinary < 0 || wildfire < 0); y++) {
            boolean w = Wildfire.wildfireYear(l.getSeed(), y, LongTerm.tuning());
            if (w && wildfire < 0) wildfire = (y - 1) * 365L + 100;
            if (!w && ordinary < 0) ordinary = (y - 1) * 365L + 100;
        }
        try {
            FireDangerIndex.pin(l, FireDanger.Level.HIGH);
            int ord = 0, wf = 0;
            for (int s = 0; s < 400; s++) {
                if (LongTerm.escalation(l, s, ordinary) != null) ord++;
                if (LongTerm.escalation(l, s, wildfire) != null) wf++;
            }
            h.assertTrue(ord == 0, "no escalation in an ordinary year below Extreme: " + ord);
            h.assertTrue(wf > 80 && wf < 160, "about 30% escalate in a wildfire year: " + wf + " of 400");
            FireDangerIndex.pin(l, FireDanger.Level.EXTREME);
            int ex = 0;
            for (int s = 0; s < 400; s++) if (LongTerm.escalation(l, s, ordinary) != null) ex++;
            h.assertTrue(ex > 200 && ex < 280, "about 60% escalate at Extreme: " + ex + " of 400");
            h.assertTrue(LongTerm.escalation(l, 1, ordinary) == null || LongTerm.escalation(l, 1, ordinary) == Wildfire.FireClass.CROWN, "Extreme starts a crown fire");
            h.succeed();
        } finally {
            FireDangerIndex.pin(l, null);
        }
    }

    // ---- food -----------------------------------------------------------------------------------------------------

    static void starterStates(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        long now = 1000;
        Random rng = new Random(7);
        StarterComp fresh = StarterComp.fresh(now);
        h.assertTrue(fresh.data().state() == Starter.State.ACTIVE, "a new starter is active");
        ItemStack flour = new ItemStack(FoodItems.BARLEY_FLOUR.get(), 4);
        ItemStack bread = Sourdough.bake(l, fresh, flour, "bread");
        h.assertTrue(bread != null, "an active starter bakes");
        int full = bread.get(YoiComponents.WEIGHT_G.get());
        h.assertTrue(full == 750 && "sourdough_bread".equals(bread.get(YoiComponents.DISH_ID.get())), "bread has its own explicit weight: " + full);
        // Five warm days unfed: hungry (hooch), half a rise.
        var hungry = new StarterComp(Starter.advance(fresh.data(), 5 * 1440, 22, rng), now, now);
        h.assertTrue(hungry.data().state() == Starter.State.HOOCH, "five unfed warm days is hungry: " + hungry.data().state());
        ItemStack weak = Sourdough.bake(l, hungry, flour, "bread");
        h.assertTrue(weak != null && weak.get(YoiComponents.WEIGHT_G.get()) < full && weak.get(YoiComponents.WEIGHT_G.get()) > full / 2 - 1, "a hungry starter bakes a smaller loaf");
        // Two feeds revive it.
        var once = Starter.feed(hungry.data(), now + 10);
        h.assertTrue(once.state() == Starter.State.HOOCH, "one feed is not enough");
        var twice = Starter.feed(once, now + 20);
        h.assertTrue(twice.state() == Starter.State.ACTIVE, "two feeds revive it");
        // Cold: asleep, and it does not age.
        var asleep = new StarterComp(Starter.advance(twice, 30 * 1440, -10, rng), now, now);
        h.assertTrue(asleep.data().state() == Starter.State.SLEEPING, "below freezing it sleeps");
        h.assertTrue(asleep.data().unfedDays() == twice.unfedDays(), "a sleeping starter does not hunger");
        h.assertTrue(Sourdough.bake(l, asleep, flour, "bread") == null, "a sleeping starter will not raise bread");
        // Left unfed a month in the warm: dead.
        var dead = Starter.advance(Starter.Data.fresh(now), 25 * 1440, 22, rng);
        h.assertTrue(dead.state() == Starter.State.DEAD, "21 unfed warm days kills it: " + dead.state());
        h.assertTrue(Sourdough.bake(l, new StarterComp(dead, now, now), flour, "bread") == null, "a dead starter bakes nothing");
        for (String id : new String[] {"pancakes", "bread", "biscuits"}) {
            ItemStack d = Sourdough.bake(l, fresh, flour, id);
            h.assertTrue(d != null && d.get(YoiComponents.WEIGHT_G.get()) == Sourdough.recipes().get(id).weightG(), id + " weight is explicit");
        }
        // The item: feeding by hand.
        ItemStack stack = new ItemStack(SourdoughRegistry.SOURDOUGH_STARTER.get());
        stack.set(SourdoughRegistry.STARTER.get(), hungry);
        ServerPlayer p = EcoGameTests.player(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(FoodItems.BARLEY_FLOUR.get(), 2));
        stack.getItem().use(l, p, InteractionHand.MAIN_HAND);
        StarterComp fed = stack.get(SourdoughRegistry.STARTER.get());
        h.assertTrue(fed.data().reviveFeeds() == 1 && p.getOffhandItem().getCount() == 1, "flour in the other hand feeds it: " + fed.data().reviveFeeds());
        h.succeed();
    }

    // ---- forage dials ---------------------------------------------------------------------------------------------

    static void coppiceShoots(GameTestHelper h) {
        long seed = 42;
        int low = 99, high = 0;
        for (int i = 0; i < 400; i++) {
            int n = Regrowth.shoots(seed, i, i * 7, Regrowth.Coppice.WILLOW);
            low = Math.min(low, n); high = Math.max(high, n);
        }
        h.assertTrue(low >= 6 && high <= 12, "a willow stool sends 6 to 12 shoots: " + low + ".." + high);
        int dead = 0;
        for (int i = 0; i < 1000; i++) if (Regrowth.diesOnCut(seed, i, i * 3, 2, Regrowth.Coppice.BIRCH)) dead++;
        h.assertTrue(dead > 400 && dead < 600, "half of birch stools die at the second cut: " + dead);
        long cut = 100;
        h.assertTrue(Regrowth.shootsShowDay(cut) >= Regrowth.firstJune1(cut) - 1, "shoots show by mid June");
        double h3 = Regrowth.poleHeightM(seed, 1, 1, cut, cut + 3 * 365L);
        h.assertTrue(h3 >= 3.0 && h3 <= 4.7, "poles are 3 to 4 m after three summers: " + h3);
        h.succeed();
    }

    static void browseExclosure(GameTestHelper h) {
        double open = Regrowth.browseShare(20, 8);
        double a = Regrowth.heightAfter(0.5, 3, open), b = Regrowth.heightAfter(0.5, 3, 0);
        h.assertTrue(open > 0.5, "a hard-browsed stand loses most of its stems: " + open);
        h.assertTrue(b - a > 1.7, "three summers inside an exclosure outgrow the browsed stand by 2 m: " + (b - a));
        h.succeed();
    }

    // ---- the stovehouse -------------------------------------------------------------------------------------------

    static final int UP = 80;

    static void stovehouse(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        EnvService.get().setOverride(l, new EnvService.WeatherOverride(-30, 3, Precip.NONE, 0));
        try {
            var wall = PieceRegistry.WALL_LOG.get();
            for (int x = 0; x <= 5; x++) for (int z = 0; z <= 5; z++) {
                h.setBlock(new BlockPos(x, UP, z), PieceRegistry.SLAB_FLOOR.get());
                h.setBlock(new BlockPos(x, UP + 3, z), PieceRegistry.ROOF_POLE_SOD.get());
                boolean ring = x == 0 || x == 5 || z == 0 || z == 5;
                for (int y = 1; y <= 2; y++) h.setBlock(new BlockPos(x, UP + y, z), ring ? wall : Blocks.AIR);
            }
            h.setBlock(new BlockPos(2, UP + 1, 0), PieceRegistry.DOOR_FRAME.get());
            BlockPos crop = h.absolutePos(new BlockPos(2, UP + 1, 2));
            long today = ClockService.get().absDay();
            // The stove is set before the room is first read: the room service caches its answer until a block changes.
            Rooms.stubSource(l, crop, 4000);
            var warm = StovehouseAdapter.inside(l, crop, today, -30);
            h.assertTrue(warm != null && warm.roomC() > 5, "a stove warms the room: " + (warm == null ? "no room" : warm.roomC()));
            h.assertTrue(warm.glazing() == com.yearofice.sim.crop.Stovehouse.Glazing.DIM, "sod roof is dim: " + warm.glazing());
            h.assertTrue(warm.lowC() > -5, "the night low inside follows the room: " + warm.lowC());
            h.setBlock(new BlockPos(2, UP + 3, 2), Blocks.GLASS);
            var glass = StovehouseAdapter.inside(l, crop, today, -30);
            h.assertTrue(glass.glazing() == com.yearofice.sim.crop.Stovehouse.Glazing.GLASS && glass.units() > warm.units(), "a glass pane over the bed lights it: " + glass.units() + " vs " + warm.units());
            // Open ground is not a stovehouse.
            h.assertTrue(StovehouseAdapter.inside(l, h.absolutePos(new BlockPos(2, UP + 8, 2)), today, -30) == null, "the open air is no stovehouse");
            h.assertTrue(StovehouseAdapter.inside(l, crop, today - 5, -30) == null, "a day long past has no room to ask");
            h.succeed();
        } finally {
            EnvService.get().setOverride(l, null);
            Rooms.clearStubs(l);
        }
    }
}

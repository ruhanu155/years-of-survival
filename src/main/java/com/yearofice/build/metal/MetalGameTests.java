package com.yearofice.build.metal;

import com.yearofice.api.build.GateService;
import com.yearofice.api.world.IceService;
import com.yearofice.build.craft.CraftSystem;
import com.yearofice.build.craft.StationRecipe;
import com.yearofice.api.world.FishSpotService;
import com.yearofice.build.craft.water.WheelWorks;
import com.yearofice.hunt.fishing.FishingRegistry;
import com.yearofice.hunt.fishing.PassiveGearService;
import com.yearofice.build.work.WorkService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.debug.EcoGameTests;
import com.yearofice.food.pan.PanJob;
import com.yearofice.food.pan.PanRegistry;
import com.yearofice.sim.build.Smelt;
import com.yearofice.sim.build.Tier;
import com.yearofice.sim.world.IceModel;
import com.yearofice.travel.canoe.CanoeEntity;
import com.yearofice.travel.canoe.CanoeRegistry;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M9 lane B: black sand to bloom to bar, the sluice at three times the pan, the iron gate, the station alias, the fish
 * wheel and the canoe. Each test uses only its own template cells; the player is a detached bot.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class MetalGameTests {
    private MetalGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos AT = new BlockPos(1, 1, 1);

    static TestFunction test(String name, int ticks, Consumer<GameTestHelper> body) {
        return new TestFunction("m9_metal", name, TEMPLATE, ticks, 0, true, body);
    }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:build/black_sand_bloom", 100, MetalGameTests::blackSandBloom));
        out.add(test("yearofice:build/sluice_triple", 100, MetalGameTests::sluiceTriple));
        out.add(test("yearofice:craft/iron_gate_opens", 100, MetalGameTests::ironGateOpens));
        out.add(test("yearofice:craft/station_alias_loads", 100, MetalGameTests::stationAliasLoads));
        out.add(test("yearofice:fish/wheel_catches", 100, MetalGameTests::wheelCatches));
        out.add(test("yearofice:travel/canoe_capsizes", 100, MetalGameTests::canoeCapsizes));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static ServerPlayer near(GameTestHelper h, BlockPos rel) {
        ServerPlayer p = EcoGameTests.player(h);
        BlockPos abs = h.absolutePos(rel);
        p.setPos(abs.getX() + 0.5, abs.getY(), abs.getZ() + 2.5);
        return p;
    }

    // ---- build/black_sand_bloom -------------------------------------------------------------------------------

    /**
     * Video 2, A10: 200 units (20 kg) of black sand and 20 kg of charcoal in a walled bloomery smelt in 6 game hours to one
     * bloom (40 units = one nodule, 5 nodules a bloom); 199 units do not; the bloom and 3 kg of charcoal consolidate in 2
     * hours to 4 bars (2.0 kg).
     */
    static void blackSandBloom(GameTestHelper h) {
        h.setBlock(AT, MetalRegistry.BLOOMERY.get());
        h.setBlock(AT.west(), Blocks.STONE);
        h.setBlock(AT.east(), Blocks.STONE);
        ServerPlayer p = near(h, AT);
        BlockPos abs = h.absolutePos(AT);
        var be = (BloomeryBlockEntity) h.getLevel().getBlockEntity(abs);
        h.assertTrue(be != null, "the bloomery has its block entity");
        h.assertTrue(BloomeryBlock.built(h.getLevel(), abs), "two stone walls make a furnace");

        be.load(new ItemStack(PanRegistry.BLACK_SAND.get(), 199));
        be.load(new ItemStack(MetalRegistry.CHARCOAL.get(), 20));
        h.assertFalse(be.canSmelt(), "199 units are short of 5 nodules");
        h.assertTrue(MetalSystem.resolve(p, abs) == null, "no smelt job without a full charge");
        be.load(new ItemStack(PanRegistry.BLACK_SAND.get(), 1));
        h.assertTrue(be.canSmelt() && be.quarters() == Smelt.QUARTERS_PER_BLOOM, "200 units are 5 nodules: " + be.quarters());

        var job = MetalSystem.resolve(p, abs);
        h.assertTrue(job instanceof SmeltJob, "holding use at a charged bloomery smelts");
        double minutes = WorkService.runFor(p, job, 4000);
        h.assertTrue(Math.abs(minutes - MetalConfig.get("smelt_hours") * 60) < 5, "a smelt is " + MetalConfig.get("smelt_hours") + " h of pumping: " + minutes + " min");
        h.assertTrue(be.blooms() == 1 && be.quarters() == 0 && be.charcoal() == 0, "one bloom, ore and charcoal used: " + be.blooms() + "/" + be.quarters() + "/" + be.charcoal());
        h.assertTrue(be.takeBlooms() == 1 && be.blooms() == 0, "the bloom comes out");

        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MetalRegistry.IRON_BLOOM.get()));
        p.getInventory().add(new ItemStack(MetalRegistry.CHARCOAL.get(), 3));
        var forge = MetalSystem.resolve(p, abs);
        h.assertTrue(forge instanceof ForgeJob, "a bloom in hand at the furnace consolidates");
        var seen = java.util.EnumSet.noneOf(com.yearofice.build.work.WorkProgressPayload.Phase.class);
        for (int t = 0; t < 6000 && !forge.finished(); t++) {
            forge.work(p, WorkService.dsPerTick());
            seen.add(forge.phase());
        }
        h.assertTrue(forge.finished(), "the consolidation finished");
        h.assertTrue(seen.size() == 3, "heat, hammer and quench all showed: " + seen);
        int bars = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(MetalRegistry.IRON_BAR.get())) bars += s.getCount();
        h.assertTrue(bars == Smelt.barUnits(false), "2.0 kg of bar is 4 bars, got " + bars);
        h.assertTrue(p.getInventory().countItem(MetalRegistry.IRON_BLOOM.get()) == 0 && p.getInventory().countItem(MetalRegistry.CHARCOAL.get()) == 0,
                "the bloom and its 3 kg of charcoal were used up");
        h.succeed();
    }

    // ---- build/sluice_triple ------------------------------------------------------------------------------------

    /** A sluice box on wet placer gravel gives 9 units (0.9 kg) an hour where the gold pan gives 3 (0.3 kg). */
    static void sluiceTriple(GameTestHelper h) {
        h.setBlock(AT, PanRegistry.PLACER_GRAVEL.get());
        h.setBlock(AT.above(), MetalRegistry.SLUICE_BOX.get());
        h.setBlock(AT.west(), Blocks.WATER);
        ServerPlayer p = near(h, AT.above());
        BlockPos box = h.absolutePos(AT.above());

        var job = SluiceJob.resolve(p, box);
        h.assertTrue(job != null, "a sluice box over wet placer gravel starts a job");
        job.pinMonth(7);
        double minutes = WorkService.runFor(p, job, 2000);
        h.assertTrue(Math.abs(minutes - 60) < 3, "a load is one game hour: " + minutes + " min");
        int sluice = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(PanRegistry.BLACK_SAND.get())) sluice += s.getCount();
        h.assertTrue(sluice == 9 && job.units() == 9, "a sluice hour on placer gravel is 900 g (9 units), got " + sluice);

        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(PanRegistry.GOLD_PAN.get()));
        PanJob pan = PanJob.resolve(p, h.absolutePos(AT));
        h.assertTrue(pan != null, "the pan works the same gravel");
        pan.pinMonth(7);
        WorkService.runFor(p, pan, 2000);
        int panned = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(PanRegistry.BLACK_SAND.get())) panned += s.getCount();
        h.assertTrue(panned == 3 && sluice == 3 * panned, "the pan gives 3 units, the sluice triple it: " + panned + " vs " + sluice);

        job = SluiceJob.resolve(p, box);
        job.pinMonth(1);
        p.getInventory().clearContent();
        WorkService.runFor(p, job, 2000);
        h.assertTrue(p.getInventory().isEmpty(), "frozen months give nothing");
        h.setBlock(AT, Blocks.DIRT);
        h.assertTrue(SluiceJob.resolve(p, box) == null, "a sluice needs gravel under it");
        h.succeed();
    }

    // ---- craft/iron_gate_opens ----------------------------------------------------------------------------------

    /** The first iron in the pack opens Iron (and only Iron); the tier's recipes then show; homestead needs a breakup and a warm room. */
    static void ironGateOpens(GameTestHelper h) {
        ServerPlayer p = near(h, AT);
        GateService g = GateService.get();
        h.assertFalse(g.isOpen(p, Tier.IRON), "iron starts closed");
        CraftSystem.scan(p);
        h.assertFalse(g.isOpen(p, Tier.IRON), "an empty pack opens nothing");
        p.getInventory().add(new ItemStack(MetalRegistry.BOG_IRON.get()));
        CraftSystem.scan(p);
        h.assertTrue(g.isOpen(p, Tier.IRON), "bog iron in hand opens Iron");
        h.assertFalse(g.isOpen(p, Tier.HOMESTEAD), "and not Homestead");
        h.assertFalse(com.yearofice.build.craft.HomesteadGate.scan(p, com.yearofice.sim.time.Calendar.dayOfYear(4, 28)), "no warm room, no Homestead");
        boolean iron = false;
        for (var holder : com.yearofice.build.craft.StationCraft.all(h.getLevel())) {
            if (holder.value().tier() == Tier.IRON && holder.id().getPath().endsWith("/iron_knife")) iron = true;
        }
        h.assertTrue(iron, "the iron knife recipe loads in the Iron tier");
        h.succeed();
    }

    // ---- craft/station_alias_loads ------------------------------------------------------------------------------

    /**
     * One recipe type: crop's quern and bone-crush recipes load as {@code yearofice:craft_station}, and the old
     * {@code yearofice:station} shape (inputs, work_min_per_kg) still decodes through the alias.
     */
    static void stationAliasLoads(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ItemStack barley = new ItemStack(com.yearofice.crop.CropRegistry.BARLEY.get());
        var quern = StationRecipe.find(level, "yearofice:hand_quern", barley);
        h.assertTrue(quern != null && quern.workMinPerKg() == 60 && quern.minutesFor(1000) == 60, "the migrated quern recipe loads from data");
        h.assertTrue(StationRecipe.find(level, "yearofice:hand_quern", new ItemStack(com.yearofice.crop.CropRegistry.WHEAT.get())) != null, "wheat grinds too");

        String old = """
                {"type":"yearofice:station","station":"yearofice:hand_quern","inputs":["yearofice:barley","yearofice:wheat"],
                 "tool":"yearofice:hand_quern","work_min_per_kg":60,"result":"yearofice:barley_flour","yield_pct":100}""";
        var json = com.google.gson.JsonParser.parseString(old).getAsJsonObject();
        var r = StationRecipe.CODEC.codec().parse(com.mojang.serialization.JsonOps.INSTANCE, json).result();
        h.assertTrue(r.isPresent(), "the old yearofice:station shape still decodes");
        h.assertTrue(r.get().workMinPerKg() == 60 && r.get().resultItem() == com.yearofice.crop.CropRegistry.BARLEY_FLOUR.get()
                && r.get().station().toString().equals("yearofice:hand_quern") && r.get().tier() == Tier.START, "alias numbers carry over");
        var ser = net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER;
        h.assertTrue(ser.containsKey(net.minecraft.resources.ResourceLocation.parse("yearofice:station")) && ser.containsKey(net.minecraft.resources.ResourceLocation.parse("yearofice:craft_station")),
                "both serializer ids are registered");
        h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse("yearofice:craft_station"))
                && !net.minecraft.core.registries.BuiltInRegistries.RECIPE_TYPE.containsKey(net.minecraft.resources.ResourceLocation.parse("yearofice:station")), "one recipe type");
        h.succeed();
    }

    // ---- fish/wheel_catches -------------------------------------------------------------------------------------

    /**
     * A fish wheel in a current takes chum salmon at the peak of their run (the passive-gear settle), gathers debris as it
     * turns, stops when fouled until a player clears it, and stops under ice. The water is a stub spot with a stocked run.
     */
    static void wheelCatches(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos wheelRel = AT;
        for (int x = 0; x <= 2; x++) h.setBlock(new BlockPos(x, 0, 1), Blocks.WATER);
        BlockPos abs = h.absolutePos(wheelRel);
        var spot = new FishSpotService.Spot("outside_bend", abs, 4, 1.0, 3.0, 1, List.of("chum_salmon"), List.of(60), true);
        FishSpotService spotsBefore = FishSpotService.get();
        FishSpotService.install(new FishSpotService() {
            @Override public List<Spot> spotsNear(ServerLevel lv, BlockPos pos, double r) { return spot.contains(pos) ? List.of(spot) : List.of(); }
            @Override public java.util.Optional<Spot> sliceSpot(ServerLevel lv) { return java.util.Optional.empty(); }
        });
        IceService iceBefore = null;
        try {
            iceBefore = IceService.get();
        } catch (IllegalStateException ignored) {
            // not installed in this world
        }
        var clock = com.yearofice.api.clock.ClockService.get();
        long day0 = clock.absDay();
        try {
            clock.setDay(com.yearofice.sim.fishing.SalmonRun.RUNS.get("chum_salmon").peak() - 1);
            h.assertTrue(WheelWorks.inCurrent(level, abs), "water below and on two sides is a current");
            h.setBlock(wheelRel, FishingRegistry.FISH_WHEEL.get());
            var g = PassiveGearService.at(level, abs);
            h.assertTrue(g != null && g.kind == PassiveGearService.Kind.FISH_WHEEL, "the wheel block registers its gear");
            long day = 1440;

            PassiveGearService.settle(level, g, g.lastMinute + day);
            int first = g.held.size();
            h.assertTrue(first >= 3, "a day at the run's peak fills the basket: " + first);
            double rate = MetalConfig.get("wheel_foul_per_h");
            h.assertTrue(WheelWorks.debrisAt(level, abs) >= 20 * rate, "debris gathers as it turns: " + WheelWorks.debrisAt(level, abs));
            g.held.clear();

            WheelWorks.setDebris(level, abs, MetalConfig.get("wheel_foul_max"));
            PassiveGearService.settle(level, g, g.lastMinute + day);
            h.assertTrue(g.held.isEmpty(), "a fouled wheel takes nothing: " + g.held.size());
            h.assertTrue(com.yearofice.api.build.WheelService.get().turningHours(level, abs, 24) == 0, "and turns no hours");
            com.yearofice.api.build.WheelService.get().clearDebris(level, abs);
            PassiveGearService.settle(level, g, g.lastMinute + day);
            h.assertTrue(g.held.size() >= 3, "cleared, it fishes again: " + g.held.size());
            g.held.clear();

            IceService.install(new IceService() {
                @Override public double thicknessCm(ServerLevel l, BlockPos pos) { return 20; }
                @Override public IceModel.Stage stage(ServerLevel l, BlockPos pos) { return IceModel.Stage.values()[0]; }
                @Override public boolean digHole(ServerLevel l, BlockPos pos) { return false; }
                @Override public boolean isOpenHole(ServerLevel l, BlockPos pos) { return false; }
                @Override public int tickHoles(ServerLevel l) { return 0; }
            });
            PassiveGearService.settle(level, g, g.lastMinute + day);
            h.assertTrue(g.held.isEmpty(), "ice over the water stops the wheel: " + g.held.size());
        } finally {
            if (iceBefore != null) IceService.install(iceBefore);
            FishSpotService.install(spotsBefore);
            clock.setDay(day0);
            PassiveGearService.book(level).all().remove(abs.asLong());
        }
        for (int x = 0; x <= 2; x++) h.setBlock(new BlockPos(x, 0, 1), Blocks.AIR);
        h.setBlock(wheelRel, Blocks.AIR);
        h.succeed();
    }

    // ---- travel/canoe_capsizes ----------------------------------------------------------------------------------

    /** A canoe in a gale or rapids throws its paddler and lies swamped; in calm water the roll never fires. */
    static void canoeCapsizes(GameTestHelper h) {
        var level = h.getLevel();
        CanoeEntity c = CanoeRegistry.CANOE_ENTITY.get().create(level);
        BlockPos abs = h.absolutePos(AT);
        c.moveTo(abs.getX() + 0.5, abs.getY() + 1, abs.getZ() + 0.5);
        level.addFreshEntity(c);
        ServerPlayer p = near(h, AT);
        p.startRiding(c, true);
        h.assertTrue(c.getPassengers().contains(p), "the paddler is aboard");
        c.windOverride = 2.0;
        c.rapidsOverride = false;
        h.assertFalse(c.capsizeRoll(0.0), "calm water never capsizes");
        c.windOverride = 25.0;
        h.assertFalse(c.capsizeRoll(0.999999), "a high draw stays upright");
        c.rapidsOverride = true; // the test cell may be under a roof: rapids stress the canoe whatever the sky
        h.assertTrue(c.capsizeRoll(0.0), "a gale with a low draw capsizes the canoe");
        h.assertTrue(c.getPassengers().isEmpty() && c.swamped(), "the paddler is thrown and the canoe swamped");
        c.discard();
        h.succeed();
    }
}

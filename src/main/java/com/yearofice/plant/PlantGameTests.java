package com.yearofice.plant;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.crop.CropBlockEntity;
import com.yearofice.crop.CropData;
import com.yearofice.crop.CropRegistry;
import com.yearofice.crop.Garden;
import com.yearofice.crop.Grain;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.core.YearOfIce;
import com.yearofice.hud.fieldnote.FieldNotes;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.item.inventory.KitItems;
import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.plant.LickModel;
import com.yearofice.sim.plant.PlantNotes;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch {@code m5_plants}: plant harvest and regrowth, plant knowledge, germination by soil temperature, and lick salt
 * (Plants and fungi › GameTests; Crop farming in play; Open world in depth › Salt from the mineral lick). Each test
 * restores the clock and the weather pin it moved.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class PlantGameTests {
    private PlantGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final String BATCH = "m5_plants";
    static final BlockPos AT = new BlockPos(1, 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> plantTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:plant/harvest_regrows", PlantGameTests::harvestRegrows));
        out.add(test("yearofice:plant/identify_by_note", PlantGameTests::identifyByNote));
        out.add(test("yearofice:crop/germinates_by_soil_temp", PlantGameTests::germinatesBySoilTemp));
        out.add(test("yearofice:forage/lick_salt_yield", PlantGameTests::lickSaltYield));
        out.add(test("yearofice:forage/lick_regrowth", PlantGameTests::lickRegrowth));
        out.add(test("yearofice:plant/poison_posts_line", PlantGameTests::poisonPostsLine));
        out.add(new TestFunction(BATCH, "yearofice:crop/garden_caught_up", TEMPLATE, 200, 0, true, PlantGameTests::gardenCaughtUp));
        out.add(test("yearofice:build/quern_mills", PlantGameTests::quernMills));
        out.add(test("yearofice:build/station_recipe_mills", PlantGameTests::stationRecipeMills));
        out.add(new TestFunction(BATCH, "yearofice:worldgen/plants_placed_by_habitat", TEMPLATE, 2400, 0, true, PlantWorldGameTests::plantsPlacedByHabitat));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, Consumer<GameTestHelper> body) { return new TestFunction(BATCH, name, TEMPLATE, 100, 0, true, body); }

    static ServerPlayer player(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "plant-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        return p;
    }

    static void resetPatches(GameTestHelper h, BlockPos abs) {
        PatchData d = PatchLedger.data(h.getLevel().getChunkAt(abs));
        d.patches.clear();
        d.ready.clear();
        PlantDials.reset();
    }

    // ---- plant/harvest_regrows -----------------------------------------------------------------------------------

    /** Soapberry: three picks empty a clump (taken 3) for about 1.3 cups, a fourth gives nothing, and 9 days later it fruits again. */
    static void harvestRegrows(GameTestHelper h) {
        long saved = ClockService.get().nowDs();
        try {
            ClockService.get().setNow(com.yearofice.sim.time.Calendar.DS_PER_DAY * 70 + 12 * 600 * 60);
            BlockPos abs = h.absolutePos(AT);
            resetPatches(h, abs);
            h.setBlock(AT.below(), Blocks.STONE);
            h.setBlock(AT, PlantRegistry.block("soapberry_bush").defaultBlockState().setValue(PlantBlock.PHASE, PlantBlock.Phase.FRUIT));
            ServerPlayer p = player(h);
            int grams = 0;
            for (int i = 1; i <= 3; i++) {
                HarvestVerbs.Result r = HarvestVerbs.act(p, h.getLevel(), abs, "pick");
                h.assertTrue(r.ok(), "pick " + i + " failed: " + r.message());
                grams += r.stack().get(YoiComponents.WEIGHT_G.get());
                h.assertTrue(h.getBlockState(AT).getValue(PlantBlock.TAKEN) == i, "taken after pick " + i);
            }
            h.assertTrue(grams >= 150 && grams <= 245, "a clump gives 1.3 cups (195 g) within the patch's seed value, got " + grams + " g");
            HarvestVerbs.Result fourth = HarvestVerbs.act(p, h.getLevel(), abs, "pick");
            h.assertTrue(!fourth.ok() && fourth.stack().isEmpty(), "a fourth pick gives nothing");
            PatchData.Patch patch = PatchLedger.data(h.getLevel().getChunkAt(abs)).patches.get("yearofice:soapberry_bush");
            h.assertTrue(patch != null && patch.picked > 0.9, "the patch ledger books the picks");
            ClockService.get().advance(9L * 24 * 60);
            HarvestVerbs.Result again = HarvestVerbs.act(p, h.getLevel(), abs, "pick");
            h.assertTrue(again.ok(), "9 days later the bush fruits again: " + again.message());
            h.assertTrue(h.getBlockState(AT).getValue(PlantBlock.TAKEN) == 1, "regrown clump starts at taken 1 after one pick");
            h.succeed();
        } finally {
            ClockService.get().setNow(saved);
            PlantDials.reset();
        }
    }

    // ---- plant/identify_by_note ----------------------------------------------------------------------------------

    /** Unknown plants read "a plant"; a field note, three growth stages, the pocket guide and a taste each identify; a fungus cannot be tasted. */
    static void identifyByNote(GameTestHelper h) {
        ServerPlayer p = player(h);
        PlantSpec cran = PlantData.get("yearofice:highbush_cranberry_bush");
        h.assertTrue(cran != null && !cran.notes.isEmpty(), "highbush cranberry names a field note");
        h.assertTrue(!Knowledge.identified(p, cran) && Knowledge.shownName(p, cran).equals("a plant"), "unknown until identified");
        h.assertTrue(FieldNotes.fire(p, cran.notes.get(0)), "the field note files");
        h.assertTrue(Knowledge.identified(p, cran) && Knowledge.shownName(p, cran).equals("Highbush cranberry"), "the note identifies it");

        PlantSpec soap = PlantData.get("yearofice:soapberry_bush");
        h.assertTrue(Knowledge.level(p, soap) == PlantNotes.Level.UNKNOWN, "soapberry unknown");
        Knowledge.seen(p, soap, com.yearofice.sim.plant.PlantPhase.LEAF);
        Knowledge.seen(p, soap, com.yearofice.sim.plant.PlantPhase.FLOWER);
        h.assertTrue(Knowledge.level(p, soap) == PlantNotes.Level.PARTLY, "two stages: partly known");
        Knowledge.seen(p, soap, com.yearofice.sim.plant.PlantPhase.FRUIT);
        h.assertTrue(Knowledge.level(p, soap) == PlantNotes.Level.IDENTIFIED, "three stages identify it");

        PlantSpec fire = PlantData.get("yearofice:fireweed");
        h.assertTrue(fire.pocketGuide && !Knowledge.identified(p, fire), "fireweed is in the pocket guide, unknown without it");
        p.getInventory().add(new ItemStack(KitItems.PLANT_GUIDE.get()));
        h.assertTrue(Knowledge.identified(p, fire), "carrying the guide identifies its 12 plants");

        PlantSpec bolete = PlantData.get("yearofice:birch_bolete");
        h.assertTrue(!Knowledge.taste(p, bolete).allowed() && !Knowledge.identified(p, bolete), "no fungus can be taste-tested");
        PlantSpec camas = PlantData.get("yearofice:death_camas");
        Knowledge.Taste t = Knowledge.taste(p, camas);
        h.assertTrue(t.allowed() && t.toxic() && t.conditionLoss() > 0 && Knowledge.identified(p, camas), "a toxic taste costs Condition and identifies");
        h.succeed();
    }

    // ---- plant/poison_posts_line ---------------------------------------------------------------------------------

    /**
     * Eating an unknown poison plant posts the Messenger line and starts poison_plant; the Condition loss waits for the
     * onset. A toxic taste starts taste_sick and its Condition loss lands only after the one-hour timer.
     */
    static void poisonPostsLine(GameTestHelper h) {
        ServerPlayer p = player(h);
        PlantSpec camas = PlantData.get("yearofice:death_camas");
        h.assertTrue(camas != null && camas.toxic != null && !Knowledge.identified(p, camas), "death camas is toxic and unknown");
        double before = com.yearofice.api.body.BodyService.get().condition(p);
        PlantPoison.eaten(p, camas, false);
        boolean line = com.yearofice.api.hud.HudBus.get().journal(p).stream().anyMatch(l -> l.key().endsWith(".warn.plant_poison"))
                || com.yearofice.api.hud.HudBus.get().sent(p).stream().anyMatch(l -> l.key().endsWith(".warn.plant_poison"))
                || com.yearofice.api.hud.HudBus.get().held(p).stream().anyMatch(l -> l.key().endsWith(".warn.plant_poison"));
        h.assertTrue(line, "the poison line was posted");
        h.assertTrue(PlantPoison.pendingMinutes(p) >= 60, "the loss waits for the onset: " + PlantPoison.pendingMinutes(p));
        h.assertTrue(com.yearofice.api.body.BodyService.get().condition(p) == before, "no Condition lost yet");
        PlantPoison.flush(p, PlantPoison.nowMinute() + 600);
        h.assertTrue(com.yearofice.api.body.BodyService.get().condition(p) < before, "the poison costs Condition at onset");

        PlantSpec lupine = PlantData.get("yearofice:death_camas");
        double c1 = com.yearofice.api.body.BodyService.get().condition(p);
        PlantPoison.tasted(p, lupine, 5);
        PlantPoison.flush(p, PlantPoison.nowMinute() + 59);
        h.assertTrue(com.yearofice.api.body.BodyService.get().condition(p) == c1, "the taste timer holds for the hour");
        PlantPoison.flush(p, PlantPoison.nowMinute() + 60);
        h.assertTrue(com.yearofice.api.body.BodyService.get().condition(p) < c1, "and lands after it");
        h.succeed();
    }

    // ---- crop/garden_caught_up -----------------------------------------------------------------------------------

    /** A barley bed left alone for 20 June days grows on the first look back: same units as day-by-day passes, once only. */
    static void gardenCaughtUp(GameTestHelper h) {
        long saved = ClockService.get().nowDs();
        try {
            ClockService.get().setNow(com.yearofice.sim.time.Calendar.DS_PER_DAY * 40 + 12 * 600 * 60);
            ServerLevel level = h.getLevel();
            BlockPos a = h.absolutePos(AT), b = a.east(2);
            for (BlockPos x : new BlockPos[] {a, b}) {
                level.setBlock(x.below(), CropRegistry.GARDEN_BED.get().defaultBlockState(), 3);
                level.setBlock(x, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                Garden.sow(level, x.below(), CropData.get("yearofice:barley"), 14.0);
            }
            var bea = (CropBlockEntity) level.getBlockEntity(a);
            var beb = (CropBlockEntity) level.getBlockEntity(b);
            h.assertTrue(bea != null && beb != null, "both crops have block entities");
            ClockService.get().setNow(com.yearofice.sim.time.Calendar.DS_PER_DAY * 60 + 12 * 600 * 60);
            long today = ClockService.get().absDay();
            h.assertTrue(bea.units() == 0 && bea.lastDone() == today - 21, "untouched while away: " + bea.units());
            int days = Garden.catchUp(level, a, bea);
            h.assertTrue(days == 20, "caught up 20 finished days, ran " + days);
            for (long d = today - 20; d <= today - 1; d++) Garden.dayTick(level, b, beb, d);
            h.assertTrue(bea.units() > 20 && Math.abs(bea.units() - beb.units()) < 1e-9, "same as the daily passes: " + bea.units() + " vs " + beb.units());
            double u = bea.units();
            h.assertTrue(Garden.catchUp(level, a, bea) == 0 && bea.units() == u, "a second look back adds nothing");
            h.succeed();
        } finally {
            ClockService.get().setNow(saved);
        }
    }

    // ---- build/quern_mills ---------------------------------------------------------------------------------------

    /** A flail threshes 2 kg of sheaf into 800 g of grain and 1.2 kg of straw in 12 min; the hand quern mills 2 kg of grain in 2 hours; a 2-year-old lot ages. */
    static void quernMills(GameTestHelper h) {
        long saved = ClockService.get().nowDs();
        try {
            ServerPlayer p = player(h);
            var inv = p.getInventory();
            ItemStack sheaf = new ItemStack(CropRegistry.GRAIN_SHEAF.get());
            sheaf.set(YoiComponents.WEIGHT_G.get(), 2000);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(CropRegistry.FLAIL.get()));
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, sheaf);
            long t0 = ClockService.get().nowDs();
            String m1 = Grain.use(p);
            h.assertTrue(ClockService.get().nowDs() - t0 == 12L * 600, "threshing 2 kg takes 12 min: " + (ClockService.get().nowDs() - t0) / 600 + " (" + m1 + ")");
            ItemStack grain = ItemStack.EMPTY, straw = ItemStack.EMPTY;
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (s.is(CropRegistry.BARLEY.get())) grain = s;
                if (s.is(CropRegistry.STRAW.get())) straw = s;
            }
            h.assertTrue(!grain.isEmpty() && grain.get(YoiComponents.WEIGHT_G.get()) == 800, "800 g of grain");
            h.assertTrue(!straw.isEmpty() && straw.get(YoiComponents.WEIGHT_G.get()) == 1200, "1200 g of straw");
            h.assertTrue(grain.get(CropRegistry.SEED_LOT.get()) != null && Grain.lotYears(grain) == 0, "the grain is a fresh seed lot");
            grain.set(YoiComponents.WEIGHT_G.get(), 2000);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(CropRegistry.HAND_QUERN.get()));
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, grain.copy());
            inv.removeItem(grain);
            long t1 = ClockService.get().nowDs();
            Grain.use(p);
            h.assertTrue(ClockService.get().nowDs() - t1 == 120L * 600, "1 kg an hour: 2 kg takes 2 h");
            ItemStack flour = ItemStack.EMPTY;
            for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(CropRegistry.BARLEY_FLOUR.get())) flour = inv.getItem(i);
            h.assertTrue(!flour.isEmpty() && flour.get(YoiComponents.WEIGHT_G.get()) == 2000, "2000 g of flour");
            h.assertTrue(p.getOffhandItem().isEmpty(), "the grain is used up");
            ItemStack old = new ItemStack(CropRegistry.BARLEY_SEED.get());
            old.set(CropRegistry.SEED_LOT.get(), ClockService.get().year() - 2);
            h.assertTrue(Grain.lotYears(old) == 2 && com.yearofice.sim.crop.CropModel.ageFactor(2, false) < com.yearofice.sim.crop.CropModel.ageFactor(0, false), "a 2-year-old lot germinates worse");
            h.succeed();
        } finally {
            ClockService.get().setNow(saved);
        }
    }

    /** The yearofice:station recipe type is registered, the quern recipe loads from data, and milling 1 kg goes through it (60 min, 1 kg of flour). */
    static void stationRecipeMills(GameTestHelper h) {
        long saved = ClockService.get().nowDs();
        try {
            ServerPlayer p = player(h);
            ItemStack grain = new ItemStack(CropRegistry.WHEAT.get());
            grain.set(YoiComponents.WEIGHT_G.get(), 1000);
            var r = com.yearofice.crop.StationRecipe.find(h.getLevel(), "yearofice:hand_quern", grain);
            h.assertTrue(r != null, "a yearofice:station recipe for the hand quern loads from data");
            h.assertTrue(r.workMinPerKg() == 60 && r.resultItem() == CropRegistry.BARLEY_FLOUR.get(), "recipe numbers");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(CropRegistry.HAND_QUERN.get()));
            p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, grain);
            long t0 = ClockService.get().nowDs();
            Grain.use(p);
            h.assertTrue(ClockService.get().nowDs() - t0 == 60L * 600, "1 kg takes 60 min");
            boolean got = false;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (s.is(CropRegistry.BARLEY_FLOUR.get()) && s.get(YoiComponents.WEIGHT_G.get()) == 1000) got = true;
            }
            h.assertTrue(got, "1000 g of flour");
            h.succeed();
        } finally {
            ClockService.get().setNow(saved);
        }
    }

    // ---- crop/germinates_by_soil_temp ----------------------------------------------------------------------------

    static double share(GameTestHelper h, BlockPos abs, double airC) {
        ServerLevel level = h.getLevel();
        EnvService.get().setOverride(level.getServer().overworld(), new EnvService.WeatherOverride(airC, Double.NaN, null, Double.NaN));
        var spec = CropData.get("yearofice:barley");
        int sown = 0, up = 0;
        for (int i = 0; i < 40; i++) {
            Garden.Sow s = Garden.sow(level, abs.below(), spec, Double.NaN);
            h.assertTrue(s != null, "sow on the bed");
            sown += s.seeds();
            up += s.up();
            level.setBlock(abs, Blocks.AIR.defaultBlockState(), 3);
        }
        return (double) up / sown;
    }

    /** Packet barley (60%): on 13 C soil 50-65% come up, on 4 C soil under 25%, and in between about 42%. */
    static void germinatesBySoilTemp(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        BlockPos abs = h.absolutePos(AT);
        try {
            h.setBlock(AT.below(), CropRegistry.GARDEN_BED.get());
            double warm = share(h, abs, 12.0);
            double cool = share(h, abs, 7.0);
            double cold = share(h, abs, 3.0);
            h.assertTrue(CropData.get("yearofice:barley") != null && Math.abs(Garden.soilC(level, abs.below()) - 4.0) < 0.01,
                    "soil reads the pinned air plus 1 C: " + Garden.soilC(level, abs.below()));
            h.assertTrue(warm >= 0.50 && warm <= 0.65, "13 C soil: " + warm);
            h.assertTrue(cool >= 0.33 && cool <= 0.51, "8 C soil: " + cool);
            h.assertTrue(cold < 0.25, "4 C soil: " + cold);
            h.succeed();
        } finally {
            EnvService.get().setOverride(level.getServer().overworld(), null);
        }
    }

    // ---- forage/lick_salt_yield ----------------------------------------------------------------------------------

    static final TagKey<Item> SALT = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "salt"));

    /** 40 scrapes over 2 full licks give 2.0 kg of crust in 10 game hours; one boil gives 0.15 kg of salt and burns 1 armload. */
    static void lickSaltYield(GameTestHelper h) {
        long saved = ClockService.get().nowDs();
        try {
            ClockService.get().setNow(com.yearofice.sim.time.Calendar.DS_PER_DAY * 40 + 12 * 600 * 60);
            int today = (int) ClockService.get().absDay();
            BlockPos a = AT, b = AT.east();
            h.setBlock(a, LickRegistry.SALT_CRUST.get());
            h.setBlock(b, LickRegistry.SALT_CRUST.get());
            ((SaltCrustBlockEntity) h.getBlockEntity(a)).init(LickModel.SEEP_CAP_G, 1000, today);
            ((SaltCrustBlockEntity) h.getBlockEntity(b)).init(LickModel.SEEP_CAP_G, 1000, today);
            int grams = 0, minutes = 0;
            for (int i = 0; i < 40; i++) {
                LickCrust.Scrape s = LickCrust.scrape(h.getLevel(), h.absolutePos(i % 2 == 0 ? a : b));
                h.assertTrue(!s.stack().isEmpty(), "scrape " + i + " came up empty");
                grams += s.stack().get(YoiComponents.WEIGHT_G.get());
                minutes += s.minutes();
            }
            h.assertTrue(grams == 2000, "40 scrapes give 2.0 kg, got " + grams);
            h.assertTrue(Math.abs(minutes / 60.0 - 10.0) <= 0.1, "10 game hours of work, got " + minutes / 60.0);
            h.assertTrue(LickCrust.scrape(h.getLevel(), h.absolutePos(a)).stack().isEmpty(), "a bare lick gives nothing");
            h.assertTrue(((SaltCrustBlockEntity) h.getBlockEntity(a)).grams() == 0 && h.getBlockState(a).getValue(SaltCrustBlock.CRUST) == 0, "the crust block reads bare");

            BlockPos ringPos = new BlockPos(1, 1, 2);
            h.setBlock(ringPos.below(), Blocks.STONE);
            h.setBlock(ringPos, FireRegistry.FIRE_RING.get());
            FireRingBlockEntity ring = h.getBlockEntity(ringPos);
            ring.forceState(FireState.FIRE, FireSetting.STEADY);
            ring.setFuel(3.0);
            ItemStack salt = LickCrust.boil(ring, grams);
            h.assertTrue(salt.is(LickRegistry.LICK_SALT.get()) && salt.get(YoiComponents.WEIGHT_G.get()) == 150, "one boil gives 0.15 kg of salt");
            h.assertTrue(Math.abs(ring.fire().armloads() - 2.0) < 1e-6, "one armload burned, " + ring.fire().armloads() + " left");
            h.assertTrue(salt.is(SALT) && new ItemStack(LickRegistry.LICK_SALT.get()).is(SALT), "lick salt is in #yearofice:salt");
            h.assertTrue(LickCrust.boil(ring, 1500).isEmpty(), "1.5 kg of crust is not enough for a boil");
            h.succeed();
        } finally {
            ClockService.get().setNow(saved);
        }
    }

    // ---- forage/lick_regrowth ------------------------------------------------------------------------------------

    /** An empty seep lick holds 0.2 kg after 7 dry days, 0.4 after a dry week that follows rain, never over 1 kg, and grows nothing from October to April. */
    static void lickRegrowth(GameTestHelper h) {
        h.setBlock(AT, LickRegistry.SALT_CRUST.get());
        SaltCrustBlockEntity be = h.getBlockEntity(AT);
        be.init(LickModel.SEEP_CAP_G, 0, 0);
        for (int d = 10; d < 17; d++) be.stepDay(d, false);
        h.assertTrue(Math.abs(be.grams() - 200) <= 10, "7 dry days: " + be.grams() + " g");
        be.init(LickModel.SEEP_CAP_G, 0, 0);
        be.stepDay(20, true);
        for (int d = 21; d < 28; d++) be.stepDay(d, false);
        h.assertTrue(Math.abs(be.grams() - 400) <= 10, "a dry week after rain: " + be.grams() + " g");
        be.init(LickModel.SEEP_CAP_G, 950, 0);
        for (int d = 30; d < 60; d++) be.stepDay(d, false);
        h.assertTrue(be.grams() <= 1000 && be.grams() >= 999, "never more than 1 kg: " + be.grams() + " g");
        be.init(LickModel.CLAY_CAP_G, 0, 0);
        for (int d = 30; d < 150; d++) be.stepDay(d, false);
        h.assertTrue(be.grams() == 500, "a clay-bank lick caps at 0.5 kg: " + be.grams() + " g");
        be.init(LickModel.SEEP_CAP_G, 0, 0);
        for (int d = 160; d < 340; d++) be.stepDay(d, false);
        h.assertTrue(be.grams() == 0, "nothing grows from October to April: " + be.grams() + " g");
        h.succeed();
    }
}

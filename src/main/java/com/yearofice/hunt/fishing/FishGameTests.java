package com.yearofice.hunt.fishing;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.FightModel;
import com.yearofice.sim.fishing.FishData;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.PassiveGear;
import com.yearofice.sim.fishing.SliceBite;
import com.yearofice.item.component.YoiComponents;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M5 lane C GameTests: the species data, stocks at generated spots, the season in the bite roll, the gill net's
 * segment rule and a landed fish's weight. A stub {@link FishSpotService} puts one lake spot around the test.
 */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class FishGameTests {
    private FishGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> fishTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(FishingGameTests.test("yearofice:fish/spot_stock_recovers", 100, FishGameTests::stockRecovers));
        out.add(FishingGameTests.test("yearofice:fish/bite_by_season", 100, FishGameTests::biteBySeason));
        out.add(FishingGameTests.test("yearofice:fish/gill_net_segments", 200, FishGameTests::gillNetSegments));
        out.add(FishingGameTests.test("yearofice:fish/fight_lands_weight", 1600, FishGameTests::fightLandsWeight));
        out.add(FishingGameTests.test("yearofice:data/fish_species_data_only", 100, FishGameTests::dataOnly));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    /** One lake spot around {@code at}: weed shelf, q 0.8, pike 14, burbot 8, whitefish 40, a lake so wide nets cover it. */
    static FishSpotService stub(BlockPos at) {
        FishSpotService.Spot spot = new FishSpotService.Spot("weed_shelf", at, 10, 0.8, 2.5, 1,
                List.of("pike", "burbot", "whitefish"), List.of(14, 8, 40), false);
        return new FishSpotService() {
            @Override
            public List<Spot> spotsNear(ServerLevel level, BlockPos pos, double radius) {
                return spot.contains(pos) ? List.of(spot) : List.of();
            }

            @Override
            public Optional<Spot> sliceSpot(ServerLevel level) { return Optional.empty(); }
        };
    }

    static void with(GameTestHelper h, Runnable body) {
        FishSpotService before = FishSpotService.get();
        FishSpotService.install(stub(h.absolutePos(BlockPos.ZERO)));
        try {
            body.run();
        } finally {
            FishSpotService.install(before);
        }
    }

    static SpotFish.Entry entry(SpotFish.Here here, String id) {
        for (SpotFish.Entry e : here.fish()) if (e.id().equals(id)) return e;
        throw new AssertionError("no " + id + " at the spot: " + here.fish());
    }

    /** Fish taken from a spot come back with the months (logistic regrowth) and the last catch is kept. */
    static void stockRecovers(GameTestHelper h) {
        with(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos pos = h.absolutePos(BlockPos.ZERO);
            SpotFish.Here here = SpotFish.at(level, pos, 40, FishingConfig.tuning());
            h.assertTrue(here.fish().size() == 3, "three species at the spot: " + here.fish());
            SpotFish.Entry pike = entry(here, "northern_pike");
            long cell = SliceSpots.key(pos) ^ 0x5151L; // a cell no other test uses
            long key = SliceSpots.speciesKey(cell, pike.id());
            SliceSpots.Stocks stocks = SliceSpots.stocks(level);
            long day = 40; // June 9
            h.assertTrue(Math.abs(SpotFish.stock(level, cell, day, pike).n() - pike.k()) < 1e-9, "starts full");
            for (int i = 0; i < 10; i++) stocks.harvest(key, day, pike.k(), pike.r(), 1, day * 1440);
            double low = SpotFish.stock(level, cell, day, pike).n();
            double later = SpotFish.stock(level, cell, day + 60, pike).n();
            double winter = SpotFish.stock(level, cell, day + 300, pike).n();
            h.assertTrue(low < pike.k() - 9.99, "fished down: " + low);
            h.assertTrue(later > low + 0.5, "recovers over two summer months: " + low + " -> " + later);
            h.assertTrue(winter <= pike.k() + 1e-9 && winter >= later - 1e-9, "never above K: " + winter);
            h.assertTrue(stocks.lastCatch(key) == day * 1440, "last catch kept");
            h.succeed();
        });
    }

    /** m_season in the bite roll: salmon absent outside their run, residents at 0.3 outside their window. */
    static void biteBySeason(GameTestHelper h) {
        int jan10 = FishSeason.day(1, 10), jul10 = FishSeason.day(7, 10), aug20 = FishSeason.day(8, 20), dec10 = FishSeason.day(12, 10), jun10 = FishSeason.day(6, 10);
        h.assertTrue(p("chinook_salmon", jul10) > 0 && p("chinook_salmon", jan10) == 0 && p("chinook_salmon", aug20) == 0, "chinook: July only");
        h.assertTrue(p("chum_salmon", aug20) > 0 && p("chum_salmon", jun10) == 0, "chum: Jul 15 to Aug 31");
        h.assertTrue(p("coho_salmon", FishSeason.day(9, 1)) > 0 && p("coho_salmon", jul10) == 0, "coho: from Aug 10");
        double gJun = p("arctic_grayling", jun10), gDec = p("arctic_grayling", dec10);
        h.assertTrue(gDec > 0 && Math.abs(gDec / gJun - 0.3) < 0.2, "grayling outside May-Sep counts about 0.3: " + gDec + " vs " + gJun);
        h.assertTrue(p("burbot", jan10) > 0 && p("northern_pike", dec10) > 0, "all-year fish bite in winter");
        h.assertTrue(!SpotFish.present("chinook_salmon", jan10, "outside_bend") && SpotFish.present("chinook_salmon", jul10, "outside_bend"), "presence");
        h.succeed();
    }

    static double p(String id, int day) {
        FishData d = FishingSpecies.data(id);
        String bait = d.species().bait().keySet().iterator().next();
        var in = new SliceBite.Inputs(d.species(), d.season(), "weed_shelf", true, 0.8, 2.5, 10, 10, 10, HookBait.fitted(bait, 5, 100),
                day, BiteModel.TimeBand.DAY, BiteModel.Weather.CLOUDY, 8.0, 0.0, false);
        return SliceBite.pBite(in);
    }

    /** A net of 3 joined segments never rolls in 200 days; 4 do. The same holds with a wing. */
    static void gillNetSegments(GameTestHelper h) {
        with(h, () -> {
            ServerLevel level = h.getLevel();
            BlockPos base = h.absolutePos(BlockPos.ZERO);
            long now = 200L * 1440;
            PassiveGearService.Gear three = PassiveGearService.place(level, PassiveGearService.Kind.GILL_NET, base.east(0), 3, 0);
            three.wingM = 30;
            PassiveGearService.Gear four = PassiveGearService.place(level, PassiveGearService.Kind.GILL_NET, base.north(1), 4, 0);
            var r3 = PassiveGearService.settle(level, three, now);
            var r4 = PassiveGearService.settle(level, four, now);
            h.assertTrue(r3.periods() >= 300, "settled every dawn and dusk: " + r3.periods());
            h.assertTrue(r3.caught() == 0 && r3.turnedAway() == 0 && three.held.isEmpty(), "3 segments never catch: " + r3);
            h.assertTrue(r4.caught() > 0 && !four.held.isEmpty(), "4 segments catch: " + r4);
            h.assertTrue(four.held.size() <= four.holds(), "holds cap");
            h.assertTrue(PassiveGear.netCoverage(3, 30, 10, false) == 0, "sim rule");
            PassiveGearService.book(level).all().clear();
            h.succeed();
        });
    }

    /** A dolly varden hooked at a stated weight lands at that weight in the pack. */
    static void fightLandsWeight(GameTestHelper h) {
        FishingGameTests.pool(h, 2);
        ServerPlayer p = FishingGameTests.angler(h);
        BobberEntity b = FishingGameTests.bobber(h, p);
        ServerLevel level = h.getLevel();
        b.biter = "dolly_varden";
        FightSession[] fight = new FightSession[1];
        FishingGameTests.hookFish(h, p, b, () -> fight[0] = b.fight);
        h.onEachTick(() -> {
            FightSession f = fight[0];
            if (f != null && !f.over()) f.accept(f.state().tick() + 1, new byte[] {FishingGameTests.bot(f)}, level.getGameTime());
        });
        h.succeedWhen(() -> {
            FightSession f = fight[0];
            h.assertTrue(f != null && f.over(), "fight over");
            h.assertTrue(f.params().species().id().equals("dolly_varden"), "species " + f.params().species().id());
            h.assertTrue(f.state().outcome() == FightModel.Outcome.LANDED, "outcome " + f.state().outcome());
            ItemStack fish = ItemStack.EMPTY;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(FishingRegistry.fishItem("dolly_varden"))) fish = p.getInventory().getItem(i);
            }
            h.assertFalse(fish.isEmpty(), "a dolly varden in the pack");
            Integer g = fish.get(YoiComponents.WEIGHT_G.get());
            h.assertTrue(g != null && g >= 300 && g <= 2000 && g == Math.round(f.params().kg() * 1000), "weight_g " + g);
            FishingGameTests.done(h, p);
        });
    }

    /** Every species is a data file with no hard-coded twin: the files parse, carry every key, and drive the sim. */
    static void dataOnly(GameTestHelper h) {
        for (String id : FishingSpecies.IDS) {
            FishData d = FishingSpecies.data(id);
            h.assertTrue(d.species().id().equals(id), id + ": id matches the file");
            h.assertTrue(d.species().bitesPerMin() > 0 && d.species().kgMax() >= d.species().kgMin(), id + ": numbers");
            h.assertTrue(FishingRegistry.fishItem(id) != null, id + ": has its landed-fish item");
            h.assertTrue(!d.capacity().isEmpty(), id + ": spot capacities");
        }
        // A species no Java names: parsed from JSON alone, it bites.
        var o = com.google.gson.JsonParser.parseString(
                "{\"id\":\"test_fish\",\"kg\":[1,2],\"bite_base\":0.1,\"kcal_per_kg\":1000,\"run_pull_x\":0.5,\"run_every_s\":[3,6],"
                + "\"run_length_s\":[1,2],\"jump_chance\":0,\"special\":\"none\",\"tire_per_s\":0.1,\"bite_through_no_leader\":0,"
                + "\"night_factor\":1,\"bait\":{\"worm\":1.0},\"season\":{\"all_year\":true},\"resident\":true,\"parasite\":\"plain\","
                + "\"net_c_per_h\":0.1,\"mesh\":\"fine\",\"capacity\":{\"x\":1},\"r_per_month\":0.3}").getAsJsonObject();
        FishData t = FishData.parse(o);
        var in = new SliceBite.Inputs(t.species(), t.season(), "x", true, 1.0, 2, 5, 10, 10, HookBait.fitted("worm", 2, 30), 100,
                BiteModel.TimeBand.DAY, BiteModel.Weather.CLOUDY, 8.0, 0.0, false);
        h.assertTrue(SliceBite.pBite(in) > 0, "a data-only species bites");
        h.succeed();
    }
}

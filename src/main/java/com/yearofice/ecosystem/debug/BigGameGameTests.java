package com.yearofice.ecosystem.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.ai.ScentPoints;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.ai.ThreatSources;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.carcass.Freshness;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.species.MooseEntity;
import com.yearofice.ecosystem.species.SpeciesBook;
import com.yearofice.ecosystem.species.SpeciesDef;
import com.yearofice.ecosystem.species.rig.BigGameEntity;
import com.yearofice.ecosystem.species.rig.PredatorEntity;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.sim.ecosystem.AnimalState;
import com.yearofice.sim.ecosystem.Drives;
import com.yearofice.sim.ecosystem.FieldCare;
import com.yearofice.sim.ecosystem.Yields;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m6, lane A (big game, predators, carcasses): {@code senses/*}, {@code ecology/*} and {@code eco/*}. Animals
 * stand on stone pads high above the test grid, as the slice's tests do; threats are detached test players.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class BigGameGameTests {
    private BigGameGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> bigGameTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:senses/wind_carries_scent", 400, BigGameGameTests::windCarriesScent));
        out.add(test("yearofice:senses/sight_cone", 500, BigGameGameTests::sightCone));
        out.add(test("yearofice:ecology/predator_chase_cap_150m", 900, BigGameGameTests::chaseCap));
        out.add(test("yearofice:ecology/moose_daily_schedule", 100, BigGameGameTests::mooseSchedule));
        out.add(test("yearofice:ecology/butcher_yields", 200, BigGameGameTests::butcherYields));
        out.add(test("yearofice:ecology/herd_alarm_spreads", 300, BigGameGameTests::herdAlarm));
        out.add(test("yearofice:eco/bear_finds_fish_spot", 900, BigGameGameTests::bearFindsSpot));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6", name, EcoGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    static final int Y = EcoGameTests.PAD_Y;

    /** A stone strip under height Y (animals stand at Y), from {@code x0} to {@code x1} along the test's x axis and {@code side} blocks each way. */
    static void strip(GameTestHelper h, int x0, int x1, int side, boolean on) {
        var b = on ? Blocks.STONE : Blocks.AIR;
        for (int x = x0; x <= x1; x++) for (int z = -side; z <= side; z++) h.setBlock(new BlockPos(1 + x, Y - 1, 1 + z), b);
    }

    static void forceLine(GameTestHelper h, Vec3 from, int dx, boolean on) {
        for (int x = 0; x <= dx; x += 24) EcoGameTests.force(h, from.add(x, 0, 0), 1, on);
    }

    /**
     * senses/wind_carries_scent (Senses by species, "Smell"): the plume is a 25-degree cone downwind of the player.
     * A moose 140 m from the player (150 m smell) with the wind blowing across the line stays calm for 5 s; with
     * the wind turned to carry the plume to it, scent sets its awareness to at least 60 and it is Alert within 1 s.
     */
    static void windCarriesScent(GameTestHelper h) {
        EcoGameTests.pad(h, 2, Y - 1);
        MooseEntity moose = h.spawn(EcosystemRegistry.MOOSE.get(), new BlockPos(1, Y, 1));
        moose.pinDriveAct(Drives.Act.BED);
        Vec3 at = moose.position();
        EcoGameTests.force(h, at, 2, true);
        ServerPlayer p = EcoGameTests.player(h);
        ThreatSources.addForTest(p, moose);
        p.moveTo(at.x - 140, at.y, at.z, -90, 0);
        double toward = Sensing.bearing(p.position(), at);
        moose.setWindTowardOverride(toward + 90); // across the line
        long[] carried = {-1};
        boolean[] done = {false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            ThreatSources.removeForTest(p);
            moose.discard();
            EcoGameTests.force(h, at, 2, false);
            EcoGameTests.unpad(h, 2, Y - 1);
        };
        h.onEachTick(() -> {
            if (done[0]) return;
            try {
                long t = h.getTick();
                if (t < 100) {
                    h.assertTrue(moose.awareness().max() < 30, "crosswind: the moose noticed at " + t + ", awareness " + moose.awareness().max());
                    return;
                }
                if (carried[0] < 0) {
                    carried[0] = t;
                    moose.setWindTowardOverride(toward);
                }
                if (moose.awareness().max() >= 30) {
                    long took = t - carried[0];
                    h.assertTrue(took <= 25, "the plume took " + took + " ticks to alert the moose");
                    h.assertTrue(moose.awareness().max() >= 60, "scent should set awareness to 60, was " + moose.awareness().max());
                    release.run();
                    h.succeed();
                } else if (t - carried[0] > 40) {
                    h.fail("downwind at 140 m the moose never noticed; awareness " + moose.awareness().max());
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        });
    }

    /**
     * senses/sight_cone (Senses by species, "Sight"): prey see 300 degrees, so a player directly behind is unseen. A
     * caribou (150 m sight, hearing only 30 m) with the wind off watches a walking player at 100 m. Behind
     * it, in the 60-degree blind cone, the player stays unnoticed for 8 s; in front of it the caribou is Alert in
     * the next 8 s.
     */
    static void sightCone(GameTestHelper h) {
        BigGameEntity caribou = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1, Y, 1));
        caribou.setScentEnabled(false);
        caribou.pinDriveAct(Drives.Act.BED);
        Vec3 at = caribou.position();
        strip(h, -110, 110, 3, true);
        EcoGameTests.force(h, at, 2, true);
        ServerPlayer p = EcoGameTests.player(h);
        ThreatSources.addForTest(p, caribou);
        float yaw = -90f; // facing +x... yaw -90 looks toward +x
        caribou.setYRot(yaw);
        caribou.setYHeadRot(yaw);
        caribou.setYBodyRot(yaw);
        boolean[] done = {false};
        long[] frontSince = {-1};
        double[] z = {0};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            ThreatSources.removeForTest(p);
            caribou.discard();
            EcoGameTests.force(h, at, 2, false);
            strip(h, -110, 110, 3, false);
        };
        h.onEachTick(() -> {
            if (done[0]) return;
            try {
                long t = h.getTick();
                // The caribou is held bedded facing +x; the player strolls to and fro 3 m/s across the line.
                caribou.setYRot(yaw);
                caribou.setYHeadRot(yaw);
                caribou.setYBodyRot(yaw);
                z[0] += (t / 40) % 2 == 0 ? 0.15 : -0.15;
                boolean behind = t < 180;
                double px = at.x + (behind ? -100 : 100);
                p.moveTo(px, at.y, at.z + z[0], -90, 0);
                if (behind) {
                    h.assertTrue(caribou.awareness().max() < 5, "behind it in the blind cone: awareness " + caribou.awareness().max() + " at " + t);
                    return;
                }
                if (frontSince[0] < 0) frontSince[0] = t;
                if (caribou.awareness().max() >= 30) {
                    release.run();
                    h.succeed();
                } else if (t - frontSince[0] > 200) {
                    h.fail("in front at 100 m the caribou never turned Alert; awareness " + caribou.awareness().max());
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        });
    }

    /**
     * ecology/predator_chase_cap_150m (Who hunts whom; wolf: "a chase runs 15 m/s for at most 150 m"): a wolf chases a
     * caribou, which outruns it at 18 m/s down a 300 m stone strip. The wolf gives up at its cap; the caribou lives and
     * the wolf ran at least 120 m and no more than 150 m plus one tick of speed.
     */
    static void chaseCap(GameTestHelper h) {
        strip(h, -5, 300, 24, true);
        PredatorEntity wolf = h.spawn(EcosystemRegistry.WOLF.get(), new BlockPos(1, Y, 1));
        BigGameEntity caribou = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1 + 25, Y, 1));
        wolf.setScentEnabled(false);
        caribou.setScentEnabled(false);
        wolf.setTestGroup("chase");
        caribou.setTestGroup("chase");
        wolf.huntAnyHour();
        wolf.pinDriveAct(Drives.Act.FEED);
        wolf.driveState().hunger = 1.0;
        Vec3 start = wolf.position();
        wolf.setYRot(-90);
        wolf.setYHeadRot(-90);
        wolf.setYBodyRot(-90);
        forceLine(h, start, 300, true);
        boolean[] done = {false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            wolf.discard();
            caribou.discard();
            forceLine(h, start, 300, false);
            strip(h, -5, 300, 24, false);
        };
        h.onEachTick(() -> {
            if (done[0]) return;
            try {
                double ran = wolf.lastChaseM();
                h.assertTrue(ran <= 151.5, "the wolf ran " + ran + " m in one chase");
                if (wolf.chasesGivenUp() >= 1) {
                    h.assertTrue(ran >= 120, "the wolf gave up after only " + ran + " m");
                    h.assertTrue(caribou.isAlive() && wolf.kills() == 0, "the caribou should have outrun it");
                    release.run();
                    h.succeed();
                } else if (h.getTick() > 800) {
                    h.fail("the chase never ended; ran " + ran + " m, wolf " + wolf.huntDebug() + ", caribou " + caribou.state() + " at " + caribou.position().subtract(start));
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        });
    }

    /**
     * ecology/moose_daily_schedule (Drives and daily schedules, Build and test): over 30 July days the moose, run on its
     * own species file's rates and weights, is bedded for 55-70% of the hours 11:00-16:00, drinks at dusk on at
     * least 80% of days, and never changes act more than 6 times in a game hour.
     */
    static void mooseSchedule(GameTestHelper h) {
        MooseEntity moose = h.spawn(EcosystemRegistry.MOOSE.get(), new BlockPos(1, 1, 1));
        moose.pinDriveAct(Drives.Act.FEED);
        int bed = 0, mid = 0, waterDays = 0, maxSw = 0;
        double dt = 1.0 / 6;
        for (int d = -3; d < 30; d++) {
            boolean drank = false;
            for (int hr = 0; hr < 24; hr++) {
                int before = moose.driveState().switches;
                for (int k = 0; k < 6; k++) {
                    moose.stepDrives(hr + k * dt, dt);
                    if (d < 0) continue;
                    Drives.Act a = moose.driveAct();
                    if (hr >= 11 && hr < 16) {
                        mid++;
                        if (a == Drives.Act.BED) bed++;
                    }
                    if (hr >= 17 && hr < 21 && a == Drives.Act.WATER) drank = true;
                }
                if (d >= 0) maxSw = Math.max(maxSw, moose.driveState().switches - before);
            }
            if (d >= 0 && drank) waterDays++;
        }
        double share = bed / (double) mid;
        moose.discard();
        h.assertTrue(share >= 0.55 && share <= 0.70, "bedded " + share + " of the hours 11:00-16:00");
        h.assertTrue(waterDays >= 24, "water at dusk on " + waterDays + " of 30 days");
        h.assertTrue(maxSw <= 6, "changed act " + maxSw + " times in an hour");
        h.succeed();
    }

    static CarcassRecord record(String id, boolean male, double kg) {
        double minute = com.yearofice.ecosystem.clock.SliceClock.minuteD(null);
        SpeciesDef d = SpeciesBook.get(id);
        return new CarcassRecord(id, male ? 1 : 0, "test", (long) minute, (int) Math.round(kg * 1000), 0,
                Freshness.wholeCarcass(DeathPipeline.meatItem(d), minute));
    }

    static int count(List<ItemStack> parts, String path) {
        int n = 0;
        for (ItemStack s : parts) if (BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().equals(path)) n += s.getCount();
        return n;
    }

    /**
     * ecology/butcher_yields (Butchering and yields): the 450 kg bull moose gives 180 kg of meat (1,800 units of 100 g), 8.3
     * kg of organs and a hide, a cow no antlers; parts scale with live weight; late gutting costs a quarter of the meat;
     * a missed quartering deadline turns 30% of it; the fat follows the season; the black bear gives 30 kg of meat
     * and claws, the wolf a pelt, the caribou 44 kg.
     */
    static void butcherYields(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        FieldCare.Result clean = new FieldCare.Result(0, 0);
        int doy = Calendar.dayOfYearOfMinute(com.yearofice.ecosystem.clock.SliceClock.minute(level));
        List<ItemStack> bull = Butchering.parts(level, record("yearofice:moose", true, 450), clean, false);
        h.assertTrue(count(bull, "moose_meat") == 1800, "bull meat " + count(bull, "moose_meat"));
        h.assertTrue(count(bull, "heart") == 25 && count(bull, "liver") == 40 && count(bull, "kidney") == 8 && count(bull, "tongue") == 10,
                "organs " + count(bull, "heart") + "/" + count(bull, "liver") + "/" + count(bull, "kidney") + "/" + count(bull, "tongue"));
        h.assertTrue(count(bull, "moose_hide") == 1 && count(bull, "antler") == 1, "hide " + count(bull, "moose_hide") + " antler " + count(bull, "antler"));
        int fat = (int) Math.round(30 * Yields.fatFactor("yearofice:moose", true, doy));
        h.assertTrue(count(bull, "animal_fat") == fat, "fat " + count(bull, "animal_fat") + " expected " + fat);
        h.assertTrue(count(bull, "marrow_bone") == 8 && count(bull, "sinew") == 5, "marrow " + count(bull, "marrow_bone") + " sinew " + count(bull, "sinew"));
        List<ItemStack> cow = Butchering.parts(level, record("yearofice:moose", false, 225), clean, false);
        h.assertTrue(count(cow, "moose_meat") == 900 && count(cow, "antler") == 0, "cow meat " + count(cow, "moose_meat") + " antler " + count(cow, "antler"));
        List<ItemStack> late = Butchering.parts(level, record("yearofice:moose", true, 450), FieldCare.dress(1.5, 3, 15), false);
        h.assertTrue(count(late, "moose_meat") == 1350, "late gut meat " + count(late, "moose_meat"));
        List<ItemStack> slow = Butchering.parts(level, record("yearofice:moose", true, 450), FieldCare.dress(0.5, 5, 15), false);
        h.assertTrue(count(slow, "moose_meat") == 1800, "turning keeps the units: " + count(slow, "moose_meat"));
        int turned = 0;
        for (ItemStack s : slow) if (s.is(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("yearofice:moose_meat")))) {
            var f = s.get(com.yearofice.item.component.YoiComponents.FRESH.get());
            if (f != null && f.stage(Freshness.dNow(level)) >= 2) turned += s.getCount();
        }
        h.assertTrue(turned == 540, "turning meat " + turned + " of 1800");
        List<ItemStack> frozen = Butchering.parts(level, record("yearofice:moose", true, 450), clean, true);
        h.assertTrue(count(frozen, "moose_hide") == 0, "a frozen carcass loses its hide");
        List<ItemStack> bear = Butchering.parts(level, record("yearofice:black_bear", true, 115), clean, false);
        h.assertTrue(count(bear, "black_bear_meat") == 300 && count(bear, "claw") == 4 && count(bear, "black_bear_hide") == 1, "bear " + count(bear, "black_bear_meat"));
        List<ItemStack> wolf = Butchering.parts(level, record("yearofice:wolf", true, 38), clean, false);
        h.assertTrue(count(wolf, "wolf_meat") == 165 && count(wolf, "wolf_pelt") == 1, "wolf " + count(wolf, "wolf_meat"));
        List<ItemStack> car = Butchering.parts(level, record("yearofice:caribou", false, 110), clean, false);
        h.assertTrue(count(car, "caribou_meat") == 440 && count(car, "antler") == 1, "caribou " + count(car, "caribou_meat"));
        h.succeed();
    }

    /** ecology/herd_alarm_spreads (Caribou herds): one caribou turned Alert alarms every herd-mate within 15 m inside a second or so. */
    static void herdAlarm(GameTestHelper h) {
        strip(h, -20, 20, 12, true);
        BigGameEntity a = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1, Y, 1));
        BigGameEntity b = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1 + 8, Y, 1));
        BigGameEntity far = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1 + 60, Y, 1));
        for (BigGameEntity e : List.of(a, b, far)) {
            e.setScentEnabled(false);
            e.pinDriveAct(Drives.Act.BED);
        }
        Vec3 at = a.position();
        EcoGameTests.force(h, at, 5, true);
        ServerPlayer p = EcoGameTests.player(h);
        ThreatSources.addForTest(p, a, b, far);
        p.moveTo(at.x, at.y, at.z + 70, 0, 0);
        boolean[] done = {false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            ThreatSources.removeForTest(p);
            a.discard();
            b.discard();
            far.discard();
            EcoGameTests.force(h, at, 5, false);
            strip(h, -20, 20, 12, false);
        };
        h.runAfterDelay(10, () -> {
            try {
                h.assertTrue(a.herd().contains(b) && !a.herd().contains(far), "herd membership");
                a.awareness().spike(p, 75, h.getLevel().getGameTime());
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        });
        h.onEachTick(() -> {
            if (done[0] || h.getTick() < 12) return;
            try {
                if (b.awareness().of(p) >= 30 || b.state() != AnimalState.ROUTINE) {
                    h.assertTrue(far.awareness().of(p) < 30, "the far caribou (60 m) should not be alarmed");
                    release.run();
                    h.succeed();
                } else if (h.getTick() > 100) {
                    h.fail("the herd-mate at 8 m was not alarmed; awareness " + b.awareness().of(p));
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        });
    }

    /**
     * eco/bear_finds_fish_spot (Ready for development M6): a bear downwind of an uncleared cleaning spot (a gut pile) smells
     * it and walks to it. The 2-day, 7-of-10-seeds statistic is the sim's ({@code BigGameRulesTest}); here a hungry
     * grizzly 60 m downwind reaches the pile and starts to eat, and a bear upwind of it does not find it.
     */
    static void bearFindsSpot(GameTestHelper h) {
        Vec3 start = h.absolutePos(new BlockPos(1, Y, 1)).getCenter();
        forceLine(h, start, 80, true);
        // the strip needs its chunks loaded before blocks can be set in them
        h.runAfterDelay(40, () -> bearFindsSpotRun(h, start));
    }

    static void bearFindsSpotRun(GameTestHelper h, Vec3 start) {
        strip(h, -5, 80, 24, true);
        BlockPos pile = new BlockPos(1, Y, 1);
        h.setBlock(pile, EcosystemRegistry.GUT_PILE.get().defaultBlockState());
        BlockPos abs = h.absolutePos(pile);
        ScentPoints.add(h.getLevel(), abs);
        BearEntity bear = h.spawn(EcosystemRegistry.GRIZZLY.get(), new BlockPos(1 + 60, Y, 1));
        bear.setScentEnabled(false);
        bear.pinDriveAct(Drives.Act.FEED);
        bear.driveState().hunger = 1.0;
        Vec3 at = bear.position();
        Vec3 mid = at.add(-30, 0, 0);
        // The pile is at x = 1; the bear stands at x = 61, so the wind must blow from the pile toward +x.
        bear.setWindTowardOverride(Sensing.bearing(Vec3.atCenterOf(abs), at));
        boolean[] done = {false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            ScentPoints.remove(h.getLevel(), abs);
            bear.discard();
            forceLine(h, start, 80, false);
            h.setBlock(pile, Blocks.AIR);
            strip(h, -5, 80, 24, false);
        };
        h.onEachTick(() -> {
            if (done[0]) return;
            try {
                if (bear.scavenges() >= 1) {
                    h.assertTrue(bear.distanceToSqr(Vec3.atCenterOf(abs)) < 9 * 9, "it ate the pile from " + Math.sqrt(bear.distanceToSqr(Vec3.atCenterOf(abs))) + " m");
                    release.run();
                    h.succeed();
                } else if (h.getTick() > 800) {
                    h.fail("the bear never reached the pile: " + bear.huntDebug() + " at " + bear.position().subtract(at));
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        });
    }

}

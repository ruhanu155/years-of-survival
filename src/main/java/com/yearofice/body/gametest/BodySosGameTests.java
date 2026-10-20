package com.yearofice.body.gametest;

import com.yearofice.api.body.SleepService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.DamageRouter;
import com.yearofice.body.LeftPack;
import com.yearofice.body.RunState;
import com.yearofice.body.SosFlow;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.body.sleep.Sleep;
import com.yearofice.core.YearOfIce;
import com.yearofice.gametest.TestFilter;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.DayPlan;
import com.yearofice.sim.body.Metabolism;
import com.yearofice.sim.env.Precip;
import com.yearofice.story.Journal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Batch {@code m4}: collapse, the Endless SOS, ground sleep, vanilla needs held and the body_sync rate (M4 lane C). */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodySosGameTests {
    private BodySosGameTests() {}

    private static final String BATCH = "m4";

    @GameTestGenerator
    public static Collection<TestFunction> m4Tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(t("yearofice:body/vanilla_needs_held", 200, BodySosGameTests::vanillaNeedsHeld));
        out.add(t("yearofice:body/ground_sleep", 200, BodySosGameTests::groundSleep));
        out.add(t("yearofice:body/sos_jump", 200, BodySosGameTests::sosJump));
        out.add(t("yearofice:body/own_body_sos", 400, BodySosGameTests::ownBodySos));
        out.add(t("yearofice:body/hardcore_end", 200, BodySosGameTests::hardcoreEnd));
        out.add(t("yearofice:body/fireless_night_standard_survives", 200, h -> firelessNight(h, "standard")));
        out.add(t("yearofice:body/fireless_night_brutal_sos", 200, h -> firelessNight(h, "brutal")));
        out.add(t("yearofice:net/body_sync_rate", 200, BodySosGameTests::bodySyncRate));
        return TestFilter.apply(out);
    }

    private static TestFunction t(String name, int ticks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, YearOfIce.MOD_ID + ":gametest/empty3", ticks, 0, true, body);
    }

    private static ServerPlayer bot(GameTestHelper h) {
        ServerPlayer p = BodyGameTests.player(h);
        var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 2, 1.5));
        p.moveTo(at.x, at.y, at.z, 0, 0);
        p.getInventory().clearContent();
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }

    private static void vanillaNeedsHeld(GameTestHelper h) {
        ServerPlayer p = bot(h);
        p.getFoodData().setFoodLevel(3);
        p.getFoodData().setSaturation(4);
        p.setSprinting(true);
        DamageRouter.route(p, h.getLevel().damageSources().fall(), 1f);
        p.setHealth(4);
        // A game day of vanilla's own food tick and the held tick, with sprint exhaustion, a meal and a fall between.
        for (int i = 0; i < 24 * 60; i++) {
            p.causeFoodExhaustion(0.5f);
            if (i % 200 == 0) p.getFoodData().eat(6, 0.8f);
            if (i % 300 == 0) DamageRouter.route(p, h.getLevel().damageSources().fall(), 0f);
            p.getFoodData().tick(p);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            h.assertTrue(p.getFoodData().getFoodLevel() == 20, "food level 20 at step " + i);
            h.assertTrue(p.getHealth() == p.getMaxHealth(), "health 20 at step " + i);
        }
        h.assertTrue(p.getFoodData().getSaturationLevel() == 0 && p.getFoodData().getExhaustionLevel() == 0,
                "saturation and exhaustion held at 0");
        h.assertTrue(!h.getLevel().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION),
                "no vanilla regeneration tick: the rule is off");
        h.succeed();
    }

    private static void groundSleep(GameTestHelper h) {
        ServerPlayer p = bot(h);
        BodyState s = BodyAttachments.of(p);
        s.energy = 2;
        long start = 22 * 60;
        Sleep sleep = (Sleep) SleepService.get();
        BlockPos at = p.blockPosition();
        h.assertTrue(sleep.lieOnGround(p, start) == SleepService.Refusal.NONE, "lies down on the ground");
        h.assertTrue(h.getLevel().getBlockState(at).is(LeftPack.GROUND_BED.get()), "a ground_bed marker is under the sleeper");
        h.assertTrue(p.isSleeping(), "asleep");
        BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(15, 0, Precip.NONE, 0, Double.NaN));
        try {
            BodyTicker.step(p, start, 8 * 60, Activity.REST, true, false, BodyTuning.STANDARD);
        } finally {
            BodyTicker.VANILLA_ENV.override(p, null);
        }
        double gained = s.energy - 2;
        h.assertTrue(gained > 5.5 && gained < 7.1, "8 h on bare ground restore about 1.25 x 0.70 x 8 = 7 bolts, got " + gained);
        p.stopSleepInBed(false, false);
        h.assertTrue(!p.isSleeping(), "awake");
        h.assertTrue(!h.getLevel().getBlockState(at).is(LeftPack.GROUND_BED.get()), "no ground_bed remains after waking");
        Sleep.forget(p);
        h.succeed();
    }

    private static void sosJump(GameTestHelper h) {
        ServerPlayer p = bot(h);
        ClockService clock = ClockService.get();
        long ds = clock.nowDs();
        try {
            BlockPos placed = p.blockPosition().offset(3, 0, 0);
            h.getLevel().setBlock(placed, Blocks.COBBLESTONE.defaultBlockState(), 3);
            BlockPos spot = p.blockPosition();
            p.getInventory().add(new ItemStack(Items.COBBLESTONE, 10));
            p.getInventory().add(new ItemStack(MessengerItems.MESSENGER.get()));
            BodyState s = BodyAttachments.of(p);
            s.kg = Metabolism.kgForBmi(16.9, s.heightCm);
            long before = clock.nowMinute();
            long t0 = System.nanoTime();
            SosFlow.trigger(p, SosFlow.Cause.STARVATION);
            double secs = (System.nanoTime() - t0) / 1e9;
            h.assertTrue(secs < 5, "the jump took " + secs + " s");
            h.assertTrue(clock.nowMinute() - before == 14 * 1440, "14 game days passed, got " + (clock.nowMinute() - before) / 1440.0);
            h.assertTrue(s.sos == 0, "flight over, sos state " + s.sos);
            h.assertTrue(Math.abs(s.bmi() - 19) < 0.01, "BMI 19, got " + s.bmi());
            h.assertTrue(Math.abs(s.condition() - 80) < 0.01, "Condition 80, got " + s.condition());
            h.assertTrue(s.warmth == 100 && s.water == 100 && s.energy == 10, "warmth, water and energy full");
            h.assertTrue(p.gameMode.getGameModeForPlayer() == GameType.SURVIVAL, "back in survival");
            BlockPos packAt = null;
            for (int dy = 0; dy < 4 && packAt == null; dy++) {
                if (h.getLevel().getBlockState(spot.above(dy)).is(LeftPack.BLOCK.get())) packAt = spot.above(dy);
            }
            h.assertTrue(packAt != null, "a left_pack block stands at the collapse spot");
            var be = (LeftPack.PackEntity) h.getLevel().getBlockEntity(packAt);
            h.assertTrue(be != null && be.items.stream().mapToInt(ItemStack::getCount).sum() == 10, "the 10 stones are in the pack");
            h.assertTrue(p.getInventory().contains(new ItemStack(MessengerItems.MESSENGER.get())), "the messenger stays");
            h.assertTrue(!p.getInventory().contains(new ItemStack(Items.COBBLESTONE)), "the pack was left");
            boolean kit = false;
            for (ItemStack st : p.getInventory().items) {
                if (BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().equals("crate_backpack")) kit = true;
            }
            h.assertTrue(kit, "the starting kit is in the pockets");
            h.assertTrue(h.getLevel().getBlockState(placed).is(Blocks.COBBLESTONE), "every placed block intact");
            List<Journal.Entry> j = Journal.entries(p);
            h.assertTrue(j.size() == 1 && j.get(0).kind().equals(Journal.FLIGHT) && j.get(0).cause().equals("starvation"),
                    "the journal holds the flight and its cause");
            h.assertTrue(SosFlow.flightsOut(p) == 1, "one flight out counted");
            h.succeed();
        } finally {
            clock.setNow(ds);
            BodyTicker.resync(h.getLevel().getServer());
        }
    }

    private static void ownBodySos(GameTestHelper h) {
        int[][] bodies = {{183, 0, 77, 18}, {160, 1, 55, 14}, {195, 0, 95, 24}};
        for (int[] b : bodies) {
            ServerPlayer p = bot(h);
            try {
                BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(15, 0, Precip.NONE, 0, Double.NaN));
                BodyState s = BodyAttachments.of(p);
                s.heightCm = b[0];
                s.sex = b[1];
                s.kg = b[2];
                long start = 6 * 60;
                BodyModel.Result r = BodyModel.Result.NONE;
                for (int day = 0; day < 40 && !r.collapsed(); day++) {
                    long m = start + day * 1440L;
                    BodyModel.drink(BodyAttachments.of(p), m, 3.0, -1);
                    r = BodyTicker.stepPlan(p, m, 1440, DayPlan.CHORES, BodyTuning.STANDARD);
                }
                h.assertTrue(r.collapse() == BodyModel.Collapse.BMI, b[0] + " cm: expected a BMI collapse, got " + r.collapse());
                double days = (r.minute() - start) / 1440.0;
                // RECONCILE 212: the 160 cm female row expects 14 +-1; the model's own formula gives 13.75.
                double tol = 1.0;
                h.assertTrue(Math.abs(days - b[3]) <= tol, b[0] + " cm " + b[2] + " kg: SOS after " + days + " days, wanted " + b[3] + " +-1");
            } finally {
                BodyTicker.VANILLA_ENV.override(p, null);
            }
        }
        h.succeed();
    }

    private static final double[] NIGHT_AIR = {3.1, 2.0, 0.9, 3.8, 3.2, 2.7, 2.3, 2.0, 2.0};

    /**
     * RECONCILE 225: a fireless first night, 21:00 to 06:00 at 1-4 degC in the starting clothes on bare ground. The
     * preset is switched with {@code /yoi preset}; Standard ends miserable (cold wakes, warmth near 30, no collapse),
     * Brutal collapses into the SOS. The game clock is not waited on: nine hourly steps of the live body model.
     */
    private static void firelessNight(GameTestHelper h, String preset) {
        ServerPlayer p = bot(h);
        var server = h.getLevel().getServer();
        var prior = com.yearofice.core.rules.YoiConfig.PRESET.get();
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "yoi preset " + preset);
            BodyTuning t = com.yearofice.body.BodyConfig.tuning();
            h.assertTrue(t.preset().equals(preset), "preset switched to " + preset + ", body reads " + t.preset());
            BodyState s = BodyAttachments.of(p);
            s.warmth = 82;
            double low = 100;
            int wakes = 0;
            long m = 21 * 60;
            boolean collapsed = false;
            for (int hr = 0; hr < NIGHT_AIR.length && !collapsed; hr++) {
                var env = com.yearofice.sim.env.EnvSample.outdoors(NIGHT_AIR[hr], 0.5);
                boolean asleep = hr >= 1 && s.warmth >= 40;
                if (hr >= 1 && !asleep) wakes++;
                var in = new BodyModel.Inputs(asleep ? Activity.SLEEP : Activity.REST, asleep, asleep ? 0.7 : 0, env, List.of(),
                        com.yearofice.sim.env.ShelterState.NONE, com.yearofice.sim.body.ColdWater.Depth.NONE, false);
                collapsed = BodyModel.step(s, in, t, m, 60).collapsed();
                m += 60;
                low = Math.min(low, s.warmth);
            }
            if (preset.equals("standard")) {
                h.assertTrue(!collapsed, "Standard survives the fireless night");
                h.assertTrue(wakes >= 1, "the cold wakes the sleeper: " + wakes);
                h.assertTrue(low >= 20 && low <= 40, "warmth bottoms near 30: " + low);
            } else {
                h.assertTrue(collapsed, "Brutal collapses on the fireless night, low " + low);
            }
        } finally {
            com.yearofice.core.rules.YoiConfig.PRESET.set(prior);
        }
        h.succeed();
    }

    private static void hardcoreEnd(GameTestHelper h) {
        ServerPlayer p = bot(h);
        var server = h.getLevel().getServer();
        Path record = server.getWorldPath(LevelResource.ROOT).resolve("yearofice").resolve("run_record.json");
        RunState rs = RunState.get(server);
        SosFlow.hardcoreOverride = () -> true;
        try {
            Files.deleteIfExists(record);
            long before = ClockService.get().nowMinute();
            SosFlow.trigger(p, SosFlow.Cause.HYPOTHERMIA);
            h.assertTrue(rs.ended && rs.cause.equals("hypothermia"), "the world flag is set");
            h.assertTrue(p.isSpectator(), "the player is a spectator");
            h.assertTrue(Files.exists(record), "the run record exists");
            h.assertTrue(ClockService.get().nowMinute() - before < 1440, "no 14-day jump in Hardcore");
            h.assertTrue(Journal.entries(p).stream().anyMatch(e -> e.kind().equals(Journal.RUN_ENDED)), "the journal holds the end");
            h.succeed();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        } finally {
            SosFlow.hardcoreOverride = null;
            rs.ended = false;
            rs.cause = "";
            rs.setDirty();
            try {
                Files.deleteIfExists(record);
            } catch (java.io.IOException ignored) {
                // test leftovers only
            }
        }
    }

    private static void bodySyncRate(GameTestHelper h) {
        ServerPlayer p = bot(h);
        long[] tick = {0};
        BodyTicker.tickSource = () -> tick[0];
        try {
            BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(15, 0, Precip.NONE, 0, Double.NaN));
            BodyTicker.resetSentCount(p);
            long start = 12 * 60;
            // A time-lapse: 180 game minutes in 360 real ticks (18 s). At most one packet a second, and the hourly record.
            for (int i = 0; i < 180; i++) {
                tick[0] += 2;
                BodyTicker.step(p, start + i, 1, Activity.REST, false, false, BodyTuning.STANDARD);
            }
            int sent = BodyTicker.sentCount(p);
            h.assertTrue(sent <= 360 / BodyTicker.MIN_SYNC_TICKS + 1, "at most 1 body_sync a second, got " + sent + " in 18 s");
            h.assertTrue(sent >= 3, "one full record per game hour, got " + sent + " in 3 game hours");
            // At the world clock (1 game minute a second) an idle player gets at most one a second.
            BodyTicker.resetSentCount(p);
            for (int i = 0; i < 60; i++) {
                tick[0] += 20;
                BodyTicker.step(p, start + 200 + i, 1, Activity.REST, false, false, BodyTuning.STANDARD);
            }
            h.assertTrue(BodyTicker.sentCount(p) <= 60, "at most 60 packets in 60 s, got " + BodyTicker.sentCount(p));
            h.succeed();
        } finally {
            BodyTicker.tickSource = null;
            BodyTicker.VANILLA_ENV.override(p, null);
        }
    }
}

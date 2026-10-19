package com.yearofice.body.gametest;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.body.BodyService;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyCodec;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.DamageRouter;
import com.yearofice.body.clock.BodyClock;
import com.yearofice.body.clock.VanillaDayClock;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.body.intake.Drinking;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.core.YearOfIce;
import com.yearofice.item.messenger.MessengerItems;
import com.yearofice.item.messenger.MessengerKit;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.DayPlan;
import com.yearofice.sim.body.Metabolism;
import com.yearofice.sim.body.part.Part;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import java.util.Arrays;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Batch {@code m2_5_body}: the body slice in a real server, on Standard. Mock players are creative, so the live
 * ticker leaves them alone and each test drives the body itself through the same entry points the ticker and
 * {@code /yoi time forward} use.
 */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BodyGameTests {
    private BodyGameTests() {}

    private static final String T = "gametest/empty3";
    private static final String BATCH = "m2_5_body";

    /**
     * A detached server player with a loopback connection: real enough for attachments, saving, teleporting and
     * messages, but never placed in the player list, so login listeners (Curios sends its own sync to every joining
     * player, which a test connection refuses) and the live ticker leave it alone. Each test drives the body
     * through the same entry points the ticker, the commands and the listeners use.
     */
    static ServerPlayer player(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "body-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.setData(BodyAttachments.BODY.get(), new BodyState());
        return p;
    }

    private static void done(GameTestHelper h, ServerPlayer p) {
        BodyTicker.VANILLA_ENV.override(p, null);
    }

    /** No food on Standard, a scripted day of chores, water drunk each morning: BMI 17 on day 15–21. */
    @GameTest(template = T, batch = BATCH, timeoutTicks = 200)
    public static void starvation_bmi17_day15_21(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(15, 0, Precip.NONE, 0, Double.NaN));
            BodyTuning t = BodyTuning.STANDARD;
            long start = 6 * 60;
            BodyModel.Result r = BodyModel.Result.NONE;
            double lastBmi = 23;
            for (int day = 0; day < 30 && !r.collapsed(); day++) {
                long m = start + day * 1440L;
                BodyModel.drink(BodyAttachments.of(p), m, 3.0, -1);
                lastBmi = BodyAttachments.of(p).bmi();
                r = BodyTicker.stepPlan(p, m, 1440, DayPlan.CHORES, t);
            }
            h.assertTrue(r.collapse() == BodyModel.Collapse.BMI, "expected a BMI collapse, got " + r.collapse()
                    + " (last BMI " + lastBmi + ")");
            double days = (r.minute() - start) / 1440.0;
            h.assertTrue(days >= 15 && days <= 21, "BMI 17 after " + days + " days, wanted 15-21");
            BodyState s = BodyAttachments.of(p);
            h.assertTrue(Math.abs(s.bmi() - 19) < 0.01 && s.sos == 0, "SOS return at BMI 19, got " + s.bmi());
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** Soaked at −20 °C, no wind, no fire: warmth 100 → 0 in 13–17 game minutes (the soaked rule). */
    @GameTest(template = T, batch = BATCH)
    public static void soaked_minus20_warmth0_13_17min(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(-20, 0, Precip.NONE, 0, Double.NaN));
            BodyState s = BodyAttachments.of(p);
            Arrays.fill(s.slotWet, 1.0);
            long m = 12 * 60;
            int minutes = 0;
            BodyModel.Result r = BodyModel.Result.NONE;
            while (!r.collapsed() && minutes < 60) {
                r = BodyTicker.step(p, m + minutes, 1, Activity.REST, false, false, BodyTuning.STANDARD);
                minutes++;
            }
            h.assertTrue(r.collapse() == BodyModel.Collapse.WARMTH, "expected hypothermia, got " + r.collapse());
            h.assertTrue(minutes >= 13 && minutes <= 17, "warmth 0 after " + minutes + " min, wanted 13-17");
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** The record written by the player's own save loads back field for field into a fresh player. */
    @GameTest(template = T, batch = BATCH)
    public static void body_attachment_survives_save_reload(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            BodyState s = BodyAttachments.of(p);
            s.kg = 70.3;
            s.warmth = 41.5;
            s.water = 33.25;
            s.energy = 4.2;
            s.stomachKg = 0.6;
            s.slotWet[3] = 0.75;
            s.kcalIn[7] = 650;
            s.kcalOut[8] = 450;
            s.macroG[30] = 25;
            s.binHour = 200;
            s.gutOnset = 4000;
            s.awakeMin = 570;
            BodyService.get().applyInjury(p, com.yearofice.api.body.InjurySpec.of(DamageRouter.FALL_OVER_3M), "test");
            h.assertTrue(s.condition() < 100 && !s.parts.problems().isEmpty(), "the injury landed");

            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            ServerPlayer fresh = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "reload-check"), ClientInformation.createDefault());
            h.assertTrue(BodyAttachments.of(fresh).kg == 77, "a fresh player starts at the default body");
            fresh.load(saved);
            BodyState back = BodyAttachments.of(fresh);
            h.assertTrue(back != s, "a new record was loaded");
            h.assertTrue(BodyCodec.sameAsSaved(s, back), "reloaded record differs");
            h.assertTrue(back.parts.problems().equals(s.parts.problems()), "problems survive");
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** Hunger is held full and inert: no starving, no regen, exhaustion wiped; vanilla foods stay edible. */
    @GameTest(template = T, batch = BATCH)
    public static void vanilla_hunger_full_and_inert(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            p.getFoodData().setFoodLevel(3);
            p.getFoodData().setSaturation(4);
            p.causeFoodExhaustion(40);
            p.setHealth(5);
            // Vanilla's own food tick, then the player tick's Post event, as Player.tick runs them.
            p.getFoodData().tick(p);
            NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
            h.assertTrue(p.getFoodData().getFoodLevel() == 20, "food " + p.getFoodData().getFoodLevel());
            h.assertTrue(p.getFoodData().getSaturationLevel() == 0, "saturation held at 0");
            h.assertTrue(p.getFoodData().getExhaustionLevel() == 0, "exhaustion held at 0");
            h.assertTrue(p.getHealth() == p.getMaxHealth(), "health held full");
            for (int i = 0; i < 200; i++) p.getFoodData().tick(p);
            h.assertTrue(p.getHealth() == p.getMaxHealth(), "no regeneration and no starving at food 20");
            h.assertTrue(!h.getLevel().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION),
                    "naturalRegeneration is off");
            h.assertTrue(Items.COOKED_BEEF.components().get(DataComponents.FOOD).canAlwaysEat(),
                    "vanilla foods are always edible");
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** The first join gives the Trail Messenger once. */
    @GameTest(template = T, batch = BATCH)
    public static void messenger_given_on_first_join(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            MessengerKit.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
            h.assertTrue(p.getInventory().contains(new ItemStack(MessengerItems.MESSENGER.get())),
                    "no Trail Messenger after the first join");
            MessengerKit.onLogin(new PlayerEvent.PlayerLoggedInEvent(p));
            h.assertFalse(MessengerKit.giveIfFirstJoin(p), "a second messenger was given");
            int count = p.getInventory().countItem(MessengerItems.MESSENGER.get());
            h.assertTrue(count == 1, "messengers carried: " + count);
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** Eating a vanilla food books its yearofice:food row: cooked rabbit is 300 g of lean meat, 345 kcal. */
    @GameTest(template = T, batch = BATCH)
    public static void eat_vanilla_food_from_data_map(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            ItemStack rabbit = new ItemStack(Items.COOKED_RABBIT);
            FoodMap.Row row = FoodMap.row(rabbit);
            h.assertTrue(row != null, "yearofice:food has no row for minecraft:cooked_rabbit");
            BodyState s = BodyAttachments.of(p);
            double before = s.balance24h();
            h.assertTrue(BodyService.get().eat(p, rabbit, row.weightG()), "eat refused");
            double kcal = s.balance24h() - before;
            h.assertTrue(Math.abs(kcal - 345) < 1, "kcal booked " + kcal);
            h.assertTrue(Math.abs(s.stomachKg - 0.3) < 1e-6, "stomach " + s.stomachKg);
            h.assertTrue(s.macroMeter(Metabolism.PROTEIN) > 0.3, "protein meter " + s.macroMeter(Metabolism.PROTEIN));
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** A sip from a water source adds 0.25 L (about 8 points). */
    @GameTest(template = T, batch = BATCH)
    public static void drink_from_water_source(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            BlockPos rel = new BlockPos(1, 1, 1);
            h.setBlock(rel, Blocks.WATER);
            BlockPos abs = h.absolutePos(rel);
            h.assertTrue(Drinking.isWaterSource(h.getLevel(), abs), "water source placed");
            BodyState s = BodyAttachments.of(p);
            s.water = 50;
            Drinking.drinkAt(p, abs);
            h.assertTrue(Math.abs(s.water - (50 + Metabolism.waterPoints(0.25))) < 1e-6, "water " + s.water);
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** Layer 7's closed-form check: 600 minutes stepped and in closed form agree within 0.5%. */
    @GameTest(template = T, batch = BATCH)
    public static void closed_form_matches(GameTestHelper h) {
        BodyModel.Inputs in = BodyModel.Inputs.resting(EnvSample.outdoors(2, 3)).doing(Activity.WALK);
        BodyState a = new BodyState(), b = new BodyState();
        for (int i = 0; i < 600; i++) BodyModel.step(a, in, BodyTuning.STANDARD, i, 1);
        BodyModel.step(b, in, BodyTuning.STANDARD, 0, 600);
        h.assertTrue(rel(a.kg, b.kg) < 0.005 && rel(a.water, b.water) < 0.005 && rel(a.energy, b.energy) < 0.005
                && rel(a.warmth, b.warmth) < 0.005, "closed form drifted: kg " + a.kg + "/" + b.kg + " water "
                + a.water + "/" + b.water + " energy " + a.energy + "/" + b.energy + " warmth " + a.warmth + "/"
                + b.warmth);
        h.succeed();
    }

    /** A vanilla fall is cancelled and becomes a body injury: 8 m costs 10 Condition, health stays full. */
    @GameTest(template = T, batch = BATCH)
    public static void fall_routes_to_injury(GameTestHelper h) {
        ServerPlayer p = player(h);
        try {
            DamageRouter.route(p, h.getLevel().damageSources().fall(), 5f);
            BodyState s = BodyAttachments.of(p);
            h.assertTrue(Math.abs(s.condition() - 90) < 0.01, "Condition after an 8 m fall: " + s.condition());
            h.assertTrue(p.getHealth() == p.getMaxHealth(), "vanilla health untouched");
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    /** The versioned codec: a round trip keeps every saved field, and a schema 1 record upgrades to schema 2. */
    @GameTest(template = T, batch = BATCH)
    public static void codec_round_trip_and_schema1_upgrade(GameTestHelper h) {
        BodyState s = new BodyState();
        s.kg = 71.4;
        s.water = 58;
        s.kcalIn[7] = 650;
        s.slotWet[2] = 0.5;
        s.gutOnset = 1234;
        s.parts = com.yearofice.sim.body.part.PartModel.split(s.parts, Part.LEFT_ARM, 8);
        JsonElement json = BodyCodec.CODEC.encodeStart(JsonOps.INSTANCE, s).getOrThrow();
        BodyState back = BodyCodec.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
        h.assertTrue(BodyCodec.sameAsSaved(s, back), "round trip differs: " + json);
        h.assertTrue(back.parts.hp(Part.LEFT_ARM) == 76, "left arm " + back.parts.hp(Part.LEFT_ARM));
        h.assertTrue(json.getAsJsonObject().get("schema").getAsInt() == 2, "writes schema 2");

        String v1 = "{\"schema\":1,\"kg\":71.4,\"water\":58,\"warmth\":74,\"energy\":6.2,\"condition\":88,"
                + "\"kcal_in\":[0,0,0,0,0,0,0,650,0,0,0,0,450,0,0,0,0,0,1000,0,0,0,0,0],"
                + "\"problems\":[{\"id\":\"cut\",\"part\":\"left_arm\",\"stage\":1,\"timer\":1412,\"dressed\":true}]}";
        BodyState old = BodyCodec.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(v1)).getOrThrow();
        h.assertTrue(Math.abs(old.condition() - 88) < 1e-6 && Math.abs(old.parts.dSys() - 12) < 1e-6,
                "schema 1 Condition moves to d_sys: " + old.condition() + " / " + old.parts.dSys());
        for (Part part : Part.values()) h.assertTrue(old.parts.hp(part) == 100, "part " + part + " at 100");
        h.assertTrue(old.parts.problems().size() == 1 && old.kcalIn[7] == 650, "problems and bins kept");
        BodyState empty = BodyCodec.CODEC.parse(JsonOps.INSTANCE, new JsonObject()).getOrThrow();
        h.assertTrue(empty.kg == 77 && empty.warmth == 100 && empty.condition() == 100, "defaults for a new body");
        h.succeed();
    }

    /** The stand-in clock and environment: minute conversions both ways, biome air and the daily swing. */
    @GameTest(template = T, batch = BATCH)
    public static void clock_and_env_conversions(GameTestHelper h) {
        h.assertTrue(VanillaDayClock.toMinute(0) == 6 * 60, "day time 0 is 06:00");
        h.assertTrue(VanillaDayClock.toMinute(24_000) == 1440 + 6 * 60, "one vanilla day is 1,440 minutes");
        for (long m = 0; m < 3 * 1440; m += 7) {
            h.assertTrue(VanillaDayClock.toMinute(VanillaDayClock.toDayTime(m)) == m, "minute " + m + " round trip");
        }
        h.assertTrue(BodyClock.day(2 * 1440 + 5) == 3 && BodyClock.hhmm(14 * 60 + 22).equals("14:22"), "calendar");
        h.assertTrue(Math.abs(VanillaEnvSampler.biomeAirC(0.8f, 64) - 14.4) < 1e-4, "plains 14.4 °C");
        h.assertTrue(Math.abs(VanillaEnvSampler.biomeAirC(0f, 164) + 8.65) < 1e-6, "lapse rate");
        h.assertTrue(Math.abs(VanillaEnvSampler.diurnalC(15 * 60) - 5) < 1e-9
                && Math.abs(VanillaEnvSampler.diurnalC(3 * 60) + 5) < 1e-9, "daily swing");
        h.succeed();
    }

    static double rel(double x, double y) { return Math.abs(x - y) / Math.max(1e-9, Math.abs(y)); }
}

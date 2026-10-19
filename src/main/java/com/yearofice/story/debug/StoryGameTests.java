package com.yearofice.story.debug;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.decay.DecayService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.fire.FireWeather;
import com.yearofice.sim.story.FirstNight;
import com.yearofice.sim.story.FirstNightStep;
import com.yearofice.story.DayOne;
import com.yearofice.story.Onboarding;
import com.yearofice.api.env.EnvService;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.DayState;
import com.yearofice.sim.weather.WeatherModel;
import com.yearofice.story.OnboardingRecord;
import com.yearofice.story.StoryAttachments;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, story: the guided first night on a real server (Ready for development › M2.5 test 6).
 *
 * <p>Parallel-safe by construction (RECONCILE 169's lesson): the bot is an unattached server player that only this
 * test holds, so the live ticker, other tests' listeners and the player list never see it; its ring is the one its
 * own place event recorded, so another test's fire can never light its step; the story runs on a scripted minute
 * passed to {@link Onboarding#check}, and the body is stepped from the clock's own minute, so the test never sets or
 * rates the shared clock and loads no chunks beyond its own structure. Fire and body run through their real paths:
 * the ring takes parts from the player's hand ({@code useBlock}), the match goes through
 * {@link FireRingBlockEntity#strikeMatch} with a fixed roll, {@code api.fire.FireStateEvent} reaches the story, and
 * food is stamped, cooked and eaten through {@link DecayService} and {@link BodyService}.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class StoryGameTests {
    private StoryGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos RING = new BlockPos(1, 1, 1);
    /** Where the bot stands and later sleeps: 2 m from the ring, inside the 3 m near-fire radius. */
    static final BlockPos BED = new BlockPos(1, 1, 3);

    @GameTestGenerator
    public static Collection<TestFunction> storyTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:story/slice_first_night", StoryGameTests::sliceFirstNight));
        out.add(test("yearofice:story/returning_player_skipped", StoryGameTests::returningPlayerSkipped));
        out.add(test("yearofice:story/dusk_warns_once", StoryGameTests::duskWarnsOnce));
        out.add(test("yearofice:story/onboarding_saves", StoryGameTests::onboardingSaves));
        out.add(test("yearofice:story/fresh_world_wakes_at_noon", StoryGameTests::freshWorldWakesAtNoon));
        return out;
    }

    static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, 100, 0, true, body);
    }

    static ServerPlayer player(GameTestHelper h, String name) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(BED));
        p.moveTo(at.x, at.y, at.z, 180, 0);
        return p;
    }

    static boolean journalHas(ServerPlayer p, String key) {
        return HudBus.get().journal(p).stream().anyMatch(l -> l.key().equals(key) && l.type().channel == HudLineType.Channel.MESSENGER);
    }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, id)); }

    /** May 1's sunset from the clock's 64°N table, as an absolute minute on day 1. */
    static long sunsetDay1() { return ClockService.get().sun().sunsetMinute(0); }

    /**
     * The bot's two clocks: the story's scripted minute, starting at 12:00 on May 1 as the Blueprint's day 1 does,
     * and the body's, which starts at the shared clock's own minute so the body's hourly bins stay in order. Both
     * move on together; the shared clock itself is never touched.
     */
    static final class Bot {
        final GameTestHelper h;
        final ServerPlayer p;
        long story = 12 * 60;
        long body;
        FireRingBlockEntity ring;
        final StringBuilder log = new StringBuilder();

        Bot(GameTestHelper h, ServerPlayer p) {
            this.h = h;
            this.p = p;
            this.body = ClockService.get().nowMinute();
        }

        /** Day 1's weather (Blueprint › Start site, Day 1 rules): 7 °C at 15:00, −3 °C before dawn, wind 2 m/s, dry. */
        static double airC(long minute) {
            double phase = (Math.floorMod(minute, 1440) - 15 * 60) / 1440.0 * 2 * Math.PI;
            return 2 + 5 * Math.cos(phase);
        }

        /** Spends {@code minutes} doing {@code a}, in 10-minute steps: body, fire (fed as a bot would), then a check. */
        void pass(int minutes, Activity a) {
            for (int done = 0; done < minutes; done += 10) {
                int m = Math.min(10, minutes - done);
                BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(airC(story), 2, Precip.NONE, 0, Double.NaN));
                if (ring != null) {
                    ring.stepMinutes(m);
                    feed();
                    log.append(story).append(':').append(ring.fire().state()).append('/')
                            .append(String.format(java.util.Locale.ROOT, "%.2f", ring.fire().armloads())).append(' ');
                }
                boolean asleep = a == Activity.SLEEP;
                BodyTicker.step(p, body, m, a, asleep, a == Activity.WALK, BodyTuning.STANDARD);
                story += m;
                body += m;
                Onboarding.check(p, story);
            }
        }

        /** Keeps the fire going: an armload (two spruce logs) on whenever less than one is queued. */
        void feed() {
            if (ring.fire().state() != FireState.FIRE || ring.fire().armloads() >= 1.0) return;
            hold(new ItemStack(Items.SPRUCE_LOG, 2));
            h.useBlock(RING, p);
            h.useBlock(RING, p);
        }

        void hold(ItemStack s) { p.setItemInHand(InteractionHand.MAIN_HAND, s); }

        FirstNightStep step() { return StoryAttachments.of(p).night().step(); }
    }

    /**
     * Ready for development › M2.5 test 6, {@code story/slice_first_night}: a bot following the prompts has a fire
     * before dark, sleeps with warmth above 40 and the night reaches done. Gather → set ring → light → food → cook
     * and eat → sleep by the fire, each prompt closing on its real event.
     */
    static void sliceFirstNight(GameTestHelper h) {
        ServerPlayer p = player(h, "first-night-bot");
        Bot b = new Bot(h, p);
        try {
            h.setBlock(RING.below(), Blocks.STONE);
            Onboarding.onJoin(p, b.story);
            FirstNight n = StoryAttachments.of(p).night();
            h.assertTrue(n.active() && b.step() == FirstNightStep.TINDER, "the first join starts the night: " + n.status());
            // No messenger on the bot: the bus keeps the device's lines in the journal (Layer 8).
            h.assertTrue(journalHas(p, "messenger.yearofice.alert.first_night_wake"), "the wake line went through the HUD bus");

            // 1. Gather: tinder, kindling, wood and the crate's matches.
            b.pass(40, Activity.WALK);
            h.assertTrue(b.step() == FirstNightStep.TINDER, "nothing gathered yet, still step 1");
            p.getInventory().setItem(9, new ItemStack(item("tinder_bundle")));
            p.getInventory().setItem(10, new ItemStack(item("kindling"), 2));
            p.getInventory().setItem(11, new ItemStack(Items.SPRUCE_LOG, 16));
            Onboarding.check(p, b.story);
            h.assertTrue(b.step() == FirstNightStep.RING, "tinder and kindling close step 1, at " + b.step());

            // 2. Set the ring on the stone, from the hand: the place event makes it the bot's ring.
            b.hold(new ItemStack(item("fire_ring")));
            BlockPos stone = h.absolutePos(RING.below());
            h.useBlock(RING.below(), p, new BlockHitResult(Vec3.atCenterOf(stone).add(0, 0.5, 0), Direction.UP, stone, false));
            h.assertTrue(h.getBlockEntity(RING) instanceof FireRingBlockEntity, "the ring was set");
            OnboardingRecord r = StoryAttachments.of(p);
            h.assertTrue(h.absolutePos(RING).equals(r.ring()), "the ring is the bot's: " + r.ring());
            Onboarding.check(p, b.story);
            h.assertTrue(b.step() == FirstNightStep.LIGHT, "the ring closes step 2, at " + b.step());

            // 3. Lay tinder, kindling and two armloads through the hand, then a match and two puffs.
            b.ring = h.getBlockEntity(RING);
            b.ring.setWeatherOverride(FireWeather.CALM);
            for (ItemStack s : List.of(new ItemStack(item("tinder_bundle")), new ItemStack(item("kindling"), 2), new ItemStack(Items.SPRUCE_LOG, 4))) {
                p.getInventory().clearOrCountMatchingItems(st -> st.is(s.getItem()), s.getCount(), p.inventoryMenu.getCraftSlots());
                b.hold(s);
                int n0 = s.getCount();
                for (int i = 0; i < n0; i++) h.useBlock(RING, p);
            }
            h.assertTrue(b.ring.fire().layInOrder(), "laid in order");
            b.hold(new ItemStack(item("match"), 3));
            h.assertTrue(b.ring.strikeMatch(p, p.getMainHandItem(), 0.0).ok(), "the match took");
            b.ring.puff();
            b.ring.puff();
            h.assertTrue(b.ring.fire().state().flaming(), "the tinder caught: " + b.ring.fire().state());
            Onboarding.check(p, b.story);
            h.assertTrue(b.step() == FirstNightStep.FOOD, "the fire event closes step 3, at " + b.step());
            long fireAt = n.fireLitMinute();
            h.assertTrue(fireAt >= 0 && fireAt < sunsetDay1(), "a fire before dark: at " + fireAt + ", sunset " + sunsetDay1());

            // 4. Food: the bot walks its snare line and comes back with a dressed hare's meat.
            b.pass(90, Activity.WALK);
            ItemStack meat = new ItemStack(item("snowshoe_hare_meat"), 3);
            DecayService.get().stamp(h.getLevel(), meat, FoodState.RAW);
            p.getInventory().setItem(20, meat); // in the pack, off the hotbar the bot's hand works from
            Onboarding.check(p, b.story);
            h.assertTrue(b.step() == FirstNightStep.EAT, "food in the pack closes step 4, at " + b.step());

            // 5. Cook it by the fire, then eat it: three 100 g units.
            b.pass(30, Activity.REST);
            int slot = -1;
            for (int i = 0; i < p.getInventory().getContainerSize() && slot < 0; i++) {
                if (p.getInventory().getItem(i).is(item("snowshoe_hare_meat"))) slot = i;
            }
            h.assertTrue(slot >= 0, "the meat is in the pack");
            ItemStack cooked = p.getInventory().getItem(slot);
            DecayService.get().changeState(h.getLevel(), cooked, FoodState.COOKED);
            for (int i = 0; i < 3; i++) {
                h.assertTrue(BodyService.get().eat(p, cooked, 100), "the body booked bite " + i);
                cooked.shrink(1);
            }
            Onboarding.check(p, b.story);
            h.assertTrue(b.step() == FirstNightStep.SLEEP, "the meal closes step 5, at " + b.step());

            // The evening by the fire, fed, until well after dusk.
            long bedtime = ClockService.get().duskMinute(0) + 20;
            while (b.story < bedtime) b.pass(30, Activity.REST);
            h.assertTrue(b.ring.fire().state() == FireState.FIRE, "the fire is still burning at bedtime: " + b.ring.fire().state() + " " + b.log);

            // 6. Sleep by the fire.
            double warmth = BodyService.get().warmth(p);
            h.assertTrue(warmth > 40, "warmth at sleep " + warmth);
            p.startSleeping(h.absolutePos(BED));
            Onboarding.check(p, b.story);
            h.assertTrue(n.status() == FirstNight.Status.DONE && n.ending() == FirstNight.Ending.SLEPT,
                    "sleeping by the fire ends the night: " + n.status() + " " + n.ending());
            for (int i = 0; i < 4; i++) {
                b.pass(30, Activity.SLEEP);
                double w = BodyService.get().warmth(p);
                h.assertTrue(w > 40, "warmth asleep after " + (i + 1) * 30 + " min: " + w);
            }
            h.assertTrue(!r.warn().fired(Onboarding.DUSK_NO_FIRE), "a bot with a fire is never warned at dusk");
            YearOfIce.LOG.info("[story] slice_first_night: fire at minute {} (sunset {}), warmth {} at sleep, {} after 2 h asleep",
                    fireAt, sunsetDay1(), Math.round(warmth), Math.round(BodyService.get().warmth(p)));
            h.succeed();
        } finally {
            if (p.isSleeping()) p.stopSleeping();
            Onboarding.forget(p);
            BodyTicker.VANILLA_ENV.override(p, null);
        }
    }

    /** A player who has played here before, with no record, never gets the night; a new one does. */
    static void returningPlayerSkipped(GameTestHelper h) {
        ServerPlayer back = player(h, "returning");
        ServerPlayer fresh = player(h, "new-player");
        try {
            back.getStats().setValue(back, Stats.CUSTOM.get(Stats.PLAY_TIME), 72_000);
            Onboarding.onJoin(back, 12 * 60);
            FirstNight n = StoryAttachments.of(back).night();
            h.assertTrue(n.status() == FirstNight.Status.SKIPPED && n.ending() == FirstNight.Ending.RETURNING,
                    "a returning player is skipped: " + n.status() + " " + n.ending());
            Onboarding.onJoin(fresh, 12 * 60);
            h.assertTrue(StoryAttachments.of(fresh).night().active(), "a new player gets the night");
            Onboarding.onJoin(fresh, 13 * 60);
            h.assertTrue(StoryAttachments.of(fresh).night().startMinute() == 12 * 60, "a second join does not restart it");
            Onboarding.skip(fresh, 13 * 60);
            h.assertTrue(StoryAttachments.of(fresh).night().status() == FirstNight.Status.SKIPPED, "skip turns it off");
            Onboarding.reset(fresh, 14 * 60);
            FirstNight again = StoryAttachments.of(fresh).night();
            h.assertTrue(again.active() && again.step() == FirstNightStep.TINDER && again.startMinute() == 14 * 60, "reset restarts at step 1");
            h.succeed();
        } finally {
            Onboarding.forget(back);
            Onboarding.forget(fresh);
        }
    }

    /** Dusk with no fire: one warning, not two; nothing in the afternoon. */
    static void duskWarnsOnce(GameTestHelper h) {
        ServerPlayer p = player(h, "no-fire-bot");
        // 40 blocks over the structure: no other test's fire can be within the 8 m that quiets the warning.
        p.moveTo(p.getX(), p.getY() + 40, p.getZ());
        try {
            Onboarding.onJoin(p, 12 * 60);
            OnboardingRecord r = StoryAttachments.of(p);
            Onboarding.check(p, 15 * 60);
            h.assertFalse(r.warn().fired(Onboarding.DUSK_NO_FIRE), "no warning in the afternoon");
            long dusk = sunsetDay1() - 30;
            Onboarding.check(p, dusk);
            h.assertTrue(r.warn().fired(Onboarding.DUSK_NO_FIRE), "warned 30 min before sunset with no fire");
            h.assertTrue(journalHas(p, "messenger.yearofice.warn.dusk_no_fire"), "the warning went through the HUD bus as a warn line");
            long warns = HudBus.get().journal(p).stream().filter(l -> l.key().equals("messenger.yearofice.warn.dusk_no_fire")).count();
            h.assertTrue(r.warn().entries().get(Onboarding.DUSK_NO_FIRE).falseSince() < 0, "the condition is true");
            Onboarding.check(p, dusk + 60);
            h.assertTrue(r.warn().entries().size() == 1 && r.warn().fired(Onboarding.DUSK_NO_FIRE), "still one warning, still fired");
            long after = HudBus.get().journal(p).stream().filter(l -> l.key().equals("messenger.yearofice.warn.dusk_no_fire")).count();
            h.assertTrue(warns == 1 && after == 1, "one warn line, not two: " + warns + " then " + after);
            h.succeed();
        } finally {
            Onboarding.forget(p);
        }
    }

    /**
     * Blueprint › Start site › Day 1 rules on the GameTest server's own fresh world: at start the clock read
     * 12:00 on May 1, and May 1's weather is the fixed day. The reading is the one {@link DayOne} took right after
     * setting it, because parallel tests move the shared clock afterwards.
     */
    static void freshWorldWakesAtNoon(GameTestHelper h) {
        DayOne.Start s = DayOne.last();
        DayOne.Data d = DayOne.data(h.getLevel().getServer());
        h.assertTrue(s != null && d.decided(), "the day-1 decision was taken at start: " + s);
        if (!s.fresh()) {
            // A GameTest world kept from an earlier local run: the decision stands and the clock was left alone.
            // CI always starts fresh, so the full check runs there.
            YearOfIce.LOG.info("[story] fresh_world_wakes_at_noon: reused world, day 1 {}", d.dayOne() ? "kept" : "not applied");
            h.succeed();
            return;
        }
        h.assertTrue(s.dayOne() && d.dayOne(), "a fresh world with the guided night gets day 1: " + s);
        h.assertTrue(s.clockDsAfter() == DayOne.WAKE_DS, "clock after start " + Calendar.format(s.clockDsAfter()));
        h.assertTrue(Calendar.format(s.clockDsAfter()).equals("May 1, year 1, 12:00"), Calendar.format(s.clockDsAfter()));
        WeatherModel.Day day = EnvService.get().day(h.getLevel(), 0);
        h.assertTrue(day.state() == DayState.CLOUDY && Math.abs(day.meanC() - 2) < 1e-9 && day.precipMm() == 0
                && Math.abs(day.windMs() - 2) < 1e-9, "May 1 is cloudy, mean 2 °C, dry, 2 m/s: " + day);
        h.succeed();
    }

    /** The attachment saves with the player and loads back field for field. */
    static void onboardingSaves(GameTestHelper h) {
        ServerPlayer p = player(h, "save-bot");
        p.moveTo(p.getX(), p.getY() + 40, p.getZ());
        try {
            Onboarding.onJoin(p, 12 * 60);
            OnboardingRecord r = StoryAttachments.of(p);
            Onboarding.check(p, ClockService.get().sun().sunsetMinute(0));
            CompoundTag saved = p.saveWithoutId(new CompoundTag());
            ServerPlayer fresh = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                    new GameProfile(UUID.randomUUID(), "reload"), ClientInformation.createDefault());
            fresh.load(saved);
            OnboardingRecord back = StoryAttachments.of(fresh);
            h.assertTrue(back != r, "a new record was loaded");
            FirstNight a = r.night(), b = back.night();
            h.assertTrue(a.status() == b.status() && a.step() == b.step() && a.startMinute() == b.startMinute()
                    && a.fireLitMinute() == b.fireLitMinute() && a.ending() == b.ending(), "night differs after reload");
            h.assertTrue(back.warn().entries().equals(r.warn().entries()), "warn-once memory differs: " + back.warn().entries());
            h.assertTrue(java.util.Objects.equals(back.startSite(), r.startSite()), "start site differs");
            h.succeed();
        } finally {
            Onboarding.forget(p);
        }
    }
}

package com.yearofice.build.gametest;

import com.mojang.authlib.GameProfile;
import com.yearofice.api.body.BodyService;
import com.yearofice.api.body.SleepService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.body.sleep.Sleep;
import com.yearofice.build.BuildConfig;
import com.yearofice.build.BuildRegistry;
import com.yearofice.build.camp.BedBuilding;
import com.yearofice.build.camp.BoughBedBlock;
import com.yearofice.build.camp.BoughCutting;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FireRingBlockEntity;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.WaterPurity;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.fire.FireState;
import com.yearofice.sim.fire.FireWeather;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, building: the bough bed (RECONCILE 181, pulled forward from M3) on a real server.
 *
 * <p>Parallel-safe: the bot is a detached server player only this test holds (never in the player list), so the live
 * tickers, the clock's own Sleep and Work requests and other tests never see it, and it never moves the shared clock;
 * its day and night run on a scripted minute, with the body stepped from the clock's own minute as
 * {@code story/slice_first_night} does. The camp sits on a platform 40 blocks over the structure, so no other test's
 * moose or fire is within the wake or heat distances; it loads no chunks of its own. Bough cutting goes through the
 * real use-key path ({@code ServerPlayerGameMode.useItemOn} and its {@code RightClickBlock} event) in real ticks.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class BuildGameTests {
    private BuildGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int UP = 40;
    /** The camp, relative to the structure: the leaf, the ring and the bed's foot and head (head 2 m from the ring). */
    static final BlockPos LEAF = new BlockPos(1, UP + 1, 0), RING = new BlockPos(0, UP + 1, 2),
            FOOT = new BlockPos(2, UP + 1, 1), HEAD = new BlockPos(2, UP + 1, 2), STAND = new BlockPos(1, UP + 1, 1);

    @GameTestGenerator
    public static Collection<TestFunction> buildTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(new TestFunction("m2_5", "yearofice:building/bough_bed_sleep", TEMPLATE, 400, 0, true, BuildGameTests::boughBedSleep));
        out.add(new TestFunction("m8_gates", "yearofice:build/bushcraft_gate_opens_on_first_fire", TEMPLATE, 100, 0, true, BuildGameTests::bushcraftGate));
        out.add(new TestFunction("m8_gates", "yearofice:build/iron_gate_opens_at_forge", TEMPLATE, 100, 0, true, BuildGameTests::ironGate));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    /**
     * Crafting and building in depth › Tiers: the first fire the player lights, by match, opens Bushcraft. The bot lays a
     * ring (tinder, kindling, logs) through the use key and strikes a match; it stands outside the fire's reach and is not
     * in the player list, so only the actor path of the fire event can open the gate. No /yoi command.
     */
    static void bushcraftGate(GameTestHelper h) {
        ServerPlayer p = bot(h, "bushcraft-gate-bot");
        com.yearofice.api.build.GateService g = com.yearofice.api.build.GateService.get();
        h.assertTrue(!g.isOpen(p, com.yearofice.sim.build.Tier.BUSHCRAFT), "Bushcraft starts shut");
        h.setBlock(RING, FireRegistry.FIRE_RING.get());
        FireRingBlockEntity ring = h.getBlockEntity(RING);
        ring.setWeatherOverride(FireWeather.CALM);
        // a match on a bare ring lights nothing and opens nothing
        p.getInventory().selected = 8;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("match"), 3));
        h.useBlock(RING, p);
        h.assertTrue(!g.isOpen(p, com.yearofice.sim.build.Tier.BUSHCRAFT), "a bare ring does not open it");
        for (ItemStack s : List.of(new ItemStack(item("tinder_bundle")), new ItemStack(item("kindling"), 2), new ItemStack(Items.SPRUCE_LOG, 2))) {
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
            for (int i = 0; i < s.getCount(); i++) h.useBlock(RING, p);
        }
        h.assertTrue(ring.strikeMatch(p, new ItemStack(item("match"), 3), 0.0).ok(), "the match took");
        ring.puff();
        ring.puff();
        h.assertTrue(ring.fire().state().flaming(), "the fire caught: " + ring.fire().state());
        h.assertTrue(g.isOpen(p, com.yearofice.sim.build.Tier.BUSHCRAFT), "the first fire the player lit opens Bushcraft");
        h.assertTrue(!g.isOpen(p, com.yearofice.sim.build.Tier.IRON), "and only Bushcraft");
        h.setBlock(RING, Blocks.AIR);
        h.succeed();
    }

    /** Iron opens at the first iron in hand (a bar counts) or the first forge or bloomery the player builds or works. */
    static void ironGate(GameTestHelper h) {
        com.yearofice.api.build.GateService g = com.yearofice.api.build.GateService.get();
        com.yearofice.sim.build.Tier iron = com.yearofice.sim.build.Tier.IRON;
        ServerPlayer smith = bot(h, "iron-gate-bot-forge");
        h.assertTrue(!g.isOpen(smith, iron), "Iron starts shut");
        h.setBlock(STAND, com.yearofice.build.metal.MetalRegistry.FORGE.get());
        smith.getInventory().selected = 8;
        smith.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        BlockPos forge = h.absolutePos(STAND);
        smith.gameMode.useItemOn(smith, h.getLevel(), smith.getMainHandItem(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(forge), Direction.UP, forge, false));
        h.assertTrue(g.isOpen(smith, iron), "working a forge opens Iron");
        h.setBlock(STAND, Blocks.AIR);
        ServerPlayer bearer = bot(h, "iron-gate-bot-bar");
        bearer.getInventory().add(new ItemStack(com.yearofice.build.metal.MetalRegistry.IRON_BAR.get()));
        com.yearofice.build.craft.CraftSystem.scan(bearer);
        h.assertTrue(g.isOpen(bearer, iron), "a bar of iron in the pack opens Iron");
        h.succeed();
    }

    static ServerPlayer bot(GameTestHelper h, String name) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(Vec3.atBottomCenterOf(STAND));
        p.moveTo(at.x, at.y, at.z, 0, 0); // yaw 0 faces south: the bed's head goes +z
        return p;
    }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, id)); }

    /** Day 1's weather (Blueprint › Start site, Day 1 rules): 7 °C at 15:00, −3 °C before dawn, wind 2 m/s, dry. */
    static double airC(long minute) {
        double phase = (Math.floorMod(minute, 1440) - 15 * 60) / 1440.0 * 2 * Math.PI;
        return 2 + 5 * Math.cos(phase);
    }

    /** The bot's two clocks, as in story/slice_first_night: a scripted minute from 12:00 May 1, and the body's. */
    static final class Night {
        final GameTestHelper h;
        final ServerPlayer p;
        FireRingBlockEntity ring;
        long story = 12 * 60;
        long body = ClockService.get().nowMinute();
        final StringBuilder log = new StringBuilder();

        Night(GameTestHelper h, ServerPlayer p) {
            this.h = h;
            this.p = p;
        }

        /** {@code minutes} of {@code a} in 10-minute steps: the fire (fed), the body, then sleep's wake check. */
        String pass(int minutes, Activity a) {
            for (int done = 0; done < minutes; done += 10) {
                int m = Math.min(10, minutes - done);
                BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(airC(story), 2, Precip.NONE, 0, Double.NaN));
                ring.stepMinutes(m);
                feed();
                BodyTicker.step(p, body, m, a, a == Activity.SLEEP, a == Activity.WALK, BodyTuning.STANDARD);
                story += m;
                body += m;
                if (p.isSleeping()) {
                    String woke = SleepService.get().check(p, story);
                    if (woke != null) return woke;
                }
            }
            return null;
        }

        /** An armload (two spruce logs) on whenever less than one is queued, from the bot's last belt slot. */
        void feed() {
            if (ring.fire().state() != FireState.FIRE || ring.fire().armloads() >= 1.0) return;
            hold(new ItemStack(Items.SPRUCE_LOG, 2));
            h.useBlock(RING, p);
            h.useBlock(RING, p);
        }

        void hold(ItemStack s) {
            p.getInventory().selected = 8;
            p.setItemInHand(InteractionHand.MAIN_HAND, s);
        }

        String hhmm() { return String.format(Locale.ROOT, "day %d %02d:%02d", story / 1440 + 1, story % 1440 / 60, story % 60); }
    }

    /**
     * {@code building/bough_bed_sleep}: a bot cuts boughs from a spruce, builds the bough bed 2 m from a lit ring,
     * sleeps on it through the May night and wakes in the morning, warmth above 40 all night and energy recovered.
     * Vanilla's night skip stays off ({@code playersSleepingPercentage} 101).
     */
    static void boughBedSleep(GameTestHelper h) {
        ServerPlayer p = bot(h, "bough-bed-bot");
        Night n = new Night(h, p);
        // The camp: a stone pad, a white spruce leaf block and a fire ring, 40 blocks up.
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, UP, z), Blocks.STONE);
        Block leaf = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "white_spruce_leaves"));
        h.setBlock(LEAF, leaf);
        h.setBlock(RING, FireRegistry.FIRE_RING.get());
        BlockPos leafAbs = h.absolutePos(LEAF);
        BlockHitResult onLeaf = new BlockHitResult(Vec3.atCenterOf(leafAbs), Direction.SOUTH, leafAbs, false);
        int need = BuildConfig.bedBoughs();
        int[] ticks = {0};
        boolean[] done = {false};
        h.onEachTick(() -> {
            if (done[0]) return;
            ticks[0]++;
            // 1. Cut boughs: the use key held on the leaf, repeating every 4 ticks with an empty hand.
            if (BoughCutting.carried(p) < need) {
                if (ticks[0] % 4 == 0) {
                    InteractionResult r = p.gameMode.useItemOn(p, h.getLevel(), p.getMainHandItem(), InteractionHand.MAIN_HAND, onLeaf);
                    if (!r.consumesAction()) h.fail("the use key on a spruce leaf was not taken: " + r);
                }
                return;
            }
            done[0] = true;
            try {
                night(h, p, n, ticks[0]);
            } finally {
                if (p.isSleeping()) p.stopSleeping();
                Sleep.forget(p);
                BoughCutting.forget(p);
                BedBuilding.cancel(p);
                BodyTicker.VANILLA_ENV.override(p, null);
                clearCamp(h);
            }
        });
    }

    /** The camp sits outside the structure's bounds, so the test clears it itself. */
    static void clearCamp(GameTestHelper h) {
        for (BlockPos q : List.of(HEAD, FOOT, RING, LEAF)) h.setBlock(q, Blocks.AIR);
        for (int x = 0; x < 3; x++) for (int z = 0; z < 3; z++) h.setBlock(new BlockPos(x, UP, z), Blocks.AIR);
    }

    private static void night(GameTestHelper h, ServerPlayer p, Night n, int cutTicks) {
        int need = BuildConfig.bedBoughs();
        h.assertTrue(h.getBlockState(LEAF).is(BoughCutting.SOURCES), "the spruce is unharmed: the leaf block stays");
        int got = BoughCutting.carried(p);
        h.assertTrue(got == need, "cut " + got + " boughs, want " + need);
        h.assertTrue(cutTicks <= 4 * 15 + 8, need + " boughs took " + cutTicks + " ticks, want 4 one-minute cuts");

        // 2. Light the ring: tinder, kindling, two armloads, a match and two puffs.
        n.ring = h.getBlockEntity(RING);
        n.ring.setWeatherOverride(FireWeather.CALM);
        for (ItemStack s : List.of(new ItemStack(item("tinder_bundle")), new ItemStack(item("kindling"), 2), new ItemStack(Items.SPRUCE_LOG, 4))) {
            n.hold(s);
            int k = s.getCount();
            for (int i = 0; i < k; i++) h.useBlock(RING, p);
        }
        n.hold(new ItemStack(item("match"), 3));
        h.assertTrue(n.ring.strikeMatch(p, p.getMainHandItem(), 0.0).ok(), "the match took");
        n.ring.puff();
        n.ring.puff();
        h.assertTrue(n.ring.fire().state().flaming(), "the fire caught: " + n.ring.fire().state());
        n.hold(ItemStack.EMPTY);

        // 3. Build the bed: hold use on the stone pad with the boughs, facing south, one game hour of work.
        int slot = -1;
        for (int i = 0; i < 9 && slot < 0; i++) if (p.getInventory().getItem(i).is(BuildRegistry.SPRUCE_BOUGHS.get())) slot = i;
        h.assertTrue(slot >= 0, "the boughs are on the belt");
        p.getInventory().selected = slot;
        ItemStack boughs = p.getMainHandItem();
        BlockPos pad = h.absolutePos(FOOT.below());
        UseOnContext ctx = new UseOnContext(p, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pad).add(0, 0.5, 0), Direction.UP, pad, false));
        h.assertTrue(boughs.useOn(ctx) == InteractionResult.CONSUME && p.isUsingItem(), "holding use on the ground starts the bed");
        BedBuilding.Job job = BedBuilding.job(p);
        h.assertTrue(job != null && job.foot.equals(h.absolutePos(FOOT)) && job.facing == Direction.SOUTH, "the job is at the foot, head south");
        int duration = boughs.getUseDuration(p);
        h.assertTrue(duration == 240, "a game hour at the Work rate is 240 ticks, got " + duration);
        for (int left = duration; left > 0; left--) boughs.onUseTick(h.getLevel(), p, left);
        boughs.finishUsingItem(h.getLevel(), p);
        p.stopUsingItem();
        BlockState foot = h.getBlockState(FOOT), head = h.getBlockState(HEAD);
        h.assertTrue(foot.is(BuildRegistry.BOUGH_BED.get()) && foot.getValue(BoughBedBlock.PART) == BedPart.FOOT
                && head.is(BuildRegistry.BOUGH_BED.get()) && head.getValue(BoughBedBlock.PART) == BedPart.HEAD, "the bed was laid: " + foot + " / " + head);
        h.assertTrue(BoughCutting.carried(p) == 0, "the bed took the boughs: " + BoughCutting.carried(p) + " left");
        double toRing = Vec3.atCenterOf(h.absolutePos(HEAD)).distanceTo(Vec3.atCenterOf(h.absolutePos(RING)));
        h.assertTrue(Math.abs(toRing - 2) < 1e-9, "the bed's head is 2 m from the ring: " + toRing);
        n.story += Math.round(BuildConfig.bedMin());

        // 4. The afternoon and evening by the fire: walking, fed, a drink, until after dusk.
        n.pass(30, Activity.HEAVY);
        BodyService.get().drink(p, 1.0, WaterPurity.Source.RAIN, true);
        long bedtime = ClockService.get().duskMinute(0) + 30;
        while (n.story < bedtime) n.pass(30, Activity.WALK);
        h.assertTrue(n.ring.fire().state() == FireState.FIRE, "the fire burns at bedtime: " + n.ring.fire().state());

        // 5. Lie down by right-clicking the bed's foot: the vanilla sleeping pose on our bed, any hour, no skip.
        double energyBefore = BodyService.get().energy(p);
        double warmBefore = BodyService.get().warmth(p);
        h.assertTrue(energyBefore < 7, "a day's work leaves the bot tired: " + energyBefore);
        n.hold(ItemStack.EMPTY);
        h.useBlock(FOOT, p);
        h.assertTrue(p.isSleeping() && p.getSleepingPos().orElseThrow().equals(h.absolutePos(HEAD)), "asleep on the bed's head");
        h.assertTrue(h.getBlockState(HEAD).getValue(BoughBedBlock.OCCUPIED), "the bed is occupied");
        SleepService.Spot spot = SleepService.get().spot(p);
        h.assertTrue(Math.abs(spot.quality() - 0.95) < 1e-9 && Math.abs(spot.warmthC() - 5) < 1e-9, "a bough bed: q 0.95, +5 °C: " + spot);
        h.assertTrue(h.getLevel().getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) > 100, "vanilla's night skip is off");
        double minWarm = Double.MAX_VALUE;
        String woke = null;
        long asleepAt = n.story;
        while (woke == null && n.story < asleepAt + 12 * 60) {
            woke = n.pass(10, Activity.SLEEP);
            minWarm = Math.min(minWarm, BodyService.get().warmth(p));
        }

        // 6. Woken by the morning, not the cold, with the night's sleep in the bank.
        double energy = BodyService.get().energy(p);
        double warmth = BodyService.get().warmth(p);
        YearOfIce.LOG.info("[building] bough_bed_sleep: asleep {} at warmth {} energy {}; woke {} ({}) at warmth {} energy {}, coldest {}; fire {}",
                asleepAt, Math.round(warmBefore), String.format(Locale.ROOT, "%.1f", energyBefore), n.hhmm(), woke, Math.round(warmth),
                String.format(Locale.ROOT, "%.1f", energy), Math.round(minWarm), n.ring.fire().state());
        h.assertTrue("morning".equals(woke), "woke for " + woke + " at " + n.hhmm() + " (warmth " + warmth + ")");
        h.assertTrue(!p.isSleeping(), "awake after the wake");
        h.assertTrue(n.story % 1440 >= 6 * 60 && n.story % 1440 < 6 * 60 + 10 && n.story / 1440 == 1, "woke at 06:00 on day 2: " + n.hhmm());
        h.assertTrue(minWarm > 40 && warmth > 40, "warmth above 40 all night: coldest " + minWarm + ", at waking " + warmth);
        double cap = BodyService.get().readout(p).energyCap();
        h.assertTrue(energy >= cap - 0.05 && energy > energyBefore + 2, "energy recovered to the cap " + cap + ": " + energyBefore + " to " + energy);
        h.succeed();
    }
}

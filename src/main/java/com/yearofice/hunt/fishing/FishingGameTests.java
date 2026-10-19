package com.yearofice.hunt.fishing;

import com.yearofice.item.component.YoiComponents;
import com.mojang.authlib.GameProfile;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.fishing.BiteModel;
import com.yearofice.sim.fishing.FightModel;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.FishingTuning;
import com.yearofice.sim.fishing.HookBait;
import com.yearofice.sim.fishing.SliceBite;
import com.yearofice.sim.fishing.StrikeJudge;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m2_5, the rod (Ready for development › M2.5, test 3 and the slice's rod method). Each test builds the
 * slice spot inside {@code gametest/empty3}: a 2 × 2 pool two blocks deep (1.89 m) on a stone bed, walled in by
 * stone and the GameTest barriers, so it meets the spot rule. The angler is a detached server
 * player standing on the east wall, facing the pool. Tests that force a bite hold the bobber's per-minute rolls so
 * the world clock cannot add bites of its own.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class FishingGameTests {
    private FishingGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final BlockPos WATER = new BlockPos(1, 1, 0);
    static final int RUNS = 200, WINDOW_MIN = 30;

    @GameTestGenerator
    public static Collection<TestFunction> fishingTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:hunt/slice_rod_bite", 100, FishingGameTests::sliceRodBite));
        out.add(test("yearofice:hunt/slice_spot_rule", 100, FishingGameTests::spotRule));
        out.add(test("yearofice:hunt/rod_cast_lands_in_water", 200, FishingGameTests::castLandsInWater));
        out.add(test("yearofice:hunt/rod_strike_lands_grayling", 1600, FishingGameTests::strikeLandsGrayling));
        out.add(test("yearofice:hunt/rod_line_snaps", 800, FishingGameTests::lineSnaps));
        out.add(test("yearofice:hunt/rod_unanswered_dip_takes_worm", 200, FishingGameTests::unansweredDip));
        out.add(test("yearofice:hunt/rod_nibble_strike_spooks", 200, FishingGameTests::nibbleSpooks));
        out.add(test("yearofice:hunt/fight_close_clock", 400, FishingGameTests::closeClock));
        out.add(test("yearofice:hunt/worms_from_riparian_soil", 100, FishingGameTests::worms));
        return out;
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, maxTicks, 0, true, body);
    }

    // ---- fixtures -----------------------------------------------------------------------------------------------

    /**
     * The 2 × 2 pool, 2 blocks deep, at x 0–1, z 0–1, y 0–1 on a stone bed, walled by stone at x 2 and z 2; the
     * GameTest barriers close the other two sides.
     */
    static void pool(GameTestHelper h, int depth) {
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                boolean inside = x <= 1 && z <= 1;
                if (inside) h.setBlock(new BlockPos(x, -1, z), Blocks.STONE);
                for (int y = 0; y <= 1; y++) {
                    h.setBlock(new BlockPos(x, y, z), inside && y > 1 - depth ? Blocks.WATER : Blocks.STONE);
                }
            }
        }
    }

    static void clearPool(GameTestHelper h) {
        for (int x = 0; x <= 2; x++) {
            for (int z = 0; z <= 2; z++) {
                for (int y = 0; y <= 2; y++) h.setBlock(new BlockPos(x, y, z), Blocks.AIR);
            }
        }
    }

    /** A detached angler on the east wall, facing west over the pool, rod in hand. */
    static ServerPlayer angler(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "fish-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        Vec3 at = h.absoluteVec(new Vec3(2.5, 2, 0.5));
        p.moveTo(at.x, at.y, at.z, 90f, 30f);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(FishingRegistry.ROD.get()));
        return p;
    }

    static BobberEntity bobber(GameTestHelper h, ServerPlayer p) {
        BobberEntity b = FishingService.placeBobber(p, h.absolutePos(WATER));
        b.holdRolls = true;
        RodItem.setBait(FishingService.heldRod(p), HookBait.fitted("worm", 2, 30));
        return b;
    }

    static void done(GameTestHelper h, ServerPlayer p) {
        BobberEntity b = FishingService.bobberOf(p);
        if (b != null) b.discard();
        clearPool(h);
    }

    /** The strategy bot: reel always; grab a tired fish in reach. */
    static byte bot(FightSession f) {
        FightModel.State s = f.state();
        boolean grab = s.phase() == FightModel.Phase.TIRED && s.lineOutM() <= FightModel.LAND_REACH_M;
        return new FightModel.Input(true, false, FightSession.side(f.params().assist()), false, grab).toByte();
    }

    /** Forces a bite and strikes on the dip; retries a hook that did not set. Calls {@code then} once hooked. */
    static void hookFish(GameTestHelper h, ServerPlayer p, BobberEntity b, Runnable then) {
        FishingService.forceBite(b);
        h.onEachTick(() -> {
            if (b.fight != null || b.isRemoved()) return;
            if (b.cue() == StrikeJudge.Cue.DIP) {
                StrikeJudge.Verdict v = FishingService.strike(p);
                if (v != null && v.hooked()) {
                    then.run();
                } else {
                    RodItem.setBait(FishingService.heldRod(p), HookBait.fitted("worm", 2, 30));
                    FishingService.forceBite(b);
                }
            }
        });
    }

    // ---- tests --------------------------------------------------------------------------------------------------

    /**
     * Test 3: worms at the spot give a first bite within 30 game minutes in 90% of 200 runs. Each run is a fresh
     * world seed casting a fresh worm at the slice spot on a clear May morning (day 1–3, starting 07:01–10:29, so the
     * half hour stays in m_time's ordinary day band), read through the adapter at the real pool, then rolled minute
     * by minute as the bobber rolls them.
     */
    static void sliceRodBite(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        BobberEntity b = bobber(h, p);
        try {
            long now = FishingClock.minute(h.getLevel().getServer());
            SliceBite.Inputs base = FishingService.inputs(b, now);
            h.assertTrue(base.atSpot(), "the pool is the slice spot");
            FishingTuning t = FishingConfig.tuning();
            // A fixed stream name, not the pool's cell, so the count does not move with where the runner puts the test.
            long key = com.yearofice.sim.fishing.Draws.name("slice_rod_bite");
            int hits = 0;
            for (int run = 0; run < RUNS; run++) {
                long seed = 0x5EED_0000L + run * 7919L;
                int day = 1 + (int) Math.floor(com.yearofice.sim.fishing.Draws.u(seed, run, 0, 90) * 3);
                int start = 7 * 60 + 1 + (int) Math.floor(com.yearofice.sim.fishing.Draws.u(seed, run, 0, 91) * (209));
                long minute = (day - 1) * 1440L + start;
                BiteModel.TimeBand band = FishingClock.band(minute);
                h.assertTrue(band == BiteModel.TimeBand.DAY && FishingClock.band(minute + WINDOW_MIN - 1) == BiteModel.TimeBand.DAY, "day band");
                double k = SliceSpots.capacity(FishSeason.dayOfYear(day), t);
                SliceBite.Inputs in = new SliceBite.Inputs(base.species(), base.season(), base.spotType(), base.atSpot(), base.spotQ(),
                        base.spotDepthM(), base.spotRadiusM(), k, k, HookBait.fitted("worm", t.wormBites(), t.wormMinutes()),
                        FishSeason.dayOfYear(day), band, BiteModel.Weather.CLEAR, t.waterTempC(), t.turbidity(), false);
                int first = SliceBite.firstBite(in, seed, key, minute, WINDOW_MIN);
                if (first >= 0) hits++;
            }
            YearOfIce.LOG.info("hunt/slice_rod_bite: {} of {} runs bit within {} game minutes", hits, RUNS, WINDOW_MIN);
            h.assertTrue(hits >= 0.9 * RUNS, "first bite within 30 min in " + hits + " of " + RUNS + " runs (need 90%)");
            h.succeed();
        } finally {
            done(h, p);
        }
    }

    static void spotRule(GameTestHelper h) {
        FishingTuning t = FishingConfig.tuning();
        pool(h, 2);
        SliceSpots.Spot deep = SliceSpots.at(h.getLevel(), h.absolutePos(WATER), t);
        h.assertTrue(deep.atSpot() && Math.abs(deep.depthM() - (1 + 8 / 9.0)) < 0.02, "2 x 2, 1.89 m deep is a spot: " + deep);
        pool(h, 1);
        SliceSpots.Spot shallow = SliceSpots.at(h.getLevel(), h.absolutePos(WATER), t);
        h.assertFalse(shallow.atSpot(), "0.89 m is too shallow");
        pool(h, 2);
        h.setBlock(new BlockPos(0, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        h.setBlock(new BlockPos(0, 0, 1), Blocks.STONE);
        h.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        SliceSpots.Spot strip = SliceSpots.at(h.getLevel(), h.absolutePos(WATER), t);
        h.assertFalse(strip.atSpot(), "a 2 x 1 strip is not 2 x 2");
        clearPool(h);
        h.succeed();
    }

    /** A short cast from the wall lands the bobber on the pool, fitting a worm from the pack first. */
    static void castLandsInWater(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        p.getInventory().add(new ItemStack(FishingRegistry.WORM.get(), 1));
        FishingService.cast(p, FishingService.heldRod(p), 0.0);
        BobberEntity b = FishingService.bobberOf(p);
        h.assertTrue(b != null, "bobber cast");
        HookBait bait = RodItem.bait(FishingService.heldRod(p));
        h.assertTrue(bait.present() && bait.bitesLeft() == 2 && bait.minutesLeft() == 30, "worm fitted: " + bait);
        h.assertTrue(p.getInventory().countItem(FishingRegistry.WORM.get()) == 0, "the worm left the pack");
        h.succeedWhen(() -> {
            h.assertTrue(b.state() == BobberEntity.State.FLOATING, "bobber " + b.state() + " at " + h.relativeVec(b.position()));
            h.assertTrue(SliceSpots.at(h.getLevel(), b.waterBlock(), FishingConfig.tuning()).atSpot(), "floating at the spot");
            done(h, p);
        });
    }

    /** Bite, strike on the dip, fight with the bot, land: a grayling in the pack, its weight, the stock and the line. */
    static void strikeLandsGrayling(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        BobberEntity b = bobber(h, p);
        ServerLevel level = h.getLevel();
        FishingTuning t = FishingConfig.tuning();
        long key = SliceSpots.key(b.waterBlock());
        long day = FishingClock.day(FishingClock.minute(level.getServer()));
        double before = SliceSpots.stocks(level).get(key, day, t).n();
        FightSession[] fight = new FightSession[1];
        hookFish(h, p, b, () -> fight[0] = b.fight);
        h.onEachTick(() -> {
            FightSession f = fight[0];
            if (f != null && !f.over()) f.accept(f.state().tick() + 1, new byte[] {bot(f)}, level.getGameTime());
        });
        h.succeedWhen(() -> {
            FightSession f = fight[0];
            h.assertTrue(f != null && f.over(), "fight over");
            h.assertTrue(f.state().outcome() == FightModel.Outcome.LANDED, "outcome " + f.state().outcome());
            int slot = p.getInventory().findSlotMatchingItem(new ItemStack(FishingRegistry.ARCTIC_GRAYLING.get()));
            ItemStack fish = ItemStack.EMPTY;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                if (p.getInventory().getItem(i).is(FishingRegistry.ARCTIC_GRAYLING.get())) fish = p.getInventory().getItem(i);
            }
            h.assertFalse(fish.isEmpty(), "a grayling in the pack (slot " + slot + ")");
            Integer g = fish.get(YoiComponents.WEIGHT_G.get());
            h.assertTrue(g != null && g >= 300 && g <= 1200 && g == Math.round(f.params().kg() * 1000), "weight_g " + g);
            double after = SliceSpots.stocks(level).get(key, day, t).n();
            h.assertTrue(Math.abs(before - after - 1) < 1e-9, "stock " + before + " -> " + after);
            int wear = f.state().reachedRed() ? 9 : 3;
            h.assertTrue(RodItem.lineCondition(FishingService.heldRod(p)) == 100 - wear, "line " + RodItem.lineCondition(FishingService.heldRod(p)));
            h.assertTrue(RodItem.bait(FishingService.heldRod(p)).bitesLeft() == 1, "the worm has one bite left");
            h.assertTrue(b.isRemoved(), "bobber reeled in with the fish");
            done(h, p);
        });
    }

    /** A 1.2 kg grayling on a wet line worn to nothing: the first run snaps it and takes hook and worm. */
    static void lineSnaps(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        BobberEntity b = bobber(h, p);
        ItemStack rod = FishingService.heldRod(p);
        rod.setDamageValue(RodItem.LINE_MAX);
        b.minutesInWater = (int) FishingConfig.tuning().lineWetMin();
        FishingService.startFight(b, 1.2);
        FightSession f = b.fight;
        h.assertTrue(f != null && f.params().tackle().wet() && Math.abs(f.params().sEff() - 2.1) < 1e-9, "wet, worn line: S_eff 2.1 kg");
        ServerLevel level = h.getLevel();
        h.onEachTick(() -> {
            if (!f.over()) f.accept(f.state().tick() + 1, new byte[] {bot(f)}, level.getGameTime());
        });
        h.succeedWhen(() -> {
            h.assertTrue(f.over() && f.state().outcome() == FightModel.Outcome.SNAP, "outcome " + f.state().outcome());
            h.assertFalse(RodItem.bait(rod).present(), "hook and worm lost");
            h.assertTrue(p.getInventory().countItem(FishingRegistry.ARCTIC_GRAYLING.get()) == 0, "no fish");
            h.assertTrue(b.isRemoved(), "line parted");
            done(h, p);
        });
    }

    /** A dip nobody strikes: the fish takes the worm, no fight starts and the bobber floats on, bare. */
    static void unansweredDip(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        BobberEntity b = bobber(h, p);
        FishingService.forceBite(b);
        boolean[] dipped = new boolean[1];
        h.onEachTick(() -> {
            if (b.cue() == StrikeJudge.Cue.DIP) dipped[0] = true;
        });
        h.succeedWhen(() -> {
            h.assertTrue(dipped[0] && b.cue() == StrikeJudge.Cue.NONE, "the dip came and went");
            h.assertFalse(RodItem.bait(FishingService.heldRod(p)).present(), "the fish took the worm");
            h.assertTrue(b.fight == null && b.state() == BobberEntity.State.FLOATING, "no fight");
            done(h, p);
        });
    }

    /** A strike at a nibble spooks the spot (bites ×0.5) and leaves no fight. */
    static void nibbleSpooks(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        BobberEntity b = bobber(h, p);
        b.setCue(StrikeJudge.Cue.NIBBLE, 20);
        long minute = FishingClock.minute(h.getLevel().getServer());
        double calm = SliceBite.pBite(FishingService.inputs(b, minute));
        StrikeJudge.Verdict v = FishingService.strike(p);
        h.assertTrue(v != null && v.result() == StrikeJudge.Result.SPOOKED, "verdict " + v);
        h.assertTrue(b.fight == null && b.cue() == StrikeJudge.Cue.NONE, "no fight");
        RodItem.setBait(FishingService.heldRod(p), HookBait.fitted("worm", 2, 30));
        double spooked = SliceBite.pBite(FishingService.inputs(b, minute));
        h.assertTrue(Math.abs(spooked - 0.5 * calm) < 1e-12, "bites x0.5: " + calm + " -> " + spooked);
        h.assertFalse(FishingService.inputs(b, minute + Math.round(FishingConfig.tuning().spookMin())).spooked(), "spooked 10 minutes");
        done(h, p);
        h.succeed();
    }

    /** From the strike the world clock runs at a tenth: 40 ticks of fight move the day time 4 ticks. */
    static void closeClock(GameTestHelper h) {
        pool(h, 2);
        ServerPlayer p = angler(h);
        BobberEntity b = bobber(h, p);
        ServerLevel level = h.getLevel();
        GameRules.BooleanValue rule = level.getGameRules().getRule(GameRules.RULE_DAYLIGHT);
        boolean was = rule.get();
        long[] start = {Long.MIN_VALUE};
        int[] ticks = {0};
        FishingService.startFight(b, 0.5);
        FightSession f = b.fight;
        h.onEachTick(() -> {
            if (start[0] == Long.MIN_VALUE) {
                rule.set(true, level.getServer());
                start[0] = level.getDayTime();
            }
            ticks[0]++;
            if (!f.over()) f.accept(f.state().tick() + 1, new byte[] {new FightModel.Input(true, false, FightModel.Side.INLINE, false, false).toByte()}, level.getGameTime());
        });
        h.succeedWhen(() -> {
            h.assertTrue(ticks[0] >= 41, "waiting");
            long moved = level.getDayTime() - start[0];
            rule.set(was, level.getServer());
            h.assertTrue(moved >= 3 && moved <= 5, "day time moved " + moved + " ticks in 40");
            done(h, p);
        });
    }

    /** Soil beside the pool turns up 0–2 worms in the thawed months, none from October to April. */
    static void worms(GameTestHelper h) {
        pool(h, 2);
        ServerLevel level = h.getLevel();
        FishingTuning t = FishingConfig.tuning();
        BlockPos dirt = h.absolutePos(new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(2, 2, 2), Blocks.DIRT);
        int n = WormDigging.wormsAt(level, dirt, Blocks.DIRT.defaultBlockState(), t);
        int month = FishSeason.month(FishSeason.dayOfYear(FishingClock.day(FishingClock.minute(level.getServer()))));
        int max = level.isRaining() ? 2 * t.wormDigMax() : t.wormDigMax();
        if (month >= 10 || month <= 4) h.assertTrue(n == 0, "frozen ground: " + n);
        else h.assertTrue(n >= 0 && n <= max, "worms " + n);
        h.assertTrue(WormDigging.wormsAt(level, dirt, Blocks.STONE.defaultBlockState(), t) == 0, "stone has no worms");
        clearPool(h);
        h.succeed();
    }
}

package com.yearofice.ecosystem.companion;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.ai.ThreatSources;
import com.yearofice.ecosystem.debug.EcoGameTests;
import com.yearofice.ecosystem.herd.CaribouSnareBlockEntity;
import com.yearofice.ecosystem.herd.LeadFence;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.species.rig.BigGameEntity;
import com.yearofice.sim.companion.LeadFenceMath;
import com.yearofice.sim.companion.SledMath;
import com.yearofice.sim.companion.Trust;
import com.yearofice.sim.ecosystem.Drives;
import com.yearofice.sim.time.Calendar;
import com.yearofice.worldgen.block.SnowCoverBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m9, lane A: {@code companion/*} (the wolf, the sled) and {@code hunt/lead_fence_funnels}. Everything stands on
 * stone cells high above the test grid ({@link EcoGameTests#stone}, shared and refcounted), every entity has its own test
 * group, and each test lets go of its floors, chunks and entities on every way out.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class CompanionGameTests {
    private CompanionGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int Y = 30;

    @GameTestGenerator
    public static Collection<TestFunction> companionTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:companion/pup_feeds_to_juvenile", 100, CompanionGameTests::pupFeeds));
        out.add(test("yearofice:companion/wolf_warns_bear", 600, CompanionGameTests::warnsBear));
        out.add(test("yearofice:companion/sled_writes_lane", 700, CompanionGameTests::sledLane));
        out.add(test("yearofice:hunt/lead_fence_funnels", 1100, CompanionGameTests::leadFence));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m9", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static void strip(GameTestHelper h, int x0, int x1, int z0, int z1, boolean on) {
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) EcoGameTests.stone(h, new BlockPos(1 + x, Y - 1, 1 + z), on);
    }

    static void forceArea(GameTestHelper h, int x0, int x1, int z0, int z1, boolean on) {
        for (int x = x0; x <= x1 + 16; x += 16) for (int z = z0; z <= z1 + 16; z += 16)
            EcoGameTests.force(h, h.absolutePos(new BlockPos(1 + Math.min(x, x1), Y, 1 + Math.min(z, z1))).getCenter(), 0, on);
    }

    /** Runs {@code setup} once, {@code wait} ticks in, then the handler it returns on every tick (one top-level onEachTick). */
    static void deferred(GameTestHelper h, int wait, java.util.function.Supplier<Runnable> setup) {
        Runnable[] handler = {null};
        h.onEachTick(() -> {
            if (h.getTick() < wait) return;
            if (handler[0] == null) handler[0] = setup.get();
            handler[0].run();
        });
    }

    static WolfEntity wolf(GameTestHelper h, String group, BlockPos rel) {
        WolfEntity w = h.spawn(EcosystemRegistry.WOLF.get(), rel);
        w.setTestGroup(group);
        w.setScentEnabled(false);
        w.setAutoDays(false);
        w.pinDriveAct(Drives.Act.BED);
        return w;
    }

    /**
     * companion/pup_feeds_to_juvenile (Taming a wolf without bones), on a compressed clock: a den pup fed once a day
     * (food on the ground until it is Tolerant, then from the hand) is a juvenile (25 trust) well before it is a
     * Companion (80), and a Companion in 30 to 60 days -- never in fewer than 32 -- growing as it goes. Naming opens at
     * Tolerant, the day's first feeding writes a notebook line, and a hundred bones do nothing.
     */
    static void pupFeeds(GameTestHelper h) {
        EcoGameTests.stone(h, new BlockPos(1, Y - 1, 1), true);
        WolfEntity w = wolf(h, "pup", new BlockPos(1, Y, 1));
        w.makeTameable(0, 60);
        ServerPlayer p = EcoGameTests.player(h);
        ServerLevel level = h.getLevel();
        double scale0 = w.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE, 64));
            for (int i = 0; i < 100; i++) w.interact(p, InteractionHand.MAIN_HAND);
            h.assertTrue(w.trust().value() == 0 && !w.owned(), "a hundred bones changed something: trust " + w.trust().value());
            long day = Calendar.absDayOfMinute(0) + 20; // May, so no February roll
            int juvenile = -1, companion = -1;
            for (int d = 1; d <= 90 && companion < 0; d++) {
                Trust.Action a = w.trust().value() >= Trust.TOLERANT ? Trust.Action.HAND_FEED : Trust.Action.FEED_GROUND;
                w.act(p, a);
                w.dailyStep(level, day + d);
                if (juvenile < 0 && w.trust().stage() != Trust.Stage.PUP) juvenile = d;
                if (w.trust().stage() == Trust.Stage.COMPANION) companion = d;
            }
            h.assertTrue(juvenile > 0 && companion > juvenile, "stages: juvenile day " + juvenile + ", companion day " + companion);
            h.assertTrue(companion >= 32 && companion <= 60, "Companion on day " + companion + ", wanted 32 to 60");
            h.assertTrue(w.owned() && !w.wolfName().isEmpty(), "naming opens at Tolerant; name '" + w.wolfName() + "'");
            h.assertTrue(w.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) > scale0, "the pup should grow");
            h.assertTrue(!com.yearofice.story.Journal.entries(p).isEmpty(), "the first feeding writes a notebook line");
            h.succeed();
        } finally {
            w.discard();
            EcoGameTests.stone(h, new BlockPos(1, Y - 1, 1), false);
        }
    }

    /**
     * companion/wolf_warns_bear (Taming a wolf: reads the wild for you): a Companion wolf growls at a bear within 60 m
     * of it, stands rather than flees, and says nothing about a bear at 70 m; a wolf nobody owns warns of nothing.
     */
    static void warnsBear(GameTestHelper h) {
        strip(h, -4, 76, -3, 3, true);
        forceArea(h, -4, 76, -3, 3, true);
        deferred(h, 40, () -> warnsBearRun(h));
    }

    static Runnable warnsBearRun(GameTestHelper h) {
        WolfEntity w = wolf(h, "warn", new BlockPos(1, Y, 1));
        WolfEntity stray = wolf(h, "warn", new BlockPos(1, Y, 3));
        BearEntity bear = h.spawn(EcosystemRegistry.BLACK_BEAR.get(), new BlockPos(1 + 70, Y, 1));
        bear.setTestGroup("warn");
        bear.setScentEnabled(false);
        bear.pinDriveAct(Drives.Act.BED);
        ServerPlayer p = EcoGameTests.player(h);
        p.moveTo(w.getX() - 3, w.getY(), w.getZ(), 0, 0);
        w.makeTameable(90, 400);
        w.setOwner(p);
        w.setCommand(WolfEntity.Command.STAY);
        stray.makeTameable(90, 400); // trust but no owner: it warns no one
        ThreatSources.addForTest(bear, w, stray);
        Vec3 start = w.position();
        boolean[] done = {false, false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            ThreatSources.removeForTest(bear);
            w.discard();
            stray.discard();
            bear.discard();
            forceArea(h, -4, 76, -3, 3, false);
            strip(h, -4, 76, -3, 3, false);
        };
        return () -> {
            if (done[0]) return;
            try {
                long t = h.getTick();
                if (t < 140) {
                    h.assertTrue(w.warnings() == 0, "the bear at 70 m is beyond the warning range, yet the wolf warned");
                    return;
                }
                if (!done[1]) {
                    done[1] = true;
                    bear.teleportTo(w.getX() + 40, w.getY(), w.getZ());
                }
                if (w.warnings() >= 1) {
                    h.assertTrue(w.position().distanceTo(start) < 6, "the wolf ran " + w.position().distanceTo(start) + " m instead of standing");
                    h.assertTrue(stray.warnings() == 0, "a wolf with no owner warned");
                    release.run();
                    h.succeed();
                } else if (t > 300) {
                    h.fail("the wolf never warned of a bear at 40 m");
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        };
    }

    /**
     * companion/sled_writes_lane (the wolf team): three grown Companion wolves hitched to a sled pull it east over bare
     * ground, then over a set trail on snow. It is slower on the bare ground than on the trail, the team keeps up, and the
     * cells it crossed become {@code sled_lane} (trail_wear 3) while a cell beside the path stays set (2).
     */
    static void sledLane(GameTestHelper h) {
        strip(h, -3, 36, -3, 3, true);
        forceArea(h, -3, 36, -3, 3, true);
        deferred(h, 40, () -> sledRun(h));
    }

    static Runnable sledRun(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        for (int x = 5; x <= 34; x++) for (int z = -2; z <= 2; z++)
            h.setBlock(new BlockPos(1 + x, Y, 1 + z), WorldBlocks.SNOW_COVER.get().defaultBlockState().setValue(SnowCoverBlock.LAYERS, 1).setValue(SnowCoverBlock.TRAIL_WEAR, 2));
        SledEntity sled = CompanionRegistry.SLED.get().create(level);
        Vec3 origin = h.absolutePos(new BlockPos(1, Y, 1)).getBottomCenter();
        sled.moveTo(origin.x, origin.y, origin.z, -90, 0);
        level.addFreshEntity(sled);
        List<WolfEntity> team = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            WolfEntity w = wolf(h, "sled", new BlockPos(1 + 3, Y, 1 - 1 + i));
            w.makeTameable(90, 400);
            w.setOwner(EcoGameTests.player(h));
            team.add(w);
        }
        int hitched = sled.hitchNear(10, "sled");
        sled.drive(-90, 1f);
        double[] bare = {0}, lane = {0};
        boolean[] done = {false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            sled.unhitchAll();
            sled.discard();
            team.forEach(WolfEntity::discard);
            for (int x = 5; x <= 34; x++) for (int z = -2; z <= 2; z++) h.setBlock(new BlockPos(1 + x, Y, 1 + z), Blocks.AIR);
            forceArea(h, -3, 36, -3, 3, false);
            strip(h, -3, 36, -3, 3, false);
        };
        return () -> {
            if (done[0]) return;
            try {
                h.assertTrue(hitched == 3 && sled.teamSize() == 3, "hitched " + hitched + " wolves");
                double x = sled.getX() - origin.x;
                if (x < 4) bare[0] = Math.max(bare[0], sled.speedMps());
                else lane[0] = Math.max(lane[0], sled.speedMps());
                if (x >= 28) {
                    var tun = CompanionConfig.tuning();
                    double wantBare = SledMath.speedMps(tun, 3, SledMath.Surface.BARE, 0);
                    h.assertTrue(bare[0] <= wantBare + 0.05, "bare-ground speed " + bare[0] + " should not exceed " + wantBare);
                    h.assertTrue(lane[0] > bare[0] + 1.0, "snow speed " + lane[0] + " should beat bare " + bare[0]);
                    int lanes = 0, beside = 0;
                    for (int cx = 6; cx <= 27; cx++) {
                        var s = level.getBlockState(h.absolutePos(new BlockPos(1 + cx, Y, 1)));
                        if (s.is(WorldBlocks.SNOW_COVER.get()) && s.getValue(SnowCoverBlock.TRAIL_WEAR) == 3) lanes++;
                        var side = level.getBlockState(h.absolutePos(new BlockPos(1 + cx, Y, 1 + 2)));
                        if (side.is(WorldBlocks.SNOW_COVER.get()) && side.getValue(SnowCoverBlock.TRAIL_WEAR) == 2) beside++;
                    }
                    h.assertTrue(lanes >= 18, "only " + lanes + " of 22 cells became a sled lane");
                    h.assertTrue(beside == 22, "cells beside the path changed: " + beside + " of 22 stayed set");
                    for (WolfEntity w : team) h.assertTrue(w.distanceTo(sled) < 8, "a wolf fell " + w.distanceTo(sled) + " m behind");
                    release.run();
                    h.succeed();
                } else if (h.getTick() > 600) {
                    h.fail("the sled got only " + x + " m east, speed " + sled.speedMps() + ", surface " + sled.surface());
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        };
    }

    /**
     * hunt/lead_fence_funnels (B12): two 81-block wings (20 segments each, 80 m) close on a mouth 16 m wide with a snare in
     * it. Read back from the blocks, the layout has two wings of 20 segments and a mouth; of 50 bands meeting a wing in
     * neutral wind 40 to 60 % follow it to the mouth, and with the wind toward the bands none do. One real caribou
     * set on the wing walks to the mouth and stands there; one with the scent blowing to it stays where it is.
     */
    static void leadFence(GameTestHelper h) {
        strip(h, 44, 84, -14, 5, true);
        forceArea(h, -2, 86, -32, 32, true);
        deferred(h, 40, () -> leadFenceRun(h));
    }

    static List<BlockPos> line(int x0, int z0, int x1, int z1) {
        List<BlockPos> out = new ArrayList<>();
        for (int x = x0; x <= x1; x++) {
            int z = z0 + Math.round((float) (z1 - z0) * (x - x0) / (x1 - x0));
            out.add(new BlockPos(1 + x, Y, 1 + z));
        }
        return out;
    }

    static Runnable leadFenceRun(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        List<BlockPos> blocks = new ArrayList<>();
        blocks.addAll(line(0, -30, 80, -8));
        blocks.addAll(line(0, 30, 80, 10));
        for (BlockPos b : blocks) h.setBlock(b, CompanionRegistry.LEAD_FENCE.get().defaultBlockState());
        BlockPos snare = new BlockPos(1 + 81, Y, 1);
        h.setBlock(snare, CompanionRegistry.CARIBOU_SNARE.get().defaultBlockState());
        BlockPos center = h.absolutePos(new BlockPos(1 + 41, Y, 1));
        BigGameEntity inside = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1 + 50, Y, 1 - 8));
        BigGameEntity scented = h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1 + 56, Y, 1 + 8));
        for (BigGameEntity c : List.of(inside, scented)) {
            c.setTestGroup("fence");
            c.setScentEnabled(false);
            c.pinDriveAct(Drives.Act.BED);
        }
        LeadFence.clear();
        boolean[] done = {false, false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            inside.discard();
            scented.discard();
            LeadFence.clear();
            for (BlockPos b : blocks) h.setBlock(b, Blocks.AIR);
            h.setBlock(snare, Blocks.AIR);
            forceArea(h, -2, 86, -32, 32, false);
            strip(h, 44, 84, -14, 5, false);
        };
        return () -> {
            if (done[0]) return;
            try {
                if (!done[1]) {
                    done[1] = true;
                    LeadFence.Layout layout = LeadFence.scan(level, center, 45);
                    h.assertTrue(layout.wings().size() == 2, "wings read back: " + layout.wings().size());
                    for (LeadFence.Wing w : layout.wings()) h.assertTrue(w.segments() == 20, "a wing of " + w.cells().size() + " blocks reads as " + w.segments() + " segments");
                    h.assertTrue(layout.mouth() != null && layout.snares().size() == 1, "no mouth found");
                    Vec3 mouth = layout.mouth();
                    Vec3 snareAt = Vec3.atCenterOf(h.absolutePos(snare));
                    h.assertTrue(mouth.distanceTo(snareAt) < 5, "the mouth is " + mouth.distanceTo(snareAt) + " m from the snare");
                    double width = layout.wings().get(0).mouthEnd().distanceTo(layout.wings().get(1).mouthEnd());
                    h.assertTrue(width >= 15 && width <= 20, "mouth width " + width);
                    Vec3 blind = mouth.add(2, 0, 0);
                    SplittableRandom rng = new SplittableRandom(20261020L);
                    int[] passed = new int[2];
                    for (int wind = 0; wind < 2; wind++) {
                        for (int band = 0; band < 50; band++) {
                            LeadFence.Wing w = layout.wings().get(band % 2);
                            List<BlockPos> along = new ArrayList<>(w.cells());
                            along.sort(java.util.Comparator.comparingDouble(c -> Vec3.atCenterOf(c).distanceTo(w.farEnd())));
                            BlockPos cell = along.get((int) (along.size() * (0.1 + 0.6 * rng.nextDouble())));
                            double heading = w.headingDeg() + (rng.nextDouble() * 56 - 28);
                            Vec3 at = Vec3.atCenterOf(cell);
                            boolean follows = LeadFence.decide(layout, at, heading, wind == 1, rng);
                            // a band that does not follow turns away from the funnel and stays outside it: it keeps its distance from the blind
                            double nearest = follows ? mouth.distanceTo(blind) : at.distanceTo(blind);
                            if (nearest <= 20) passed[wind]++;
                        }
                    }
                    double neutral = passed[0] / 50.0;
                    h.assertTrue(neutral >= 0.40 && neutral <= 0.60, "in neutral wind " + passed[0] + " of 50 bands passed within 20 m of the blind");
                    h.assertTrue(passed[1] == 0, "with the wind toward the bands " + passed[1] + " passed");
                    // a real caribou: aimed along the wing, scent away -> follows; scent toward -> does not
                    LeadFence.Wing wing = layout.wings().get(0);
                    inside.setYRot((float) wing.headingDeg());
                    inside.setWindTowardOverride(Sensing.bearing(mouth, inside.position()) + 180);
                    scented.setYRot((float) wing.headingDeg());
                    scented.setWindTowardOverride(Sensing.bearing(mouth, scented.position()));
                    java.util.random.RandomGenerator yes = () -> 0L; // every roll lands
                    h.assertTrue(LeadFence.consider(level, inside, yes), "the band with the scent blowing away should follow");
                    h.assertFalse(LeadFence.consider(level, scented, yes), "the band with the scent blowing to it should not follow");
                    return;
                }
                Vec3 mouth = LeadFence.scan(level, center, 45).mouth();
                CaribouSnareBlockEntity seen = (CaribouSnareBlockEntity) level.getBlockEntity(h.absolutePos(snare));
                if (Math.hypot(inside.getX() - mouth.x, inside.getZ() - mouth.z) <= 6 && seen != null && seen.passes() >= 1) {
                    h.assertTrue(!LeadFence.leading(scented), "the scented band followed the fence");
                    h.assertTrue(Math.hypot(scented.getX() - mouth.x, scented.getZ() - mouth.z) > 20, "the scented caribou moved to the mouth");
                    CaribouSnareBlockEntity be = (CaribouSnareBlockEntity) level.getBlockEntity(h.absolutePos(snare));
                    h.assertTrue(be != null && be.passes() >= 1, "the snare saw no pass");
                    release.run();
                    h.succeed();
                } else if (h.getTick() > 1000) {
                    h.fail("the caribou never reached the mouth; it is " + Math.hypot(inside.getX() - mouth.x, inside.getZ() - mouth.z) + " m away, state " + inside.state());
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        };
    }
}

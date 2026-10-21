package com.yearofice.ecosystem.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.ai.ScentPoints;
import com.yearofice.ecosystem.ai.Sensing;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.pool.ValleyPool;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.species.rig.BigGameEntity;
import com.yearofice.ecosystem.species.rig.PredatorEntity;
import com.yearofice.sim.ecosystem.Drives;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Batch m6, lane G (the last 50 m): a hungry predator with no prey follows the strongest scent it smells to a gut pile and
 * feeds, and a wolf with live prey nearby keeps the prey. Each test runs in its own far region (own chunks, own test group).
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class ScentGameTests {
    private ScentGameTests() {}

    static final int FAR_BEAR = 12 * 4096, FAR_WOLF = 13 * 4096;
    static final String GROUP = "scent_g";

    @GameTestGenerator
    public static Collection<TestFunction> scentTests() {
        List<TestFunction> out = new ArrayList<>();
        int walkTicks = (int) Math.ceil(30 * SliceClock.TICKS_PER_MINUTE);
        out.add(test("yearofice:eco/bear_walks_to_pile", walkTicks + 120, h -> run(h, true, FAR_BEAR, walkTicks)));
        out.add(test("yearofice:eco/wolf_ignores_pile_when_hunting", 400, h -> run(h, false, FAR_WOLF, 300)));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6", name, EcoGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    static final int Y = BigGameGameTests.Y;

    static void run(GameTestHelper h, boolean bearCase, int zOff, int limit) {
        int len = bearCase ? 130 : 60;
        Vec3 start = h.absolutePos(new BlockPos(1, Y, 1 + zOff)).getCenter();
        BigGameGameTests.forceLine(h, start, len, true);
        BigGameGameTests.deferred(h, 40, () -> body(h, bearCase, zOff, limit, start, len)); // the strip needs its chunks loaded first
    }

    static Runnable body(GameTestHelper h, boolean bearCase, int zOff, int limit, Vec3 start, int len) {
        BigGameGameTests.strip(h, -5, len, 8, true, zOff);
        BlockPos pile = new BlockPos(1, Y, 1 + zOff);
        h.setBlock(pile, EcosystemRegistry.GUT_PILE.get().defaultBlockState());
        BlockPos abs = h.absolutePos(pile);
        ScentPoints.add(h.getLevel(), abs);
        // no global weather pin here: a parallel weather/ test reads the same flag. The wind is pinned per entity and by ValleyPool.windOverride; 120 m is far inside any rain-trimmed reach.
        double[] wind0 = ValleyPool.windOverride;
        int dx = bearCase ? 120 : 40;
        PredatorEntity eater = bearCase ? h.spawn(EcosystemRegistry.GRIZZLY.get(), new BlockPos(1 + dx, Y, 1 + zOff))
                : h.spawn(EcosystemRegistry.WOLF.get(), new BlockPos(1 + dx, Y, 1 + zOff));
        BigGameEntity prey = bearCase ? null : h.spawn(EcosystemRegistry.CARIBOU.get(), new BlockPos(1 + dx - 20, Y, 1 + zOff));
        eater.setScentEnabled(false);
        eater.setTestGroup(GROUP);
        eater.getRandom().setSeed(4401L);
        if (prey != null) {
            prey.setScentEnabled(false);
            prey.setTestGroup(GROUP);
            prey.getRandom().setSeed(4402L);
        }
        eater.huntAnyHour();
        eater.pinDriveAct(Drives.Act.FEED);
        eater.driveState().hunger = 1.0;
        Vec3 at = eater.position();
        double toward = Sensing.bearing(Vec3.atCenterOf(abs), at); // the wind carries the pile's scent to the predator
        eater.setWindTowardOverride(toward);
        ValleyPool.windOverride = new double[] {toward, 3.0};
        boolean[] done = {false};
        Runnable release = () -> {
            if (done[0]) return;
            done[0] = true;
            ScentPoints.remove(h.getLevel(), abs);
            eater.discard();
            if (prey != null) prey.discard();
            ValleyPool.windOverride = wind0;
            BigGameGameTests.forceLine(h, start, len, false);
            h.setBlock(pile, Blocks.AIR);
            BigGameGameTests.strip(h, -5, len, 8, false, zOff);
        };
        return () -> {
            if (done[0]) return;
            try {
                long t = h.getTick() - 40;
                if (bearCase) {
                    if (eater.scavenges() >= 1) {
                        h.assertTrue(eater.distanceToSqr(Vec3.atCenterOf(abs)) < 9 * 9, "it fed from " + Math.sqrt(eater.distanceToSqr(Vec3.atCenterOf(abs))) + " m");
                        h.assertTrue(t <= limit, "it took " + t + " ticks (limit " + limit + " = 30 game minutes)");
                        release.run();
                        h.succeed();
                    } else if (t > limit) {
                        h.fail("the bear never fed at the pile in 30 game minutes: " + eater.huntDebug() + " moved " + eater.position().subtract(at));
                    }
                } else if (t < 20) {
                    return;
                } else if (eater.scavenges() > 0 && eater.kills() == 0) {
                    h.fail("the wolf went to the pile with live prey in reach: " + eater.huntDebug());
                } else if (eater.kills() >= 1 || (t > 120 && eater.chasing())) {
                    h.assertTrue(eater.scavenges() == 0, "it kept the prey, not the pile");
                    release.run();
                    h.succeed();
                } else if (t > limit) {
                    h.fail("the wolf neither chased nor killed: " + eater.huntDebug());
                }
            } catch (RuntimeException e) {
                release.run();
                throw e;
            }
        };
    }
}

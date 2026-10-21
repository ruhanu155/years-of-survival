package com.yearofice.ecosystem.debug;

import com.yearofice.core.YearOfIce;
import com.yearofice.api.hud.HudBus;
import com.yearofice.ecosystem.EcoConfig;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.ai.ScentPoints;
import com.yearofice.ecosystem.ai.ThreatSources;
import com.yearofice.ecosystem.pool.ValleyPool;
import com.yearofice.ecosystem.pool.ValleyPool.Area;
import com.yearofice.ecosystem.species.BearEntity;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.env.weather.WeatherSystem;
import com.yearofice.sim.ecosystem.BearContact;
import com.yearofice.sim.ecosystem.PoolModel;
import com.yearofice.sim.time.Calendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M6 lane F (RECONCILE 306, "The valley's bears"): {@code eco/scent_pulls_bear_from_valley},
 * {@code eco/offscreen_pool_conserves}, {@code eco/pool_seeds_by_density} and {@code encounter/bear_contact_sequence}.
 * The pool tests run on a detached pool in a far region with calm weather and a pinned wind; the hours are stepped by
 * hand on a pinned date (September 20, when bears fatten for the den).
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class PoolGameTests {
    private PoolGameTests() {}

    @GameTestGenerator
    public static Collection<TestFunction> poolTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:eco/scent_pulls_bear_from_valley", 100, PoolGameTests::scentPulls));
        out.add(test("yearofice:eco/offscreen_pool_conserves", 300, PoolGameTests::conserves));
        out.add(test("yearofice:eco/pool_seeds_by_density", 100, PoolGameTests::seedsByDensity));
        out.add(test("yearofice:encounter/bear_contact_sequence", 700, PoolGameTests::contactSequence));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6", name, EcoGameTests.TEMPLATE, maxTicks, 0, true, body);
    }

    static final long SEP_20 = 1440L * (365L * 3 + Calendar.dayOfYear(9, 20) - 1);

    static Area area(int farZ, int sideM) { return new Area("test" + farZ, 0, sideM - 1, farZ, farZ + sideM - 1); }

    // ---- density -------------------------------------------------------------------------------------------------

    static void seedsByDensity(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        ValleyPool pool = new ValleyPool();
        Area a = area(EcoGameTests.FAR_POOL, 10_000), b = area(EcoGameTests.FAR_POOL + 20_000, 3_000);
        double mult = EcoConfig.tuning().predatorDensityMult();
        double[] sum = new double[ValleyPool.SPECIES.size()];
        int salts = 200;
        for (int s = 0; s < salts; s++) {
            pool.reset(List.of(a, b), s);
            pool.ensureSeeded(l);
            for (int i = 0; i < sum.length; i++) {
                String sp = ValleyPool.SPECIES.get(i);
                double expected = com.yearofice.ecosystem.species.SpeciesBook.get(sp).spawn().densityPerKm2() * mult * (100 + 9);
                int n = pool.population(sp);
                h.assertTrue(n >= Math.floor(expected) - 1 && n <= Math.ceil(expected) + 1, sp + " seeded " + n + " for expected " + expected);
                sum[i] += n;
            }
        }
        for (int i = 0; i < sum.length; i++) {
            String sp = ValleyPool.SPECIES.get(i);
            double expected = com.yearofice.ecosystem.species.SpeciesBook.get(sp).spawn().densityPerKm2() * mult * 109;
            h.assertTrue(Math.abs(sum[i] / salts - expected) < Math.max(0.5, expected * 0.1), sp + " mean " + sum[i] / salts + " vs density x area " + expected);
        }
        h.succeed();
    }

    // ---- scent pulls a bear -------------------------------------------------------------------------------------

    static void scentPulls(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        boolean calm = WeatherSystem.calmOverride;
        WeatherSystem.calmOverride = true;
        ValleyPool pool = new ValleyPool();
        ValleyPool.testPool = pool;
        int reached = 0, seeds = 10;
        try {
            for (int seed = 0; seed < seeds; seed++) {
                pool.reset(List.of(area(EcoGameTests.FAR_POOL, 12_000)), seed);
                pool.ensureSeeded(l);
                // the pile sits on the creek bank 900 m from the valley's bear nearest the middle, and the wind blows from it toward that bear
                var bear = nearestBear(pool, 6000, EcoGameTests.FAR_POOL + 6000);
                h.assertTrue(bear != null, "the valley holds a bear (seed " + seed + ")");
                double ang = Math.PI * 2 * ((seed * 0.37) % 1);
                int px = (int) Math.round(bear.homeX + Math.cos(ang) * 900), pz = (int) Math.round(bear.homeZ + Math.sin(ang) * 900);
                BlockPos pile = new BlockPos(px, 70, pz);
                EcoGameTests.force(h, Vec3.atCenterOf(pile), 0, true);
                l.setBlock(pile.below(), Blocks.STONE.defaultBlockState(), 3);
                l.setBlock(pile, EcosystemRegistry.GUT_PILE.get().defaultBlockState(), 3);
                h.assertTrue(ScentPoints.in(l).contains(pile), "the pile posts its scent");
                ValleyPool.windOverride = new double[] {PoolModel.bearing(px, pz, bear.homeX, bear.homeZ), 3.0};
                for (int hour = 0; hour < 48; hour++) pool.hour(l, SEP_20 + hour * 60L);
                boolean got = false;
                for (var r : ValleyPool.REACHED) if (r.species().endsWith("bear") || r.species().endsWith("grizzly")) {
                    if (r.source().getX() == px && r.source().getZ() == pz && r.distM() <= 20) got = true;
                }
                if (got) reached++;
                ScentPoints.remove(l, pile);
                l.setBlock(pile, Blocks.AIR.defaultBlockState(), 3);
                l.setBlock(pile.below(), Blocks.AIR.defaultBlockState(), 3);
                EcoGameTests.force(h, Vec3.atCenterOf(pile), 0, false);
            }
        } finally {
            ValleyPool.testPool = null;
            ValleyPool.windOverride = null;
            WeatherSystem.calmOverride = calm;
        }
        com.mojang.logging.LogUtils.getLogger().info("scent_pulls_bear_from_valley: {} of {} seeds had a bear within 20 m of the pile", reached, seeds);
        h.assertTrue(reached >= 7, "only " + reached + " of " + seeds + " seeds had a bear reach the pile");
        h.succeed();
    }

    static com.yearofice.sim.ecosystem.PoolModel.Indiv nearestBear(ValleyPool pool, double x, double z) {
        PoolModel.Indiv best = null;
        double bd = Double.MAX_VALUE;
        for (var a : pool.individuals()) {
            if (!(a.species.endsWith("bear") || a.species.endsWith("grizzly"))) continue;
            double d = Math.hypot(a.homeX - x, a.homeZ - z);
            if (d < bd) { bd = d; best = a; }
        }
        return best;
    }

    // ---- conservation -------------------------------------------------------------------------------------------

    static void conserves(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        boolean calm = WeatherSystem.calmOverride;
        WeatherSystem.calmOverride = true;
        ValleyPool pool = new ValleyPool();
        ValleyPool.testPool = pool;
        Area area = area(EcoGameTests.FAR_POOL_CONSERVE, 6_000);
        pool.reset(List.of(area), 3);
        pool.ensureSeeded(l);
        int pop = pool.population(null);
        BlockPos pile = new BlockPos(3000, 70, EcoGameTests.FAR_POOL_CONSERVE + 3000);
        Vec3 c = Vec3.atCenterOf(pile);
        EcoGameTests.force(h, c, 10, true);
        l.setBlock(pile.below(), Blocks.STONE.defaultBlockState(), 3);
        l.setBlock(pile, EcosystemRegistry.GUT_PILE.get().defaultBlockState(), 3);
        // a viewer camps at the pile; every approach that crosses its 128 m radius becomes a body at the edge
        pool.anchors = List.of(new double[] {pile.getX(), pile.getZ()});
        var bear = nearestBear(pool, pile.getX(), pile.getZ());
        h.assertTrue(bear != null, "the valley holds a bear");
        ValleyPool.windOverride = new double[] {PoolModel.bearing(pile.getX(), pile.getZ(), bear.homeX, bear.homeZ), 3.0};
        bear.hunger = 1.0;
        bear.x = pile.getX() + Math.signum(bear.homeX - pile.getX()) * 400;
        bear.z = pile.getZ() + Math.signum(bear.homeZ - pile.getZ()) * 300;
        bear.tx = pile.getX();
        bear.tz = pile.getZ();
        bear.mode = PoolModel.Mode.APPROACHING;
        bear.sourceKey = pile.asLong();
        try {
            pool.advance(l, SEP_20, 3600, null);
            h.assertTrue(bear.live >= 1 && pool.instantiated() == 1, "the approach reached the radius and became a body: live " + bear.live + ", out " + pool.instantiated());
            int[] c1 = pool.census();
            h.assertTrue(c1[0] + c1[1] + c1[2] == pop && pop == pool.population(null), "live + pooled + dead = population " + pop);
            var body = l.getEntitiesOfClass(BearEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(200));
            h.assertTrue(body.size() == 1, "one body stands at the edge, found " + body.size());
            double edge = Math.hypot(body.get(0).getX() - pile.getX(), body.get(0).getZ() - pile.getZ());
            h.assertTrue(edge > 100 && edge <= 128, "it stands at the active radius edge, " + edge + " m from the viewer");
        } catch (RuntimeException e) {
            cleanup(h, l, pile, c, calm);
            throw e;
        }
        // no real player is near: the next spawner pass demotes the body, and the individual walks home
        h.onEachTick(() -> {
            if (h.getTick() < 120) return;
            try {
                int[] c2 = pool.census();
                h.assertTrue(c2[1] == 0 && pool.returned() == pool.instantiated() && pool.returned() == 1, "the body left and went back: live " + c2[1] + ", in " + pool.returned() + ", out " + pool.instantiated());
                h.assertTrue(c2[0] + c2[1] + c2[2] == pop, "pooled + live + dead = population after the return");
                h.assertTrue(bear.mode == PoolModel.Mode.RETURNING, "it is walking home: " + bear.mode);
                // a second trip ends in a death: the individual is gone for good, and the count still holds
                bear.mode = PoolModel.Mode.APPROACHING;
                bear.sourceKey = pile.asLong();
                bear.x = pile.getX() + 300;
                bear.z = pile.getZ();
                pool.advance(l, SEP_20, 3600, null);
                h.assertTrue(bear.live == 1, "second approach became a body");
                var b = l.getEntitiesOfClass(BearEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(200)).get(0);
                SlotSpawner.reportDeath(l, b, true);
                b.discard();
                int[] c3 = pool.census();
                h.assertTrue(c3[2] == 1 && c3[0] + c3[1] + c3[2] == pop && pool.returned() == pool.instantiated(), "a death is counted out for good: " + java.util.Arrays.toString(c3));
                cleanup(h, l, pile, c, calm);
                h.succeed();
            } catch (RuntimeException e) {
                cleanup(h, l, pile, c, calm);
                throw e;
            }
        });
    }

    static boolean cleaned;

    static void cleanup(GameTestHelper h, ServerLevel l, BlockPos pile, Vec3 c, boolean calm) {
        ValleyPool.testPool = null;
        ValleyPool.windOverride = null;
        WeatherSystem.calmOverride = calm;
        ScentPoints.remove(l, pile);
        l.setBlock(pile, Blocks.AIR.defaultBlockState(), 3);
        l.setBlock(pile.below(), Blocks.AIR.defaultBlockState(), 3);
        for (var b : l.getEntitiesOfClass(BearEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(300))) b.discard();
        EcoGameTests.force(h, c, 10, false);
    }

    // ---- the contact sequence -----------------------------------------------------------------------------------

    /**
     * Blueprint › How a bear decides: a bear startled by a player moving inside 25 m notices (5 s here), huffs, and
     * rushes; a bluff (roll 0, a still player) stops 3-10 m short; a real rush (roll 0.99) reaches contact (2.5 m).
     * Two bears run side by side, each with its own detached player.
     */
    static void contactSequence(GameTestHelper h) {
        int zOff = EcoGameTests.FAR_CONTACT;
        // the bears only tick in loaded, entity-ticking chunks: force the whole field (no player holds it open) for the test's length
        Vec3 mid = h.absolutePos(new BlockPos(1 + 18, BigGameGameTests.Y, 1 + zOff)).getCenter();
        EcoGameTests.force(h, mid, 3, true);
        BigGameGameTests.deferred(h, 40, () -> {
            BigGameGameTests.strip(h, -5, 40, 35, true, zOff);
            return contactRun(h, zOff);
        });
    }

    static Runnable contactRun(GameTestHelper h, int zOff) {
        BearContactSetup s = new BearContactSetup();
        s.zOff = zOff;
        s.bluff = make(h, zOff - 15, 0.0);
        s.contact = make(h, zOff + 15, 0.99);
        return () -> {
            try {
                step(h, s.bluff);
                step(h, s.contact);
                if (s.bluff.done() && s.contact.done()) {
                    check(h, s.bluff, true);
                    check(h, s.contact, false);
                    finish(h, s);
                    h.succeed();
                } else if (h.getTick() > 640) {
                    String msg = "sequence incomplete: bluff " + s.bluff.bear.contact.trace + ", contact " + s.contact.bear.contact.trace
                            + "; bear " + s.bluff.bear.isAlive() + " " + s.bluff.bear.position() + " phase " + s.bluff.bear.contact.phase + " state " + s.bluff.bear.state()
                            + " player " + s.bluff.p.position() + " near " + ThreatSources.near((ServerLevel) s.bluff.bear.level(), s.bluff.bear.position(), 50, s.bluff.bear);
                    finish(h, s);
                    h.fail(msg);
                }
            } catch (RuntimeException e) {
                finish(h, s);
                throw e;
            }
        };
    }

    static final class BearContactSetup {
        int zOff;
        Run bluff, contact;
    }

    static final class Run {
        BearEntity bear;
        net.minecraft.server.level.ServerPlayer p;
        Vec3 at;
        boolean moved, ended;
        long t0;
        boolean done() { return ended; }
    }

    static Run make(GameTestHelper h, int zRel, double roll) {
        Run r = new Run();
        r.t0 = h.getTick();
        r.bear = h.spawn(EcosystemRegistry.GRIZZLY.get(), new BlockPos(1, BigGameGameTests.Y, 1 + zRel));
        r.bear.setScentEnabled(false);
        r.bear.setTestGroup("bear_contact" + roll);
        r.bear.pinDriveAct(com.yearofice.sim.ecosystem.Drives.Act.BED);
        r.bear.contact.noticeRoll = 0.0;
        r.bear.contact.stopRoll = 0.5;
        r.bear.contact.chargeRoll = roll;
        r.p = EcoGameTests.player(h);
        r.at = h.absolutePos(new BlockPos(1 + 20, BigGameGameTests.Y, 1 + zRel)).getCenter();
        r.p.moveTo(r.at.x, r.at.y, r.at.z, 0, 0);
        ThreatSources.addForTest(r.p, r.bear);
        return r;
    }

    static void step(GameTestHelper h, Run r) {
        long t = h.getTick();
        var d = r.bear.contact;
        if (t >= r.t0 + 30 && !r.moved) {
            // the player takes two steps toward the bear: startled inside 25 m
            r.p.moveTo(r.at.x - 1.0, r.at.y, r.at.z, 0, 0);
            r.moved = true;
        }
        if (d.phase == BearContact.Phase.NONE && !d.trace.isEmpty()) r.ended = true;
    }

    static void check(GameTestHelper h, Run r, boolean bluff) {
        List<String> tr = r.bear.contact.trace;
        String all = String.join(",", tr);
        h.assertTrue(tr.size() == 4 && tr.get(0).startsWith("NOTICE") && tr.get(1).startsWith("HUFF") && tr.get(2).startsWith("CHARGE") && tr.get(3).startsWith("LEAVE"),
                (bluff ? "bluff" : "contact") + " sequence is notice, huff, charge, leave: " + all);
        double notice = dist(tr.get(0)), leave = dist(tr.get(3));
        h.assertTrue(notice <= BearContact.SURPRISED_M, "it noticed inside 25 m: " + notice);
        if (bluff) h.assertTrue(leave >= BearContact.STOP_MIN_M - 0.6 && leave <= BearContact.STOP_MAX_M + 0.6, "a bluff stops 3-10 m short, stopped at " + leave);
        else h.assertTrue(leave <= BearContact.CONTACT_M + 1.0, "contact reaches the player, stopped at " + leave);
        var keys = HudBus.get().journal(r.p).stream().map(l -> l.key()).toList();
        h.assertTrue(keys.contains("messenger.yearofice.alert.bear_noticed"), "the sighting line was posted: " + keys);
        h.assertTrue(keys.contains(bluff ? "messenger.yearofice.nearmiss.bear_bluff" : "messenger.yearofice.nearmiss.bear_contact"), "the outcome line was posted: " + keys);
    }

    static double dist(String entry) { return Double.parseDouble(entry.substring(entry.indexOf('@') + 1)); }

    static void finish(GameTestHelper h, BearContactSetup s) {
        for (Run r : new Run[] {s.bluff, s.contact}) {
            ThreatSources.removeForTest(r.p);
            r.bear.discard();
        }
        BigGameGameTests.strip(h, -5, 40, 35, false, s.zOff);
        EcoGameTests.force(h, h.absolutePos(new BlockPos(1 + 18, BigGameGameTests.Y, 1 + s.zOff)).getCenter(), 3, false);
    }
}

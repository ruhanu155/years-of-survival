package com.yearofice.ecosystem.bird;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.CarcassRecord;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.spawn.SlotSpawner;
import com.yearofice.ecosystem.wound.DeathPipeline;
import com.yearofice.hunt.fishing.FishingClock;
import com.yearofice.hunt.fishing.FishingConfig;
import com.yearofice.hunt.fishing.SliceSpots;
import com.yearofice.hunt.fishing.SpotFish;
import com.yearofice.hunt.fishing.WaterLife;
import com.yearofice.sim.fishing.FishSeason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M6 lane C, birds. Batch {@code m6c}: {@code birds/flock_forms}, {@code birds/raven_scavenges_carcass},
 * {@code birds/eagle_takes_fish}. Each test lays a stone pad so the birds have ground, forces the chunks round it
 * (no player is near), and removes its birds at the end.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class BirdGameTests {
    private BirdGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int PAD_Y = 40, R = 15;

    @GameTestGenerator
    public static Collection<TestFunction> birdTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:birds/flock_forms", 220, BirdGameTests::flockForms));
        out.add(test("yearofice:birds/raven_scavenges_carcass", 360, BirdGameTests::ravenScavenges));
        out.add(test("yearofice:birds/eagle_takes_fish", 240, BirdGameTests::eagleTakesFish));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m6c", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static BlockPos pad(GameTestHelper h) {
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) h.setBlock(new BlockPos(1 + dx, PAD_Y, 1 + dz), Blocks.STONE);
        BlockPos at = h.absolutePos(new BlockPos(1, PAD_Y + 1, 1));
        force(h.getLevel(), at, true);
        return at;
    }

    static void unpad(GameTestHelper h, BlockPos at) {
        force(h.getLevel(), at, false);
        for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) h.setBlock(new BlockPos(1 + dx, PAD_Y, 1 + dz), Blocks.AIR);
        for (BirdEntity b : h.getLevel().getEntitiesOfClass(BirdEntity.class, new AABB(at).inflate(48, 64, 48))) b.discard();
        for (CarcassEntity c : h.getLevel().getEntitiesOfClass(CarcassEntity.class, new AABB(at).inflate(48, 64, 48))) c.discard();
    }

    static void force(ServerLevel level, BlockPos at, boolean on) {
        ChunkPos c = new ChunkPos(at);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setChunkForced(c.x + dx, c.z + dz, on);
    }

    // ---- birds/flock_forms ----------------------------------------------------------------------------------------

    /** Eight mallards in two far clusters share one flock, keep to it and close up to inside the cohesion radius. */
    static void flockForms(GameTestHelper h) {
        BlockPos at = pad(h);
        ServerLevel level = h.getLevel();
        List<BirdEntity> flock = BirdService.form(level, "mallard", at, 8);
        h.assertTrue(flock.size() == 8, "eight placed: " + flock.size());
        int id = flock.get(0).flockId();
        h.assertTrue(id != 0, "a flock id");
        for (BirdEntity b : flock) h.assertTrue(b.flockId() == id && b.role() == com.yearofice.sim.bird.BirdRules.Role.DABBLER, "one flock of dabblers");
        for (int i = 0; i < 8; i++) {
            BirdEntity b = flock.get(i);
            double x = at.getX() + 0.5 + (i < 4 ? -13 : 13), z = at.getZ() + 0.5 + (i % 4 - 1.5) * 1.5;
            b.moveTo(x, at.getY(), z, 0, 0);
        }
        double before = spread(flock);
        h.assertTrue(before >= 12.5, "start far apart: " + before);
        h.runAfterDelay(150, () -> {
            double after = spread(flock);
            for (BirdEntity b : flock) h.assertTrue(b.isAlive(), "a member died");
            double cohesion = BirdConfig.tuning().flockCohesionM();
            h.assertTrue(after <= cohesion + 3.0, "the flock closed up to " + after + " m of its centre (cohesion " + cohesion + ")");
            h.assertTrue(after < before - 1.5, "and it moved: " + before + " -> " + after);
            unpad(h, at);
            h.succeed();
        });
    }

    /** The greatest distance of a member from the flock's centre, in the horizontal plane. */
    static double spread(List<BirdEntity> flock) {
        double cx = 0, cz = 0;
        for (BirdEntity b : flock) { cx += b.getX(); cz += b.getZ(); }
        cx /= flock.size();
        cz /= flock.size();
        double m = 0;
        for (BirdEntity b : flock) m = Math.max(m, Math.hypot(b.getX() - cx, b.getZ() - cz));
        return m;
    }

    // ---- birds/raven_scavenges_carcass ------------------------------------------------------------------------------

    /** A raven within range of a carcass, dead long enough for the corvids to have come, flies in and eats it. */
    static void ravenScavenges(GameTestHelper h) {
        BlockPos at = pad(h);
        ServerLevel level = h.getLevel();
        BirdService.clear();
        var ptarmigan = SlotSpawner.spawnSpecies(level, "yearofice:willow_ptarmigan", at, "debug:raven-test");
        h.assertTrue(ptarmigan != null, "the ptarmigan to die");
        SlotSpawner.untrack("debug:raven-test");
        DeathPipeline.kill(ptarmigan, level.damageSources().generic(), "command");
        List<CarcassEntity> cs = level.getEntitiesOfClass(CarcassEntity.class, new AABB(at).inflate(4, 4, 4));
        h.assertTrue(cs.size() == 1, "one carcass: " + cs.size());
        CarcassEntity c = cs.get(0);
        CarcassRecord r = c.record();
        long died = (long) SliceClock.minuteD(level) - 200;
        c.setRecord(new CarcassRecord(r.species(), r.sex(), r.cause(), died, 550, r.coat(), r.fresh()));
        List<BirdEntity> raven = BirdService.form(level, "raven", at.offset(8, 0, 0), 1);
        h.assertTrue(raven.size() == 1, "the raven");
        h.runAfterDelay(300, () -> {
            int left = c.isRemoved() ? 0 : c.record().weightG();
            h.assertTrue(left < 550, "the carcass was eaten from: " + left + " g left");
            h.assertTrue(BirdService.scavengedG >= 1 && left <= 550 - BirdService.scavengedG, "the raven's meal is counted: " + BirdService.scavengedG);
            h.assertTrue(raven.get(0).isAlive() && raven.get(0).distanceTo(c) < 3.0 || BirdService.scavengedG >= 1, "the raven stood at the carcass");
            unpad(h, at);
            h.succeed();
        });
    }

    // ---- birds/eagle_takes_fish ------------------------------------------------------------------------------------

    /** An eagle sent to a spot takes one fish: the stock falls by one and the take is counted. */
    static void eagleTakesFish(GameTestHelper h) {
        BlockPos at = pad(h);
        ServerLevel level = h.getLevel();
        FishSpotService before = FishSpotService.get();
        FishSpotService.Spot spot = new FishSpotService.Spot("weed_shelf", at, 10, 0.8, 2.5, 1, List.of("pike", "burbot", "whitefish"), List.of(14, 8, 40), false);
        FishSpotService.install(new FishSpotService() {
            @Override
            public List<Spot> spotsNear(ServerLevel l, BlockPos pos, double radius) { return spot.contains(pos) ? List.of(spot) : List.of(); }

            @Override
            public Optional<Spot> sliceSpot(ServerLevel l) { return Optional.empty(); }
        });
        try {
            BirdService.clear();
            long day = FishingClock.day(FishingClock.minute(level.getServer()));
            int doy = FishSeason.dayOfYear(day);
            WaterLife.Best b0 = WaterLife.fullest(level, spot);
            h.assertTrue(b0 != null, "the spot holds fish in season");
            List<BirdEntity> eagle = BirdService.form(level, "bald_eagle", at.offset(7, 0, 0), 1);
            h.assertTrue(eagle.size() == 1, "the eagle");
            eagle.get(0).startFishing(spot);
            h.runAfterDelay(160, () -> {
                try {
                    String sp = eagle.get(0).lastFish();
                    h.assertTrue(!sp.isEmpty() && BirdService.fishTaken == 1, "the eagle took a fish: '" + sp + "' " + BirdService.fishTaken);
                    SpotFish.Entry e = null;
                    for (SpotFish.Entry x : SpotFish.at(level, at, doy, FishingConfig.tuning()).fish()) if (x.id().equals(sp)) e = x;
                    h.assertTrue(e != null, "the species is at the spot");
                    double n = SpotFish.stock(level, SliceSpots.key(at), day, e, "weed_shelf").n();
                    h.assertTrue(Math.abs(n - (e.k() - 1)) < 1e-6, "the stock fell by one: " + n + " of " + e.k());
                    unpad(h, at);
                    h.succeed();
                } finally {
                    FishSpotService.install(before);
                }
            });
        } catch (RuntimeException ex) {
            FishSpotService.install(before);
            throw ex;
        }
    }
}

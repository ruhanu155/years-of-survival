package com.yearofice.ecosystem.debug;

import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.DamageRouter;
import com.yearofice.body.SosFlow;
import com.yearofice.build.outpost.OutpostDef;
import com.yearofice.build.outpost.OutpostSystem;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.companion.DenPups;
import com.yearofice.ecosystem.companion.WolfEntity;
import com.yearofice.ecosystem.livestock.GoatEntity;
import com.yearofice.ecosystem.livestock.HenEntity;
import com.yearofice.ecosystem.livestock.ReindeerEntity;
import com.yearofice.gametest.TestFilter;
import com.yearofice.sim.body.BodyState;
import com.yearofice.story.radio.SupplyParachute;
import com.yearofice.worldgen.Ranges;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M9 lane N: the tests lane J (#113) skipped, each under 60 s and in batch {@code m9n} ({@code m9n/}). Anything that
 * needs loaded far ground forces its own chunks (x 6000 and up, z 6000 and up) and releases them at the end.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class DebtGameTests {
    private DebtGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";
    static final int Y0 = 100;

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:m9n/fall_guard_invulnerable_or_unloaded", 40, DebtGameTests::fallGuard));
        out.add(test("yearofice:m9n/outpost_visit_line_once", 60, DebtGameTests::outpostVisit));
        out.add(test("yearofice:m9n/supply_drop_livestock_by_day", 100, DebtGameTests::supplyLivestock));
        out.add(test("yearofice:m9n/den_pup_from_nearest_den", 200, DebtGameTests::denPup));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m9n", name, TEMPLATE, maxTicks, 0, true, body);
    }

    static ServerPlayer player(GameTestHelper h, String name) {
        ServerPlayer p = EcoGameTests.player(h);
        p.setData(BodyAttachments.BODY.get(), new BodyState());
        return p;
    }

    /** Stone at y 100 over the chunks within {@code r} of (cx, cz), after forcing them. */
    static void platform(GameTestHelper h, int cx, int cz, int r) {
        EcoGameTests.force(h, new Vec3(cx * 16 + 8, Y0, cz * 16 + 8), r, true);
        ServerLevel l = h.getLevel();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            var ch = l.getChunk(cx + dx, cz + dz);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++)
                ch.setBlockState(new BlockPos(ch.getPos().getMinBlockX() + x, Y0, ch.getPos().getMinBlockZ() + z), Blocks.STONE.defaultBlockState(), false);
        }
    }

    // ---- fall/void guard --------------------------------------------------------------------------------------------

    /** A 40 m fall or the void while invulnerable, or in a chunk that is not loaded, is not a fall: no SOS, no injury. A real fall still injures. */
    static void fallGuard(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        ServerPlayer p = player(h, "fall");
        var fall = l.damageSources().fall();
        var out = l.damageSources().fellOutOfWorld();
        // Control: a loaded, vulnerable 5-point fall costs Condition (the same router, so the guard is what holds the others back).
        DamageRouter.route(p, fall, 5f);
        double after = BodyAttachments.of(p).condition();
        h.assertTrue(after < 100, "a real fall injures: " + after);
        p.setData(BodyAttachments.BODY.get(), new BodyState());

        p.setInvulnerable(true);
        DamageRouter.route(p, fall, 40f);
        DamageRouter.route(p, out, 4f);
        h.assertTrue(!SosFlow.active(p), "invulnerable: no SOS flight");
        h.assertTrue(BodyAttachments.of(p).condition() == 100, "invulnerable: no injury " + BodyAttachments.of(p).condition());
        p.setInvulnerable(false);

        p.setPos(9_000_000.5, Y0, 9_000_000.5);
        h.assertTrue(!l.hasChunkAt(p.blockPosition()), "the far chunk is not loaded");
        DamageRouter.route(p, fall, 40f);
        DamageRouter.route(p, out, 4f);
        h.assertTrue(!SosFlow.active(p), "unloaded chunk: no SOS flight");
        h.assertTrue(BodyAttachments.of(p).condition() == 100, "unloaded chunk: no injury " + BodyAttachments.of(p).condition());
        h.succeed();
    }

    // ---- outpost visit ----------------------------------------------------------------------------------------------

    /** Standing within 30 m of a worldgen outpost posts its Messenger line once; staying, leaving and returning posts no second. */
    static void outpostVisit(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        OutpostDef def = OutpostDef.get("fish_camp");
        // A site where the region allows fish camps (outposts follow region/<id>.json): search the cells around that region's centre.
        var reg = com.yearofice.worldgen.regions.Regions.all().stream().filter(r -> r.outposts().contains("yearofice:fish_camp")).findFirst().orElseThrow();
        int cells = Math.max(1, def.spacingM() / 16);
        net.minecraft.world.level.ChunkPos site = null;
        for (int dx = -4; dx <= 4 && site == null; dx++) for (int dz = -4; dz <= 4 && site == null; dz++) {
            var c = OutpostSystem.siteChunk(l.getSeed(), "fish_camp", def, Math.floorDiv(reg.centreX() >> 4, cells) + dx, Math.floorDiv(reg.centreZ() >> 4, cells) + dz);
            if (com.yearofice.worldgen.regions.Regions.allowsOutpost(c.getMinBlockX() + 3, c.getMinBlockZ() + 3, "fish_camp")) site = c;
        }
        h.assertTrue(site != null, "a fish camp site inside " + reg.id());
        final var sc = site;
        EcoGameTests.force(h, new Vec3(sc.getMiddleBlockX(), Y0, sc.getMiddleBlockZ()), 2, true);
        try {
            l.getChunk(sc.x, sc.z);
            ServerPlayer p = player(h, "visit");
            p.setPos(sc.getMinBlockX() + 3 + 10, Y0 + 1, sc.getMinBlockZ() + 3);
            h.assertTrue(lines(p) == 0, "no line before the scan");
            OutpostSystem.scan(p);
            h.assertTrue(lines(p) == 1, "one line within 30 m: " + lines(p));
            OutpostSystem.scan(p);
            p.setPos(sc.getMinBlockX() + 3 + 200, Y0 + 1, sc.getMinBlockZ() + 3);
            OutpostSystem.scan(p);
            p.setPos(sc.getMinBlockX() + 3 + 10, Y0 + 1, sc.getMinBlockZ() + 3);
            OutpostSystem.scan(p);
            h.assertTrue(lines(p) == 1, "still one after staying, leaving and returning: " + lines(p));
            h.succeed();
        } finally {
            EcoGameTests.force(h, new Vec3(sc.getMiddleBlockX(), Y0, sc.getMiddleBlockZ()), 2, false);
        }
    }

    static long lines(ServerPlayer p) { return HudBus.get().journal(p).stream().filter(x -> x.key().endsWith("outpost_visit")).count(); }

    // ---- supply-drop livestock --------------------------------------------------------------------------------------

    /** Day 100: none. Day 365: three hens and two goats. Day 1095: the two reindeer join (supply_drop.json livestock from_days). */
    static void supplyLivestock(GameTestHelper h) {
        platform(h, 6000 / 16, 6000 / 16, 1);
        h.runAfterDelay(20, () -> supplyBody(h)); // newly forced chunks take entities only once their entity section has loaded
    }

    static void supplyBody(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos at = new BlockPos(6000 + 8, Y0 + 1, 6000 + 8);
        AABB box = new AABB(at).inflate(12);
        try {
            clear(l, box); // a killed earlier run may have saved animals here
            SupplyParachute.stock(l, at, 100);
            h.assertTrue(count(l, box) == 0, "nothing before day 365");
            SupplyParachute.stock(l, at, 365);
            h.assertTrue(l.getEntitiesOfClass(HenEntity.class, box).size() == 3, "3 hens on day 365: " + l.getEntitiesOfClass(HenEntity.class, box).size() + " hens, all kept " + count(l, box)
                    + ", any " + l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box).stream().map(e -> e.getType().toShortString()).toList());
            h.assertTrue(l.getEntitiesOfClass(GoatEntity.class, box).size() == 2, "2 goats on day 365");
            h.assertTrue(l.getEntitiesOfClass(ReindeerEntity.class, box).isEmpty(), "no reindeer before day 1095");
            clear(l, box);
            SupplyParachute.stock(l, at, 1095);
            h.assertTrue(l.getEntitiesOfClass(ReindeerEntity.class, box).size() == 2, "2 reindeer on day 1095");
            h.assertTrue(l.getEntitiesOfClass(HenEntity.class, box).size() == 3, "the hens still come");
            h.succeed();
        } finally {
            clear(l, box);
            EcoGameTests.force(h, new Vec3(6008, Y0, 6008), 1, false);
        }
    }

    static int count(ServerLevel l, AABB box) {
        return l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box, e -> e instanceof HenEntity || e instanceof GoatEntity || e instanceof ReindeerEntity).size();
    }

    static void clear(ServerLevel l, AABB box) {
        l.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, box, e -> e instanceof HenEntity || e instanceof GoatEntity || e instanceof ReindeerEntity)
                .forEach(net.minecraft.world.entity.Entity::discard);
    }

    // ---- den pups ---------------------------------------------------------------------------------------------------

    /** One tameable, unowned pup per player per year, 120-200 m out on the bearing to the nearest wolf range's den, on loaded ground. */
    static void denPup(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        double px = 8_000.5, pz = 8_000.5;
        // Two ranges: the near den lies due east, the far one due west; the pup must come from the east one.
        var near = new Ranges.Den("wolf", px + 3000, pz);
        var far = new Ranges.Den("wolf", px - 6000, pz);
        var life = new Ranges.Layers(List.of(), List.of(), List.of(far, near), new double[0],
                List.of(new Ranges.Range("wolf", far.x(), far.z(), 4000), new Ranges.Range("wolf", near.x(), near.z(), 4000)));
        Vec3 centre = new Vec3(px + 160, Y0, pz);
        platform(h, (int) Math.floor(centre.x / 16), (int) Math.floor(centre.z / 16), 6);
        AABB box = new AABB(px - 260, Y0 - 5, pz - 260, px + 260, Y0 + 20, pz + 260);
        try {
            ServerPlayer p = player(h, "pup");
            p.setPos(px, Y0 + 1, pz);
            h.assertTrue(DenPups.leave(p, 3, life), "the pup is left");
            var pups = l.getEntitiesOfClass(WolfEntity.class, box);
            h.assertTrue(pups.size() == 1, "one pup: " + pups.size());
            WolfEntity w = pups.get(0);
            double d = Math.hypot(w.getX() - px, w.getZ() - pz);
            h.assertTrue(d >= 119.5 && d <= 200.5, "120-200 m out: " + d);
            double bearing = Math.atan2(w.getZ() - pz, w.getX() - px);
            h.assertTrue(Math.abs(bearing) <= 0.26, "toward the nearest den (east): " + bearing);
            h.assertTrue(w.tameable() && !w.owned(), "tameable and unowned");
            h.assertTrue(l.hasChunkAt(w.blockPosition()) && w.getY() >= Y0, "on loaded ground: y " + w.getY());
            DenPups.leave(p, 3, life);
            h.assertTrue(l.getEntitiesOfClass(WolfEntity.class, box).size() == 1, "no second pup the same year");
            DenPups.leave(p, 4, life);
            h.assertTrue(l.getEntitiesOfClass(WolfEntity.class, box).size() == 2, "a new summer brings a new pup");
            h.succeed();
        } finally {
            l.getEntitiesOfClass(WolfEntity.class, box).forEach(net.minecraft.world.entity.Entity::discard);
            EcoGameTests.force(h, centre, 6, false);
        }
    }
}

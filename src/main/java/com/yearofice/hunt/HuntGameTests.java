package com.yearofice.hunt;

import com.yearofice.api.event.CallSoundedEvent;
import com.yearofice.api.event.EncounterStateEvent;
import com.yearofice.api.hunt.ConcealmentService;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.hunt.danger.Encounters;
import com.yearofice.hunt.wound.WoundTracker;
import com.yearofice.sim.ecosystem.Awareness;
import com.yearofice.sim.hunt.Ballistics;
import com.yearofice.sim.hunt.Calls;
import com.yearofice.sim.hunt.EncounterRules;
import com.yearofice.sim.hunt.HuntData;
import com.yearofice.sim.hunt.Shot;
import com.yearofice.sim.hunt.WoundModel;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * M6 lane B GameTests: arrow drop and wind, the lung shot that bleeds, the blood trail along a wound's path, a call
 * raising an answer's odds, a blind lowering what animals see and the bear's charge posts. All on Standard.
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class HuntGameTests {
    private HuntGameTests() {}

    static final String TEMPLATE = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> huntTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:hunt/arrow_drop_and_wind", 60, HuntGameTests::arrowDropAndWind));
        out.add(test("yearofice:hunt/hitzone_lung_bleeds", 80, HuntGameTests::hitzoneLungBleeds));
        out.add(test("yearofice:hunt/blood_trail_follows_wound", 60, HuntGameTests::bloodTrailFollowsWound));
        out.add(test("yearofice:hunt/call_raises_approach", 40, HuntGameTests::callRaisesApproach));
        out.add(test("yearofice:hunt/blind_reduces_detection", 40, HuntGameTests::blindReducesDetection));
        out.add(test("yearofice:hunt/bear_charge_posts_state", 40, HuntGameTests::bearChargePosts));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    static TestFunction test(String name, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction("m2_5", name, TEMPLATE, maxTicks, 0, true, body);
    }

    /** A stone floor at y 0 under x 0..{@code len}, z 0..4, outside the template where needed. */
    static void floor(GameTestHelper h, int len) {
        for (int x = 0; x <= len; x++) for (int z = 0; z <= 4; z++) h.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
    }

    // ---- hunt/arrow_drop_and_wind -------------------------------------------------------------------------------

    /** A 50 m/s arrow drops about 1.9 m in 30 m, drifts with a crosswind and does not without one; the entity matches the model. */
    static void arrowDropAndWind(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        Vec3 from = h.absoluteVec(new Vec3(0.5, 10, 0.5));
        Vec3 from2 = from.add(0, 0, 2);
        var bow = HuntData.weapon("longbow");
        ArrowEntity calm = new ArrowEntity(HuntRegistry.ARROW_ENTITY.get(), level).withWind(0, 0);
        ArrowEntity windy = new ArrowEntity(HuntRegistry.ARROW_ENTITY.get(), level).withWind(0, 6);
        calm.launch(null, from, new Vec3(1, 0, 0), bow.arrowSpeedMps(), bow.fullEnergyJ(), bow, "knapped_stone");
        windy.launch(null, from2, new Vec3(1, 0, 0), bow.arrowSpeedMps(), bow.fullEnergyJ(), bow, "knapped_stone");
        level.addFreshEntity(calm);
        level.addFreshEntity(windy);
        double[] want = Ballistics.dropAndDrift(bow.arrowSpeedMps(), 30, 0, com.yearofice.sim.hunt.HuntTuning.standard());
        double[] seen = new double[4];
        for (int t = 1; t <= 40; t++) {
            h.runAfterDelay(t, () -> {
                if (calm.getX() - from.x >= 30 && seen[0] == 0) {
                    seen[0] = from.y - calm.getY();
                    seen[1] = calm.getZ() - from.z;
                    seen[2] = from2.y - windy.getY();
                    seen[3] = windy.getZ() - from2.z;
                }
            });
        }
        h.runAfterDelay(30, () -> {
            h.assertTrue(seen[0] > 1.0 && seen[0] < 2.8, "drop in 30 m should be about 1.9 m, was " + seen[0]);
            h.assertTrue(Math.abs(seen[0] - want[0]) < 0.45, "entity " + seen[0] + " vs model " + want[0]);
            h.assertTrue(Math.abs(seen[1]) < 1e-6, "no wind, no drift: " + seen[1]);
            h.assertTrue(seen[3] > 0.04 && seen[3] < 0.8, "a 6 m/s crosswind drifts the shaft sideways, was " + seen[3]);
            calm.discard();
            windy.discard();
            h.succeed();
        });
    }

    // ---- hunt/hitzone_lung_bleeds -------------------------------------------------------------------------------

    /** A broadside lung shot on a moose at 1.3 m with a flint head kills through the vitals channel: it bleeds, runs and drops, laying 3 drops a metre. */
    static void hitzoneLungBleeds(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h, 4);
        var moose = h.spawn(EcosystemRegistry.MOOSE.get(), new BlockPos(2, 1, 1));
        moose.setNoAi(true);
        moose.setYBodyRot(0);
        moose.setYRot(0);
        Vec3 m = moose.position();
        var bow = HuntData.weapon("longbow");
        double drop = Ballistics.dropAndDrift(bow.arrowSpeedMps(), 1.3, 0, com.yearofice.sim.hunt.HuntTuning.standard())[0];
        ArrowEntity arrow = new ArrowEntity(HuntRegistry.ARROW_ENTITY.get(), level).withWind(0, 0);
        arrow.launch(null, new Vec3(m.x - 1.4, m.y + 1.3 + drop, m.z + 0.45), new Vec3(1, 0, 0), bow.arrowSpeedMps(), bow.fullEnergyJ(), bow, "knapped_stone");
        level.addFreshEntity(arrow);
        h.runAfterDelay(12, () -> {
            Shot.Outcome o = HuntDebug.lastOutcome();
            h.assertTrue(o != null && o.zone().equals("heart_lungs"), "the arrow found the lungs: " + (o == null ? "no shot; arrow removed=" + arrow.isRemoved() + " at " + arrow.position() + " vel " + arrow.velocity() + " moose " + moose.getBoundingBox() + " last " + HuntDebug.last(null) : o));
            h.assertTrue(o.result() == Shot.Result.KILL && o.channel() == Shot.Channel.VITALS, "a lung hit: " + o);
            var s = WoundTracker.of(moose);
            h.assertTrue(s != null, "the moose carries a wound");
            h.assertTrue(s.wound.bleedPerMin() > 1.0, "bleeds out in under a minute: " + s.wound.bleedPerMin());
            h.assertTrue(s.wound.dropsPer10m() == 30, "30 drops per 10 m");
            s.driven = false;
            s.place = false;
            double x = m.x;
            for (int i = 0; i < 2000 && !moose.isRemoved(); i++) {
                x += 0.5;
                WoundTracker.advance(level, moose, s, new Vec3(x, m.y, m.z), 0);
            }
            h.assertTrue(moose.isRemoved(), "it drops after its run of " + s.runTargetM + " m");
            h.assertTrue(s.travelledM >= 50 && s.travelledM <= 150.6, "ran 50-150 m: " + s.travelledM);
            h.assertTrue(Math.abs(s.dropsPlaced - 3 * s.travelledM) < 1.6, "3 drops a metre: " + s.dropsPlaced + " over " + s.travelledM + " m");
            h.succeed();
        });
    }

    // ---- hunt/blood_trail_follows_wound -------------------------------------------------------------------------

    /** Blood lies along the walked path at the wound's rate, a leg wound's trail ends at 100 m, a pushed animal's thins by half and tracking reads the next patch. */
    static void bloodTrailFollowsWound(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        floor(h, 12);
        BlockPos start = h.absolutePos(new BlockPos(1, 1, 2));
        Pig pig = EntityType.PIG.create(level);
        pig.setNoAi(true);
        pig.moveTo(start.getX(), start.getY(), start.getZ() + 0.5);
        level.addFreshEntity(pig);
        var lung = WoundModel.open(Shot.Channel.VITALS, 0, 1, 0.5);
        var s = WoundTracker.wound(level, pig, lung, null, "knapped_stone", 0.5, false);
        for (int i = 0; i < 8; i++) { // 2 m in quarter-metre steps, inside the test's own blocks
            Vec3 now = new Vec3(start.getX() + (i + 1) * 0.25, start.getY(), start.getZ() + 0.5);
            pig.setPos(now);
            WoundTracker.advance(level, pig, s, now, 0);
        }
        h.assertTrue(Math.abs(s.dropsPlaced - 6) <= 1, "the lungs lay 3 drops a metre: " + s.dropsPlaced);
        int[] cnt = BloodTrail.count(level, start.offset(1, 0, 0), 3);
        h.assertTrue(cnt[0] >= 2 && cnt[1] >= s.dropsPlaced - 1.5 && cnt[1] <= s.dropsPlaced, "drops land on patches along the path, thickening to 3 where they pile up (the cap): " + cnt[0] + " patches, " + cnt[1] + " drops");
        BlockPos first = BloodTrail.next(level, start, 3);
        h.assertTrue(first != null, "tracking finds a patch");
        BlockPos after = BloodTrail.next(level, first.east(2), 3);
        h.assertTrue(after != null && Math.abs(after.getZ() - first.getZ()) <= 1, "the patches lie along the line walked");
        h.assertTrue(level.getBlockState(first).getValue(BloodTrailBlock.KIND) == BloodTrailBlock.Kind.BRIGHT, "lung blood is bright");

        // A leg wound: 5 drops per 10 m for the first 100 m, then none.
        Pig leg = EntityType.PIG.create(level);
        leg.setNoAi(true);
        leg.moveTo(start.getX() + 0.5, start.getY() + 20, start.getZ() + 0.5);
        level.addFreshEntity(leg);
        var ls = WoundTracker.wound(level, leg, WoundModel.open(Shot.Channel.LEG, 0, 1, 0.5), null, "knapped_stone", 0.5, false);
        ls.place = false;
        double at100 = 0;
        for (int i = 1; i <= 130; i++) {
            Vec3 now = new Vec3(start.getX() + 0.5 + i, start.getY() + 20, start.getZ() + 0.5);
            WoundTracker.advance(level, leg, ls, now, 0);
            if (i == 100) at100 = ls.dropsPlaced;
        }
        h.assertTrue(Math.abs(at100 - 50) <= 1, "5 per 10 m for 100 m: " + at100);
        h.assertTrue(ls.dropsPlaced == at100, "none past 100 m: " + ls.dropsPlaced);

        // Pushing a gut-shot animal halves the trail and adds two hours to the death timer.
        Pig gut = EntityType.PIG.create(level);
        gut.setNoAi(true);
        gut.moveTo(start.getX() + 0.5, start.getY() + 40, start.getZ() + 0.5);
        level.addFreshEntity(gut);
        var gs = WoundTracker.wound(level, gut, WoundModel.open(Shot.Channel.GUT, 0, 1, 0.0), null, "knapped_stone", 0.5, false);
        gs.place = false;
        double die = gs.wound.dieAfterMin();
        for (int i = 1; i <= 100; i++) WoundTracker.advance(level, gut, gs, new Vec3(start.getX() + 0.5 + i, start.getY() + 40, start.getZ() + 0.5), 0);
        double before = gs.dropsPlaced;
        WoundTracker.push(level, gut, gs, 0.5);
        h.assertTrue(gs.wound.dieAfterMin() == die + 120, "death timer +2 h: " + gs.wound.dieAfterMin());
        h.assertTrue(gs.runTargetM >= 300 && gs.runTargetM <= 800, "runs 300-800 m again: " + gs.runTargetM);
        Vec3 g0 = gs.last;
        for (int i = 1; i <= 100; i++) WoundTracker.advance(level, gut, gs, g0.add(i, 0, 0), 0);
        h.assertTrue(Math.abs(before - 30) <= 1 && Math.abs((gs.dropsPlaced - before) - 15) <= 1, "30 drops per 100 m, then 15 after the push: " + before + ", " + (gs.dropsPlaced - before));
        pig.discard();
        leg.discard();
        gut.discard();
        h.succeed();
    }

    // ---- hunt/call_raises_approach ------------------------------------------------------------------------------

    static void callRaisesApproach(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = com.yearofice.ecosystem.debug.EcoGameTests.player(h);
        p.moveTo(h.absoluteVec(new Vec3(1.5, 1, 1.5)));
        Collector listener = new Collector();
        List<CallSoundedEvent> seen = listener.calls;
        NeoForge.EVENT_BUS.register(listener);
        try {
            CallSoundedEvent ev = CallItem.sound(level, p, "moose_call", 3.0);
            h.assertTrue(seen.size() == 1 && seen.get(0) == ev, "the call posts one event");
            h.assertTrue(ev.sound().equals("cow_moan") && ev.baseP().containsKey("yearofice:moose"), "a 3 s hold is the cow moan for moose: " + ev.sound());
            // the listener stands downwind, so the wind (random in the world) cannot shorten R_eff below the 400 m
            Vec3 listenerPos = ev.windMs() < 1.0 ? ev.origin().add(400, 0, 0) : ev.origin().add(400 * ev.windToX(), 0, 400 * ev.windToZ());
            double rut = Calls.rut(9, 20), off = Calls.rut(7, 20);
            double p400 = ev.pRespond("yearofice:moose", listenerPos, rut, 1, false, 1);
            double expect = 0.4 * ev.quality() * (1 - 400 / ev.reffAt(listenerPos));
            h.assertTrue(p400 > 0.1 && Math.abs(p400 - expect) < 1e-9, "P = 0.4 q (1 - d/R) at 400 m, q " + ev.quality() + ": " + p400 + " vs " + expect);
            h.assertTrue(ev.pRespond("yearofice:moose", listenerPos, off, 1, false, 1) == 0, "no answer out of the rut");
            h.assertTrue(ev.pRespond("yearofice:moose", listenerPos.add(1000, 0, 0), rut, 1, false, 1) == 0, "none past 1.5 x the range");
            h.assertTrue(ev.pRespond("yearofice:moose", listenerPos, rut, 1, true, 1) < p400 * 0.31, "a call-shy bull answers at 0.3");
            h.assertTrue(ev.pRespond("yearofice:wolverine", listenerPos, rut, 1, false, 1) == 0, "a moose call draws only the moose");

            // Wind: with the air moving toward +x, a listener downwind hears farther than one upwind.
            CallSoundedEvent windy = new CallSoundedEvent(level, p, Vec3.ZERO, "moose_call", "cow_moan", 1.0, 800, false, ev.baseP(), 1, 0, 5);
            double down = windy.pRespond("yearofice:moose", new Vec3(700, 0, 0), 1, 1, false, 1);
            double up = windy.pRespond("yearofice:moose", new Vec3(-700, 0, 0), 1, 1, false, 1);
            h.assertTrue(down > 0.1 && up == 0, "downwind 700 m answers (" + down + "), upwind does not (" + up + ")");

            // The squeak is flat inside its range.
            CallSoundedEvent sq = CallItem.sound(level, p, "squeak_call", 2.5);
            Vec3 dir = sq.windMs() < 1.0 ? new Vec3(1, 0, 0) : new Vec3(sq.windToX(), 0, sq.windToZ());
            double near = sq.pRespond("yearofice:red_fox", sq.origin().add(dir.scale(20)), 1, 1, false, 1);
            double far = sq.pRespond("yearofice:red_fox", sq.origin().add(dir.scale(250)), 1, 1, false, 1);
            h.assertTrue(near > 0 && Math.abs(near - far) < 1e-9, "flat falloff: " + near + " vs " + far);
            h.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
    }

    // ---- hunt/blind_reduces_detection ---------------------------------------------------------------------------

    static void blindReducesDetection(GameTestHelper h) {
        try { blindBody(h); } finally { HuntConcealment.snowOverrideCm = null; }
    }

    static void blindBody(GameTestHelper h) {
        ServerLevel level = h.getLevel();
        ServerPlayer p = com.yearofice.ecosystem.debug.EcoGameTests.player(h);
        BlockPos open = h.absolutePos(new BlockPos(0, 1, 2));
        BlockPos blind = h.absolutePos(new BlockPos(1, 1, 0));
        level.setBlockAndUpdate(blind, HuntRegistry.POLE_BLIND.get().defaultBlockState().setValue(HuntBlocks.PoleBlind.FRESH, false));
        var cs = ConcealmentService.get();
        HuntConcealment.snowOverrideCm = 0.0;
        p.moveTo(Vec3.atBottomCenterOf(open));
        h.assertTrue(cs.effectFor(level, p, 20, true, true, false).sightX() == 1.0, "no blind, no cover");
        p.moveTo(Vec3.atBottomCenterOf(blind.offset(0, 0, 1)));
        var in = cs.effectFor(level, p, 20, true, true, false);
        h.assertTrue(in.sightX() < 0.05 && in.noiseX() == 0.5 && in.scentX() == 1.0, "a still player in a blind is unseen, noise x0.5, scent unchanged: " + in);
        double senses = 60;
        double openA = Awareness.gain(0, Awareness.GAIN_SIGHT, Awareness.stimulus(20, senses), com.yearofice.sim.ecosystem.Posture.WALK, 5);
        double blindA = Awareness.gain(0, Awareness.GAIN_SIGHT, Awareness.stimulus(20, senses * in.sightX()), com.yearofice.sim.ecosystem.Posture.WALK, 5);
        h.assertTrue(blindA < openA * 0.1, "five seconds in view: " + openA + " in the open, " + blindA + " in the blind");
        level.setBlockAndUpdate(blind, HuntRegistry.POLE_BLIND.get().defaultBlockState()); // fresh: only halves a moose's sight
        h.assertTrue(cs.effectFor(level, p, 20, true, true, false).sightX() == 0.5, "a fresh blind halves a moose's sight");
        BlockPos stand = h.absolutePos(new BlockPos(2, 1, 2));
        level.setBlockAndUpdate(stand.below(), HuntRegistry.TREE_STAND.get().defaultBlockState());
        p.moveTo(Vec3.atBottomCenterOf(stand));
        var up = cs.effectFor(level, p, 20, true, true, false);
        var bear = cs.effectFor(level, p, 20, true, false, true);
        h.assertTrue(up.sightX() == 0.3 && bear.sightX() == 0.7 && up.scentClearM() == 40 && up.viewX() == 1.5, "a tree stand: prey x0.3, bears x0.7, scent clear 40 m, view x1.5: " + up + " " + bear);
        h.succeed();
    }

    // ---- hunt/bear_charge_posts_state ---------------------------------------------------------------------------

    static void bearChargePosts(GameTestHelper h) {
        ServerPlayer p = com.yearofice.ecosystem.debug.EcoGameTests.player(h);
        Collector l = new Collector();
        l.who = p;
        List<EncounterStateEvent.State> seen = l.states;
        NeoForge.EVENT_BUS.register(l);
        try {
            var bluff = Encounters.bearCharge(p, 12, false, false, EncounterRules.Reaction.STAND_GROUND, 0.0);
            h.assertTrue(bluff == EncounterRules.Charge.BLUFF && seen.equals(List.of(EncounterStateEvent.State.ENDED)), "a bluff stops short: " + seen);
            seen.clear();
            var hit = Encounters.bearCharge(p, 12, true, true, EncounterRules.Reaction.RUN, 0.0);
            h.assertTrue(hit == EncounterRules.Charge.CONTACT && seen.equals(List.of(EncounterStateEvent.State.STRIKE, EncounterStateEvent.State.PINNED)), "running draws a real charge: " + seen);
            seen.clear();
            Encounters.bearCharge(p, 12, true, true, EncounterRules.Reaction.PLAY_DEAD, 0.99);
            h.assertTrue(seen.equals(List.of(EncounterStateEvent.State.STRIKE, EncounterStateEvent.State.PLAY_DEAD)), "playing dead: " + seen);
            h.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(l);
        }
    }

    /** Collects the events the tests post. */
    public static final class Collector {
        final List<CallSoundedEvent> calls = new ArrayList<>();
        final List<EncounterStateEvent.State> states = new ArrayList<>();
        ServerPlayer who;

        @net.neoforged.bus.api.SubscribeEvent
        public void onCall(CallSoundedEvent e) { calls.add(e); }

        @net.neoforged.bus.api.SubscribeEvent
        public void onState(EncounterStateEvent e) { if (e.player() == who) states.add(e.state()); }
    }
}

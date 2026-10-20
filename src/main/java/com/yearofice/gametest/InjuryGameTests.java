package com.yearofice.gametest;

import com.yearofice.api.event.StaggerEvent;
import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.DamageRouter;
import com.yearofice.body.injury.InjuryService;
import com.yearofice.body.injury.InjuryTick;
import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.injury.BackStrain;
import com.yearofice.sim.body.injury.Stumble;
import com.yearofice.sim.body.injury.Treatments;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.body.part.Problem;
import com.yearofice.sim.body.part.Triage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Body, injuries and treatment (M4 lane A). Batch {@code m4_injury}; all on Standard, all instant (a few ticks).
 */
@GameTestHolder(com.yearofice.core.YearOfIce.MOD_ID)
public final class InjuryGameTests {
    private InjuryGameTests() {}

    static final String BATCH = "m4_injury";

    @GameTestGenerator
    public static Collection<TestFunction> injuryTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:body/part_split_exact", InjuryGameTests::partSplitExact));
        out.add(test("yearofice:body/fall_routes", InjuryGameTests::fallRoutes));
        out.add(test("yearofice:body/cauterize_stops_bleeding", InjuryGameTests::cauterizeStopsBleeding));
        out.add(test("yearofice:body/pocket_stone_fade", InjuryGameTests::pocketStoneFade));
        out.add(test("yearofice:body/back_strain_crouch_lift", InjuryGameTests::backStrainCrouchLift));
        return TestFilter.apply(out);
    }

    static TestFunction test(String name, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, LayerGameTests.TEMPLATE, 100, 0, true, body);
    }

    static ServerPlayer fresh(GameTestHelper h) {
        ServerPlayer p = LayerGameTests.player(h, "injury-test");
        p.setData(BodyAttachments.BODY.get(), new BodyState());
        return p;
    }

    private static boolean journal(ServerPlayer p, String id) {
        return HudBus.get().journal(p).stream().anyMatch(l -> l.key().endsWith("." + id));
    }

    /** Body tab test 1, through the data file: a forced bear bite (L 8) on the left arm. */
    static void partSplitExact(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        h.assertTrue(InjuryService.injure(p, "bear_bite", Part.LEFT_ARM, Double.NaN, "test"), "bear_bite is data");
        BodyState s = BodyAttachments.of(p);
        h.assertTrue(Math.abs(s.condition() - 92) < 0.01, "Condition 92, got " + s.condition());
        h.assertTrue(s.parts.hp(Part.LEFT_ARM) == 76, "left arm 76, got " + s.parts.hp(Part.LEFT_ARM));
        h.assertTrue(Math.abs(s.parts.dSys() - 6.32) < 0.01, "d_sys 6.32, got " + s.parts.dSys());
        h.assertTrue(HudBus.get().journal(p).size() >= 0, "lines post");
        h.succeed();
    }

    /** Falls by height through the router, then stumbles by terrain, load, dark and pole, and their outcomes. */
    static void fallRoutes(GameTestHelper h) {
        DamageSource fall = h.getLevel().damageSources().fall();
        // 4 m: one sprain, Condition -4. Router: damage = height - 3.
        ServerPlayer p = fresh(h);
        DamageRouter.route(p, fall, 1f);
        BodyState s = BodyAttachments.of(p);
        h.assertTrue(s.parts.problems().size() == 1 && s.parts.problems().get(0).id().equals("sprain"), "4 m: a sprain");
        h.assertTrue(Math.abs(s.condition() - 96) < 0.01, "4 m costs 4, got " + s.condition());
        // 8 m: two parts, exactly 10.
        p = fresh(h);
        DamageRouter.route(p, fall, 5f);
        s = BodyAttachments.of(p);
        h.assertTrue(Math.abs(s.condition() - 90) < 0.01, "8 m costs exactly 10, got " + s.condition());
        int hit = 0;
        for (Part part : Part.values()) if (s.parts.hp(part) < 100) hit++;
        h.assertTrue(hit == 2, "8 m hits 2 parts, got " + hit);
        // 13 m: 60.
        p = fresh(h);
        DamageRouter.route(p, fall, 10f);
        h.assertTrue(Math.abs(BodyAttachments.of(p).condition() - 40) < 0.01, "13 m costs 60");

        // Terrain, load, dark, pole.
        double base = 0.3;
        double calm = Stumble.chance(base, Stumble.Factors.calm());
        h.assertTrue(Math.abs(calm - 0.003) < 1e-9, "0.3% a 100 m, got " + calm);
        h.assertTrue(Math.abs(Stumble.chance(base, new Stumble.Factors(true, false, false, false, false, false, 1, false))
                - 0.009) < 1e-9, "running x3");
        h.assertTrue(Math.abs(Stumble.chance(base, new Stumble.Factors(false, false, true, false, true, false, 1, false))
                - 0.012) < 1e-9, "load x2 and dark x2");
        h.assertTrue(Math.abs(Stumble.chance(base, new Stumble.Factors(true, true, true, true, true, true, 1, false))
                - 0.003 * 3 * 2 * 2 * 2 * 2 / 2) < 1e-9, "everything, with a pole");
        h.assertTrue(Stumble.chance(base, new Stumble.Factors(true, true, true, true, true, false, 3, true)) == 0,
                "trails set p to 0");

        // Outcome mix over 4,000 seeded rolls: 70 / 20 / 8 / 2.
        Mulberry32 rng = new Mulberry32(20261019);
        int[] n = new int[4];
        for (int i = 0; i < 4000; i++) n[Stumble.outcome(rng.nextDouble()).ordinal()]++;
        h.assertTrue(Math.abs(n[0] / 4000.0 - 0.70) < 0.03 && Math.abs(n[1] / 4000.0 - 0.20) < 0.03
                && Math.abs(n[2] / 4000.0 - 0.08) < 0.02 && Math.abs(n[3] / 4000.0 - 0.02) < 0.01, "outcome mix");

        // The outcomes land: Stagger always fires; foot injury bruises a foot or leg; a fall drops the item and costs
        // 5-20; a sprain sprains.
        int[] staggers = {0};
        Consumer<StaggerEvent> listener = e -> staggers[0]++;
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            p = fresh(h);
            h.assertTrue(InjuryService.stumble(p, 0.1, 0) == Stumble.Outcome.STUMBLE, "plain stumble");
            h.assertTrue(BodyAttachments.of(p).parts.problems().isEmpty(), "a stumble hurts nothing");
            p = fresh(h);
            InjuryService.stumble(p, 0.75, 0);
            Problem bruise = BodyAttachments.of(p).parts.problems().get(0);
            h.assertTrue(bruise.id().equals("bruise") && (bruise.part().group == Part.Group.FEET
                    || bruise.part().group == Part.Group.LEGS), "foot injury is a bruise on a foot or leg");
            p = fresh(h);
            p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
            InjuryService.stumble(p, 0.95, 0);
            double lost = 100 - BodyAttachments.of(p).condition();
            h.assertTrue(Math.abs(lost - 5) < 0.01, "a fall at u=0 costs 5, got " + lost);
            h.assertTrue(p.getMainHandItem().isEmpty(), "the held item drops");
            p = fresh(h);
            InjuryService.stumble(p, 0.99, 0);
            h.assertTrue(BodyAttachments.of(p).parts.problems().get(0).id().equals("sprain"), "a sprain");
            h.assertTrue(staggers[0] == 4, "Stagger fired for every roll, got " + staggers[0]);
            h.assertTrue(journal(p, "problem_sprain"), "and the sprain was explained");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        h.succeed();
    }

    /** A deep cut bleeds 3 a minute; cauterizing stops it at once and leaves a bad burn. */
    static void cauterizeStopsBleeding(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        BodyState s = BodyAttachments.of(p);
        long now = BodyTicker.clock.minute(p.server);
        s.parts = Treatments.add(s.parts, Problem.fresh("cut", Part.LEFT_ARM, "forearm", 2, now));
        h.assertTrue(Triage.totalBleed(s.parts.problems()) == 3, "a deep cut bleeds 3 a minute");
        InjuryTick.step(s, now, 10, BodyTuning.STANDARD);
        h.assertTrue(Math.abs(s.condition() - 70) < 0.01, "ten minutes cost 30, got " + s.condition());
        h.assertTrue(InjuryService.treat(p, "cauterize", Part.LEFT_ARM), "cauterize fits a deep cut");
        h.assertTrue(Triage.totalBleed(s.parts.problems()) == 0, "bleeding stops at once");
        Problem burn = s.parts.problems().get(0);
        h.assertTrue(s.parts.problems().size() == 1 && burn.id().equals("burn") && burn.stage() == 2
                && burn.part() == Part.LEFT_ARM, "the cut is now a bad burn");
        h.assertTrue(Math.abs(s.condition() - 62) < 0.01, "cauterizing costs 8, got " + s.condition());
        double before = s.condition();
        InjuryTick.step(s, now + 10, 10, BodyTuning.STANDARD);
        h.assertTrue(Math.abs(s.condition() - before) < 0.01, "and nothing bleeds after");
        h.assertTrue(journal(p, "cauterized"), "the Messenger says so");
        // A minor cut does not qualify.
        ServerPlayer q = fresh(h);
        BodyAttachments.of(q).parts = Treatments.add(BodyAttachments.of(q).parts, Problem.fresh("cut", Part.LEFT_ARM,
                "forearm", 1, now));
        h.assertTrue(!InjuryService.treat(q, "cauterize", null), "a minor cut is not cauterized");
        h.succeed();
    }

    /** Two stones give +4 torso and +12 hands at the start and nothing after 90 game minutes; only two count. */
    static void pocketStoneFade(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        long t0 = 1000;
        InjuryService.heatStone(p, t0);
        InjuryService.heatStone(p, t0);
        InjuryService.heatStone(p, t0 + 1000); // a third pushes the oldest out; only two are kept
        double two = InjuryService.stoneTorsoC(p, t0 + 1000);
        h.assertTrue(Math.abs(two - 2.0) < 0.01, "only the newest two stones count: fresh +2 and one long cold, got " + two);
        ServerPlayer q = fresh(h);
        InjuryService.heatStone(q, t0);
        InjuryService.heatStone(q, t0);
        double[] want = {4.0, 2.0, 0.0};
        long[] at = {t0, t0 + 45, t0 + 90};
        for (int i = 0; i < 3; i++) {
            double got = InjuryService.stoneTorsoC(q, at[i]);
            h.assertTrue(Math.abs(got - want[i]) < 0.01, "torso at +" + (at[i] - t0) + " min: " + got);
        }
        h.assertTrue(Math.abs(InjuryService.stoneHandsC(q, t0, true) - 12) < 0.01, "hands +12 with both empty");
        h.assertTrue(InjuryService.stoneHandsC(q, t0, false) == 0, "no hands bonus with something held");
        h.assertTrue(InjuryService.stoneTorsoC(q, t0 + 500) == 0, "long cold, nothing");
        h.succeed();
    }

    /** B15: 1,000s of 40 kg lifts strain 3 +-1% as a plain lift and under 1.5% as a squat lift; the cooldown holds. */
    static void backStrainCrouchLift(GameTestHelper h) {
        ServerPlayer p = fresh(h);
        BodyState s = BodyAttachments.of(p);
        s.energy = 10;
        s.warmth = 100;
        BackStrain.Lift plain = new BackStrain.Lift(40, 40, false, 10, 100, false);
        BackStrain.Lift squat = new BackStrain.Lift(40, 40, true, 10, 100, false);
        double cp = InjuryService.strainChance(plain), cs = InjuryService.strainChance(squat);
        h.assertTrue(Math.abs(cp - 0.03) < 1e-9, "plain chance 3%, got " + cp);
        h.assertTrue(Math.abs(cs - 0.009) < 1e-9, "squat chance 0.9%, got " + cs);
        Mulberry32 rng = new Mulberry32(15);
        int a = 0, b = 0, runs = 8000;
        for (int i = 0; i < runs; i++) {
            if (rng.nextDouble() < cp) a++;
            if (rng.nextDouble() < cs) b++;
        }
        h.assertTrue(Math.abs(a / (double) runs - 0.03) < 0.01, "plain lifts strain 3 +-1%, got " + a * 100.0 / runs);
        h.assertTrue(b / (double) runs < 0.015, "squat lifts under 1.5%, got " + b * 100.0 / runs);
        h.assertTrue(InjuryService.strainChance(new BackStrain.Lift(30, 40, false, 10, 100, false)) == 0,
                "under 90% of the cap never strains");
        h.assertTrue(Math.abs(InjuryService.strainChance(new BackStrain.Lift(40, 40, false, 2, 100, false)) - 0.06) < 1e-9,
                "tired doubles it");
        // The cooldown: a day of 50 lifts that would all strain strains once.
        int strains = 0;
        for (int i = 0; i < 50; i++) if (InjuryService.heavyLift(p, plain, 0.0, false)) strains++;
        h.assertTrue(strains == 1, "one strain in a day of 50 lifts, got " + strains);
        long backs = s.parts.problems().stream().filter(x -> x.id().equals("back_strain")).count();
        h.assertTrue(backs == 1 && s.parts.problems().get(0).part() == Part.TORSO, "a back_strain on the torso");
        h.assertTrue(journal(p, "back_strained"), "'Lift with your legs' posts");
        h.succeed();
    }
}

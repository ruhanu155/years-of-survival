package com.yearofice.sim.body.part;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.body.FrostModel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Body, injury and sickness › GameTests 1–8, written as JUnit on the pure model. The adapter-only halves
 * (attribute modifiers, {@code body_public}, real-second hold times, the SOS flight) stay GameTests for M4
 * and M6; each test below says what it covers.
 */
class BodyTabTest {
    private static final long DAY = 1440;

    private static BodyRecord force(InjurySource s, Part p, BodyRecord r, int seed) {
        return Injuries.apply(s, r, new Mulberry32(seed), InjuryContext.DEFAULT.forcing(p)).record();
    }

    /** 1. body/part_split_exact. */
    @Test
    void partSplitExact() {
        BodyRecord r = force(BlueprintSources.BEAR_BITE, Part.LEFT_ARM, BodyRecord.healthy(), 1);
        assertEquals(92.00, r.condition(), 0.01);
        assertEquals(76, r.hp(Part.LEFT_ARM));
        assertEquals(6.32, r.dSys(), 0.01);
        assertEquals(r.conditionFromParts(), r.condition(), 1e-9);
        assertEquals(1, r.injurySeq());
    }

    /** The worked table under the split rule, and the record example: 6.32 + 6.25 = 12.57, Condition 82.0. */
    @Test
    void workedExamples() {
        BodyRecord swipe = force(BlueprintSources.BEAR_SWIPE, Part.TORSO, BodyRecord.healthy(), 2);
        assertEquals(55, swipe.hp(Part.TORSO));
        assertEquals(85, swipe.condition(), 1e-9);
        BodyRecord stomp = force(BlueprintSources.MOOSE_STOMP, Part.TORSO, BodyRecord.healthy(), 3);
        assertEquals(40, stomp.hp(Part.TORSO));
        assertEquals(80, stomp.condition(), 1e-9);
        BodyRecord glance = force(BlueprintSources.HATCHET_GLANCE, Part.LEFT_LEG, BodyRecord.healthy(), 4);
        assertEquals(70, glance.hp(Part.LEFT_LEG));
        assertEquals(90, glance.condition(), 1e-9);

        BodyRecord both = force(BlueprintSources.HATCHET_GLANCE, Part.LEFT_LEG,
                force(BlueprintSources.BEAR_BITE, Part.LEFT_ARM, BodyRecord.healthy(), 5), 6);
        assertEquals(12.57, both.dSys(), 1e-9);
        assertEquals(82.0, both.condition(), 1e-9);
        assertArrayEquals(new int[] {100, 100, 76, 100, 100, 100, 70, 100, 100, 100}, both.partHp());
    }

    /** 2. body/parts_heal_with_condition: Condition and every part reach 100 in the same step, after 8 game hours. */
    @Test
    void partsHealWithCondition() {
        BodyRecord r = force(BlueprintSources.BEAR_BITE, Part.LEFT_ARM, BodyRecord.healthy(), 1);
        double rate = 1.0; // Condition per game hour: a fed player asleep in a warm bed, 8 points in 8 hours
        for (int h = 1; h <= 8; h++) {
            r = PartModel.healHour(r, rate);
            assertEquals(r.conditionFromParts(), r.condition(), 1e-9);
            if (h < 8) {
                assertTrue(r.condition() < 100 && r.hp(Part.LEFT_ARM) < 100, "hour " + h);
            }
        }
        assertEquals(100, r.condition(), 1e-9);
        for (Part p : Part.values()) assertEquals(100, r.hp(p), p.id());
        assertEquals(0, r.dSys(), 1e-9);
    }

    /** 3. body/source_weights. */
    @Test
    void sourceWeights() {
        Mulberry32 rng = new Mulberry32(11179);
        int leftHand = 0;
        for (int i = 0; i < 1000; i++) {
            Injuries.Result res = Injuries.apply(BlueprintSources.KNIFE_SLIP, BodyRecord.healthy(), rng, InjuryContext.DEFAULT);
            if (res.partsHit().get(0) == Part.LEFT_HAND) leftHand++;
        }
        assertTrue(leftHand >= 800 && leftHand <= 900, "left hand " + leftHand);

        int footOrLeg = 0;
        for (int i = 0; i < 500; i++) {
            Injuries.Result res = Injuries.apply(BlueprintSources.FALL_OVER_3M, BodyRecord.healthy(), rng, InjuryContext.DEFAULT);
            assertEquals(Problems.SPRAIN, res.problem().id());
            assertEquals(1, res.record().problems().size());
            if (res.partsHit().get(0).isLimbOfGait()) footOrLeg++;
        }
        assertTrue(footOrLeg >= 325 && footOrLeg <= 375, "foot or leg " + footOrLeg);

        for (int i = 0; i < 200; i++) {
            Injuries.Result res = Injuries.apply(BlueprintSources.FALL_OVER_6M, BodyRecord.healthy(), rng, InjuryContext.DEFAULT);
            assertEquals(2, new HashSet<>(res.partsHit()).size());
            assertEquals(90.0, res.record().condition(), 1e-9);
        }
        for (int i = 0; i < 200; i++) {
            Injuries.Result res = Injuries.apply(BlueprintSources.FALL_OVER_12M, BodyRecord.healthy(), rng, InjuryContext.DEFAULT);
            assertEquals(3, new HashSet<>(res.partsHit()).size());
            assertEquals(40.0, res.record().condition(), 1e-9);
            Part firstBreakable = res.partsHit().get(0) == Part.HEAD ? res.partsHit().get(1) : res.partsHit().get(0);
            assertEquals(firstBreakable, res.problem().part(), "the break lands on the first part it can");
        }
    }

    /** 4. body/sprain_limp_and_stick (the factor and limp; the attribute modifier itself is the M4 GameTest). */
    @Test
    void sprainLimpAndStick() {
        Problem sprain = Problem.fresh(Problems.SPRAIN, Part.RIGHT_FOOT, "ankle", 1, 0).wrapped(0);
        BodyRecord r = BodyRecord.healthy().withProblems(List.of(sprain));
        assertEquals(0.50, Gait.walk(r, 60, false), 0.01);
        assertEquals(new Gait.Limp(Side.RIGHT, 2), Gait.limp(r, 60));
        assertEquals(0.75, Gait.walk(r, 60, true), 0.01);
        assertEquals(Gait.Limp.NONE, Gait.limp(BodyRecord.healthy(), 0));
    }

    /** 5. body/broken_arm_blocks. */
    @Test
    void brokenArmBlocks() {
        Problem broken = Problem.fresh(Problems.BROKEN_BONE, Part.LEFT_ARM, "forearm", 1, 0);
        BodyRecord r = BodyRecord.healthy().withProblems(List.of(broken));
        assertFalse(Use.canUse(r, Use.Tool.BOW, Side.RIGHT, 0));
        assertFalse(Use.canUse(r, Use.Tool.TWO_HANDED, Side.RIGHT, 0));
        assertTrue(Use.canUse(r, Use.Tool.ONE_HANDED, Side.RIGHT, 0));

        long t0 = 5 * DAY;
        BodyRecord splinted = BodyRecord.healthy().withProblems(List.of(broken.splinted(t0)));
        assertFalse(Use.canUse(splinted, Use.Tool.ROD, Side.RIGHT, t0 + 10 * DAY));
        assertTrue(Use.canUse(splinted, Use.Tool.ROD, Side.RIGHT, t0 + 11 * DAY));
        assertFalse(Use.canUse(splinted, Use.Tool.BOW, Side.RIGHT, t0 + 11 * DAY));

        Mulberry32 rng = new Mulberry32(42);
        int rebroken = 0;
        for (int i = 0; i < 100; i++) {
            if (!Use.fallOnSplint(broken.splinted(t0), rng.nextDouble(), t0 + DAY).isSplinted()) rebroken++;
        }
        assertTrue(rebroken >= 20 && rebroken <= 30, "re-broken " + rebroken);
        Problem slung = broken.splinted(t0).withSling(true);
        for (int i = 0; i < 100; i++) assertTrue(Use.fallOnSplint(slung, rng.nextDouble(), t0 + DAY).isSplinted());
    }

    /** 6. body/treat_all_order (the order and the out-of-bandages stop; hold times are the M4 GameTest). */
    @Test
    void treatAllOrder() {
        List<Problem> wounds = List.of(
                Problem.fresh(Problems.CUT, Part.LEFT_LEG, "shin", 1, 0),
                Problem.fresh(Problems.CUT, Part.TORSO, "back", 2, 0),
                Problem.fresh(Problems.CUT, Part.RIGHT_ARM, "forearm", 2, 0));
        assertEquals(7, Triage.totalBleed(wounds), 1e-12);
        assertTrue(Triage.shock(wounds));

        Triage.TreatAll three = Triage.dressBleeding(wounds, 3, 100);
        assertEquals(3, three.treated().size());
        assertEquals(2, three.treated().get(0).stage());
        assertEquals(2, three.treated().get(1).stage());
        assertEquals(1, three.treated().get(2).stage());
        assertFalse(three.ranOut());
        assertEquals(0, Triage.totalBleed(three.problems()), 1e-12);

        Triage.TreatAll two = Triage.dressBleeding(wounds, 2, 100);
        assertTrue(two.ranOut());
        assertEquals(Part.LEFT_LEG, two.stillBleeding().part());
        assertEquals(1, Triage.totalBleed(two.problems()), 1e-12);
    }

    /** 7. body/digit_survives_sos. */
    @Test
    void digitSurvivesSos() {
        int[] frost = new int[Part.COUNT];
        frost[Part.LEFT_HAND.ordinal()] = FrostModel.DEEP;
        BodyRecord r = force(BlueprintSources.BEAR_BITE, Part.LEFT_ARM, BodyRecord.healthy(), 9).withFrost(frost);
        int p = r.frost(Part.LEFT_HAND);
        int[] digits = r.lostDigits();
        for (int m = 0; m < 6 * 60; m++) {
            FrostModel.PartStep s = FrostModel.stepMinute(p, -20, -30, false, true);
            p = s.points();
            if (s.digitLost()) digits = Digits.loseNext(digits, Part.LEFT_HAND);
        }
        r = r.withDigits(digits);
        assertTrue(Digits.lost(r.lostDigits(), Part.LEFT_HAND, 0), "the little finger goes first");
        assertEquals(1, Digits.count(r.lostDigits()[Digits.FINGERS]));
        assertEquals(1.05, Digits.craftingFactor(r.lostDigits()), 1e-12);

        BodyRecord back = PartModel.sosReturn(r);
        assertTrue(Digits.lost(back.lostDigits(), Part.LEFT_HAND, 0));
        for (Part part : Part.values()) assertEquals(100, back.hp(part));
        assertEquals(80, back.condition(), 1e-9);
        assertEquals(back.conditionFromParts(), back.condition(), 1e-9);
        assertTrue(back.problems().isEmpty());
    }

    /** 8. ecosystem/play_dead_parts (M6): face down with a pack, the first bite hits the pack and few later ones reach the head. */
    @Test
    void playDeadParts() {
        Mulberry32 rng = new Mulberry32(2026);
        int laterBites = 0, laterHead = 0;
        for (int contact = 0; contact < 500; contact++) {
            BodyRecord r = BodyRecord.healthy();
            for (int bite = 0; bite < 4; bite++) {
                InjuryContext ctx = InjuryContext.DEFAULT.in(Posture.FACE_DOWN, true, bite == 0);
                Injuries.Result res = Injuries.apply(BlueprintSources.BEAR_BITE, r, rng, ctx);
                if (bite == 0) {
                    assertTrue(res.hitPack());
                    assertEquals(100, res.record().condition(), 1e-12);
                } else {
                    laterBites++;
                    if (res.partsHit().get(0) == Part.HEAD) laterHead++;
                }
                r = res.record();
            }
        }
        assertTrue(laterHead < 0.08 * laterBites, "face down head " + laterHead + "/" + laterBites);

        int bites = 0, head = 0;
        for (int contact = 0; contact < 500; contact++) {
            for (int bite = 0; bite < 4; bite++) {
                InjuryContext ctx = InjuryContext.DEFAULT.in(Posture.STANDING, true, bite == 0);
                Injuries.Result res = Injuries.apply(BlueprintSources.BEAR_BITE, BodyRecord.healthy(), rng, ctx);
                assertFalse(res.hitPack());
                bites++;
                if (res.partsHit().get(0) == Part.HEAD) head++;
            }
        }
        double share = head / (double) bites;
        assertTrue(share >= 0.08 && share <= 0.12, "standing head share " + share);
    }

    @Test
    void conditionIdentityAndFloorsHoldUnderAnyInjurySequence() {
        Mulberry32 rng = new Mulberry32(99);
        for (int run = 0; run < 200; run++) {
            BodyRecord r = BodyRecord.healthy();
            for (int i = 0; i < 12; i++) {
                InjurySource s = BlueprintSources.ALL.get(rng.nextInt(BlueprintSources.ALL.size()));
                r = Injuries.apply(s, r, rng, InjuryContext.DEFAULT.mainHand(rng.nextInt(2) == 0 ? Side.LEFT : Side.RIGHT)).record();
                assertEquals(r.conditionFromParts(), r.condition(), 1e-9);
                assertTrue(r.hp(Part.HEAD) >= 10 && r.hp(Part.TORSO) >= 10);
                assertTrue(r.dSys() >= 0);
                assertTrue(r.problems().size() <= BodyRecord.MAX_PROBLEMS);
            }
        }
    }

    @Test
    void theSameSeedReplaysTheSameInjury() {
        for (int seed = 0; seed < 50; seed++) {
            BodyRecord a = Injuries.applyInjury(BlueprintSources.BEAR_SWIPE, BodyRecord.healthy(), new Mulberry32(seed));
            BodyRecord b = Injuries.applyInjury(BlueprintSources.BEAR_SWIPE, BodyRecord.healthy(), new Mulberry32(seed));
            assertEquals(a, b);
        }
        assertEquals(Injuries.rngFor(1, "p", 3).nextUInt(), Injuries.rngFor(1, "p", 3).nextUInt());
    }

    @Test
    void aProblemThatCannotLandOnThePartIsDropped() {
        BodyRecord r = Injuries.apply(BlueprintSources.MOOSE_STOMP, BodyRecord.healthy(), new Mulberry32(1),
                InjuryContext.DEFAULT.forcing(Part.HEAD).withLoss(5)).record();
        for (Problem p : r.problems()) assertFalse(Problems.BROKEN_BONE.equals(p.id()));
        assertEquals(95, r.condition(), 1e-9);
    }

    @Test
    void systemicSourcesRollNoPart() {
        Injuries.Result res = Injuries.apply(BlueprintSources.EXPOSURE, BodyRecord.healthy(), new Mulberry32(1), InjuryContext.DEFAULT);
        assertTrue(res.partsHit().isEmpty());
        assertEquals(5, res.record().dSys(), 1e-12);
        assertNull(res.problem());
    }

    @Test
    void animalStrikesTearTheHitZonesOuterGarment() {
        Injuries.Result res = Injuries.apply(BlueprintSources.BEAR_SWIPE, BodyRecord.healthy(), new Mulberry32(1),
                InjuryContext.DEFAULT.forcing(Part.RIGHT_LEG));
        assertEquals(com.yearofice.sim.body.Zone.LEGS, res.tearZone());
        assertEquals(0.15, res.tear(), 1e-12);
    }

    @Test
    void c1HealingStopsForBleedingInfectionOrCold() {
        Problem cut = Problem.fresh(Problems.CUT, Part.LEFT_ARM, "forearm", 1, 0);
        assertFalse(PartModel.healingAllowed(List.of(cut), 80));
        assertTrue(PartModel.healingAllowed(List.of(cut.dressed("moss", "fibre_wrap", 720)), 80));
        assertFalse(PartModel.healingAllowed(List.of(Problem.fresh(Problems.INFECTION, Part.LEFT_ARM, "", 1, 0)), 80));
        assertFalse(PartModel.healingAllowed(List.of(), 29.9));
        Problem splinted = Problem.fresh(Problems.BROKEN_BONE, Part.LEFT_LEG, "shin", 1, 0).splinted(0);
        assertTrue(PartModel.healingAllowed(List.of(splinted), 30));
    }

    @Test
    void theSeventeenthMinorProblemFoldsIntoItsTwin() {
        List<Problem> l = new ArrayList<>();
        for (int i = 0; i < 16; i++) l.add(Problem.fresh(Problems.BRUISE, Part.values()[i % 10], "", 1, i));
        List<Problem> full = Injuries.addProblem(List.of(), l.get(0));
        for (int i = 1; i < 16; i++) full = Injuries.addProblem(full, l.get(i));
        assertEquals(16, full.size());
        List<Problem> folded = Injuries.addProblem(full, Problem.fresh(Problems.BRUISE, Part.HEAD, "", 1, 500));
        assertEquals(16, folded.size());
        assertTrue(folded.stream().anyMatch(p -> p.part() == Part.HEAD && p.started() == 500));
        List<Problem> dropped = Injuries.addProblem(full, Problem.fresh(Problems.BROKEN_BONE, Part.TORSO, "ribs", 1, 600));
        assertEquals(full, dropped);
    }

    @Test
    void localOngoingLossesAreSplitToo() {
        BodyRecord r = BodyRecord.healthy();
        for (int h = 0; h < 24; h++) r = PartModel.split(r, Part.LEFT_ARM, 10.0 / 24);
        assertEquals(90, r.condition(), 1e-9);
        assertTrue(r.hp(Part.LEFT_ARM) < 100);
    }

    @Test
    void schemaOneUpgradeSetsEveryPartTo100() {
        BodyRecord r = BodyRecord.upgradeFromSchema1(88, List.of());
        assertEquals(2, r.schema());
        assertEquals(88, r.condition(), 1e-12);
        assertEquals(12, r.dSys(), 1e-12);
        for (Part p : Part.values()) assertEquals(100, r.hp(p));
    }

    @Test
    void sharesAddToOneAndMatchTheZones() {
        double total = 0;
        for (Part p : Part.values()) total += p.share;
        assertEquals(1.0, total, 1e-12);
        for (com.yearofice.sim.body.Zone z : com.yearofice.sim.body.Zone.values()) {
            double zs = 0;
            for (Part p : Part.values()) if (p.zone == z) zs += p.share;
            assertEquals(z.share, zs, 1e-12, z.id());
        }
    }

    @Test
    void bandsAndOneBandLower() {
        assertEquals(Band.FINE, Band.of(80));
        assertEquals(Band.SORE, Band.of(76));
        assertEquals(Band.HURT, Band.of(55));
        assertEquals(Band.BADLY_HURT, Band.of(10));
        assertEquals(Band.USELESS, Band.of(0));
        assertEquals(Band.BADLY_HURT, Band.BADLY_HURT.lower(Part.HEAD));
        assertEquals(Band.USELESS, Band.BADLY_HURT.lower(Part.LEFT_ARM));
        assertEquals(0.8, Band.HURT.torsoSwim(), 1e-12);
        assertEquals(0.6, Band.BADLY_HURT.torsoSwim(), 1e-12);
    }
}

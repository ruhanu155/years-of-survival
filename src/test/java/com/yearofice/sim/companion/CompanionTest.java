package com.yearofice.sim.companion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

/** Trust, the February leave model, the sled and the lead fence (M9 lane A). */
class CompanionTest {
    static final CompanionTuning STD = CompanionTuning.Preset.STANDARD.tuning;
    static final CompanionTuning GENTLE = CompanionTuning.Preset.GENTLE.tuning;
    static final CompanionTuning BRUTAL = CompanionTuning.Preset.BRUTAL.tuning;

    /** Days until Companion when every action is taken every day (the fastest a player can go). */
    static int daysSpamming(double start) {
        Trust t = new Trust(start);
        for (int day = 1; day <= 200; day++) {
            for (Trust.Action a : Trust.Action.values()) for (int k = 0; k < 4; k++) t.apply(a, STD);
            t.endDay(STD);
            if (t.stage() == Trust.Stage.COMPANION) return day;
        }
        return -1;
    }

    @Test
    void pupNeedsAtLeast32DaysEvenWithEveryAction() {
        assertEquals(32, daysSpamming(0));
        int orphan = daysSpamming(10);
        assertTrue(orphan >= 28 && orphan <= 31, "orphan " + orphan);
    }

    @Test
    void steadyOneFeedingADayLandsInThirtyToSixtyDays() {
        Trust t = new Trust(0);
        int day = 0, juvenile = -1;
        while (t.stage() != Trust.Stage.COMPANION && day < 200) {
            day++;
            t.apply(t.value() >= Trust.TOLERANT ? Trust.Action.HAND_FEED : Trust.Action.FEED_GROUND, STD);
            t.apply(Trust.Action.ROUTINE, STD);
            t.endDay(STD);
            if (juvenile < 0 && t.stage() == Trust.Stage.JUVENILE) juvenile = day;
        }
        assertTrue(day >= 30 && day <= 60, "companion on day " + day);
        assertTrue(juvenile > 0 && juvenile < day, "juvenile on day " + juvenile);
    }

    @Test
    void neglectDrainsAfterGraceAndStrikesCostThirty() {
        Trust t = new Trust(60);
        for (int i = 0; i < STD.neglectGraceDays(); i++) t.endDay(STD);
        assertEquals(60, t.value());
        t.endDay(STD);
        assertEquals(59, t.value());
        t.lose(30);
        assertEquals(29, t.value());
        Trust back = Trust.load(t.save());
        assertEquals(t.value(), back.value());
    }

    @Test
    void actionsRespectStageAndDailyLimit() {
        Trust t = new Trust(0);
        assertEquals(0, t.apply(Trust.Action.HAND_FEED, STD), "hand feeding needs Tolerant");
        assertEquals(2, t.apply(Trust.Action.FEED_GROUND, STD));
        assertEquals(1, t.apply(Trust.Action.FEED_GROUND, STD), "the day's cap is +3");
        assertEquals(0, t.apply(Trust.Action.FEED_GROUND, STD));
    }

    @Test
    void thousandFebruariesAtSixtyTrust() {
        var std = WolfLeaving.simulate(STD, 60, false, 1000, 20261019L);
        assertTrue(std.lostPct() >= 70 && std.lostPct() <= 85, "Standard lost " + std.lostPct());
        assertEquals(100.0, std.returnedOfLostPct(), "every wolf comes back on Standard");
        var gentle = WolfLeaving.simulate(GENTLE, 60, false, 1000, 20261019L);
        assertEquals(0, gentle.lost());
        var brutal = WolfLeaving.simulate(BRUTAL, 60, false, 1000, 20261019L);
        assertTrue(brutal.lostPct() >= 70 && brutal.returned() == 0, "Brutal keeps no return: " + brutal);
        var companion = WolfLeaving.simulate(STD, 80, false, 1000, 1L);
        assertEquals(0, companion.lost(), "Companion never leaves");
    }

    @Test
    void sleepingRoofedAndFedHalvesTheRoll() {
        assertEquals(WolfLeaving.dailyP(STD, 60, false) / 2, WolfLeaving.dailyP(STD, 60, true), 1e-12);
        assertEquals(0.05, WolfLeaving.dailyP(STD, 60, false), 1e-12);
    }

    @Test
    void sledSpeedAndCapacity() {
        assertEquals(0, SledMath.speedMps(STD, 1, SledMath.Surface.LANE, 0), "one wolf cannot pull");
        double lane = SledMath.speedMps(STD, 4, SledMath.Surface.LANE, 30);
        double snow = SledMath.speedMps(STD, 4, SledMath.Surface.SNOW, 30);
        double bare = SledMath.speedMps(STD, 4, SledMath.Surface.BARE, 30);
        assertTrue(lane > snow && snow > bare && bare > 0, lane + " " + snow + " " + bare);
        assertTrue(SledMath.speedMps(STD, 6, SledMath.Surface.SNOW, 30) > snow, "bigger team is faster");
        assertEquals(0, SledMath.speedMps(STD, 2, SledMath.Surface.SNOW, 500), "over capacity");
        assertEquals(6 * STD.sledPullKg(), SledMath.capacityKg(STD, 9, SledMath.Surface.LANE), 1e-9, "team capped at six");
    }

    @Test
    void leadFenceFollowRates() {
        assertEquals(0.5, LeadFenceMath.followChance(STD, 20), 1e-9);
        assertEquals(0.8, LeadFenceMath.followChance(STD, 50), 1e-9);
        SplittableRandom r = new SplittableRandom(7);
        int follow = 0;
        for (int i = 0; i < 50; i++) if (LeadFenceMath.bandFollows(STD, 20, 10, 0, false, r)) follow++;
        assertTrue(follow >= 20 && follow <= 30, "followed " + follow);
        for (int i = 0; i < 200; i++) assertTrue(!LeadFenceMath.bandFollows(STD, 50, 10, 0, true, r), "wind toward the band");
        assertTrue(!LeadFenceMath.bandFollows(STD, 50, 90, 0, false, r), "outside 30 degrees");
        assertEquals(0.5, LeadFenceMath.routeUse(STD, 3));
    }
}

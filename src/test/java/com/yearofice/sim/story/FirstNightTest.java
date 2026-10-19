package com.yearofice.sim.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.LongUnaryOperator;
import org.junit.jupiter.api.Test;

/** The guided first night's pure parts: the step machine, warn once, the day arithmetic and the beacon's fade. */
class FirstNightTest {
    static final FirstNightFacts NONE = FirstNightFacts.NONE;
    /** A May-like sun: dawn 03:30, sunset 21:45. */
    static final LongUnaryOperator DAWN = d -> d * 1440 + 3 * 60 + 30;
    static final LongUnaryOperator SUNSET = d -> d * 1440 + 21 * 60 + 45;
    static final long MORNING = StoryDay.firstMorning(720, DAWN);

    static FirstNight started() {
        FirstNight n = new FirstNight();
        n.start(720);
        return n;
    }

    @Test
    void stepsCloseInOrderOnFactsOnly() {
        FirstNight n = started();
        assertEquals(FirstNight.Advance.NOTHING, n.advance(NONE, 800, MORNING), "no facts, no progress, however long");
        assertEquals(FirstNightStep.TINDER, n.step());
        assertFalse(n.advance(NONE.withTinder(true, false), 801, MORNING).moved(), "tinder alone is not enough");
        FirstNight.Advance a = n.advance(NONE.withTinder(true, true), 802, MORNING);
        assertEquals(List.of(FirstNightStep.TINDER), a.completed());
        assertEquals(FirstNightStep.RING, a.now());
        assertEquals(FirstNightStep.LIGHT, n.advance(NONE.withRing(true), 810, MORNING).now());
        assertEquals(FirstNightStep.FOOD, n.advance(NONE.withRing(true).withFireLit(true), 830, MORNING).now());
        assertEquals(830, n.fireLitMinute());
        assertEquals(FirstNightStep.EAT, n.advance(NONE.withFood(true), 900, MORNING).now());
        assertFalse(n.advance(NONE.withFood(true), 910, MORNING).moved(), "food in the pack is not a meal");
        assertEquals(FirstNightStep.SLEEP, n.advance(NONE.withAte(true), 920, MORNING).now());
        assertFalse(n.advance(NONE.withFireLit(true), 1300, MORNING).moved(), "awake by the fire is not sleeping by it");
        FirstNight.Advance end = n.advance(NONE.withAsleepByFire(true), 1380, MORNING);
        assertTrue(end.finished());
        assertNull(end.now());
        assertEquals(FirstNight.Status.DONE, n.status());
        assertEquals(FirstNight.Ending.SLEPT, n.ending());
        assertEquals(1380, n.endMinute());
        assertFalse(n.advance(NONE.withAsleepByFire(true), 1400, MORNING).moved(), "gone for good after the first sleep");
    }

    @Test
    void workingAheadClosesEarlierSteps() {
        FirstNight n = started();
        FirstNight.Advance a = n.advance(NONE.withFireLit(true).withFood(true), 760, MORNING);
        assertEquals(List.of(FirstNightStep.TINDER, FirstNightStep.RING, FirstNightStep.LIGHT, FirstNightStep.FOOD), a.completed());
        assertEquals(FirstNightStep.EAT, n.step());
        assertEquals(760, n.fireLitMinute());
    }

    @Test
    void anEarlyMealDoesNotCountForLater() {
        FirstNight n = started();
        assertFalse(n.advance(NONE.withAte(true), 750, MORNING).moved(), "eating on step 1 closes nothing");
        assertEquals(FirstNightStep.TINDER, n.step());
    }

    @Test
    void stayingUpEndsAtTheFirstMorning() {
        FirstNight n = started();
        assertEquals(1440 + 210, MORNING, "the first dawn at least 12 h after 12:00 is the next day's 03:30");
        assertFalse(n.advance(NONE, MORNING - 1, MORNING).moved());
        FirstNight.Advance a = n.advance(NONE, MORNING, MORNING);
        assertTrue(a.finished());
        assertEquals(FirstNight.Ending.STAYED_UP, n.ending());
        assertEquals(FirstNight.Status.DONE, n.status());
    }

    @Test
    void skipAndReset() {
        FirstNight n = started();
        n.skip(800, FirstNight.Ending.SKIPPED);
        assertEquals(FirstNight.Status.SKIPPED, n.status());
        assertFalse(n.advance(NONE.withTinder(true, true), 801, MORNING).moved());
        n.reset();
        assertEquals(FirstNight.Status.NEW, n.status());
        n.start(900);
        assertTrue(n.active());
        assertEquals(FirstNightStep.TINDER, n.step());
        assertEquals(-1, n.fireLitMinute());
        FirstNight done = started();
        done.advance(NONE.withFireLit(true).withAte(true).withAsleepByFire(true), 800, MORNING);
        assertEquals(FirstNight.Status.DONE, done.status());
        done.skip(900, FirstNight.Ending.SKIPPED);
        assertEquals(FirstNight.Ending.SLEPT, done.ending(), "skip never overwrites a finished night");
    }

    @Test
    void sixPromptsInTheBlueprintsOrder() {
        assertEquals(6, FirstNightStep.COUNT);
        assertEquals(FirstNightStep.TINDER, FirstNightStep.byId("tinder"));
        for (FirstNightStep s : FirstNightStep.values()) assertEquals(s, FirstNightStep.byId(s.id));
        assertNull(FirstNightStep.SLEEP.next());
        assertEquals(FirstNightStep.Target.FIRE, FirstNightStep.SLEEP.beacon);
        assertEquals(FirstNightStep.Target.GATHER, FirstNightStep.TINDER.beacon);
    }

    @Test
    void warnOnceFiresOnceAndRearmsAfter24FalseHours() {
        WarnOnce w = new WarnOnce();
        assertFalse(w.update("dusk_no_fire", false, 0));
        assertTrue(w.update("dusk_no_fire", true, 100), "first time true: fires");
        assertFalse(w.update("dusk_no_fire", true, 101), "still true: quiet");
        assertFalse(w.update("dusk_no_fire", false, 200));
        assertFalse(w.update("dusk_no_fire", true, 300), "true again within 24 h: quiet");
        assertFalse(w.update("dusk_no_fire", false, 400));
        assertFalse(w.update("dusk_no_fire", false, 400 + WarnOnce.REARM_MIN - 1));
        assertTrue(w.fired("dusk_no_fire"));
        assertFalse(w.update("dusk_no_fire", false, 400 + WarnOnce.REARM_MIN), "24 false hours: re-armed");
        assertFalse(w.fired("dusk_no_fire"));
        assertTrue(w.update("dusk_no_fire", true, 400 + WarnOnce.REARM_MIN + 5), "fires again");
        WarnOnce copy = new WarnOnce();
        copy.restore(w.entries());
        assertEquals(w.entries(), copy.entries());
    }

    @Test
    void duskWindowRunsFromLeadBeforeSunsetToDawn() {
        int lead = 75;
        assertFalse(StoryDay.inDuskWindow(20 * 60 + 29, lead, SUNSET, DAWN), "20:29");
        assertTrue(StoryDay.inDuskWindow(20 * 60 + 30, lead, SUNSET, DAWN), "20:30, the Blueprint's line");
        assertTrue(StoryDay.inDuskWindow(1440 + 60, lead, SUNSET, DAWN), "01:00 next day is still the night");
        assertFalse(StoryDay.inDuskWindow(1440 + 3 * 60 + 30, lead, SUNSET, DAWN), "dawn closes it");
        assertEquals("21:45", StoryDay.hhmm(SUNSET.applyAsLong(0)));
        assertEquals(2, StoryDay.day(1440 + 5));
    }

    @Test
    void beaconFadesOutUpClose() {
        assertEquals(0, BeaconLook.alpha(3, 4, 12, 0.3));
        assertEquals(0, BeaconLook.alpha(4, 4, 12, 0.3));
        assertEquals(0.15, BeaconLook.alpha(8, 4, 12, 0.3), 1e-9);
        assertEquals(0.3, BeaconLook.alpha(12, 4, 12, 0.3), 1e-9);
        assertEquals(0.3, BeaconLook.alpha(500, 4, 12, 0.3), 1e-9);
        assertEquals(1, BeaconLook.along(0), 1e-9);
        assertEquals(0, BeaconLook.along(1), 1e-9);
        assertTrue(BeaconLook.along(0.5) < 0.5, "fades faster than linear on the way up");
    }

    @Test
    void presetsAreValid() {
        for (OnboardingTuning.Preset p : OnboardingTuning.Preset.values()) assertTrue(p.tuning.nearFireRadiusM() >= 3);
        assertEquals(3, OnboardingTuning.standard().nearFireRadiusM());
        assertEquals(75, OnboardingTuning.standard().duskLeadMin());
    }
}

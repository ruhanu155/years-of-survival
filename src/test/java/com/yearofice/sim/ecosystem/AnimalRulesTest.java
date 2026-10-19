package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Awareness, the prey transitions, gaits, looks, the calendar and dressing. */
class AnimalRulesTest {
    static final PreyRules.Behavior HARE = new PreyRules.Behavior(30, 70, 20, true, 5, 0, 20, 40, 30, 60);
    static final PreyRules.Behavior MOOSE = new PreyRules.Behavior(30, 70, 20, false, 0, 40, 200, 400, 30, 60);

    @Test
    void stillCrouchedPlayerAtHalfRangeTakesAboutSevenSeconds() {
        // The build rule's worked example: crouched, still, at half range, wind in the face (sight only here).
        Senses moose = new Senses(60, 40, 150, 300);
        double r = Awareness.sightRange(moose, Posture.CROUCH, 1, 1);
        double a = 0;
        double t = 0;
        while (a < 70 && t < 600) {
            a = Awareness.gain(a, Awareness.GAIN_SIGHT, Awareness.stimulus(r / 2, r), Posture.CROUCH, 0.25);
            t += 0.25;
        }
        assertTrue(t > 5 && t < 12, "crouched: " + t + " s");
        double up = 0;
        t = 0;
        double rw = Awareness.sightRange(moose, Posture.WALK, 1, 1);
        while (up < 70) {
            up = Awareness.gain(up, Awareness.GAIN_SIGHT, Awareness.stimulus(rw / 2, rw), Posture.WALK, 0.25);
            t += 0.25;
        }
        assertTrue(t >= 2 && t <= 4.5, "walking: " + t + " s");
    }

    @Test
    void scentSetsSixtyAtOnceAndDecayIsTwoASecond() {
        assertTrue(Awareness.smell(0, 0.1, Posture.WALK, 0.05) >= 60);
        assertEquals(8, Awareness.decay(10, 1, false), 1e-12);
        assertEquals(5, Awareness.decay(10, 1, true), 1e-12);
    }

    @Test
    void flushDistanceFollowsPostureAndApproach() {
        assertEquals(7.5, Awareness.flushDistance(5, Posture.WALK, 1, false), 1e-12);
        assertEquals(3.5, Awareness.flushDistance(5, Posture.WALK, 0, false), 1e-12);
        assertEquals(4.5, Awareness.flushDistance(5, Posture.CROUCH, 1, false), 1e-12);
        assertEquals(1.5, Awareness.flushDistance(5, Posture.STILL, 1, false), 1e-12);
        assertTrue(Awareness.inView(300, 0, 140) && !Awareness.inView(300, 0, 170));
    }

    @Test
    void hareFreezesThenBoltsInsideItsFlushDistance() {
        assertEquals(AnimalState.FREEZE, PreyRules.next(AnimalState.ROUTINE, HARE, 35, 12, 7.5, false, false));
        assertEquals(AnimalState.FLEE, PreyRules.next(AnimalState.FREEZE, HARE, 40, 7, 7.5, false, false));
        assertEquals(AnimalState.FLEE, PreyRules.next(AnimalState.FREEZE, HARE, 71, 15, 7.5, false, false));
        assertEquals(AnimalState.FLEE, PreyRules.next(AnimalState.FLEE, HARE, 0, 30, 7.5, false, false), "a flee runs its distance");
        assertEquals(AnimalState.FREEZE, PreyRules.next(AnimalState.FLEE, HARE, 10, 30, 7.5, true, true), "then hides");
        assertEquals(AnimalState.ROUTINE, PreyRules.next(AnimalState.FREEZE, HARE, 10, 30, 7.5, true, false));
    }

    @Test
    void mooseAlertsThenFleesAtFortyMetresAndNeverCharges() {
        assertEquals(AnimalState.ALERT, PreyRules.next(AnimalState.ROUTINE, MOOSE, 32, 55, 0, false, false));
        assertEquals(AnimalState.FLEE, PreyRules.next(AnimalState.ALERT, MOOSE, 32, 39, 0, false, false));
        for (AnimalState s : AnimalState.values()) {
            for (double a = 0; a <= 100; a += 5) {
                for (double d = 0; d < 80; d += 4) {
                    AnimalState n = PreyRules.next(s, MOOSE, a, d, 0, false, false);
                    assertTrue(n.ordinal() <= AnimalState.FLEE.ordinal());
                }
            }
        }
    }

    @Test
    void gaitsInvertMinecraftGroundSpeed() {
        assertEquals(12, Gaits.mps(Gaits.input(12)), 1e-9);
        // vanilla zombie input 0.23 walks about 2.3 m/s
        assertEquals(2.33, Gaits.mps(0.23), 0.02);
    }

    @Test
    void coatsAndAntlersByDate() {
        assertEquals(SeasonalLooks.Coat.SUMMER, SeasonalLooks.hare(SliceCalendar.doy(5, 1)));
        assertEquals(SeasonalLooks.Coat.MOTTLED, SeasonalLooks.hare(SliceCalendar.doy(10, 10)));
        assertEquals(SeasonalLooks.Coat.WINTER, SeasonalLooks.hare(SliceCalendar.doy(1, 15)));
        assertEquals(SeasonalLooks.Coat.MOTTLED, SeasonalLooks.hare(SliceCalendar.doy(4, 20)));
        assertEquals(SeasonalLooks.Coat.WINTER, SeasonalLooks.ptarmigan(SliceCalendar.doy(12, 1)));
        assertEquals(SeasonalLooks.Coat.MOULT, SeasonalLooks.ptarmigan(SliceCalendar.doy(10, 5)));
        assertEquals(SeasonalLooks.Coat.MOULT, SeasonalLooks.ptarmigan(SliceCalendar.doy(5, 5)));
        assertEquals(SeasonalLooks.Coat.SUMMER, SeasonalLooks.ptarmigan(SliceCalendar.doy(7, 1)));
        assertEquals(0, SeasonalLooks.antlers(SliceCalendar.doy(2, 1)), 1e-12);
        assertEquals(1, SeasonalLooks.antlers(SliceCalendar.doy(9, 20)), 1e-12);
        double june = SeasonalLooks.antlers(SliceCalendar.doy(6, 15));
        assertTrue(june > 0.3 && june < 0.6, "June antlers " + june);
    }

    @Test
    void calendarAndRolls() {
        assertEquals(364, SliceCalendar.doy(4, 30));
        assertEquals("Oct 21", SliceCalendar.date(SliceCalendar.doy(10, 21)));
        assertEquals(0, SliceCalendar.rollAtOrBefore(360, 360, 1080));
        assertEquals(-1, SliceCalendar.rollAtOrBefore(359, 360, 1080));
        assertEquals(3, SliceCalendar.rollAtOrBefore(1440 + 1100, 360, 1080));
        assertEquals(1440 + 1080, SliceCalendar.rollMinute(3, 360, 1080));
    }

    @Test
    void hareDressesInThirteenMinutesAndTheCycleHasTenYears() {
        assertEquals(13, 3 + 5 + 5);
        assertEquals(52, Dressing.holdTicks(13, 5));
        assertEquals(5 * Math.pow(0.55 / 1.5, 0.6), Dressing.scaled(5, 1.5, 0.55), 1e-12);
        int p = HareCycle.phase(1234);
        assertTrue(p >= 0 && p < 10);
        assertEquals(HareCycle.multiplier(p, 10), HareCycle.multiplier(p, 0));
        assertEquals(2, Density.count(1.4, 0.3));
        assertEquals(1, Density.count(1.4, 0.5));
    }
}

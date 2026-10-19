package com.yearofice.sim.decay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.core.hash.Mulberry32;
import org.junit.jupiter.api.Test;

/**
 * {@code yearofice:fresh} on its clock: the Layer 5 GameTests as JUnit (decay/loaded_equals_unloaded,
 * rehome_one_band, one_clock, frozen_stops), the Spoilage model's tests, and the TFC-derived trait, state and
 * merge rules.
 */
class FreshTest {
    private static final ColdCurve C = ColdCurve.STANDARD;
    private static final double MEAT = DecayCore.tauFromSafeLifeDays(6);
    private static final double LIVER = DecayCore.tauFromSafeLifeDays(1.8);
    private static final double BAND = 1.0 / Fresh.DEFAULT_BANDS;

    private static int band(double p) { return (int) StrictMath.floor(p * Fresh.DEFAULT_BANDS + 1e-9); }

    @Test
    void loaded_equals_unloaded() {
        DecayClimate clim = DecayClimate.livingRot(Climates.diurnal(8, 5, 10), C);
        int end = clim.hours();
        for (int i = 0; i < 27; i++) {
            int start = i * 7;
            Fresh f = Fresh.stamp(FoodState.RAW, 0, clim.decayHoursAt(start), i % 3 == 0 ? LIVER : MEAT);
            Fresh loaded = f;
            for (int h = start; h <= end; h++) loaded = loaded.settle(clim.decayHoursAt(h));
            Fresh unloaded = f.settle(clim.decayHoursAt(end));
            assertEquals(band(loaded.progress(clim.decayHoursAt(end))), band(unloaded.progress(clim.decayHoursAt(end))));
            assertEquals(loaded, unloaded);
        }
    }

    @Test
    void rehome_one_band() {
        DecayClimate a = DecayClimate.livingRot(Climates.diurnal(10, 6, 20), C);
        DecayClimate b = DecayClimate.livingRot(Climates.constant(3, 20 * 24), C); // the cellar
        Mulberry32 r = new Mulberry32(5);
        Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, MEAT);
        DecayClimate here = a;
        int clock = 0;
        int hour = 0;
        for (int move = 0; move < 100; move++) {
            hour += r.nextInt(3);
            DecayClimate there = here == a ? b : a;
            double before = f.progress(here.decayHoursAt(hour));
            f = f.rehome(1 - clock, here.decayHoursAt(hour), there.decayHoursAt(hour), Fresh.DEFAULT_BANDS);
            double after = f.progress(there.decayHoursAt(hour));
            assertTrue(after - before <= BAND + 1e-12 && after >= before - 1e-12, "move " + move + ": " + before + " → " + after);
            here = there;
            clock = 1 - clock;
        }
    }

    @Test
    void one_clock() {
        // Meat in a pack, on a rack and in an opened carcass at 10 °C share the clock and reach Turning together.
        DecayClimate clim = DecayClimate.livingRot(Climates.constant(10, 200), C);
        int[] turning = new int[3];
        for (int i = 0; i < 3; i++) {
            Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, MEAT);
            int h = 0;
            while (f.stage(clim.decayHoursAt(h)) < 2) h++;
            turning[i] = h;
        }
        assertTrue(Math.abs(turning[0] - turning[1]) <= 1 && Math.abs(turning[1] - turning[2]) <= 1);
    }

    @Test
    void frozen_stops() {
        DecayClimate clim = DecayClimate.livingRot(Climates.constant(-10, 30 * 24), C);
        Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, MEAT);
        assertEquals(f.progress(0), f.progress(clim.decayHoursAt(clim.hours())), 0);
    }

    @Test
    void liverTurnsThreeTimesFaster() {
        DecayClimate clim = DecayClimate.livingRot(Climates.constant(15, 100), C);
        double meat = Fresh.stamp(FoodState.RAW, 0, 0, MEAT).decayHoursUntil(0.7, 0) / C.rate(15);
        double liver = Fresh.stamp(FoodState.RAW, 0, 0, LIVER).decayHoursUntil(0.7, 0) / C.rate(15);
        assertEquals(3.33, meat / liver, 0.05);
        assertTrue(clim.hours() > liver);
    }

    @Test
    void stagesAndFreshness() {
        Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, 100);
        assertEquals(0, f.stage(39.9));
        assertEquals(1, f.stage(40));
        assertEquals(2, f.stage(70));
        assertEquals(3, f.stage(90));
        assertEquals(4, f.stage(100));
        assertEquals(30.0, f.freshness(70), 1e-9);
        assertEquals(30, DecayLadder.tooltipFreshness(0.71));
    }

    @Test
    void traitKeepsProgressWithinOneBand() {
        Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, MEAT);
        double d = 50;
        double p = f.progress(d);
        Fresh salted = f.withTrait(FreshTrait.SALTED, d, Fresh.DEFAULT_BANDS);
        assertEquals(p, salted.progress(d), BAND);
        assertEquals(2 * f.lifeDh(), salted.lifeDh(), 1e-9);
        // Salted food decays half as fast from here on.
        double dp = f.progress(d + 10) - f.progress(d);
        double dps = salted.progress(d + 10) - salted.progress(d);
        assertEquals(dp / 2, dps, 1e-9);
        Fresh unsalted = salted.withoutTrait(FreshTrait.SALTED, d, Fresh.DEFAULT_BANDS);
        assertEquals(p, unsalted.progress(d), 2 * BAND);
        assertSame(salted, salted.withTrait(FreshTrait.SALTED, d, 40));
    }

    @Test
    void rottenFoodTakesNoTraitAndSettlesRotten() {
        Fresh f = Fresh.stamp(FoodState.RAW, 0, 0, 10);
        assertSame(f, f.withTrait(FreshTrait.SALTED, 20, 40));
        Fresh s = f.settle(20);
        assertEquals(Fresh.ROTTEN, s.dueDh());
        assertEquals(1.0, s.progress(-1e9), 0); // stays rotten whatever the clock says
    }

    @Test
    void neverDecays() {
        Fresh honey = Fresh.never(FoodState.RAW, 0, 100);
        assertEquals(0, honey.progress(1e12), 0);
        assertSame(honey, honey.settle(1e12));
        assertFalse(honey.rotten(1e12));
    }

    @Test
    void cookingKeepsProgressAndSmokingStartsFresh() {
        Fresh raw = Fresh.stamp(FoodState.RAW, 0, 0, MEAT);
        double d = 0.5 * raw.lifeDh(); // half way
        Fresh cooked = raw.withState(FoodState.COOKED, DecayCore.tauFromSafeLifeDays(9), true, d, 0, d, 40);
        assertEquals(0.5, cooked.progress(d), BAND);
        Fresh smoked = raw.withState(FoodState.SMOKED, 30 * 24, false, d, 7, 1000, 40);
        assertEquals(0.0, smoked.progress(1000), BAND);
        assertEquals(7, smoked.clock());
    }

    @Test
    void mergeTakesTheWorseAndNeedsEqualTraits() {
        Fresh a = Fresh.stamp(FoodState.RAW, 0, 0, MEAT);
        Fresh b = Fresh.stamp(FoodState.RAW, 0, 30, MEAT);
        assertSame(a, Fresh.merge(a, b));
        Fresh salted = b.withTrait(FreshTrait.SALTED, 30, 40);
        assertFalse(a.mergeableWith(salted));
        assertThrows(IllegalArgumentException.class, () -> Fresh.merge(a, salted));
    }

    @Test
    void sameBandSameClockAreEqual() {
        double life = MEAT;
        double band = life / 40;
        Fresh a = Fresh.stamp(FoodState.RAW, 0, 3 * band + 0.1, life);
        Fresh b = Fresh.stamp(FoodState.RAW, 0, 3 * band + 0.9 * band, life);
        assertEquals(a, b);
        assertEquals(Fresh.quantise(a.dueDh(), life, 40), a.dueDh(), 0);
    }
}

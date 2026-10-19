package com.yearofice.sim.decay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The cold curve, the safe-life table and the decay-day unit (Decay and time › The cold curve, The decay-day). */
class ColdCurveTest {
    private static final ColdCurve C = ColdCurve.STANDARD;

    @Test
    void rateTable() {
        assertEquals(1.0, C.rate(5), 0);
        assertEquals(3.0, C.rate(15), 0);
        assertEquals(5.2, C.rate(20), 0.01);
        assertEquals(1.7, C.rate(10), 0.04);
        assertEquals(0.58, C.rate(0), 0.005);
        assertEquals(0.2, C.rate(-1), 0);
        assertEquals(0.2, C.rate(-5), 0);
        assertEquals(0.0, C.rate(-5.01), 0);
        assertEquals(0.0, C.rate(-25), 0);
    }

    @Test
    void wholeDegreeTableInterpolates() {
        double mid = C.rate(12.5);
        assertTrue(mid > C.rate(12) && mid < C.rate(13));
        assertEquals((C.rate(12) + C.rate(13)) / 2, mid, 1e-12);
    }

    @Test
    void moseQuarterSafeLives() {
        // Decay and time › What it means for a moose quarter: safe until Turning, rotten after.
        double[][] rows = {{20, 1.2, 1.7}, {15, 2.0, 2.9}, {10, 3.5, 4.9}, {5, 6.0, 8.6}, {0, 10.4, 14.9}, {-3, 30, 43}};
        for (double[] r : rows) {
            double safe = C.safeLifeDays(r[0], 1.0);
            assertEquals(r[1], safe, 0.06, "safe at " + r[0]);
            assertEquals(r[2], safe / DecayCore.SAFE_P, 0.2, "rotten at " + r[0]);
        }
        assertEquals(Double.POSITIVE_INFINITY, C.safeLifeDays(-6, 1.0));
    }

    @Test
    void decayDayMonthTable() {
        double[][] months = {{9, 31, 48}, {15, 30, 90}, {17, 31, 116}, {14, 31, 83}, {8, 30, 42}, {-2, 61, 12}};
        for (double[] m : months) assertEquals(m[2], C.rate(m[0]) * m[1], 1.0, "month at " + m[0]);
    }

    @Test
    void standardYearIsAbout390DecayDays() {
        DecayClimate y = DecayClimate.livingRot(Climates.standardYears(1), C);
        assertEquals(DecayCore.VALLEY_YEAR_DECAY_DAYS, y.decayDaysBetween(0, y.hours()), 2.0);
    }

    @Test
    void fixedLifeAndSnow() {
        assertEquals(1.0, C.fixedLifeRate(10, 30, 14), 0);
        assertEquals(30.0 / 14, C.fixedLifeRate(16, 30, 14), 1e-12);
        assertEquals(0.0, C.fixedLifeRate(-6, 30, 14), 0);
        assertEquals(0.0, ColdCurve.underSnow(3), 0);
        assertEquals(-11.0, ColdCurve.underSnow(-11), 0);
        assertEquals(0.0, ColdCurve.rust(-0.5), 0);
        assertEquals(1.5, C.withFlies().applyAsDouble(10.5) / C.rate(10.5), 1e-12);
    }

    @Test
    void normalThawDayFromTheClimateNormal() {
        // The Standard year's monthly means put April at −2 °C: the trailing 7-day mean passes −5 °C a few days
        // into April, the order of "about April 10" for the open pole.
        int d = DecayClimate.normalThawDay(Climates.standardDailyMeans(), ThawCountdown.dayOfYear(2, 18) - 1, -5.0);
        int april1 = ThawCountdown.dayOfYear(4, 1) - 1;
        assertTrue(d >= april1 && d <= april1 + 10, "thaw index " + d);
    }
}

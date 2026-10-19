package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.time.Calendar;
import org.junit.jupiter.api.Test;

/**
 * B4, seasoning by the calendar, against the doc's own table ("What it means" for green birch at 50%) and the
 * Blueprint's woodshed line (about 58 days from a June 1 split). Day numbers are absolute days of year 1
 * (May 1 is day 0), so March 20 is in year 1's spring of the next calendar year.
 */
class SeasoningTest {
    static final Seasoning.Rates R = Seasoning.Rates.STANDARD;

    static long day(int month, int dom) { return Calendar.dayOfYear(month, dom); }

    static String date(long absDay) {
        int doy = Calendar.dayOfYearOfDay(absDay);
        return Calendar.monthName(Calendar.month(doy)) + " " + Calendar.dayOfMonth(doy);
    }

    static void within(long got, long want, int days, String what) {
        assertTrue(Math.abs(got - want) <= days, what + ": dry on " + date(got) + " (day " + got + "), the doc says about " + date(want));
    }

    @Test
    void theDocsTable() {
        long dry = Seasoning.forecast(R, 50, day(5, 25), 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        within(dry, day(8, 10), 4, "May 25 split, covered rack");
        dry = Seasoning.forecast(R, 50, day(5, 25), 20, Seasoning.Place.WOODSHED, Seasoning.Form.SPLIT, 730);
        within(dry, day(7, 20), 4, "May 25 split, woodshed");
        dry = Seasoning.forecast(R, 50, day(6, 1), 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        within(dry, day(8, 20), 3, "June 1 split, covered rack");
        dry = Seasoning.forecast(R, 50, day(6, 1), 20, Seasoning.Place.WOODSHED, Seasoning.Form.SPLIT, 730);
        within(dry, day(7, 29), 3, "June 1 split, woodshed (about 58 days)");
        assertTrue(Math.abs((dry - day(6, 1)) - 58) <= 3, "about 58 days: " + (dry - day(6, 1)));
        dry = Seasoning.forecast(R, 50, day(7, 10), 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        within(dry, 365 + day(5, 15), 4, "July 10 split, covered rack: next May");
        dry = Seasoning.forecast(R, 50, day(10, 15), 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        within(dry, 365 + day(7, 4), 4, "October 15 split, covered rack: next July");
        dry = Seasoning.forecast(R, 50, day(3, 20), 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        within(dry, 365 + day(7, 4), 4, "March 20 split, covered rack");
    }

    /** GameTest fire/seasoning_calendar's numbers (M8), checked here on the pure sum. */
    @Test
    void calendarTest() {
        long dry = Seasoning.forecast(R, 50, day(5, 25), 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        assertTrue(dry >= day(7, 25) && dry <= day(8, 15), "May 25 birch on a rack dries between July 25 and August 15: " + date(dry));
        double oct15 = Seasoning.dry(R, 50, day(7, 10), day(10, 15), Seasoning.Place.RACK, Seasoning.Form.SPLIT);
        assertTrue(oct15 > 20, "July 10 birch is still over 20% on October 15: " + oct15);
    }

    @Test
    void multipliersAndSeasons() {
        int june = Calendar.dayOfYear(6, 10);
        assertEquals(0.45, Seasoning.rate(R, june, Seasoning.Place.RACK, Seasoning.Form.SPLIT), 1e-9);
        assertEquals(0.585, Seasoning.rate(R, june, Seasoning.Place.WOODSHED, Seasoning.Form.SPLIT), 1e-9);
        assertEquals(0.18, Seasoning.rate(R, june, Seasoning.Place.RACK, Seasoning.Form.ROUND), 1e-9);
        assertEquals(0.45 * 0.7 * 0.25, Seasoning.rate(R, june, Seasoning.Place.UNCOVERED, Seasoning.Form.LOG), 1e-9);
        assertEquals(0, Seasoning.rate(R, Calendar.dayOfYear(1, 15), Seasoning.Place.WOODSHED, Seasoning.Form.SPLIT), 1e-9);
        assertEquals(0, Seasoning.rate(R, Calendar.dayOfYear(4, 10), Seasoning.Place.RACK, Seasoning.Form.SPLIT), 1e-9, "before melt-out");
        assertEquals(0.10, Seasoning.rate(R, Calendar.dayOfYear(4, 20), Seasoning.Place.RACK, Seasoning.Form.SPLIT), 1e-9);
        assertEquals(0, Seasoning.rate(R, Calendar.dayOfYear(10, 20), Seasoning.Place.RACK, Seasoning.Form.SPLIT), 1e-9, "after freeze-up");
        assertEquals(12, Seasoning.dry(R, 14, day(5, 1), day(9, 1), Seasoning.Place.RACK, Seasoning.Form.SPLIT), 1e-9, "the floor");
        assertEquals(50, Seasoning.dry(R, 50, day(12, 1), day(2, 1), Seasoning.Place.RACK, Seasoning.Form.SPLIT), 1e-9, "no drying in winter");
    }
}

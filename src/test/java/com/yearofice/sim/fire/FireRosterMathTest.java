package com.yearofice.sim.fire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.env.FireSetting;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Fire time wording and the roster, against Every concept from the video › Fire time and the fire roster. */
class FireRosterMathTest {
    static final FireTuning T = FireTuning.standard();

    static FireRosterMath.Row row(FireState s, FireSetting set, double left, double perDay, boolean busy, double dist) {
        return new FireRosterMath.Row("Fire", s, set, left, perDay, busy, dist);
    }

    @Test
    void timesReadMinutesThenHoursThenDays() {
        assertEquals("52 min", FireTime.format(52));
        assertEquals("1 h", FireTime.format(60));
        assertEquals("1 h 10 min", FireTime.format(70));
        assertEquals("about 2 days", FireTime.format(2 * 1440));
        assertEquals("about 1 day", FireTime.format(1440));
        assertEquals("0 min", FireTime.format(-5));
    }

    @Test
    void rowLinesFollowTheTable() {
        assertEquals("Fire, Low. Fuel 52 min.", row(FireState.FIRE, FireSetting.LOW, 52, 1, false, 5).line());
        assertEquals("Coals. Out in 40 min.", row(FireState.COALS, FireSetting.STEADY, 40, 0, false, 5).line());
        assertEquals("Fire, Steady. Fuel 10 min. Low fuel.", row(FireState.FIRE, FireSetting.STEADY, 10, 1, true, 5).line());
    }

    @Test
    void amberNeedsAPotAndUnderFifteenMinutes() {
        assertTrue(row(FireState.FIRE, FireSetting.STEADY, 14, 1, true, 5).amber());
        assertFalse(row(FireState.FIRE, FireSetting.STEADY, 15, 1, true, 5).amber());
        assertFalse(row(FireState.FIRE, FireSetting.STEADY, 5, 1, false, 5).amber());
        assertFalse(row(FireState.COALS, FireSetting.STEADY, 5, 0, true, 5).amber());
    }

    @Test
    void sortedByFuelLeftWithinTwoKmAndCappedAtSixteen() {
        List<FireRosterMath.Row> rows = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) rows.add(row(FireState.FIRE, FireSetting.STEADY, 100 - i, 1, false, 10));
        rows.add(row(FireState.FIRE, FireSetting.STEADY, 1, 1, false, 2500));
        rows.add(row(FireState.OUT, FireSetting.STEADY, 0, 0, false, 3));
        rows.add(row(FireState.EMBER, FireSetting.STEADY, 0, 0, false, 3));
        List<FireRosterMath.Row> out = FireRosterMath.sorted(rows);
        assertEquals(16, out.size());
        assertEquals(81, out.get(0).minutesLeft(), 1e-9);
        for (int i = 1; i < out.size(); i++) assertTrue(out.get(i - 1).minutesLeft() <= out.get(i).minutesLeft());
    }

    @Test
    void headerTotalsTheDaysWood() {
        // Dead spruce burns x0.7 as long, so a Steady ring fire uses 1440 / (40 * 0.7) armloads a day and a Low one 0.4 of that.
        double steady = 1440 * 1.0 / (T.armloadMin(FireBuild.RING) * 0.7);
        List<FireRosterMath.Row> rows = List.of(row(FireState.FIRE, FireSetting.STEADY, 60, steady, false, 5),
                row(FireState.FIRE, FireSetting.LOW, 90, steady * 0.4, false, 5), row(FireState.COALS, FireSetting.STEADY, 30, 0, false, 5));
        assertEquals("3 fires: about " + Math.round(steady * 1.4) + " armloads a day.", FireRosterMath.header(rows));
        assertEquals("No fires lit.", FireRosterMath.header(List.of()));
    }
}

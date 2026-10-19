package com.yearofice.sim.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.fishing.PassiveGear.Catch;
import com.yearofice.sim.fishing.PassiveGear.Fate;
import com.yearofice.sim.fishing.PassiveGear.Hour;
import com.yearofice.sim.fishing.PassiveGear.Kind;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Waters and fish › Passive gear while away: the three GameTests as JUnit, plus the settle's rules. */
class PassiveGearTest {
    private static final long SEED = 2026L;
    private static final double[] C_PER_HOUR = {0.10, 0.05, 0.02}; // whitefish, sucker, grayling, fine mesh
    private static final double[] R_PER_MONTH = {0.3, 0.5, 0.5};

    /** The Environment field's abstract sample for an hour: season and turbidity as a pure function of time. */
    private static Hour sample(int absHour, double q, double coverage, FishStock[] stocks) {
        int day = absHour / 24;
        double season = day < 15 ? 0.5 : 1.0;
        double turbidity = (day % 7 == 3) ? 0.6 : 0.1;
        double[] m = new double[C_PER_HOUR.length];
        for (int s = 0; s < m.length; s++) {
            m[s] = PassiveGear.netHourly(C_PER_HOUR[s], q, coverage, season, stocks[s].fraction()) * (1 - 0.6 * turbidity);
        }
        return new Hour(m);
    }

    private static boolean settleHour(int absHour) { int hod = absHour % 24; return hod == 6 || hod == 18; }

    private static FishStock[] stocks() { return new FishStock[] {FishStock.full(40), FishStock.full(30), FishStock.full(20)}; }

    private static void harvest(FishStock[] st, List<Catch> catches) {
        int[] kept = PassiveGear.kept(catches, st.length);
        for (int s = 0; s < st.length; s++) st[s] = st[s].harvest(kept[s]);
    }

    private static void maybeRegrow(FishStock[] st, int absHour) {
        if (absHour % (24 * 30) == 0 && absHour > 0) {
            for (int s = 0; s < st.length; s++) st[s] = st[s].regrow(R_PER_MONTH[s], FishStock.IMMIGRATION_LAKE, 7);
        }
    }

    private static int period(int absHour) { return absHour % 24 == 6 ? 0 : 1; }

    /** Live: hours sampled as they pass, settled at each dawn and dusk. */
    private static List<Catch> live(int segments, int days) {
        double cov = PassiveGear.netCoverage(segments, 0, 0, true);
        FishStock[] st = stocks();
        List<Catch> all = new ArrayList<>();
        List<Hour> pending = new ArrayList<>();
        for (int h = 0; h < days * 24; h++) {
            maybeRegrow(st, h);
            pending.add(sample(h, 0.6, cov, st));
            int next = h + 1;
            if (settleHour(next)) {
                List<Catch> c = PassiveGear.settle(SEED, 4, 62, -9, next / 24, period(next), Kind.GILL_NET,
                        segments * PassiveGear.HOLDS_NET_SEGMENT, pending, false, true);
                harvest(st, c);
                all.addAll(c);
                pending = new ArrayList<>();
            }
        }
        return all;
    }

    /** Caught up: the chunk loads at the end and settles every missed dawn and dusk from the abstract sample. */
    private static List<Catch> caughtUp(int segments, int days) {
        double cov = PassiveGear.netCoverage(segments, 0, 0, true);
        FishStock[] st = stocks();
        List<Catch> all = new ArrayList<>();
        int last = 0;
        for (int b = 1; b <= days * 24; b++) {
            if (!settleHour(b)) continue;
            List<Hour> hours = new ArrayList<>();
            for (int h = last; h < b; h++) {
                maybeRegrow(st, h);
                hours.add(sample(h, 0.6, cov, st));
            }
            List<Catch> c = PassiveGear.settle(SEED, 4, 62, -9, b / 24, period(b), Kind.GILL_NET,
                    segments * PassiveGear.HOLDS_NET_SEGMENT, hours, false, true);
            harvest(st, c);
            all.addAll(c);
            last = b;
        }
        return all;
    }

    @Test
    void net_catchup_equals_live() {
        List<Catch> a = live(4, 30);
        List<Catch> b = caughtUp(4, 30);
        assertTrue(a.size() > 20, "the net should fish: " + a.size());
        assertEquals(a, b);
    }

    @Test
    void net_three_segments() {
        assertEquals(0.0, PassiveGear.netCoverage(3, 10, 12, false), 0);
        assertTrue(live(3, 30).isEmpty());
        assertTrue(PassiveGear.netCoverage(4, 0, 12, false) == 1.0);
        assertEquals(0.4, PassiveGear.netCoverage(4, 0, 0, true), 1e-12);
    }

    /** food/setline_escape: fish hooked and left 10 game hours are gone 65 ±3% of the time. */
    @Test
    void setline_escape() {
        int n = 10_000, gone = 0;
        for (int i = 0; i < n; i++) {
            double u = Thresholds.stream(SEED, "trap", i, 0, 0, 0, 0).nextDouble();
            if (!PassiveGear.staysOnLine(10, false, u)) gone++;
        }
        assertEquals(65.0, 100.0 * gone / n, 3.0);
        // Through the settle: one hook, a catch certain in the first hour, then 9-10 hours on the line.
        int escaped = 0, trials = 4000;
        for (int i = 0; i < trials; i++) {
            List<Hour> hours = new ArrayList<>();
            hours.add(new Hour(new double[] {50}));
            for (int h = 1; h < 10; h++) hours.add(new Hour(new double[] {0}));
            List<Catch> c = PassiveGear.settle(SEED, i, 0, 0, 1, 0, Kind.SET_LINE, 1, hours, false, false);
            if (c.stream().anyMatch(k -> k.fate() == Fate.ESCAPED)) escaped++;
        }
        double share = (double) escaped / trials;
        assertTrue(share > 0.58 && share < 0.66, "escaped " + share);
    }

    @Test
    void holdsCapTurnsFishAway() {
        List<Hour> hours = new ArrayList<>();
        for (int h = 0; h < 12; h++) hours.add(new Hour(new double[] {5}));
        List<Catch> c = PassiveGear.settle(SEED, 1, 2, 3, 4, 0, Kind.WEIR, PassiveGear.HOLDS_WEIR, hours, false, false);
        long held = c.stream().filter(k -> k.fate() != Fate.TURNED_AWAY).count();
        assertEquals(PassiveGear.HOLDS_WEIR, held);
        for (int i = 1; i < c.size(); i++) assertTrue(c.get(i).catchHour() >= c.get(i - 1).catchHour());
    }

    @Test
    void poissonMean() {
        double sum = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) sum += PassiveGear.poisson(2.5, Thresholds.stream(1, "p", i).nextDouble());
        assertEquals(2.5, sum / n, 0.05);
        assertEquals(0, PassiveGear.poisson(0, 0.99));
    }
}

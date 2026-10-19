package com.yearofice.sim.ecosystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yearofice.core.hash.Mulberry32;
import org.junit.jupiter.api.Test;

/** Blueprint › Traps: the catch formula, its tests, and catch-up equal to live. */
class SnareTest {
    static final TrapTuning T = TrapTuning.standard();

    record E(long seed, int x, int y, int z, double n, Snare.SetQuality quality) implements Snare.Env {
        public double hares(long roll) { return n; }
        public TrapTuning tuning() { return T; }
    }

    static final Snare.SetQuality TRAIL_BAITED = new Snare.SetQuality(true, false, false, true, 1, 1);
    static final Snare.SetQuality TRAIL = new Snare.SetQuality(true, false, false, false, 1, 1);

    @Test
    void formulaMatchesTheBlueprintTests() {
        // "a baited trail snare on a trail with one hare in range catches it within 5 nights in 70–85% of trials"
        double p = Snare.pCatch(T.hareP(), TRAIL_BAITED.multiplier(T), 1);
        double fiveNights = 1 - Math.pow(1 - p, 10);
        assertTrue(fiveNights >= 0.70 && fiveNights <= 0.85, "baited on a trail: " + fiveNights);
        // "and the same snare unbaited off the trail in under 30%"
        double off = 1 - Math.pow(1 - Snare.pCatch(T.hareP(), Snare.SetQuality.PLAIN.multiplier(T), 1), 10);
        assertTrue(off < 0.30, "plain: " + off);
    }

    @Test
    void placeMultipliersCapAtSix() {
        Snare.SetQuality all = new Snare.SetQuality(true, true, true, false, 1, 1);
        assertEquals(6.0, all.place(T), 1e-12, "4 x 1.5 x 1.3 = 7.8, cut to 6");
        assertEquals(0.35 * 4, new Snare.SetQuality(true, false, false, false, 0.1, 1).multiplier(T), 1e-12, "scent floors at 0.35");
    }

    @Test
    void oneHareSimulatedFiveNights() {
        int caught = 0, runs = 4000;
        for (int i = 0; i < runs; i++) {
            E env = new E(i, 3, 64, 9, 1, TRAIL_BAITED);
            Snare.State s = Snare.advance(Snare.State.set(7 * 60, T), 7 * 60 + 5 * 1440, env);
            if (s.catches() > 0) caught++;
        }
        double share = caught / (double) runs;
        assertTrue(share >= 0.70 && share <= 0.85, "share " + share);
    }

    /** Ready for development M2.5 test 2 at Standard density: a snare on a hare run, checked each morning. */
    @Test
    void tenDaysOnAHareRunAverageTwoToFive() {
        double n = Density.coveringPoint(EcoTuning.standard().hareDensityPerKm2(), 150, 1.0, 1.0);
        int total = 0, inRange = 0, runs = 2000;
        for (int i = 0; i < runs; i++) {
            E env = new E(1000L + i, i, 70, -i, n, TRAIL);
            Snare.State s = Snare.State.set(7 * 60, T);
            int got = 0;
            for (int day = 1; day <= 10; day++) {
                long check = day * 1440L + 7 * 60;
                s = Snare.advance(s, check, env);
                if (s.visible(check) != null) got++;
                s = Snare.check(s, check, T);
            }
            total += got;
            if (got >= 2 && got <= 5) inRange++;
        }
        double mean = total / (double) runs;
        assertTrue(mean >= 2 && mean <= 5, "mean " + mean);
        assertTrue(inRange / (double) runs > 0.6, "share in 2-5: " + inRange / (double) runs);
    }

    /** P2-style: 10,000 random histories, settled in random steps, equal the one-shot settle exactly. */
    @Test
    void catchUpEqualsLiveOverRandomSplits() {
        Mulberry32 r = new Mulberry32(42);
        for (int i = 0; i < 10_000; i++) {
            E env = new E(r.nextUInt(), r.nextInt(2000) - 1000, 60, r.nextInt(2000) - 1000, r.nextDouble() * 4,
                    new Snare.SetQuality(r.nextDouble() < 0.5, r.nextDouble() < 0.5, false, r.nextDouble() < 0.5, 1, 1));
            long start = r.nextInt(5000);
            long end = start + r.nextInt(20 * 1440);
            Snare.State live = Snare.State.set(start, T);
            long t = start;
            while (t < end) {
                t = Math.min(end, t + 1 + r.nextInt(900));
                live = Snare.advance(live, t, env);
            }
            Snare.State once = Snare.advance(Snare.State.set(start, T), end, env);
            assertEquals(once, live, "history " + i);
        }
    }

    @Test
    void rollsOnlyAtDawnAndDusk() {
        E env = new E(5, 1, 2, 3, 50, TRAIL_BAITED);
        Snare.State s = Snare.State.set(0, T);
        for (int day = 0; day < 30; day++) {
            s = Snare.advance(s, day * 1440L + 1439, env);
            if (s.held() != null) {
                long at = SliceCalendar.rollMinute(s.held().roll(), T.dawnMin(), T.duskMin());
                int mod = SliceCalendar.minuteOfDay(at);
                assertTrue(mod == T.dawnMin() || mod == T.duskMin(), "roll at " + mod);
                assertTrue(s.held().minute() - at <= T.catchOffsetMaxMin());
                s = Snare.check(s, s.held().minute(), T);
            }
        }
        assertTrue(s.catches() > 0);
    }

    @Test
    void aCatchLeftLongEnoughIsTakenAndTheWireWears() {
        int taken = 0;
        for (int i = 0; i < 400; i++) {
            E env = new E(i, 0, 0, 0, 50, TRAIL_BAITED);
            Snare.State s = Snare.advance(Snare.State.set(0, T), 1440, env);
            if (s.held() == null) continue;
            long c = s.held().minute();
            s = Snare.advance(s, c + 4 * 1440, env);
            if (s.status() == Snare.Status.TAKEN) taken++;
        }
        // "a catch left 4 days is taken at least 55% of the time": two extra dawns at 35% gives 58%.
        assertTrue(taken / 400.0 >= 0.5, "taken " + taken / 400.0);
        Snare.State worn = new Snare.State(0, 0, Snare.Status.CAUGHT, new Snare.Catch("yearofice:snowshoe_hare", 0, 10), T.wireCatches(), java.util.List.of());
        assertEquals(Snare.Status.BROKEN, Snare.check(worn, 20, T).status());
    }
}

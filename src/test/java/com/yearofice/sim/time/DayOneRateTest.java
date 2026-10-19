package com.yearofice.sim.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Day 1's half-speed clock (Blueprint › Start site › Day 1 rules) as a Layer 1 rate: exact, and lowest priority. */
class DayOneRateTest {
    static final Rates R = Rates.STANDARD;

    @Test
    void day_one_is_half_the_world_rate_on_every_day_length() {
        for (int l : R.dayLengths()) {
            int world = R.dsPerTick(Rate.WORLD, l);
            assertEquals(world * 50, R.centiDsPerTick(Rate.DAY_ONE, l), "L" + l);
            assertEquals(world / 2, R.dsPerTick(Rate.DAY_ONE, l), "L" + l + " whole ds (floor)");
            assertEquals(world * 100, R.centiDsPerTick(Rate.WORLD, l));
        }
    }

    /** The clock's carry: summed over any run of ticks, Day 1 never drifts from half the World rate by a ds. */
    @Test
    void the_carry_keeps_day_one_exact() {
        for (int l : R.dayLengths()) {
            int carry = 0;
            long total = 0;
            for (int t = 1; t <= 1200; t++) {
                carry += R.centiDsPerTick(Rate.DAY_ONE, l);
                int step = carry / 100;
                carry -= step * 100;
                total += step;
                long exact = (long) R.dsPerTick(Rate.WORLD, l) * t / 2;
                assertEquals(exact, total, "L" + l + " after " + t + " ticks");
            }
        }
        // 24-minute day: 15 ds a tick, so 20 real minutes (24,000 ticks) cover 10 game hours, 12:00 to 22:00
        assertEquals(10 * 60 * 600, 24_000L * R.dsPerTick(Rate.DAY_ONE, 24));
    }

    @Test
    void any_other_request_wins_for_the_player_and_the_slowest_wins_the_world() {
        assertEquals(Rate.DAY_ONE, RateArbiter.forPlayer(EnumSet.of(Rate.DAY_ONE)));
        assertEquals(Rate.TRAVEL, RateArbiter.forPlayer(EnumSet.of(Rate.DAY_ONE, Rate.TRAVEL)));
        assertEquals(Rate.SLEEP, RateArbiter.forPlayer(EnumSet.of(Rate.DAY_ONE, Rate.SLEEP)));
        assertEquals(Rate.WORK, RateArbiter.forPlayer(EnumSet.of(Rate.DAY_ONE, Rate.WORK)));
        assertEquals(Rate.DAY_ONE, RateArbiter.forWorld(List.of(Rate.WORLD, Rate.DAY_ONE), R, 24));
        assertEquals(Rate.TRAVEL, RateArbiter.forWorld(List.of(Rate.TRAVEL, Rate.DAY_ONE), R, 24));
        // on the 72-minute day half the World rate (5) is slower than Travel (6)
        assertEquals(Rate.DAY_ONE, RateArbiter.forWorld(List.of(Rate.TRAVEL, Rate.DAY_ONE), R, 72));
    }

    @Test
    void the_share_is_data_and_checked() {
        Rates third = new Rates(3, 6, 30, 150, 600, 1800, new int[] {24, 48}, 33);
        assertEquals(30 * 33, third.centiDsPerTick(Rate.DAY_ONE, 24));
        assertThrows(IllegalArgumentException.class, () -> new Rates(3, 6, 30, 150, 600, 1800, new int[] {24}, 0));
        assertThrows(IllegalArgumentException.class, () -> new Rates(3, 6, 30, 150, 600, 1800, new int[] {24}, 101));
    }
}

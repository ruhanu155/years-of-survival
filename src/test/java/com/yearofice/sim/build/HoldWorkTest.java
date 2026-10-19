package com.yearofice.sim.build;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Hold-to-work timing for the slice's camp work: cut boughs (1 game minute) and the bough bed (1 game hour). */
class HoldWorkTest {
    @Test
    void work_rate_turns_game_minutes_into_ticks() {
        // Work is 150 ds a tick, 5 game minutes a real second: the bed's game hour is 12 s, 240 ticks.
        assertEquals(240, HoldWork.ticksFor(60, 150, 10));
        assertEquals(10, HoldWork.ticksFor(1, 150, 10), "a one-minute cut still shows its ring for 10 ticks");
        assertEquals(4, HoldWork.ticksFor(1, 150, 1));
        assertEquals(1, HoldWork.ticksFor(0, 150, 0));
        assertEquals(6, HoldWork.ticksFor(1.5, 150, 1), "exactly 6 ticks");
        assertEquals(5, HoldWork.ticksFor(1.1, 150, 1), "4.4 ticks rounds up to 5");
    }

    @Test
    void a_held_key_continues_and_a_gap_restarts() {
        HoldWork.Hold h = HoldWork.Hold.start(42, 100);
        h = h.seen(42, 104).seen(42, 108).seen(42, 114);
        assertEquals(14, h.held(114), "repeats 4-6 ticks apart keep the hold");
        assertTrue(h.live(120));
        assertFalse(h.live(121), "7 ticks without the key is let go");
        HoldWork.Hold gap = h.seen(42, 121);
        assertEquals(0, gap.held(121), "a gap starts over");
        HoldWork.Hold other = h.seen(43, 116);
        assertEquals(0, other.held(116), "another block starts over");
    }

    @Test
    void yields_fit_the_stack() {
        assertEquals(10, HoldWork.fits(10, 30, 64));
        assertEquals(4, HoldWork.fits(10, 60, 64));
        assertEquals(0, HoldWork.fits(10, 64, 64));
    }
}

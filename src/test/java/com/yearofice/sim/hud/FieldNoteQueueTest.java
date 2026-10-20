package com.yearofice.sim.hud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FieldNoteQueueTest {
    @Test
    void oneCardPerTenRealMinutesAndInOrder() {
        FieldNoteQueue q = new FieldNoteQueue();
        assertTrue(q.trigger("a", 0));
        assertTrue(q.trigger("b", 1));
        assertFalse(q.trigger("a", 2), "never twice");
        assertEquals("a", q.poll(100, 5, false).orElseThrow());
        assertTrue(q.poll(100 + FieldNoteQueue.GAP_TICKS - 1, 6, false).isEmpty(), "inside the gap");
        assertEquals("b", q.poll(100 + FieldNoteQueue.GAP_TICKS, 7, false).orElseThrow());
        assertTrue(q.poll(10_000_000, 8, false).isEmpty());
    }

    @Test
    void blockedKeepsItsPlaceAndStaleCardsDrop() {
        FieldNoteQueue q = new FieldNoteQueue();
        q.trigger("a", 0);
        assertTrue(q.poll(0, 1, true).isEmpty(), "close clock / danger");
        assertEquals(1, q.pending());
        assertEquals("a", q.poll(1, 2, false).orElseThrow());
        q.trigger("old", 10);
        assertTrue(q.poll(10_000_000, 10 + FieldNoteQueue.STALE_GAME_MIN + 1, false).isEmpty(), "filed unseen");
        assertEquals(0, q.pending());
        assertTrue(q.isKnown("old"));
    }

    @Test
    void musicBandsFollowTheDoc() {
        long long_ = 60L * 60 * 20, far = 10 * long_;
        assertEquals(MusicPlan.Piece.DAWN, MusicPlan.choose(80, true, 99, far, far, false, 0));
        assertEquals(MusicPlan.Piece.DAY, MusicPlan.choose(80, false, 99, far, far, false, 0));
        assertEquals(MusicPlan.Piece.NONE, MusicPlan.choose(80, false, 99, 10, far, false, 0));
        assertEquals(MusicPlan.Piece.DAY, MusicPlan.choose(50, false, 99, far, far, false, 0.5));
        assertEquals(MusicPlan.Piece.NONE, MusicPlan.choose(20, false, 99, far, far, false, 0), "low: no fire, no music");
        assertEquals(MusicPlan.Piece.FIRE, MusicPlan.choose(20, false, 3, far, far, false, 0));
        assertEquals(MusicPlan.Piece.NONE, MusicPlan.choose(20, false, 5, far, far, false, 0), "beyond 4 m");
        assertEquals(MusicPlan.Piece.NONE, MusicPlan.choose(80, false, 3, far, 100, false, 0), "5 min after danger");
        assertEquals(MusicPlan.Piece.NONE, MusicPlan.choose(80, true, 3, far, far, true, 0));
    }

    @Test
    void optionRule() {
        var on = com.yearofice.hud.fieldnote.FieldNoteOptions.Mode.ON;
        var first = com.yearofice.hud.fieldnote.FieldNoteOptions.Mode.FIRST_YEAR;
        var off = com.yearofice.hud.fieldnote.FieldNoteOptions.Mode.OFF;
        assertTrue(com.yearofice.hud.fieldnote.FieldNoteOptions.shows(on, false));
        assertTrue(com.yearofice.hud.fieldnote.FieldNoteOptions.shows(first, true));
        assertFalse(com.yearofice.hud.fieldnote.FieldNoteOptions.shows(first, false));
        assertFalse(com.yearofice.hud.fieldnote.FieldNoteOptions.shows(off, true));
    }
}

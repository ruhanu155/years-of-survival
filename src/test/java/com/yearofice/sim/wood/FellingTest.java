package com.yearofice.sim.wood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The felling numbers (Trees, felling and firewood › Tools; GameTest {@code build/fell_swings}'s arithmetic): the
 * crate hatchet fells a 25 cm white spruce in 6 swings and 24 game minutes, the felling axe in 4 swings and 10
 * minutes, and a 40 cm spruce takes the hatchet 16; the preview's 180 kcal; the notch is the first third.
 */
class FellingTest {
    @Test
    void swingsByTheFormula() {
        assertEquals(6, Felling.swings(ToolStats.CRATE_HATCHET.swings25(), 25));
        assertEquals(24.0, Felling.minutes(ToolStats.CRATE_HATCHET, 25), 1e-9);
        assertEquals(4, Felling.swings(ToolStats.FELLING_AXE.swings25(), 25));
        assertEquals(10.0, Felling.minutes(ToolStats.FELLING_AXE, 25), 1e-9);
        assertEquals(16, Felling.swings(ToolStats.CRATE_HATCHET.swings25(), 40));
        assertEquals(3, Felling.swings(ToolStats.WOODEN_AXE.swings25(), 10), "the wooden axe: 3 swings at 10 cm");
        assertEquals(180.0, Felling.kcal(24), 1e-9, "Fell spruce: 6 swings, about 0.4 h, 180 kcal");
    }

    @Test
    void diameterAndReach() {
        assertEquals(24.0, TreeKind.WHITE_SPRUCE.diameterCm(15), 1e-9);
        assertEquals(6, Felling.swings(6, TreeKind.WHITE_SPRUCE.diameterCm(15)), "a 15 m white spruce is a 6-swing tree");
        assertTrue(Felling.canFell(ToolStats.CRATE_HATCHET, 45));
        assertFalse(Felling.canFell(ToolStats.CRATE_HATCHET, 45.5));
        assertFalse(Felling.canFell(ToolStats.WOODEN_AXE, 24));
        assertEquals(2, Felling.notchSwings(6));
        assertEquals(5, Felling.notchSwings(16));
        assertEquals(1, Felling.notchSwings(2));
    }

    @Test
    void bearings() {
        assertEquals(0, Felling.bearing(0, -1), 1e-9, "north is -z");
        assertEquals(90, Felling.bearing(1, 0), 1e-9);
        assertEquals(180, Felling.bearing(0, 1), 1e-9);
        assertEquals(270, Felling.bearing(-1, 0), 1e-9);
        assertEquals(20, Felling.angleBetween(350, 10), 1e-9);
        assertEquals(180, Felling.angleBetween(0, 180), 1e-9);
        assertEquals("south-east", Felling.compass(135));
        double[] u = Felling.unit(90);
        assertEquals(1, u[0], 1e-9);
        assertEquals(0, u[1], 1e-9);
    }

    /** Notch from the arrow side, then the back cut from the far side; the tree falls along the arrow (toward the notch). */
    @Test
    void notchThenBackCut() {
        FellingGame g = new FellingGame(6);
        assertEquals(FellingGame.Strike.NOTCH, g.strike(180));
        assertEquals(180, g.arrow(), 1e-9, "the first swing locks the arrow toward the feller");
        assertEquals(FellingGame.Strike.NOTCH_FIRST, g.strike(0), "the far side counts nothing before the notch is cut");
        assertEquals(FellingGame.Strike.NOTCH_HALF, g.strike(110), "off the marked notch counts half");
        assertEquals(FellingGame.Strike.NOTCH_HALF, g.strike(250));
        assertTrue(g.notchDone());
        assertEquals(FellingGame.Strike.WRONG_SIDE, g.strike(170), "from the notch side the back cut counts nothing");
        for (int i = 0; i < 3; i++) assertEquals(FellingGame.Strike.BACK, g.strike(5));
        assertFalse(g.done());
        assertTrue(g.lastSwingNext());
        assertEquals(FellingGame.Strike.BACK, g.strike(355));
        assertTrue(g.done());
        assertEquals(180, g.fallBearing(), 1e-9);
        assertEquals(1.0, g.share(), 1e-9);
        assertEquals(9, g.struck());
    }

    @Test
    void cleanFellIsExactlyTheSwings() {
        FellingGame g = new FellingGame(Felling.swings(6, 25));
        int n = 0;
        while (!g.done()) {
            g.strike(g.notchDone() ? 90 : 270);
            n++;
        }
        assertEquals(6, n);
    }

    @Test
    void buckingNumbers() {
        assertEquals(14.0, Bucking.usableM(15), 1e-9);
        assertEquals(28, Bucking.rounds(14));
        assertEquals(0.75, Bucking.roundMinutes(ToolStats.CRATE_HATCHET.buckMin()), 1e-9);
        assertEquals(3.0, Bucking.placedBlockMinutes(3), 1e-9, "cut one placed block: 3 min with the crate hatchet");
        assertEquals(6.0, Bucking.sneakTopMinutes(3, 2.0), 1e-9, "cut the top block: 6 min");
        assertEquals(40, Bucking.boughs(TreeKind.WHITE_SPRUCE));
        assertEquals(0, Bucking.boughs(TreeKind.BIRCH));
    }

    @Test
    void crack() {
        assertTrue(Splitting.onCrack(0.5, 0.5, 0));
        assertTrue(Splitting.onCrack(0.5, 0.2, 0), "a 0° crack runs north-south across the end");
        assertFalse(Splitting.onCrack(0.8, 0.5, 0));
        assertTrue(Splitting.onCrack(0.8, 0.5, 2), "90° runs east-west");
        assertTrue(Splitting.onCrack(0.7, 0.7, 3) || Splitting.onCrack(0.7, 0.7, 1), "a diagonal");
        assertFalse(Splitting.onCrack(0.98, 0.5, 2), "off the round");
        assertEquals(2, Splitting.strikesLeft(ToolStats.CRATE_HATCHET, 1));
    }

    @Test
    void marksRoundTrip() {
        long m = TreeMarks.pack(15, 3, -60, TreeMarks.State.TOPPED, 11, 4_000_000_000L);
        assertEquals(15, TreeMarks.lx(m));
        assertEquals(3, TreeMarks.lz(m));
        assertEquals(-60, TreeMarks.baseY(m));
        assertEquals(TreeMarks.State.TOPPED, TreeMarks.state(m));
        assertEquals(11, TreeMarks.height(m));
        assertEquals(4_000_000_000L, TreeMarks.day(m));
        assertEquals(TreeMarks.key(15, 3, -60), TreeMarks.key(m));
        long f = TreeMarks.pack(0, 0, 320, TreeMarks.State.FELLED, 0, 0);
        assertEquals(320, TreeMarks.baseY(f));
        assertEquals(TreeMarks.State.FELLED, TreeMarks.state(f));
    }
}

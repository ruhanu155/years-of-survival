package com.yearofice.sim.wood;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A1, placed logs never fall (Trees, felling and firewood › Placed logs never fall): the fall search counts only
 * natural logs joined to a rooted base, follows a drunken spruce's lean, and never crosses a stop.
 */
class TrunkSearchTest {
    static final int WS = TreeKind.WHITE_SPRUCE.ordinal(), BS = TreeKind.BLACK_SPRUCE.ordinal();

    static final class World implements TrunkSearch.Probe {
        final Map<Long, TrunkSearch.Seen> cells = new HashMap<>();

        static long k(int x, int y, int z) { return ((long) x & 0xFFFFF) | (((long) y & 0xFFFFF) << 20) | (((long) z & 0xFFFFF) << 40); }

        World put(int x, int y, int z, TrunkSearch.Seen s) { cells.put(k(x, y, z), s); return this; }

        World ground(int r) {
            for (int z = -r; z <= r; z++) for (int x = -r; x <= r; x++) put(x, 0, z, TrunkSearch.Seen.GROUND);
            return this;
        }

        World log(int x, int y, int z, int kind) { return put(x, y, z, new TrunkSearch.Seen(TrunkSearch.Cell.LOG, kind)); }

        World leaves(int x, int y, int z, int kind) { return put(x, y, z, new TrunkSearch.Seen(TrunkSearch.Cell.LEAVES, kind)); }

        @Override
        public TrunkSearch.Seen at(int x, int y, int z) { return cells.getOrDefault(k(x, y, z), TrunkSearch.Seen.OTHER); }
    }

    @Test
    void rootedColumn() {
        World w = new World().ground(4);
        for (int y = 1; y <= 8; y++) w.log(0, y, 0, WS);
        TrunkSearch.Trunk t = TrunkSearch.find(w, 0, 5, 0);
        assertNotNull(t);
        assertEquals(8, t.height());
        assertEquals(1, t.base()[1]);
        assertEquals(8, t.top()[1]);
        assertNull(t.stoppedAt());
    }

    @Test
    void notRootedIsNoTree() {
        World w = new World();
        for (int y = 5; y <= 8; y++) w.log(0, y, 0, WS);
        assertNull(TrunkSearch.find(w, 0, 6, 0), "a floating log column is not a tree");
        World v = new World().ground(2).put(0, 1, 0, TrunkSearch.Seen.STOP);
        for (int y = 2; y <= 5; y++) v.log(0, y, 0, WS);
        assertNull(TrunkSearch.find(v, 0, 3, 0), "logs standing on a placed block are not rooted");
    }

    /** A wall of placed logs on top of a trunk ends the column: nothing past the stop is counted. */
    @Test
    void neverCrossesAStop() {
        World w = new World().ground(4);
        for (int y = 1; y <= 4; y++) w.log(0, y, 0, WS);
        w.put(0, 5, 0, TrunkSearch.Seen.STOP);
        for (int y = 6; y <= 9; y++) w.log(0, y, 0, WS);
        TrunkSearch.Trunk t = TrunkSearch.find(w, 0, 2, 0);
        assertEquals(4, t.height());
        assertEquals(5, t.stoppedAt()[1]);
        for (int[] p : t.logs()) assertTrue(p[1] < 5);
        assertNull(TrunkSearch.find(w, 0, 7, 0), "the logs above the stop stand on it, not on soil");
    }

    /** A cabin's placed logs report as stops: a search started from one finds no tree. */
    @Test
    void cabinIsNoTree() {
        World w = new World().ground(6);
        for (int y = 1; y <= 4; y++) for (int x = -2; x <= 2; x++) { w.put(x, y, -2, TrunkSearch.Seen.STOP); w.put(x, y, 2, TrunkSearch.Seen.STOP); }
        assertNull(TrunkSearch.find(w, 0, 2, -2));
        // A natural spruce right against the wall clears only its own column.
        for (int y = 1; y <= 10; y++) w.log(0, y, -3, WS);
        TrunkSearch.Trunk t = TrunkSearch.find(w, 0, 1, -3);
        assertEquals(10, t.height());
        for (int[] p : t.logs()) assertEquals(-3, p[2]);
    }

    @Test
    void drunkenLean() {
        World w = new World().ground(6);
        int x = 0;
        for (int y = 1; y <= 9; y++) {
            if (y > 1 && (y - 1) % 3 == 0) x++;
            w.log(x, y, 0, BS);
        }
        TrunkSearch.Trunk t = TrunkSearch.find(w, 2, 8, 0);
        assertNotNull(t);
        assertEquals(9, t.height());
        assertEquals(0, t.base()[0]);
        assertEquals(2, t.top()[0]);
    }

    @Test
    void otherKindIsNotPartOfTheTrunk() {
        World w = new World().ground(3);
        for (int y = 1; y <= 4; y++) w.log(0, y, 0, WS);
        for (int y = 5; y <= 7; y++) w.log(0, y, 0, BS);
        assertEquals(4, TrunkSearch.find(w, 0, 1, 0).height());
    }

    /** The crown takes this tree's leaves, not a neighbour's that are nearer the neighbour, and never past a stop. */
    @Test
    void crownStaysWithItsTree() {
        World w = new World().ground(8);
        for (int y = 1; y <= 6; y++) { w.log(0, y, 0, WS); w.log(4, y, 0, WS); }
        for (int y = 3; y <= 7; y++) for (int x = -3; x <= 7; x++) for (int z = -2; z <= 2; z++) {
            if ((x == 0 || x == 4) && z == 0 && y <= 6) continue;
            w.leaves(x, y, z, WS);
        }
        w.put(-3, 5, 0, TrunkSearch.Seen.STOP);
        TrunkSearch.Trunk t = TrunkSearch.find(w, 0, 1, 0);
        List<int[]> crown = TrunkSearch.crown(w, t);
        assertTrue(!crown.isEmpty());
        for (int[] p : crown) {
            assertTrue(p[0] <= 2, "leaf at x " + p[0] + " is nearer the neighbour at x 4");
            assertTrue(!(p[0] == -3 && p[1] == 5 && p[2] == 0), "a stop is never in the crown");
        }
        assertTrue(crown.stream().anyMatch(p -> p[0] == 2 && p[2] == 0), "the midway leaf (tie) stays with this tree");
    }
}

package com.yearofice.sim.wood;

import java.util.ArrayList;
import java.util.List;

/**
 * The fall search (video 2, A1; Trees, felling and firewood › Placed logs never fall): which blocks a felling
 * clears. Pure, over a {@link Probe} the adapter answers from the level.
 *
 * <ul>
 *   <li>Only natural log blocks of one kind count, and only when the column is rooted: soil within 1 block below
 *       its lowest log.</li>
 *   <li>The column runs up from that base, straight or one block sideways where a drunken black spruce leans, and
 *       ends at the first block that is a stop ({@code #yearofice:fell_stops}, {@code placed=true}, or a bit in
 *       {@code yearofice:placed_mask}). Nothing past a stop is ever counted, so a cabin of placed logs, or a wall
 *       built against a trunk, can never come down with a tree.</li>
 *   <li>The crown is the leaves of the same kind joined to the trunk, within {@link #CROWN_R} blocks of it
 *       sideways, that are nearer this trunk than any other trunk of that kind; a stop is never crossed.</li>
 * </ul>
 */
public final class TrunkSearch {
    private TrunkSearch() {}

    /** What the probe sees in one block. */
    public enum Cell { OTHER, LOG, LEAVES, STOP, GROUND }

    /** One block as the probe reports it: a cell and, for logs and leaves, the tree kind's ordinal. */
    public record Seen(Cell cell, int kind) {
        public static final Seen OTHER = new Seen(Cell.OTHER, -1), STOP = new Seen(Cell.STOP, -1), GROUND = new Seen(Cell.GROUND, -1);

        public boolean log(int k) { return cell == Cell.LOG && kind == k; }

        public boolean leaves(int k) { return cell == Cell.LEAVES && kind == k; }
    }

    public interface Probe {
        Seen at(int x, int y, int z);
    }

    /** The tallest column searched. */
    public static final int MAX_HEIGHT = 64;
    /** Crown reach from the trunk, in blocks (spruce crowns reach 3, birch 3; one more for jitter). */
    public static final int CROWN_R = 4;
    /** Leaves searched above the top log (the spruce leader is 2). */
    public static final int CROWN_ABOVE = 3;

    /**
     * A rooted trunk: its kind, base (the lowest log) and every log from base to top in order.
     *
     * @param stoppedAt the stop above the top log, or null when the column ended in air or leaves
     */
    public record Trunk(int kind, int[] base, List<int[]> logs, int[] stoppedAt) {
        public int height() { return logs.size(); }

        public int[] top() { return logs.get(logs.size() - 1); }

        public boolean contains(int x, int y, int z) {
            for (int[] p : logs) if (p[0] == x && p[1] == y && p[2] == z) return true;
            return false;
        }
    }

    private static final int[][] SIDE = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    /** The rooted trunk through the natural log at (x, y, z), or null when there is none (not a log, or not rooted). */
    public static Trunk find(Probe p, int x, int y, int z) {
        Seen s = p.at(x, y, z);
        if (s.cell() != Cell.LOG) return null;
        int k = s.kind();
        // Down to the lowest log of the column.
        int cx = x, cy = y, cz = z;
        for (int i = 0; i < MAX_HEIGHT; i++) {
            int[] next = step(p, k, cx, cy - 1, cz);
            if (next == null) break;
            cx = next[0]; cy = next[1]; cz = next[2];
        }
        // Rooted: soil right below, or one block of air or litter and then soil.
        Seen below = p.at(cx, cy - 1, cz);
        boolean rooted = below.cell() == Cell.GROUND
                || (below.cell() == Cell.OTHER && p.at(cx, cy - 2, cz).cell() == Cell.GROUND);
        if (!rooted) return null;
        // Up from the base to the first stop.
        List<int[]> logs = new ArrayList<>();
        logs.add(new int[] {cx, cy, cz});
        int[] stopped = null;
        for (int i = 1; i < MAX_HEIGHT; i++) {
            Seen up = p.at(cx, cy + 1, cz);
            if (up.cell() == Cell.STOP) { stopped = new int[] {cx, cy + 1, cz}; break; }
            int[] next = step(p, k, cx, cy + 1, cz);
            if (next == null) break;
            cx = next[0]; cy = next[1]; cz = next[2];
            logs.add(next);
        }
        return new Trunk(k, logs.get(0), List.copyOf(logs), stopped);
    }

    /** The log of kind {@code k} continuing the column at level {@code y}: straight, else one block sideways (a lean). */
    private static int[] step(Probe p, int k, int x, int y, int z) {
        if (p.at(x, y, z).log(k)) return new int[] {x, y, z};
        if (p.at(x, y, z).cell() != Cell.OTHER && p.at(x, y, z).cell() != Cell.LEAVES) return null;
        for (int[] d : SIDE) if (p.at(x + d[0], y, z + d[1]).log(k)) return new int[] {x + d[0], y, z + d[1]};
        return null;
    }

    /**
     * The crown of {@code t}: leaves of its kind joined face to face to the trunk, within {@link #CROWN_R} blocks of
     * the trunk's log at that height (or its top above it) and nearer this trunk than any other log of the kind.
     */
    public static List<int[]> crown(Probe p, Trunk t) {
        int k = t.kind();
        int baseY = t.base()[1], topY = t.top()[1];
        int side = 2 * (CROWN_R + 2) + 1, h = topY - baseY + CROWN_ABOVE + 2;
        // Visited flags on a box around the trunk; trunk offsets differ by the lean, so the box is centred on the base.
        int lean = 0;
        for (int[] l : t.logs()) lean = Math.max(lean, Math.max(Math.abs(l[0] - t.base()[0]), Math.abs(l[2] - t.base()[2])));
        int r = CROWN_R + 1 + lean;
        side = 2 * r + 1;
        boolean[] seen = new boolean[side * side * h];
        List<int[]> out = new ArrayList<>();
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        for (int[] l : t.logs()) q.add(l);
        int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] f : faces) {
                int nx = c[0] + f[0], ny = c[1] + f[1], nz = c[2] + f[2];
                int ox = nx - t.base()[0] + r, oz = nz - t.base()[2] + r, oy = ny - baseY;
                if (ox < 0 || oz < 0 || ox >= side || oz >= side || oy < 0 || oy >= h) continue;
                int i = (oy * side + oz) * side + ox;
                if (seen[i]) continue;
                seen[i] = true;
                if (!p.at(nx, ny, nz).leaves(k)) continue;
                int[] axis = axisAt(t, ny);
                int dx = nx - axis[0], dz = nz - axis[2];
                if (Math.abs(dx) > CROWN_R || Math.abs(dz) > CROWN_R) continue;
                if (ny > topY + CROWN_ABOVE) continue;
                if (nearerOther(p, t, k, nx, ny, nz, dx * dx + dz * dz)) continue;
                int[] leaf = {nx, ny, nz};
                out.add(leaf);
                q.add(leaf);
            }
        }
        return out;
    }

    /** The trunk's log at height y, or its top above the top. */
    static int[] axisAt(Trunk t, int y) {
        int i = y - t.base()[1];
        if (i < 0) return t.base();
        if (i >= t.logs().size()) return t.top();
        return t.logs().get(i);
    }

    /** Whether another trunk's log of the kind, at the leaf's height, is nearer the leaf than this trunk (d2 away). */
    private static boolean nearerOther(Probe p, Trunk t, int k, int x, int y, int z, int d2) {
        int r = (int) Math.ceil(Math.sqrt(d2));
        for (int dz = -r; dz <= r; dz++) for (int dx = -r; dx <= r; dx++) {
            int e2 = dx * dx + dz * dz;
            if (e2 >= d2) continue;
            int ox = x + dx, oz = z + dz;
            // A neighbour's log at the leaf's height, or its top a little below (a crown reaches over its top).
            for (int dy = 0; dy <= CROWN_ABOVE; dy++) {
                if (t.contains(ox, y - dy, oz)) break;
                if (p.at(ox, y - dy, oz).log(k)) return true;
            }
        }
        return false;
    }
}

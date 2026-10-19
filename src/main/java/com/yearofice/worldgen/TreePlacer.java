package com.yearofice.worldgen;

/**
 * Where a planned tree's blocks go in the world (Terrain to blocks, trees): pure, so the feature and the tests
 * share it. Everything is read from the layers, never from the level, so a tree comes out the same whichever
 * neighbouring chunk was decorated first.
 *
 * <ul>
 *   <li>The base is the block above the tree's own column's ground, as {@link ValleyWorld#columns} gives it (the
 *       numbers the generator wrote), and only on a soil column: never on water, a riffle lip or rock.</li>
 *   <li>A part at or below its column's top (inside the ground or the water) is not written.</li>
 *   <li>Only parts joined to the base through the tree's own parts (26-neighbours) are written, so ground
 *       clipping a crown on a slope never leaves a piece hanging in the air.</li>
 *   <li>A fallen log's pieces lie each on their own column's ground, within a block of the stump's, on soil.</li>
 * </ul>
 */
public final class TreePlacer {
    private TreePlacer() {}

    /** Blocks around a chunk that every standing tree whose base is in it fits in (crowns reach at most 3). */
    public static final int MARGIN = 4;

    public interface Sink {
        void put(int x, int y, int z, TreeShapes.Part part);
    }

    /** The columns a chunk's trees read: the chunk at (x0, z0) plus {@link #MARGIN} on every side. */
    public static ValleyWorld.Columns columnsFor(ValleyWorld w, int x0, int z0) {
        return w.columns(x0 - MARGIN, z0 - MARGIN, 16 + 2 * MARGIN, 16 + 2 * MARGIN);
    }

    /** The base block's y (first block above the ground) of a column, or {@link ValleyWorld#NONE} if no tree stands there. */
    public static int baseY(ValleyWorld.Columns c, int x, int z) {
        int i = c.at(x, z);
        return c.kind[i] == ValleyWorld.SOIL ? c.ground[i] + 1 : ValleyWorld.NONE;
    }

    /** Rank of a part where two parts of one tree share a block: the same order the feature writes in. */
    static int rank(TreeShapes.Part p) {
        return switch (p) {
            case LOG, STEM -> 4;
            case STUMP, FALLEN_X, FALLEN_Z -> 3;
            case LEAVES -> 2;
            case BRUSH -> 1;
        };
    }

    /** Whether a part is wood a crown hangs from. */
    public static boolean wood(TreeShapes.Part p) { return p == TreeShapes.Part.LOG || p == TreeShapes.Part.STEM || p == TreeShapes.Part.STUMP; }

    /**
     * Emits tree {@code t}'s blocks in world coordinates by the rules above; {@code c} should hold the tree's
     * column and its crown's (other columns are read one at a time). Returns false if the tree cannot stand.
     */
    public static boolean place(ValleyWorld w, TreePlan.Tree t, ValleyWorld.Columns c, Sink out) {
        if (!c.contains(t.x(), t.z())) c = w.columns(t.x() - MARGIN, t.z() - MARGIN, 2 * MARGIN + 1, 2 * MARGIN + 1);
        int by = baseY(c, t.x(), t.z());
        if (by == ValleyWorld.NONE) return false;
        final ValleyWorld.Columns cols = c;
        // Standing parts on a small dense grid around the base (no hashing: the worldgen lint keeps the stages free
        // of hash-ordered collections). Crowns reach 3 and a drunken trunk a few blocks, well inside R.
        int sy = t.height() + 4, side = 2 * R + 1;
        byte[] grid = new byte[side * side * sy];
        TreeShapes.build(t, (dx, dy, dz, part) -> {
            int px = t.x() + dx, pz = t.z() + dz;
            ValleyWorld.Columns cc = cols.contains(px, pz) ? cols : w.columns(px, pz, 1, 1);
            int i = cc.at(px, pz);
            if (part == TreeShapes.Part.FALLEN_X || part == TreeShapes.Part.FALLEN_Z) {
                if (cc.kind[i] != ValleyWorld.SOIL) return;
                int gy = cc.ground[i] + 1;
                if (Math.abs(gy - by) <= 1) out.put(px, gy, pz, part);
                return;
            }
            if (by + dy <= cc.top(i)) return; // inside the ground or the water
            if (Math.abs(dx) > R || Math.abs(dz) > R || dy < 0 || dy >= sy) return; // never: M2WorldTest holds shapes inside R
            int g = ((dy * side) + dz + R) * side + dx + R;
            byte had = grid[g];
            if (had == 0 || rank(part) > rank(PARTS[had - 1])) grid[g] = (byte) (part.ordinal() + 1);
        });
        // Keep what hangs from the base.
        int base = (R * side) + R;
        if (grid[base] == 0) return true;
        int[] q = new int[grid.length];
        int head = 0, tail = 0;
        q[tail++] = base;
        grid[base] = (byte) -grid[base]; // negative: reached
        while (head < tail) {
            int g = q[head++];
            int x = g % side - R, z = (g / side) % side - R, y = g / (side * side);
            out.put(t.x() + x, by + y, t.z() + z, PARTS[-grid[g] - 1]);
            for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx, ny = y + dy, nz = z + dz;
                if (nx < -R || nx > R || nz < -R || nz > R || ny < 0 || ny >= sy) continue;
                int o = ((ny * side) + nz + R) * side + nx + R;
                if (grid[o] > 0) {
                    grid[o] = (byte) -grid[o];
                    q[tail++] = o;
                }
            }
        }
        return true;
    }

    /** Half-width of the placer grid around a base. */
    static final int R = 8;
    private static final TreeShapes.Part[] PARTS = TreeShapes.Part.values();

    // Offsets packed into a long: 21 bits each, biased.
    private static final int B = 1 << 20;

    static long key(int x, int y, int z) {
        return ((long) (x + B) << 42) | ((long) (y + B) << 21) | (z + B);
    }

    static int kx(long k) { return (int) ((k >>> 42) & 0x1FFFFF) - B; }

    static int ky(long k) { return (int) ((k >>> 21) & 0x1FFFFF) - B; }

    static int kz(long k) { return (int) (k & 0x1FFFFF) - B; }
}

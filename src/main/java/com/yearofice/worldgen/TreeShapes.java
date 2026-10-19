package com.yearofice.worldgen;

/**
 * Tree builders as block offsets (Registry table: plants, trees and fungi; builders {@code WhiteSpruce},
 * {@code BlackSpruce}, {@code Birch}, {@code Deadwood}, and the willow stool). Offsets are from the base: (0, 0, 0)
 * is the first block above the ground. Pure, so the shapes can be tested and drawn on the debug map; the
 * feature turns parts into blocks and writes only into air (logs may also replace leaves), which makes
 * overlapping trees from neighbouring chunks come out the same in any order.
 *
 * <ul>
 *   <li>White spruce: a straight trunk, a bare bole, and a narrow cone of whorls (radius at most
 *       {@value #WHITE_SPRUCE_MAX_R}, tapering to 1) to a single-leader spire.</li>
 *   <li>Black spruce: a thin pole with one-block tufts and the dense "club" at the top (radius at most
 *       {@value #BLACK_SPRUCE_MAX_R}); on permafrost it leans (a drunken tree), one block over every few blocks of
 *       height.</li>
 *   <li>Paper birch: a trunk with a rounded crown over its upper half.</li>
 *   <li>Willow: a stool of 2–4 stems and a blob of brush around them.</li>
 *   <li>Deadwood: a stump, and for a fallen tree a log lying from the stump along the ground.</li>
 * </ul>
 */
public final class TreeShapes {
    private TreeShapes() {}

    public enum Part { LOG, LEAVES, STEM, BRUSH, STUMP, FALLEN_X, FALLEN_Z }

    public interface Sink {
        void put(int dx, int dy, int dz, Part part);
    }

    public static void build(TreePlan.Tree t, Sink out) {
        switch (t.kind()) {
            case STUMP -> out.put(0, 0, 0, Part.STUMP);
            case FALLEN -> fallen(t, out);
            case LIVE -> {
                switch (t.species()) {
                    case Cover.WHITE_SPRUCE -> whiteSpruce(t, out);
                    case Cover.BLACK_SPRUCE -> blackSpruce(t, out);
                    case Cover.BIRCH -> birch(t, out);
                    default -> willow(t, out);
                }
            }
        }
    }

    private static int bit(int seed, int k) { return (Noise.hash(seed, k, 0x7F4A7C15) >>> 7) & 1; }

    private static double unit(int seed, int a, int b) { return (Noise.hash(seed, a, b) >>> 8) / 16777216.0; }

    /**
     * Crown radius limits in blocks, measured from the trunk (Euclidean, so a crown never reaches a corner past
     * them). The docs give no width ratios, so the Director's numbers hold: interior spruce are narrow cones, white
     * spruce at most 3 at full height and black spruce at most 2, both tapering to 1 at the top.
     */
    public static final int WHITE_SPRUCE_MAX_R = 3, BLACK_SPRUCE_MAX_R = 2;

    /** Bare bole under a white spruce crown: the first whorl is at least this high, so a player walks under it. */
    static final int WHITE_SPRUCE_BOLE = 2;

    static void whiteSpruce(TreePlan.Tree t, Sink out) {
        int h = t.height();
        for (int y = 0; y < h; y++) out.put(0, y, 0, Part.LOG);
        int y0 = Math.max(WHITE_SPRUCE_BOLE, h / 4);
        // Full radius only on a full-grown tree: 11 blocks reach 2.4, 16 and over reach 3.
        double maxR = Math.min(WHITE_SPRUCE_MAX_R, 1.0 + h / 8.0);
        for (int y = y0; y < h; y++) {
            double f = h - 1 - y0 <= 0 ? 1.0 : (double) (y - y0) / (h - 1 - y0); // 1 at the top log layer
            double r = 1.0 + (maxR - 1.0) * (1.0 - f);
            // Whorls: every other layer is pulled in a block, so the crown reads as tiers, not one mass.
            if (((h - 1 - y) & 1) == 1) r = Math.max(1.0, r - 1.0);
            int ri = (int) Math.floor(r);
            for (int dz = -ri; dz <= ri; dz++) for (int dx = -ri; dx <= ri; dx++) {
                if (dx == 0 && dz == 0) continue;
                int d2 = dx * dx + dz * dz;
                if (d2 <= 1 || d2 <= r * r + 0.5 - 0.8 * unit(t.seed(), dx * 31 + y, dz)) out.put(dx, y, dz, Part.LEAVES);
            }
        }
        // The single leader.
        out.put(0, h, 0, Part.LEAVES);
        out.put(0, h + 1, 0, Part.LEAVES);
    }

    static void blackSpruce(TreePlan.Tree t, Sink out) {
        int h = t.height();
        int ox = 0, oz = 0;
        int every = 3 + bit(t.seed(), 1) + bit(t.seed(), 2);
        int lx = t.lean() < 0 ? 0 : ViewDirs.DX[t.lean()], lz = t.lean() < 0 ? 0 : ViewDirs.DZ[t.lean()];
        for (int y = 0; y < h; y++) {
            if (t.lean() >= 0 && y > 0 && y % every == 0) { ox += lx; oz += lz; }
            out.put(ox, y, oz, Part.LOG);
            boolean club = y >= h - 3;
            if (y >= 1 && (club || ((y + bit(t.seed(), y)) & 1) == 0)) {
                for (int k = 0; k < 4; k++) {
                    if (!club && bit(t.seed(), y * 4 + k) == 0) continue;
                    out.put(ox + ViewDirs.DX[k], y, oz + ViewDirs.DZ[k], Part.LEAVES);
                }
                if (club && y == h - 2) {
                    for (int dz = -1; dz <= 1; dz += 2) for (int dx = -1; dx <= 1; dx += 2) out.put(ox + dx, y, oz + dz, Part.LEAVES);
                }
            }
        }
        // A spire over the club: one leader block, two on a tree of 7 or more.
        out.put(ox, h, oz, Part.LEAVES);
        if (h >= 7) out.put(ox, h + 1, oz, Part.LEAVES);
    }

    static void birch(TreePlan.Tree t, Sink out) {
        int h = t.height();
        for (int y = 0; y < h; y++) out.put(0, y, 0, Part.LOG);
        int c0 = Math.max(2, (int) (h * 0.45));
        double cy = (c0 + h) / 2.0, ry = (h + 1 - c0) / 2.0 + 0.5;
        double rx = 2.0 + 0.8 * unit(t.seed(), 1, 2);
        int ri = (int) Math.ceil(rx);
        for (int y = c0; y <= h + 1; y++) for (int dz = -ri; dz <= ri; dz++) for (int dx = -ri; dx <= ri; dx++) {
            if (dx == 0 && dz == 0 && y < h) continue;
            double e = (dx * dx + dz * dz) / (rx * rx) + (y - cy) * (y - cy) / (ry * ry);
            if (e <= 1 - 0.18 * unit(t.seed(), dx * 17 + y, dz)) out.put(dx, y, dz, Part.LEAVES);
        }
    }

    static void willow(TreePlan.Tree t, Sink out) {
        int h = t.height();
        int stems = 2 + bit(t.seed(), 3) + bit(t.seed(), 4);
        int[][] at = {{0, 0}, {1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        for (int s = 0; s < stems; s++) {
            int sh = Math.max(1, h - bit(t.seed(), 10 + s));
            for (int y = 0; y < sh; y++) out.put(at[s][0], y, at[s][1], Part.STEM);
        }
        double r = 1.4 + 0.8 * unit(t.seed(), 5, 6);
        int ri = (int) Math.ceil(r);
        for (int y = 1; y <= h; y++) for (int dz = -ri; dz <= ri; dz++) for (int dx = -ri; dx <= ri; dx++) {
            double rr = r * (y == h ? 0.7 : 1.0);
            if (dx * dx + dz * dz + 0.4 * unit(t.seed(), dx + 7 * y, dz) <= rr * rr) out.put(dx, y, dz, Part.BRUSH);
        }
    }

    static void fallen(TreePlan.Tree t, Sink out) {
        out.put(0, 0, 0, Part.STUMP);
        int dir = t.seed() & 3;
        int len = Math.max(3, Math.min(9, t.height() / 2 + 1));
        int dx = ViewDirs.DX[dir], dz = ViewDirs.DZ[dir];
        Part p = dx != 0 ? Part.FALLEN_X : Part.FALLEN_Z;
        for (int k = 2; k < 2 + len; k++) out.put(dx * k, 0, dz * k, p);
    }

    /** The four horizontal directions: east, south, west, north. */
    static final class ViewDirs {
        static final int[] DX = {1, 0, -1, 0};
        static final int[] DZ = {0, 1, 0, -1};

        private ViewDirs() {}
    }
}

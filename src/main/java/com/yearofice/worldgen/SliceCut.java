package com.yearofice.worldgen;

/**
 * The slice valley (M2.5, preset {@code yearofice:slice_valley}): a square cut of the core, {@code size_m} on a
 * side, holding the outpost at least {@code edge_margin_m} from its edge, a creek, a fishing spot, spruce and
 * birch, a willow flat and a gravel bar. Every window on a {@code step_m} grid that holds the outpost is scored
 * by summed-area tables over the 2 m layers; one that meets every minimum wins over one that does not, then the
 * one with the most of the scarcest feature, then the one most centred on the outpost.
 *
 * <p>The cut changes nothing in the layers: inside it the slice's blocks are the home valley's blocks. Only the
 * world border (the cut) and the spawn (the outpost) differ.
 */
public final class SliceCut {
    private SliceCut() {}

    /** A window in world blocks, the counts that chose it, and whether it met every minimum. */
    public record Window(int minX, int minZ, int size, int creekCells, int barCells, int riparianCells, int spruceCells,
                         int birchCells, int spots, boolean complete) {
        public int centreX() { return minX + size / 2; }

        public int centreZ() { return minZ + size / 2; }

        public boolean contains(double x, double z) { return x >= minX && z >= minZ && x < minX + size && z < minZ + size; }
    }

    static Window choose(ValleyWorld vw) {
        TerrainParams.Slice sp = vw.p.terrain().slice();
        CoreGrid g = vw.g;
        int n = g.n;
        int size = sp.sizeM();
        int win = (int) (size / g.cell);
        if (win > n) win = n;
        SetPieces.Placement start = vw.outpost();
        double sx = start == null ? g.originX + g.size / 2 : start.x(), sz = start == null ? g.originZ + g.size / 2 : start.z();

        Cover.Params cp = vw.p.cover();
        int[] creek = new int[n * n], bar = new int[n * n], rip = new int[n * n], spruce = new int[n * n], birch = new int[n * n];
        int willowFlat = cp.index("willow_flat"), gravelBar = cp.index("gravel_bar");
        for (int i = 0; i < n * n; i++) {
            if (vw.wType[i] == WaterStage.CREEK) creek[i] = 1;
            int c = vw.cover.classAt(i);
            if (vw.bank[i] == WaterStage.BAR || c == gravelBar) bar[i] = 1;
            if (c == willowFlat) rip[i] = 1;
            Cover.ClassDef d = cp.classes().get(c);
            if (d.treeWeight(Cover.WHITE_SPRUCE) + d.treeWeight(Cover.BLACK_SPRUCE) >= 0.5) spruce[i] = 1;
            if (d.treeWeight(Cover.BIRCH) >= 0.2) birch[i] = 1;
        }
        long[] sCreek = sat(creek, n), sBar = sat(bar, n), sRip = sat(rip, n), sSpruce = sat(spruce, n), sBirch = sat(birch, n);

        int step = Math.max(1, (int) (sp.stepM() / g.cell));
        int margin = (int) (sp.edgeMarginM() / g.cell);
        Window best = null;
        double bestKey = -Double.MAX_VALUE;
        for (int k0 = 0; k0 + win <= n; k0 += step) {
            for (int i0 = 0; i0 + win <= n; i0 += step) {
                double wx0 = g.originX + i0 * g.cell, wz0 = g.originZ + k0 * g.cell;
                double ox = sx - wx0, oz = sz - wz0;
                // The outpost inside the cut, ideally edge_margin_m from its edge.
                if (ox < 0 || oz < 0 || ox >= size || oz >= size) continue;
                double edge = Math.min(Math.min(ox, oz), Math.min(size - ox, size - oz));
                boolean margined = edge >= sp.edgeMarginM();
                int cr = (int) box(sCreek, n, i0, k0, win), ba = (int) box(sBar, n, i0, k0, win), ri = (int) box(sRip, n, i0, k0, win);
                int sr = (int) box(sSpruce, n, i0, k0, win), bi = (int) box(sBirch, n, i0, k0, win);
                int spots = 0;
                for (WaterStage.Spot s : vw.spots) if (s.x() >= wx0 && s.z() >= wz0 && s.x() < wx0 + size && s.z() < wz0 + size) spots++;
                boolean complete = margined && cr >= sp.minCreekCells() && ba >= sp.minBarCells() && ri >= sp.minRiparianCells()
                        && sr >= sp.minSpruceCells() && bi >= sp.minBirchCells() && spots >= 1;
                double scarce = Math.min(Math.min(cr / (double) sp.minCreekCells(), ba / (double) sp.minBarCells()),
                        Math.min(ri / (double) sp.minRiparianCells(), Math.min(sr / (double) sp.minSpruceCells(),
                                Math.min(bi / (double) sp.minBirchCells(), spots))));
                double off = Math.abs(ox - size / 2.0) + Math.abs(oz - size / 2.0);
                double key = (complete ? 1e9 : 0) + (margined ? 1e8 : edge * 1e5) + Math.min(scarce, 4) * 1e5 - off;
                if (key > bestKey) {
                    bestKey = key;
                    best = new Window((int) Math.floor(wx0), (int) Math.floor(wz0), size, cr, ba, ri, sr, bi, spots, complete);
                }
            }
        }
        if (best == null) {
            int x0 = (int) Math.floor(Math.max(g.originX, Math.min(g.originX + g.size - size, sx - size / 2.0)));
            int z0 = (int) Math.floor(Math.max(g.originZ, Math.min(g.originZ + g.size - size, sz - size / 2.0)));
            best = new Window(x0, z0, size, 0, 0, 0, 0, 0, 0, false);
        }
        return best;
    }

    /** Summed-area table with a zero row and column. */
    static long[] sat(int[] a, int n) {
        long[] s = new long[(n + 1) * (n + 1)];
        for (int k = 0; k < n; k++) {
            long row = 0;
            for (int i = 0; i < n; i++) {
                row += a[k * n + i];
                s[(k + 1) * (n + 1) + i + 1] = s[k * (n + 1) + i + 1] + row;
            }
        }
        return s;
    }

    static long box(long[] s, int n, int i0, int k0, int w) {
        int m = n + 1;
        return s[(k0 + w) * m + i0 + w] - s[k0 * m + i0 + w] - s[(k0 + w) * m + i0] + s[k0 * m + i0];
    }
}

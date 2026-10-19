package com.yearofice.worldgen;

/**
 * Geometry of the core's 2 m layer grid, shared by stages 4 and 5. Cells are row-major, z then x, north row
 * first, in real (mirrored) orientation; "local" coordinates are metres from the core's north-west corner.
 * The canonical frame (river flowing west to east) differs only by {@code x -> L - x} when mirrored.
 *
 * <p>Only exactly rounded operations are used ({@code + − × ÷ sqrt}, comparisons); the few angles go through
 * {@link StrictMath}, which is bit-identical on every JVM.
 */
public final class CoreGrid {
    public final int n;
    public final double cell, size, originX, originZ;
    public final boolean mirror;

    /** The 8 neighbours: dx, dz and step length in cells. */
    public static final int[] DX = {1, 1, 0, -1, -1, -1, 0, 1};
    public static final int[] DZ = {0, 1, 1, 1, 0, -1, -1, -1};
    public static final double[] DL = {1, 1.4142135623730951, 1, 1.4142135623730951, 1, 1.4142135623730951, 1,
            1.4142135623730951};

    public CoreGrid(WorldgenParams p, boolean mirror) {
        WorldgenParams.Grid g = p.grid();
        this.n = g.coreCells();
        this.cell = g.cellM();
        this.size = g.coreSizeM();
        this.originX = g.coreOriginXM();
        this.originZ = g.coreOriginZM();
        this.mirror = mirror;
    }

    public int idx(int i, int k) { return k * n + i; }

    public int col(int idx) { return idx % n; }

    public int row(int idx) { return idx / n; }

    public boolean in(int i, int k) { return i >= 0 && k >= 0 && i < n && k < n; }

    public boolean edge(int idx) {
        int i = idx % n, k = idx / n;
        return i == 0 || k == 0 || i == n - 1 || k == n - 1;
    }

    /** Local metres of a cell centre. */
    public double lx(int idx) { return (idx % n + 0.5) * cell; }

    public double lz(int idx) { return (idx / n + 0.5) * cell; }

    public double worldX(int idx) { return originX + lx(idx); }

    public double worldZ(int idx) { return originZ + lz(idx); }

    /** Canonical x (metres along the valley, entry at 0) to local x, and back. */
    public double canonToLocalX(double cx) { return mirror ? size - cx : cx; }

    /** Cell containing local metres, or −1 outside the core. */
    public int cellAt(double lx, double lz) {
        int i = (int) Math.floor(lx / cell), k = (int) Math.floor(lz / cell);
        return in(i, k) ? idx(i, k) : -1;
    }

    /** Cell nearest to local metres, clamped into the core. */
    public int cellClamped(double lx, double lz) {
        int i = (int) Math.floor(lx / cell), k = (int) Math.floor(lz / cell);
        i = Math.max(0, Math.min(n - 1, i));
        k = Math.max(0, Math.min(n - 1, k));
        return idx(i, k);
    }

    /** Bilinear sample of a cell-centred float layer at local metres (clamped at the borders). */
    public double sample(float[] a, double lx, double lz) {
        double fx = lx / cell - 0.5, fz = lz / cell - 0.5;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        if (x0 < 0) { x0 = 0; tx = 0; }
        if (z0 < 0) { z0 = 0; tz = 0; }
        if (x0 > n - 2) { x0 = n - 2; tx = 1; }
        if (z0 > n - 2) { z0 = n - 2; tz = 1; }
        double a00 = a[z0 * n + x0], a10 = a[z0 * n + x0 + 1], a01 = a[(z0 + 1) * n + x0], a11 = a[(z0 + 1) * n + x0 + 1];
        double top = a00 + (a10 - a00) * tx, bot = a01 + (a11 - a01) * tx;
        return top + (bot - top) * tz;
    }

    /** Gradient (dh/dx, dh/dz) of a layer at a cell by central differences, in m per m. */
    public double gradX(float[] h, int idx) {
        int i = idx % n, k = idx / n;
        int l = i > 0 ? i - 1 : i, r = i < n - 1 ? i + 1 : i;
        return (h[k * n + r] - h[k * n + l]) / ((r - l) * cell);
    }

    public double gradZ(float[] h, int idx) {
        int i = idx % n, k = idx / n;
        int u = k > 0 ? k - 1 : k, d = k < n - 1 ? k + 1 : k;
        return (h[d * n + i] - h[u * n + i]) / ((d - u) * cell);
    }

    /** Slope as rise over run (tangent) at a cell. */
    public double slope(float[] h, int idx) {
        double gx = gradX(h, idx), gz = gradZ(h, idx);
        return Math.sqrt(gx * gx + gz * gz);
    }

    /** Slope layer as tangents, for the whole grid. */
    public float[] slopes(float[] h) {
        float[] s = new float[n * n];
        for (int i = 0; i < s.length; i++) s[i] = (float) slope(h, i);
        return s;
    }

    /** tan of an angle in degrees (StrictMath: identical on every JVM). */
    public static double tanDeg(double deg) { return StrictMath.tan(deg * (StrictMath.PI / 180.0)); }

    /** Degrees of a slope tangent. */
    public static double deg(double tangent) { return StrictMath.atan(tangent) * (180.0 / StrictMath.PI); }

    /** Compass bearing in degrees (0 north, 90 east) of a direction given in local x (east) and z (south). */
    public static double bearing(double dx, double dz) {
        double b = StrictMath.atan2(dx, -dz) * (180.0 / StrictMath.PI);
        return b < 0 ? b + 360 : b;
    }

    /**
     * Distance in metres from every cell to the nearest source cell: a two-pass chamfer transform with the
     * 5-7-11 mask (error under 2%), integer arithmetic so it is exact everywhere. Cells with no source get
     * {@code Float.MAX_VALUE}.
     */
    public float[] distance(boolean[] source) {
        final int big = Integer.MAX_VALUE / 4;
        int w = n + 4; // two cells of padding on every side: no bounds checks in the passes
        int[] d = new int[w * w];
        java.util.Arrays.fill(d, big);
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) d[(k + 2) * w + i + 2] = source[k * n + i] ? 0 : big;
        int[] off = {-w - 1, -w, -w + 1, -1, -w - 2, -2 * w - 1, -2 * w + 1, -w + 2};
        int[] ow = {7, 5, 7, 5, 11, 11, 11, 11};
        for (int k = 2; k < n + 2; k++) {
            for (int i = 2, idx = k * w + 2; i < n + 2; i++, idx++) {
                int v = d[idx];
                for (int m = 0; m < 8; m++) {
                    int c = d[idx + off[m]] + ow[m];
                    if (c < v) v = c;
                }
                d[idx] = v;
            }
        }
        for (int k = n + 1; k >= 2; k--) {
            for (int i = n + 1, idx = k * w + n + 1; i >= 2; i--, idx--) {
                int v = d[idx];
                for (int m = 0; m < 8; m++) {
                    int c = d[idx - off[m]] + ow[m];
                    if (c < v) v = c;
                }
                d[idx] = v;
            }
        }
        float[] out = new float[n * n];
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) {
            int v = d[(k + 2) * w + i + 2];
            out[k * n + i] = v >= big ? Float.MAX_VALUE : (float) (v * cell / 5.0);
        }
        return out;
    }
}

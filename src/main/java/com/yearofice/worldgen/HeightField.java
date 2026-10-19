package com.yearofice.worldgen;

/**
 * Ground height anywhere in the 8 km home valley, in world metres: the core's carved 2 m ground inside the
 * core, the 32 m coarse field elsewhere. Stage 5's line-of-sight tests read it.
 */
public final class HeightField {
    private final CoreGrid grid;
    private final float[] core;
    private final float[] coarse;
    private final int cn;
    private final WorldgenParams.Grid g;

    public HeightField(WorldgenParams p, CoreGrid grid, float[] coreGround, Landform.Layers land) {
        this.grid = grid;
        this.core = coreGround;
        this.coarse = land.coarse();
        this.cn = land.coarseCells();
        this.g = p.grid();
    }

    public double half() { return g.worldSizeM() / 2.0; }

    public boolean inWorld(double wx, double wz) {
        double h = half();
        return wx >= -h && wx <= h && wz >= -h && wz <= h;
    }

    public boolean inCore(double wx, double wz) {
        double lx = wx - grid.originX, lz = wz - grid.originZ;
        return lx >= 0 && lz >= 0 && lx < grid.size && lz < grid.size;
    }

    /**
     * Ground at a world position. Beyond the home valley (where the Interior plan lands at M10) the border's height
     * is carried straight out, so sight lines from the Interior still have ground under them.
     */
    public double at(double wx, double wz) {
        if (inCore(wx, wz)) return grid.sample(core, wx - grid.originX, wz - grid.originZ);
        double h = half() - 1;
        return Landform.coarseAt(coarse, cn, g, Math.max(-h, Math.min(h, wx)), Math.max(-h, Math.min(h, wz)));
    }

    /** Region index 0–15 (row × 4 + column, A1 = 0) of a world position. */
    public int region(double wx, double wz) {
        double h = half(), rs = g.worldSizeM() / 4.0;
        int col = (int) Math.floor((wx + h) / rs), row = (int) Math.floor((wz + h) / rs);
        col = Math.max(0, Math.min(3, col));
        row = Math.max(0, Math.min(3, row));
        return row * 4 + col;
    }

    /**
     * True when the sight line from an eye {@code eyeM} above (ax, az) to the point {@code targetY} at (bx, bz)
     * clears the ground plus {@code canopyM}, ignoring the first and last {@code skipM} metres.
     */
    public boolean clear(double ax, double az, double eyeM, double bx, double bz, double targetY, double stepM,
                         double skipM, double canopyM) {
        double ay = at(ax, az) + eyeM;
        double dx = bx - ax, dz = bz - az;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len <= 2 * skipM) return true;
        for (double s = skipM; s < len - skipM; s += stepM) {
            double t = s / len;
            double x = ax + dx * t, z = az + dz * t;
            double line = ay + (targetY - ay) * t;
            if (at(x, z) + canopyM > line) return false;
        }
        return true;
    }
}

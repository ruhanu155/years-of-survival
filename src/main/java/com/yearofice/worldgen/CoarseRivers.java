package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4, step 1: the 8 km home valley's river network on the 32 m coarse height (Pipeline › stage 4: "coarse
 * world river network first (D8 at 32 m), fixing border crossings and inflow"). Built at M1 so the regions that
 * stream in at M9 read the same crossings and need no migration.
 *
 * <p>An ε priority flood from the world border fills every pit, D8 receivers follow the steepest descent on the
 * filled surface, and accumulation runs in reverse flood order. The main river's virtual upstream catchment
 * (stage 1, 64–200 km²) enters one cell inside the world border where the valley trough meets it. The trace
 * of the main river gives the core's entry and exit points; every other coarse river that flows into the core
 * gives a crossing whose catchment the core injects at its edge.
 */
public final class CoarseRivers {
    private CoarseRivers() {}

    /** A river crossing the core's boundary: local core metres and the catchment carried across. */
    public record Crossing(double lx, double lz, double catchmentM2) {}

    /**
     * @param receiver  coarse cell each cell drains to, −1 at the world border
     * @param catchment accumulated catchment in m², virtual inflow included
     * @param mainTrace coarse cells of the main river from its injection cell to the world border
     */
    public record Net(int cells, double cellM, double half, int[] receiver, float[] catchment, int[] mainTrace,
                      Crossing entry, Crossing exit, List<Crossing> inflows, double virtualM2) {
        public double worldX(int c) { return -half + (c % cells + 0.5) * cellM; }

        public double worldZ(int c) { return -half + (c / cells + 0.5) * cellM; }

        /** The largest catchment within one coarse cell of a world position: the river's, near a river. */
        public double catchmentNear(double wx, double wz) {
            int ci = (int) Math.floor((wx + half) / cellM), ck = (int) Math.floor((wz + half) / cellM);
            double best = 0;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                int x = ci + dx, z = ck + dz;
                if (x < 0 || z < 0 || x >= cells || z >= cells) continue;
                best = Math.max(best, catchment[z * cells + x]);
            }
            return best;
        }

        public int hash() {
            StringBuilder b = new StringBuilder();
            b.append(com.yearofice.core.hash.StageHash.of(catchment)).append(':');
            for (int c : mainTrace) b.append(c).append(',');
            b.append(String.format(java.util.Locale.ROOT, "%.3f,%.3f,%.3f,%.3f", entry.lx(), entry.lz(), exit.lx(), exit.lz()));
            for (Crossing c : inflows) b.append(String.format(java.util.Locale.ROOT, ";%.2f,%.2f,%.0f", c.lx(), c.lz(), c.catchmentM2()));
            return com.yearofice.core.hash.StageHash.of(b.toString());
        }
    }

    public static Net run(SeedStage.SeedParams seed, Landform.Layers land, WorldgenParams p) {
        WorldgenParams.Grid g = p.grid();
        int cn = land.coarseCells();
        float[] h = land.coarse();
        double cm = g.coarseCellM(), half = g.worldSizeM() / 2.0;
        int total = cn * cn;

        // ε priority flood from the world border.
        double eps = 1e-4;
        double[] e = new double[total];
        boolean[] seen = new boolean[total];
        int[] order = new int[total];
        int popped = 0;
        CellHeap heap = new CellHeap(cn * 4);
        for (int c = 0; c < total; c++) {
            int i = c % cn, k = c / cn;
            if (i == 0 || k == 0 || i == cn - 1 || k == cn - 1) {
                e[c] = h[c];
                seen[c] = true;
                heap.push(e[c], c);
            }
        }
        while (!heap.isEmpty()) {
            int c = heap.pop();
            order[popped++] = c;
            int i = c % cn, k = c / cn;
            for (int m = 0; m < 8; m++) {
                int x = i + CoreGrid.DX[m], z = k + CoreGrid.DZ[m];
                if (x < 0 || z < 0 || x >= cn || z >= cn) continue;
                int nb = z * cn + x;
                if (seen[nb]) continue;
                seen[nb] = true;
                e[nb] = Math.max(h[nb], e[c] + eps);
                heap.push(e[nb], nb);
            }
        }
        int[] rcv = new int[total];
        for (int c = 0; c < total; c++) {
            int i = c % cn, k = c / cn;
            rcv[c] = -1;
            if (i == 0 || k == 0 || i == cn - 1 || k == cn - 1) continue;
            double best = 0;
            for (int m = 0; m < 8; m++) {
                int nb = (k + CoreGrid.DZ[m]) * cn + i + CoreGrid.DX[m];
                double drop = (e[c] - e[nb]) / CoreGrid.DL[m];
                if (drop > best) { best = drop; rcv[c] = nb; }
            }
        }

        // Inject the virtual catchment where the trough meets the upstream world border.
        double virtual = seed.virtualCatchmentKm2() * 1e6;
        boolean westIn = !seed.mirror(); // canonical entry is the west edge unless mirrored
        int col = westIn ? 1 : cn - 2;
        double entryWz = g.coreOriginZM() + seed.entryOffsetM();
        int k0 = (int) Math.floor((entryWz + half) / cm);
        int inject = -1;
        for (int dz = -4; dz <= 4; dz++) {
            int k = k0 + dz;
            if (k < 1 || k > cn - 2) continue;
            int c = k * cn + col;
            if (inject < 0 || h[c] < h[inject]) inject = c;
        }

        float[] acc = new float[total];
        double[] a = new double[total];
        double cellArea = cm * cm;
        for (int c = 0; c < total; c++) a[c] = cellArea;
        a[inject] += virtual;
        for (int t = total - 1; t >= 0; t--) {
            int c = order[t];
            if (rcv[c] >= 0) a[rcv[c]] += a[c];
        }
        for (int c = 0; c < total; c++) acc[c] = (float) a[c];

        // Trace the main river.
        List<Integer> trace = new ArrayList<>();
        for (int c = inject; c >= 0 && trace.size() < total; c = rcv[c]) trace.add(c);
        int[] mainTrace = new int[trace.size()];
        for (int t = 0; t < mainTrace.length; t++) mainTrace[t] = trace.get(t);

        double x0 = g.coreOriginXM(), z0 = g.coreOriginZM(), L = g.coreSizeM();
        Crossing entry = null, exit = null;
        for (int t = 1; t < mainTrace.length; t++) {
            int pc = mainTrace[t - 1], cc = mainTrace[t];
            double ax = -half + (pc % cn + 0.5) * cm, az = -half + (pc / cn + 0.5) * cm;
            double bx = -half + (cc % cn + 0.5) * cm, bz = -half + (cc / cn + 0.5) * cm;
            boolean ain = inside(ax, az, x0, z0, L), bin = inside(bx, bz, x0, z0, L);
            if (!ain && bin && entry == null) {
                double[] q = clipToBoundary(bx, bz, ax, az, x0, z0, L);
                entry = new Crossing(q[0] - x0, q[1] - z0, a[pc]);
            }
            if (ain && !bin) {
                double[] q = clipToBoundary(ax, az, bx, bz, x0, z0, L);
                exit = new Crossing(q[0] - x0, q[1] - z0, a[pc]);
            }
        }
        if (entry == null || exit == null) {
            // The trough always crosses the core; this only guards a malformed field.
            double ex = westIn ? 0 : L, xx = westIn ? L : 0;
            entry = new Crossing(ex, seed.entryOffsetM(), virtual);
            exit = new Crossing(xx, seed.exitOffsetM(), virtual);
        }

        // Other rivers flowing into the core.
        double minCross = p.hydro().coarse().crossingMinM2();
        boolean[] onMain = new boolean[total];
        for (int c : mainTrace) onMain[c] = true;
        List<Crossing> inflows = new ArrayList<>();
        for (int c = 0; c < total; c++) {
            int r = rcv[c];
            if (r < 0 || onMain[c] || a[c] < minCross) continue;
            double ax = -half + (c % cn + 0.5) * cm, az = -half + (c / cn + 0.5) * cm;
            double bx = -half + (r % cn + 0.5) * cm, bz = -half + (r / cn + 0.5) * cm;
            if (!inside(ax, az, x0, z0, L) && inside(bx, bz, x0, z0, L)) {
                double[] q = clipToBoundary(bx, bz, ax, az, x0, z0, L);
                inflows.add(new Crossing(q[0] - x0, q[1] - z0, a[c]));
            }
        }
        return new Net(cn, cm, half, rcv, acc, mainTrace, entry, exit, List.copyOf(inflows), virtual);
    }

    private static boolean inside(double x, double z, double x0, double z0, double L) {
        return x >= x0 && x < x0 + L && z >= z0 && z < z0 + L;
    }

    /** The point where the segment from an inside point to an outside point leaves the core square. */
    private static double[] clipToBoundary(double ix, double iz, double ox, double oz, double x0, double z0, double L) {
        double t = 1.0;
        double dx = ox - ix, dz = oz - iz;
        if (dx < 0) t = Math.min(t, (x0 - ix) / dx);
        if (dx > 0) t = Math.min(t, (x0 + L - ix) / dx);
        if (dz < 0) t = Math.min(t, (z0 - iz) / dz);
        if (dz > 0) t = Math.min(t, (z0 + L - iz) / dz);
        double x = ix + dx * t, z = iz + dz * t;
        x = Math.max(x0, Math.min(x0 + L, x));
        z = Math.max(z0, Math.min(z0 + L, z));
        return new double[] {x, z};
    }
}

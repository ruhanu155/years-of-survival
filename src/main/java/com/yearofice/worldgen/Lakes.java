package com.yearofice.worldgen;

import java.util.Arrays;

/**
 * Stage 4, step 2: depressions to lakes (Terrain and hydrology step 3; Lakes and bogs).
 *
 * <p>An ε priority flood over the ground left by the main river's carve, draining to the core's edge and to the
 * main river's water, gives two surfaces: the true fill (each basin's water surface at its spill level) and an
 * ε fill whose steepest descent is a D8 receiver tree with no flats, popped in an order that is a topological
 * order of that tree. Basins at least 1.5 m deep and 2,000 m² become lakes, each draining by exactly one
 * outlet, the pour point the flood entered it by; smaller basins over 200 m² become bog; smaller pits only
 * route water. The landform's closed bowls in the hills hold deep tarns this way. Every lake gets its weed shelf (0–2 m deep, the first 20 m from shore) and its drop-offs.
 */
final class Lakes {
    private Lakes() {}

    /** The flood: pop order (ascending), D8 receivers (−1 at outlets) and the true fill. */
    record Flood(int[] order, int[] receiver, float[] fill) {}

    /** The raw flood: ε surface, true fill, pop order and position, and the cell that pushed each cell. */
    private record Raw(double[] e, float[] fill, int[] order, int[] popAt, int[] parent, float[] ground) {}

    private static Raw flood(Hydro h) {
        CoreGrid g = h.grid;
        int n = h.n, total = h.total;
        float[] ground = new float[total];
        for (int i = 0; i < total; i++) ground[i] = h.h0[i] - h.carveMain[i];
        double eps = 1e-5;
        double[] e = new double[total];
        float[] fill = new float[total];
        boolean[] seen = new boolean[total];
        int[] order = new int[total];
        int[] popAt = new int[total];
        int[] parent = new int[total];
        Arrays.fill(parent, -1);
        int popped = 0;
        CellHeap heap = new CellHeap(n * 8);
        for (int i = 0; i < total; i++) {
            boolean main = h.type[i] == Hydro.MAIN;
            if (main || g.edge(i)) {
                double level = main ? h.ws[i] : ground[i];
                e[i] = level;
                fill[i] = (float) level;
                seen[i] = true;
                heap.push(level, i);
            }
        }
        while (!heap.isEmpty()) {
            int c = heap.pop();
            popAt[c] = popped;
            order[popped++] = c;
            int ci = c % n, ck = c / n;
            for (int m = 0; m < 8; m++) {
                int x = ci + CoreGrid.DX[m], z = ck + CoreGrid.DZ[m];
                if (x < 0 || z < 0 || x >= n || z >= n) continue;
                int nb = z * n + x;
                if (seen[nb]) continue;
                seen[nb] = true;
                parent[nb] = c;
                e[nb] = Math.max(ground[nb], e[c] + eps);
                fill[nb] = Math.max(ground[nb], fill[c]);
                heap.push(e[nb], nb);
            }
        }
        return new Raw(e, fill, order, popAt, parent, ground);
    }

    /**
     * The lake valley's lake must reach {@code min_lake_area_m2} (the M1 signature, 0.2 km²), but stage 2's
     * smaller basins hold less water than that, and the floor around them drains to the river. Where it falls
     * short, stage 4 scours the basin wider: the basin's ellipse, grown until it is big enough, kept 25 m or more
     * from the river and never cut over 8 m into rising ground, is lowered to a bowl 1.2–3.2 m below the lowest
     * ground on its rim. The lake then fills it to that rim. Stage 2's height is untouched: the scour is
     * in the carve layer.
     */
    private static Raw shallows(Hydro h, Raw raw) {
        Landform.Shape.Basin b = h.land.shape().basin();
        Archetype.Params.Basin ab = h.arch.basin();
        if (b == null || ab == null || ab.minLakeAreaM2() <= 0) return raw;
        CoreGrid g = h.grid;
        int n = h.n, total = h.total;
        int centre = g.cellClamped(g.canonToLocalX(b.centerX()), b.centerZ());
        if (raw.fill()[centre] - raw.ground()[centre] <= 0.02f) return raw; // no water at the basin centre
        boolean[] lake = component(h, raw, centre);
        int lakeCells = 0;
        for (boolean v : lake) if (v) lakeCells++;
        double area = lakeCells * h.cellArea;
        if (area >= ab.minLakeAreaM2()) return raw;

        boolean[] main = new boolean[total];
        for (int i = 0; i < total; i++) main[i] = h.type[i] == Hydro.MAIN;
        float[] dMain = g.distance(main);
        double cx = g.canonToLocalX(b.centerX()), cz = b.centerZ();
        boolean[] in = new boolean[total];
        double bed = 0, ax = 1, az = 1;
        int count = 0;
        for (double k = 1.0; k <= 2.0; k += 0.1) {
            ax = b.lengthM() / 2 * k;
            az = b.widthM() / 2 * k;
            // The scour ellipse and the lake, then the lowest ground on their rim.
            for (int i = 0; i < total; i++) {
                double dx = (g.lx(i) - cx) / ax, dz = (g.lz(i) - cz) / az;
                in[i] = lake[i] || (dx * dx + dz * dz <= 1 && dMain[i] >= 25 && h.type[i] == Hydro.DRY && !g.edge(i));
            }
            double rim = Double.MAX_VALUE;
            for (int i = 0; i < total; i++) {
                if (in[i]) continue;
                int x = g.col(i), z = g.row(i);
                boolean edge = false;
                for (int m = 0; m < 8 && !edge; m++) {
                    int xx = x + CoreGrid.DX[m], zz = z + CoreGrid.DZ[m];
                    if (g.in(xx, zz) && in[g.idx(xx, zz)]) edge = true;
                }
                if (edge) rim = Math.min(rim, h.type[i] == Hydro.DRY ? h.h0[i] - h.carveMain[i] : h.ws[i]);
            }
            bed = rim - 1.2;
            for (int i = 0; i < total; i++) {
                if (in[i] && !lake[i] && h.h0[i] - h.carveMain[i] > rim + 8.0) in[i] = false;
            }
            // Only what joins the basin lake: a piece cut off by a knoll would be a separate pit.
            boolean[] joined = new boolean[total];
            int[] q = new int[total];
            int qh = 0, qt = 0;
            joined[centre] = true;
            q[qt++] = centre;
            while (qh < qt) {
                int c = q[qh++];
                for (int m = 0; m < 8; m += 2) {
                    int x = c % n + CoreGrid.DX[m], z = c / n + CoreGrid.DZ[m];
                    if (!g.in(x, z)) continue;
                    int nb = z * n + x;
                    if (!in[nb] || joined[nb]) continue;
                    joined[nb] = true;
                    q[qt++] = nb;
                }
            }
            in = joined;
            count = qt;
            if (count * h.cellArea >= ab.minLakeAreaM2() * 1.1) break;
        }
        int scoured = 0;
        for (int i = 0; i < total; i++) {
            if (!in[i]) continue;
            double dx = (g.lx(i) - cx) / ax, dz = (g.lz(i) - cz) / az;
            double r2 = Math.min(1, dx * dx + dz * dz);
            double floor = bed - 2.0 * (1 - r2); // a bowl, deepest at the middle
            double gr = h.h0[i] - h.carveMain[i];
            if (gr > floor) {
                h.carveMain[i] += (float) (gr - floor);
                scoured++;
            }
        }
        h.notes.add(String.format(java.util.Locale.ROOT, "lake valley: scoured %.0f m2 of shallows round a %.0f m2 basin lake",
                scoured * h.cellArea, area));
        return flood(h);
    }

    /** The flooded cells connected to {@code start}. */
    private static boolean[] component(Hydro h, Raw raw, int start) {
        CoreGrid g = h.grid;
        int n = h.n;
        boolean[] in = new boolean[h.total];
        int[] q = new int[h.total];
        int qh = 0, qt = 0;
        in[start] = true;
        q[qt++] = start;
        while (qh < qt) {
            int c = q[qh++];
            for (int m = 0; m < 8; m++) {
                int x = c % n + CoreGrid.DX[m], z = c / n + CoreGrid.DZ[m];
                if (!g.in(x, z)) continue;
                int nb = z * n + x;
                if (in[nb] || raw.fill()[nb] - raw.ground()[nb] <= 0.02f || h.type[nb] != Hydro.DRY) continue;
                in[nb] = true;
                q[qt++] = nb;
            }
        }
        return in;
    }

    static void run(Hydro h) {
        HydroParams.Water wp = h.hp.water();
        CoreGrid g = h.grid;
        int n = h.n, total = h.total;
        Raw raw = shallows(h, flood(h));
        double[] e = raw.e();
        float[] fill = raw.fill();
        int[] order = raw.order();
        int[] popAt = raw.popAt();
        int[] parent = raw.parent();
        float[] ground = raw.ground();
        int[] rcv = new int[total];
        for (int c = 0; c < total; c++) {
            rcv[c] = -1;
            if (h.type[c] == Hydro.MAIN || g.edge(c)) continue;
            int ci = c % n, ck = c / n;
            double best = 0;
            for (int m = 0; m < 8; m++) {
                int nb = (ck + CoreGrid.DZ[m]) * n + ci + CoreGrid.DX[m];
                double drop = (e[c] - e[nb]) / CoreGrid.DL[m];
                if (drop > best) { best = drop; rcv[c] = nb; }
            }
        }
        h.flood0 = new Flood(order, rcv, fill);

        // Flooded components.
        int[] comp = new int[total];
        Arrays.fill(comp, -1);
        int[] queue = new int[total];
        int comps = 0;
        int[] exitOf = new int[total];
        Arrays.fill(exitOf, -1);
        int[] compCells = new int[total];
        for (int start = 0; start < total; start++) {
            if (comp[start] >= 0 || fill[start] - ground[start] <= 0.02f || h.type[start] != Hydro.DRY) continue;
            int qh = 0, qt = 0;
            queue[qt++] = start;
            comp[start] = comps;
            while (qh < qt) {
                int c = queue[qh++];
                int ci = c % n, ck = c / n;
                for (int m = 0; m < 8; m++) {
                    int x = ci + CoreGrid.DX[m], z = ck + CoreGrid.DZ[m];
                    if (x < 0 || z < 0 || x >= n || z >= n) continue;
                    int nb = z * n + x;
                    if (comp[nb] >= 0 || fill[nb] - ground[nb] <= 0.02f || h.type[nb] != Hydro.DRY) continue;
                    comp[nb] = comps;
                    queue[qt++] = nb;
                }
            }
            double area = qt * h.cellArea, maxDepth = 0, sumDepth = 0, level = 0;
            int ox = 0;
            for (int t = 0; t < qt; t++) {
                int c = queue[t];
                double d = fill[c] - ground[c];
                sumDepth += d;
                if (d > maxDepth) maxDepth = d;
                level = Math.max(level, fill[c]);
                if (h.oxbowCarved[c]) ox++;
            }
            boolean lake = maxDepth >= wp.lakeMinDepthM() && area >= wp.lakeMinAreaM2();
            if (lake) {
                boolean oxbow = ox * 2 > qt;
                int bodyId = h.bodies.size() + 1;
                double cx = 0, cz = 0;
                for (int t = 0; t < qt; t++) {
                    int c = queue[t];
                    h.type[c] = oxbow ? Hydro.OXBOW : Hydro.LAKE;
                    h.ws[c] = fill[c];
                    h.body[c] = (short) bodyId;
                    cx += g.lx(c);
                    cz += g.lz(c);
                }
                // The outlet is the pour point the flood entered the basin by: the parent of its first popped
                // cell. Every lake cell drains straight to it, which keeps the pop order topological.
                int first = queue[0];
                for (int t = 1; t < qt; t++) if (popAt[queue[t]] < popAt[first]) first = queue[t];
                int k = parent[first];
                for (int t = 0; t < qt; t++) rcv[queue[t]] = k;
                h.lakes.add(new WaterStage.Lake(bodyId, oxbow ? "oxbow" : "lake", qt, area, level, maxDepth,
                        sumDepth / qt, k, cx / qt, cz / qt, 0));
                h.bodies.add(new WaterStage.Body(bodyId, oxbow ? "oxbow" : "lake", area, sumDepth / qt, k, 0));
            } else if (area >= wp.bogMinAreaM2()) {
                for (int t = 0; t < qt; t++) h.bank[queue[t]] = Hydro.BOG;
            }
            comps++;
        }

        // One outlet per lake: every lake cell's receiver chain must leave the basin at the same cell.
        int[] outletCount = new int[h.lakes.size()];
        int[] firstExit = new int[h.lakes.size()];
        Arrays.fill(firstExit, -2);
        for (int t = 0; t < total; t++) {
            int c = order[t];
            if (h.type[c] != Hydro.LAKE && h.type[c] != Hydro.OXBOW) continue;
            int r = rcv[c];
            exitOf[c] = r >= 0 && h.body[r] == h.body[c] && (h.type[r] == Hydro.LAKE || h.type[r] == Hydro.OXBOW) ? exitOf[r] : r;
            int li = lakeIndex(h, h.body[c]);
            if (firstExit[li] == -2) { firstExit[li] = exitOf[c]; outletCount[li] = 1; }
            else if (firstExit[li] != exitOf[c] && outletCount[li] == 1) outletCount[li] = 2;
        }
        for (int li = 0; li < h.lakes.size(); li++) {
            WaterStage.Lake l = h.lakes.get(li);
            h.lakes.set(li, new WaterStage.Lake(l.bodyId(), l.kind(), l.cells(), l.areaM2(), l.levelM(), l.maxDepthM(),
                    l.meanDepthM(), l.outlet(), l.cx(), l.cz(), outletCount[li]));
        }

        // Weed shelves and drop-offs.
        if (!h.lakes.isEmpty()) {
            boolean[] shore = new boolean[total];
            for (int i = 0; i < total; i++) shore[i] = h.type[i] != Hydro.LAKE && h.type[i] != Hydro.OXBOW;
            float[] fromShore = g.distance(shore);
            for (int i = 0; i < total; i++) {
                if (h.type[i] != Hydro.LAKE && h.type[i] != Hydro.OXBOW) continue;
                double d = fill[i] - ground[i];
                if (d <= wp.weedShelfMaxDepthM() && fromShore[i] <= wp.weedShelfWidthM()) h.bank[i] = Hydro.WEED_SHELF;
                double gx = g.gradX(ground, i), gz = g.gradZ(ground, i);
                if (Math.sqrt(gx * gx + gz * gz) > wp.dropOffGradient() && d > 2.0) h.bank[i] = Hydro.DROP_OFF;
            }
        }
    }

    static int lakeIndex(Hydro h, int bodyId) {
        for (int i = 0; i < h.lakes.size(); i++) if (h.lakes.get(i).bodyId() == bodyId) return i;
        return -1;
    }
}

package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4, step 4: falls and pools (Terrain and hydrology step 5; Falls guarantee; Springs and thin water).
 *
 * <p>Along every channel, a fall is a run of water surface dropping more than 3 m over 6 m, ending where the
 * channel eases again; contiguous steep cells are one fall, so a cascade down a rock face is one fall with
 * its full height. The canyon's rock steps on the main river become falls this way. A pool of radius 4–8 m is
 * scoured at each foot, and the 30 m below is flagged thin for the winter.
 *
 * <p>If the archetype's minimum is not met, the largest north creeks are deepened by 3–5 m where they leave
 * the ridged mountains, tapered over the 30 m below, in the guarantee's own carve; the creek surface is
 * re-run and the falls found again.
 */
final class Falls {
    private Falls() {}

    static void run(Hydro h) {
        HydroParams.FallsParams fp = h.hp.falls();
        List<WaterStage.Fall> found = detect(h, false);
        int need = h.arch.minimumFalls();
        List<Integer> deepened = new ArrayList<>();
        for (int round = 0; round < 3 && found.size() < need; round++) {
            int added = guarantee(h, found, need - found.size(), deepened);
            if (added == 0) break;
            Rivers.creekSurface(h);
            found = detect(h, false);
        }
        // Mark the guaranteed falls.
        List<WaterStage.Fall> out = new ArrayList<>();
        for (WaterStage.Fall f : found) {
            boolean g = false;
            for (int c : deepened) if (dist(h, c, f.lip()) < 20) g = true;
            out.add(g ? f.withGuaranteed() : f);
        }
        h.falls.addAll(out);
        if (found.size() < need) h.notes.add("falls: " + found.size() + " of " + need + " after the guarantee");

        // Pools and thin water.
        for (int k = 0; k < h.falls.size(); k++) {
            WaterStage.Fall f = h.falls.get(k);
            double r = fp.poolRadiusM().draw(h.rng);
            h.falls.set(k, f.withPool(r));
            pool(h, f.foot(), r);
            thin(h, f);
        }
    }

    private static double dist(Hydro h, int a, int b) {
        double dx = h.grid.lx(a) - h.grid.lx(b), dz = h.grid.lz(a) - h.grid.lz(b);
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Every fall on the main channels and the creeks, foot spacing at least 40 m. */
    static List<WaterStage.Fall> detect(Hydro h, boolean unused) {
        HydroParams.FallsParams fp = h.hp.falls();
        CoreGrid g = h.grid;
        List<WaterStage.Fall> out = new ArrayList<>();
        // Main channels: points every 2 m.
        int lineNo = 0;
        for (Rivers.Line ln : h.main.lines()) {
            int run = (int) Math.round(fp.runM() / Channels.DS);
            int ease = (int) Math.round(10 / Channels.DS);
            int i = 0;
            while (i + run < ln.size()) {
                if (ln.ws()[i] - ln.ws()[i + run] > fp.minDropM()) {
                    int lip = i, j = i;
                    while (j + run < ln.size() && ln.ws()[j] - ln.ws()[j + run] > fp.minDropM()) j++;
                    int foot = Math.min(ln.size() - 1, j - 1 + run);
                    double height = ln.ws()[lip] - ln.ws()[foot];
                    int lipCell = g.cellAt(ln.x()[lip], ln.z()[lip]), footCell = g.cellAt(ln.x()[foot], ln.z()[foot]);
                    if (lipCell >= 0 && footCell >= 0) {
                        add(h, out, new WaterStage.Fall(lipCell, footCell, height, (foot - lip) * Channels.DS, 1, true,
                                false, 0, faceHeight(h, footCell)), fp);
                    }
                    i = j + ease;
                } else {
                    i++;
                }
            }
            lineNo++;
        }
        // Creeks: ordered cells with their distances.
        for (WaterStage.Creek c : h.creeks) {
            int[] cs = c.cells();
            int m = cs.length;
            double[] s = new double[m];
            for (int k = 1; k < m; k++) s[k] = s[k - 1] + Rivers.stepLen(g, cs[k - 1], cs[k]);
            int k = 0;
            while (k < m) {
                int j = ahead(s, k, fp.runM());
                if (j < 0) break;
                if (h.ws[cs[k]] - h.ws[cs[j]] > fp.minDropM()) {
                    int lip = k, e = k;
                    int foot = j;
                    while (true) {
                        int jj = ahead(s, e, fp.runM());
                        if (jj < 0 || h.ws[cs[e]] - h.ws[cs[jj]] <= fp.minDropM()) break;
                        foot = jj;
                        e++;
                    }
                    // The channel must ease below the foot, or this is a steep run, not a fall yet.
                    int easeTo = ahead(s, foot, 10);
                    double after = easeTo < 0 ? 0 : (h.ws[cs[foot]] - h.ws[cs[easeTo]]) / (s[easeTo] - s[foot]);
                    if (easeTo < 0 || after < fp.footMaxSlope() || s[foot] - s[lip] > 60) {
                        double height = h.ws[cs[lip]] - h.ws[cs[foot]];
                        add(h, out, new WaterStage.Fall(cs[lip], cs[foot], height, s[foot] - s[lip], c.bodyId(), false,
                                false, 0, faceHeight(h, cs[foot])), fp);
                    }
                    k = Math.max(e + 1, foot);
                } else {
                    k++;
                }
            }
        }
        return out;
    }

    private static int ahead(double[] s, int k, double run) {
        for (int j = k + 1; j < s.length; j++) if (s[j] - s[k] >= run - 1e-9) return j;
        return -1;
    }

    private static void add(Hydro h, List<WaterStage.Fall> out, WaterStage.Fall f, HydroParams.FallsParams fp) {
        for (int i = 0; i < out.size(); i++) {
            WaterStage.Fall o = out.get(i);
            if (dist(h, o.foot(), f.foot()) < fp.minSpacingM()) {
                if (f.heightM() > o.heightM()) out.set(i, f);
                return;
            }
        }
        out.add(f);
    }

    /** Height of rock face beside a fall: the relief of rock cells within 30 m of the foot. */
    private static double faceHeight(Hydro h, int foot) {
        CoreGrid g = h.grid;
        double tan = CoreGrid.tanDeg(h.hp.rock().minSlopeDeg());
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        int ci = g.col(foot), ck = g.row(foot);
        for (int dz = -15; dz <= 15; dz += 1) for (int dx = -15; dx <= 15; dx += 1) {
            if (!g.in(ci + dx, ck + dz)) continue;
            int idx = g.idx(ci + dx, ck + dz);
            if (h.land.hardnessAt(idx) <= h.hp.rock().minHardness() || g.slope(h.h0, idx) <= tan) continue;
            lo = Math.min(lo, h.h0[idx]);
            hi = Math.max(hi, h.h0[idx]);
        }
        return hi > lo ? hi - lo : 0;
    }

    /** Deepens the largest north creeks where they leave the ridged mountains. Returns how many it deepened. */
    private static int guarantee(Hydro h, List<WaterStage.Fall> have, int want, List<Integer> deepened) {
        HydroParams.FallsParams fp = h.hp.falls();
        CoreGrid g = h.grid;
        double boundary = h.arch.mountainFraction() * g.size;
        List<int[]> cand = new ArrayList<>(); // {creek index, cell index in creek}
        for (int ci = 0; ci < h.creeks.size(); ci++) {
            int[] cs = h.creeks.get(ci).cells();
            int at = -1;
            for (int k = 1; k < cs.length; k++) {
                if (g.lz(cs[k - 1]) < boundary && g.lz(cs[k]) >= boundary) { at = k; break; }
            }
            if (at < 0) {
                // A creek that never crosses: take its steepest cell in the north half.
                double best = 0;
                for (int k = 1; k + 3 < cs.length; k++) {
                    if (g.lz(cs[k]) > g.size / 2) continue;
                    double d = h.ws[cs[k]] - h.ws[cs[k + 3]];
                    if (d > best) { best = d; at = k; }
                }
            }
            if (at < 0 || at + 4 >= cs.length) continue;
            boolean taken = false;
            for (WaterStage.Fall f : have) if (dist(h, f.lip(), cs[at]) < 150) taken = true;
            for (int d : deepened) if (dist(h, d, cs[at]) < 150) taken = true;
            if (!taken) cand.add(new int[] {ci, at});
        }
        cand.sort((x, y) -> {
            double ax = h.catchment[h.creeks.get(x[0]).cells()[x[1]]], ay = h.catchment[h.creeks.get(y[0]).cells()[y[1]]];
            int c = Double.compare(ay, ax);
            return c != 0 ? c : Integer.compare(x[0], y[0]);
        });
        int done = 0;
        for (int[] c : cand) {
            if (done >= want) break;
            int[] cs = h.creeks.get(c[0]).cells();
            double depth = fp.guaranteeDeepenM().draw(h.rng);
            double s = 0;
            for (int k = c[1] + 1; k < cs.length && s < fp.guaranteeRunM(); k++) {
                s += Rivers.stepLen(g, cs[k - 1], cs[k]);
                double e = depth * (1 - s / fp.guaranteeRunM());
                if (e > h.extra[cs[k]]) h.extra[cs[k]] = (float) e;
            }
            deepened.add(cs[c[1]]);
            done++;
        }
        return done;
    }

    /** Scours a plunge pool at a fall's foot. */
    private static void pool(Hydro h, int foot, double r) {
        CoreGrid g = h.grid;
        HydroParams.Water wp = h.hp.water();
        double level = h.ws[foot];
        double depth = (h.type[foot] == Hydro.MAIN ? wp.depth(h.width[foot]) : wp.depth(h.width[foot])) + h.hp.falls().poolExtraDepthM();
        int rc = (int) Math.ceil(r / g.cell);
        int ci = g.col(foot), ck = g.row(foot);
        for (int dz = -rc; dz <= rc; dz++) for (int dx = -rc; dx <= rc; dx++) {
            if (!g.in(ci + dx, ck + dz)) continue;
            int idx = g.idx(ci + dx, ck + dz);
            double d = Math.sqrt(dx * dx + dz * dz) * g.cell;
            if (d > r) continue;
            byte t = h.type[idx];
            if (t == Hydro.LAKE || t == Hydro.OXBOW) continue;
            double ground = h.h0[idx] - h.carveMain[idx] - h.carveCreek[idx];
            if (t == Hydro.DRY && ground < level) continue; // never a pool cell below its own surface
            if (t == Hydro.MAIN && Math.abs(h.ws[idx] - level) > 1.0) continue; // the reach above the lip
            if (t == Hydro.CREEK && idx != foot && Math.abs(h.ws[idx] - level) > 0.5) continue;
            double surf = t == Hydro.DRY ? level : h.ws[idx];
            double bed = surf - depth * (1 - 0.6 * (d / r) * (d / r));
            float cv = (float) Math.max(0, ground - bed);
            if (cv > h.carvePool[idx]) h.carvePool[idx] = cv;
            if (t == Hydro.DRY || idx == foot && t == Hydro.CREEK) {
                h.type[idx] = Hydro.POOL;
                h.ws[idx] = (float) level;
                h.body[idx] = h.body[foot];
                h.width[idx] = (float) (2 * r);
                if (t == Hydro.DRY) { h.dir[idx] = h.dir[foot]; h.speed[idx] = 6; }
            }
            h.flag(idx, Hydro.F_FALLS_POOL);
        }
    }

    /** Flags the 30 m below a fall as thin water. */
    private static void thin(Hydro h, WaterStage.Fall f) {
        CoreGrid g = h.grid;
        double reach = h.hp.falls().thinBelowM();
        if (f.onMain()) {
            double fx = g.lx(f.foot()), fz = g.lz(f.foot());
            int rc = (int) Math.ceil(reach / g.cell);
            for (int dz = -rc; dz <= rc; dz++) for (int dx = -rc; dx <= rc; dx++) {
                int x = g.col(f.foot()) + dx, z = g.row(f.foot()) + dz;
                if (!g.in(x, z)) continue;
                int idx = g.idx(x, z);
                if (h.type[idx] != Hydro.MAIN) continue;
                double ex = g.lx(idx) - fx, ez = g.lz(idx) - fz;
                if (ex * ex + ez * ez > reach * reach) continue;
                if (h.ws[idx] <= h.ws[f.foot()] + 0.05) h.flag(idx, Hydro.F_THIN);
            }
            return;
        }
        int[] rcv = h.receiver;
        double s = 0;
        int k = f.foot();
        while (k >= 0 && s <= reach) {
            if (h.type[k] != Hydro.DRY) h.flag(k, Hydro.F_THIN);
            int r = rcv[k];
            if (r < 0 || h.type[r] == Hydro.LAKE || h.type[r] == Hydro.OXBOW) break;
            s += Rivers.stepLen(g, k, r);
            k = r;
        }
    }
}

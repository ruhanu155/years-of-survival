package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stage 4's rivers (Terrain and hydrology › Rivers, River geometry, Braids, Oxbows, Flood lines and banks,
 * Springs and thin water).
 *
 * <p><b>The main river</b> is laid first, from the coarse net's crossings: its meandering channel
 * ({@link Channels}), cut-offs kept as oxbows, and braids in the braided flats. Along each channel the water
 * surface is the running minimum of the cross-section's lowest ground less a bank drop, so it never rises
 * downstream and the banks always stand above it; width, depth and current follow W = max(0.8, 0.0015 √A),
 * D = 0.3 + 0.08 W and V = min(3, max(0.3, 0.6 + 150 S)), with A from the coarse net and S over 100 m.
 *
 * <p><b>Creeks</b> come from D8 flow accumulation on the flooded ground ({@link Lakes}): every cell past the
 * 20,000 m² threshold that is not lake or main river. Their surface is carried down the receiver tree the
 * same way, so a creek cuts through any sill in its way. The bed carve lives in its own layers; stage 3's
 * height is never written.
 */
public final class Rivers {
    private Rivers() {}

    /** One channel of the main river, in local metres (real orientation). */
    public record Line(double[] x, double[] z, double[] w, double[] d, double[] ws, double[] a, double[] v, double[] tx,
                double[] tz, double[] kappa) {
        public int size() { return x.length; }
    }

    /** The main river: its channels, the line the braids follow, the oxbows, and where it meets the edges. */
    public record Main(List<Line> lines, Line axis, List<Channels.Line> oxbows, int oxbowLakes, double omega,
                double wavelengthM, int retries, int braids, double beltM, double lengthM,
                float[] dist, int[] ownerLine, int[] ownerPoint) {}

    // ---------------------------------------------------------------------------------------- main river

    static void mainChannel(Hydro h) {
        HydroParams.Water wp = h.hp.water();
        CoreGrid g = h.grid;
        Landform.Shape shape = h.land.shape();
        RiverPath path = h.land.path();
        Archetype.Params a = h.arch;
        double L = g.size;
        int phaseSeed = (int) h.rng.nextUInt(), ampSeed = (int) h.rng.nextUInt();
        double omega0 = a.meander().omegaRad().draw(h.rng);
        double lambdaWidths = a.meander().wavelengthWidths().draw(h.rng);
        int braidCount = a.channels().drawInt(h.rng);
        double belt = a.gravelBeltM() != null ? a.gravelBeltM().draw(h.rng) : 0;
        double nodeSpacing = wp.braidNodeSpacingM().draw(h.rng);
        double nodePhase = 2 * StrictMath.PI * h.rng.nextDouble();
        int[] wanderSeeds = new int[8];
        for (int k = 0; k < wanderSeeds.length; k++) wanderSeeds[k] = (int) h.rng.nextUInt();
        double oxDepthLo = a.oxbows() != null ? a.oxbows().depthM().min() : 2, oxDepthHi = a.oxbows() != null ? a.oxbows().depthM().max() : 3;
        int oxLakesWanted = a.oxbows() != null ? a.oxbows().lakes().drawInt(h.rng) : 0;
        double[] oxDepthDraw = new double[16];
        for (int k = 0; k < oxDepthDraw.length; k++) oxDepthDraw[k] = oxDepthLo + (oxDepthHi - oxDepthLo) * h.rng.nextDouble();

        boolean braided = belt > 0 && braidCount > 1 && shape.floorGradientPct() < wp.braidMaxGradientPct();
        int chans = braided ? braidCount : 1;
        double virtual = h.coarse.virtualM2();

        // Entry and exit in canonical z (the crossings sit on the west and east edges).
        double entryZ = h.coarse.entry().lz(), exitZ = h.coarse.exit().lz();

        Channels.WidthFn widthFn = (cx, cz) -> wp.width(catchmentAt(h, cx, cz, virtual) / chans);
        double w0 = widthFn.width(0, entryZ);
        double lambda = lambdaWidths * w0;

        Channels.Envelope env = new Channels.Envelope() {
            @Override public double maxOffset(double x) {
                double fw = shape.floorWidthM();
                if (shape.canyon() != null && x > shape.canyon().reachStart() - 50 && x < shape.canyon().reachEnd() + 50) {
                    fw = shape.canyon().floorWidthM();
                }
                double flatHalf = Math.max(a.troughFlatFraction(), 0.2) * fw / 2;
                double room = a.meander().maxOffsetFraction() * flatHalf - (braided ? belt / 2 : 0);
                return Math.max(6.0, room);
            }

            @Override public double minOffset(double x) {
                Landform.Shape.Basin b = shape.basin();
                if (b != null && Math.abs(x - b.centerX()) < b.lengthM() / 2 + 120) return 8.0;
                return Double.NEGATIVE_INFINITY;
            }
        };

        // Meander, cut-offs, and the oxbow retry (×1.2 on the swing, up to 3 times).
        Channels.Meander m = null;
        double omega = omega0;
        int maxRetries = a.oxbows() != null ? a.oxbows().maxRetries() : 0;
        for (int r = 0; r <= maxRetries; r++) {
            Channels.Line walked = Channels.walk(path, L, entryZ, exitZ, omega, lambda, a.meander().restoreRad(), env,
                    wp.edgePinM(), phaseSeed, ampSeed);
            m = Channels.cutOffs(walked, widthFn, wp.cutoffMinSeparationWidths(), wp.cutoffMaxGapWidths(), omega, lambda, r);
            if (a.oxbows() == null || usableLoops(m, widthFn, wp) >= a.oxbows().lakes().min()) break;
            omega *= a.oxbows().swingRetryFactor();
        }

        // The axis line and the channels, in local metres.
        Channels.Line axisC = m.line();
        int np = axisC.size();
        double[] pin = new double[np];
        for (int i = 0; i < np; i++) {
            double x = Noise.clamp(axisC.x()[i], 0, L);
            pin[i] = Noise.smooth(x / wp.edgePinM()) * Noise.smooth((L - x) / wp.edgePinM());
        }
        Line axis = profile(h, toLocal(g, axisC), widthFn, chans, null);
        List<Line> lines = new ArrayList<>();
        if (!braided) {
            lines.add(axis);
        } else {
            double beltHalf = Math.min(belt / 2, 0.9 * Math.max(a.troughFlatFraction(), 0.2) * shape.floorWidthM() / 2);
            for (int k = 0; k < chans; k++) {
                double[] off = Channels.braidOffsets(k, chans, np, beltHalf, nodeSpacing, nodePhase, wanderSeeds[k], pin);
                double[] bx = new double[np], bz = new double[np];
                for (int i = 0; i < np; i++) {
                    // Normal of the canonical line (rotate the tangent left), then to local.
                    int lo = Math.max(0, i - 3), hi = Math.min(np - 1, i + 3);
                    double tx = axisC.x()[hi] - axisC.x()[lo], tz = axisC.z()[hi] - axisC.z()[lo];
                    double len = Math.sqrt(tx * tx + tz * tz);
                    double nx = len > 0 ? -tz / len : 0, nz = len > 0 ? tx / len : 1;
                    bx[i] = axisC.x()[i] + off[i] * nx;
                    bz[i] = axisC.z()[i] + off[i] * nz;
                }
                Channels.Line bl = new Channels.Line(bx, bz);
                lines.add(profile(h, toLocal(g, bl), widthFn, chans, axis));
            }
        }

        // Oxbows: trim plugs, then the largest become lakes and the rest old, shallow scars.
        List<Channels.Line> oxbows = new ArrayList<>();
        List<double[]> oxDepth = new ArrayList<>();
        if (a.oxbows() != null) {
            List<Channels.Line> trimmed = new ArrayList<>();
            List<Double> areas = new ArrayList<>();
            for (Channels.Line loop : m.cutLoops()) {
                Channels.Line t = trimPlug(loop, widthFn, wp.oxbowPlugM());
                if (t == null) continue;
                trimmed.add(t);
                areas.add(Channels.length(t) * widthFn.width(t.x()[t.size() / 2], t.z()[t.size() / 2]));
            }
            Integer[] order = new Integer[trimmed.size()];
            for (int k = 0; k < order.length; k++) order[k] = k;
            Arrays.sort(order, (x, y) -> {
                int c = Double.compare(areas.get(y), areas.get(x));
                return c != 0 ? c : Integer.compare(x, y);
            });
            for (int r = 0; r < order.length; r++) {
                int k = order[r];
                boolean lake = r < oxLakesWanted && areas.get(k) >= wp.lakeMinAreaM2();
                oxbows.add(toLocal(g, trimmed.get(k)));
                oxDepth.add(new double[] {lake ? oxDepthDraw[r % oxDepthDraw.length] : 0.8, lake ? 1 : 0});
            }
        }

        // Stamp the channels.
        int total = h.total;
        float[] dist = new float[total];
        Arrays.fill(dist, Float.MAX_VALUE);
        int[] ownerLine = new int[total], ownerPoint = new int[total];
        Arrays.fill(ownerLine, -1);
        double margin = wp.bankMarginM();
        for (int li = 0; li < lines.size(); li++) {
            Line ln = lines.get(li);
            for (int i = 0; i < ln.size(); i++) {
                double r = ln.w()[i] / 2 + margin;
                int i0 = (int) Math.floor((ln.x()[i] - r) / g.cell), i1 = (int) Math.floor((ln.x()[i] + r) / g.cell);
                int k0 = (int) Math.floor((ln.z()[i] - r) / g.cell), k1 = (int) Math.floor((ln.z()[i] + r) / g.cell);
                for (int k = Math.max(0, k0); k <= Math.min(g.n - 1, k1); k++) {
                    for (int c = Math.max(0, i0); c <= Math.min(g.n - 1, i1); c++) {
                        double dx = (c + 0.5) * g.cell - ln.x()[i], dz = (k + 0.5) * g.cell - ln.z()[i];
                        double d = Math.sqrt(dx * dx + dz * dz);
                        int idx = k * g.n + c;
                        if (d <= r && d < dist[idx]) {
                            dist[idx] = (float) d;
                            ownerLine[idx] = li;
                            ownerPoint[idx] = i;
                        }
                    }
                }
            }
        }
        for (int idx = 0; idx < total; idx++) {
            int li = ownerLine[idx];
            if (li < 0) continue;
            Line ln = lines.get(li);
            int i = ownerPoint[idx];
            double half = ln.w()[i] / 2;
            double d = dist[idx];
            if (d > half) continue;
            double dn = d / half;
            double lat = ((g.lx(idx) - ln.x()[i]) * -ln.tz()[i] + (g.lz(idx) - ln.z()[i]) * ln.tx()[i]);
            double bend = Noise.clamp01(5 * ln.w()[i] * Math.abs(ln.kappa()[i]) - 0.5) * Noise.clamp01(Math.abs(lat) / half);
            boolean outside = lat * ln.kappa()[i] < 0;
            double scour = outside ? 1 + wp.bendScour() * bend : 1 - 0.4 * bend;
            double depth = ln.d()[i] * (1 - 0.7 * dn * dn) * scour;
            double bed = ln.ws()[i] - depth;
            h.type[idx] = Hydro.MAIN;
            h.ws[idx] = (float) ln.ws()[i];
            h.carveMain[idx] = (float) Math.max(0, h.h0[idx] - bed);
            h.body[idx] = 1;
            h.catchment[idx] = (float) ln.a()[i];
            h.width[idx] = (float) ln.w()[i];
            h.setCurrent(idx, ln.tx()[i], ln.tz()[i], ln.v()[i]);
        }

        // Carve the oxbows (never over the live channel).
        int lakes = 0;
        for (int k = 0; k < oxbows.size(); k++) {
            Channels.Line ox = oxbows.get(k);
            double depth = oxDepth.get(k)[0];
            if (oxDepth.get(k)[1] > 0) lakes++;
            double gmin = Double.MAX_VALUE;
            for (int i = 0; i < ox.size(); i++) gmin = Math.min(gmin, g.sample(h.h0, ox.x()[i], ox.z()[i]));
            for (int i = 0; i < ox.size(); i++) {
                double half = widthFn.width(g.canonToLocalX(ox.x()[i]), ox.z()[i]) / 2;
                int i0 = (int) Math.floor((ox.x()[i] - half) / g.cell), i1 = (int) Math.floor((ox.x()[i] + half) / g.cell);
                int k0 = (int) Math.floor((ox.z()[i] - half) / g.cell), k1 = (int) Math.floor((ox.z()[i] + half) / g.cell);
                for (int kk = Math.max(0, k0); kk <= Math.min(g.n - 1, k1); kk++) {
                    for (int c = Math.max(0, i0); c <= Math.min(g.n - 1, i1); c++) {
                        int idx = kk * g.n + c;
                        if (h.type[idx] != Hydro.DRY || dist[idx] < Float.MAX_VALUE) continue;
                        double dx = (c + 0.5) * g.cell - ox.x()[i], dz = (kk + 0.5) * g.cell - ox.z()[i];
                        double d = Math.sqrt(dx * dx + dz * dz);
                        if (d > half) continue;
                        double dn = d / half;
                        double bed = gmin - depth * (1 - 0.6 * dn * dn);
                        float cv = (float) Math.max(0, h.h0[idx] - bed);
                        if (cv > h.carveMain[idx]) h.carveMain[idx] = cv;
                        h.oxbowCarved[idx] = true;
                    }
                }
            }
        }

        h.main = new Main(List.copyOf(lines), axis, List.copyOf(oxbows), lakes, m.omega(), m.wavelengthM(), m.retries(),
                braided ? chans : 1, braided ? belt : 0, Channels.length(new Channels.Line(axis.x(), axis.z())),
                dist, ownerLine, ownerPoint);
        h.bodies.add(new WaterStage.Body(1, "river_reach", 0, 0, -1, h.main.lengthM()));
    }

    /** Catchment of the main river at a canonical position: the coarse net's, never under the virtual inflow. */
    static double catchmentAt(Hydro h, double cx, double cz, double virtual) {
        double lx = h.grid.canonToLocalX(Noise.clamp(cx, 0, h.grid.size));
        return Math.max(virtual, h.coarse.catchmentNear(h.grid.originX + lx, h.grid.originZ + cz));
    }

    private static int usableLoops(Channels.Meander m, Channels.WidthFn w, HydroParams.Water wp) {
        int c = 0;
        for (Channels.Line loop : m.cutLoops()) {
            Channels.Line t = trimPlug(loop, w, wp.oxbowPlugM());
            if (t != null && Channels.length(t) * w.width(t.x()[t.size() / 2], t.z()[t.size() / 2]) >= wp.lakeMinAreaM2()) c++;
        }
        return c;
    }

    /** Drops the loop's ends that lie within a width plus the plug of the chord the live channel now takes. */
    static Channels.Line trimPlug(Channels.Line loop, Channels.WidthFn w, double plug) {
        int n = loop.size();
        double ax = loop.x()[0], az = loop.z()[0], bx = loop.x()[n - 1], bz = loop.z()[n - 1];
        double width = w.width(ax, az);
        double keep = width + plug;
        int first = -1, last = -1;
        for (int i = 0; i < n; i++) {
            double d = segDist(loop.x()[i], loop.z()[i], ax, az, bx, bz);
            if (d >= keep) {
                if (first < 0) first = i;
                last = i;
            }
        }
        if (first < 0 || last - first < 4) return null;
        double[] x = Arrays.copyOfRange(loop.x(), first, last + 1), z = Arrays.copyOfRange(loop.z(), first, last + 1);
        return new Channels.Line(x, z);
    }

    static double segDist(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az;
        double l2 = vx * vx + vz * vz;
        double t = l2 > 0 ? Noise.clamp01(((px - ax) * vx + (pz - az) * vz) / l2) : 0;
        double dx = px - (ax + t * vx), dz = pz - (az + t * vz);
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static Channels.Line toLocal(CoreGrid g, Channels.Line c) {
        double[] x = new double[c.size()];
        for (int i = 0; i < x.length; i++) x[i] = g.canonToLocalX(c.x()[i]);
        return new Channels.Line(x, c.z().clone());
    }

    /**
     * Widths, depths, surface, current and curvature along one channel (local metres). A braid's surface never
     * stands above the axis line's at the same point, so the braids fall together.
     */
    private static Line profile(Hydro h, Channels.Line c, Channels.WidthFn widthFn, int chans, Line axis) {
        HydroParams.Water wp = h.hp.water();
        CoreGrid g = h.grid;
        int n = c.size();
        double[] w = new double[n], d = new double[n], ws = new double[n], a = new double[n], v = new double[n];
        double[] tx = new double[n], tz = new double[n], th = new double[n], kappa = new double[n];
        double virtual = h.coarse.virtualM2();
        double amax = 0;
        for (int i = 0; i < n; i++) {
            double cx = g.mirror ? g.size - c.x()[i] : c.x()[i];
            amax = Math.max(amax, catchmentAt(h, cx, c.z()[i], virtual));
            a[i] = amax; // never shrinks downstream
            w[i] = wp.width(a[i] / chans);
            d[i] = wp.depth(w[i]);
            int lo = Math.max(0, i - 2), hi = Math.min(n - 1, i + 2);
            double ex = c.x()[hi] - c.x()[lo], ez = c.z()[hi] - c.z()[lo];
            double len = Math.sqrt(ex * ex + ez * ez);
            tx[i] = len > 0 ? ex / len : 1;
            tz[i] = len > 0 ? ez / len : 0;
            th[i] = StrictMath.atan2(tz[i], tx[i]);
        }
        double run = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            double r = w[i] / 2 + wp.bankMarginM();
            double low = Double.MAX_VALUE;
            for (double o = -r; o <= r + 1e-9; o += 1.0) {
                low = Math.min(low, g.sample(h.h0, c.x()[i] - tz[i] * o, c.z()[i] + tx[i] * o));
            }
            double want = low - wp.bankDropM();
            if (axis != null) want = Math.min(want, axis.ws()[Math.min(i, axis.size() - 1)]);
            run = Math.min(run, want);
            ws[i] = run;
        }
        int win = (int) Math.round(wp.slopeWindowM() / 2 / Channels.DS);
        for (int i = 0; i < n; i++) {
            int lo = Math.max(0, i - win), hi = Math.min(n - 1, i + win);
            double s = hi > lo ? (ws[lo] - ws[hi]) / ((hi - lo) * Channels.DS) : 0;
            v[i] = wp.speed(Math.max(0, s));
            int m = Math.max(2, (int) Math.round(w[i] / Channels.DS));
            int a0 = Math.max(0, i - m), a1 = Math.min(n - 1, i + m);
            double dth = th[a1] - th[a0];
            while (dth > StrictMath.PI) dth -= 2 * StrictMath.PI;
            while (dth < -StrictMath.PI) dth += 2 * StrictMath.PI;
            kappa[i] = a1 > a0 ? dth / ((a1 - a0) * Channels.DS) : 0;
        }
        return new Line(c.x(), c.z(), w, d, ws, a, v, tx, tz, kappa);
    }

    // -------------------------------------------------------------------------------------------- creeks

    /**
     * Flow accumulation on the flooded ground, creek cells past the threshold, and creek bodies. Fills
     * {@link Hydro#creekOrder} (upstream first) and {@link Hydro#receiver}.
     */
    static void creeks(Hydro h) {
        HydroParams.Water wp = h.hp.water();
        CoreGrid g = h.grid;
        Lakes.Flood f = h.flood0;
        int total = h.total;
        double[] acc = new double[total];
        for (int i = 0; i < total; i++) acc[i] = h.cellArea;
        // Rivers that cross into the core from the coarse net: their catchment enters two cells inside the edge.
        for (CoarseRivers.Crossing c : h.coarse.inflows()) {
            double lx = Noise.clamp(c.lx(), 2.5 * g.cell, g.size - 2.5 * g.cell);
            double lz = Noise.clamp(c.lz(), 2.5 * g.cell, g.size - 2.5 * g.cell);
            int idx = g.cellClamped(lx, lz);
            if (h.type[idx] == Hydro.DRY) acc[idx] += c.catchmentM2();
        }
        int[] order = f.order();
        int[] rcv = f.receiver();
        for (int t = order.length - 1; t >= 0; t--) {
            int c = order[t];
            if (rcv[c] >= 0) acc[rcv[c]] += acc[c];
        }
        for (int i = 0; i < total; i++) if (h.type[i] != Hydro.MAIN) h.catchment[i] = (float) acc[i];
        h.receiver = rcv;

        // Creek cells, upstream first, with creek ids that follow the larger donor.
        int[] id = new int[total];
        Arrays.fill(id, -1);
        double[] bestDonor = new double[total];
        int[] fromId = new int[total];
        Arrays.fill(fromId, -1);
        int[] tmp = new int[total];
        int count = 0, next = 0;
        List<int[]> heads = new ArrayList<>();
        for (int t = order.length - 1; t >= 0; t--) {
            int c = order[t];
            if (acc[c] < wp.riverThresholdM2() || h.type[c] != Hydro.DRY) continue;
            int myId = fromId[c] >= 0 ? fromId[c] : next++;
            id[c] = myId;
            tmp[count++] = c;
            int r = rcv[c];
            if (r >= 0 && h.type[r] == Hydro.DRY && acc[c] > bestDonor[r]) {
                bestDonor[r] = acc[c];
                fromId[r] = myId;
            }
        }
        h.creekOrder = Arrays.copyOf(tmp, count);
        for (int c : h.creekOrder) h.type[c] = Hydro.CREEK;

        // Creek bodies: cells in order per id.
        int[] len = new int[next];
        for (int c : h.creekOrder) len[id[c]]++;
        int[][] cells = new int[next][];
        for (int k = 0; k < next; k++) cells[k] = new int[len[k]];
        int[] fill = new int[next];
        for (int c : h.creekOrder) cells[id[c]][fill[id[c]]++] = c;
        int bodyBase = h.bodies.size() + 1;
        for (int k = 0; k < next; k++) {
            int[] cs = cells[k];
            double length = 0;
            for (int i = 1; i < cs.length; i++) length += stepLen(g, cs[i - 1], cs[i]);
            int last = cs[cs.length - 1];
            int r = rcv[last];
            String into = r < 0 ? "edge" : h.type[r] == Hydro.MAIN ? "main" : h.type[r] == Hydro.LAKE || h.type[r] == Hydro.OXBOW ? "lake" : h.type[r] == Hydro.CREEK ? "creek" : "edge";
            int bodyId = bodyBase + k;
            for (int c : cs) h.body[c] = (short) bodyId;
            h.creeks.add(new WaterStage.Creek(bodyId, cs, length, acc[last], into));
            h.bodies.add(new WaterStage.Body(bodyId, "creek", cs.length * h.cellArea, 0, last, length));
        }
        creekSurface(h);
    }

    static double stepLen(CoreGrid g, int a, int b) {
        int dx = Math.abs(g.col(a) - g.col(b)), dz = Math.abs(g.row(a) - g.row(b));
        return (dx != 0 && dz != 0 ? 1.4142135623730951 : 1.0) * g.cell;
    }

    /** Creek water surface, carve, width and current, carried downstream. Re-run after the falls guarantee. */
    static void creekSurface(Hydro h) {
        HydroParams.Water wp = h.hp.water();
        CoreGrid g = h.grid;
        int total = h.total;
        int[] rcv = h.receiver;
        Arrays.fill(h.carveCreek, 0f);
        // Clear the side cells a previous run stamped, so a re-run starts from the creek lines alone.
        boolean[] line = new boolean[total];
        for (int c : h.creekOrder) line[c] = true;
        for (int i = 0; i < total; i++) {
            if (h.type[i] == Hydro.CREEK && !line[i]) {
                h.type[i] = Hydro.DRY;
                h.ws[i] = 0;
                h.body[i] = 0;
                h.width[i] = 0;
                h.dir[i] = (byte) Hydro.STILL;
                h.speed[i] = 0;
            }
        }
        float[] wsIn = new float[total];
        Arrays.fill(wsIn, Float.MAX_VALUE);
        for (int c : h.creekOrder) {
            double ground = h.h0[c] - h.carveMain[c] - h.extra[c];
            double s = Math.min(ground - wp.creekBankDropM(), wsIn[c]);
            h.ws[c] = (float) s;
            int r = rcv[c];
            if (r >= 0 && h.type[r] == Hydro.CREEK && s < wsIn[r]) wsIn[r] = (float) s;
        }
        // Water never stands below what it flows into: from the mouths up, raise any cell to its receiver's
        // surface (a short backwater where a creek meets a lake or the river at bank height).
        for (int t = h.creekOrder.length - 1; t >= 0; t--) {
            int c = h.creekOrder[t];
            int r = rcv[c];
            if (r >= 0 && h.type[r] != Hydro.DRY && h.ws[r] > h.ws[c]) h.ws[c] = h.ws[r];
        }
        for (int c : h.creekOrder) {
            double depth = wp.depth(wp.width(h.catchment[c]));
            h.carveCreek[c] = (float) Math.max(0, h.h0[c] - h.carveMain[c] - (h.ws[c] - depth));
            h.width[c] = (float) wp.width(h.catchment[c]);
        }
        // Current: the D8 direction, and the slope of the surface over about 100 m downstream.
        double window = wp.slopeWindowM();
        for (int c : h.creekOrder) {
            int r = rcv[c];
            if (r < 0) {
                h.dir[c] = (byte) Hydro.STILL;
                h.speed[c] = 0;
                continue;
            }
            double dist = 0;
            int k = c;
            while (dist < window && rcv[k] >= 0 && h.type[rcv[k]] == Hydro.CREEK) {
                dist += stepLen(g, k, rcv[k]);
                k = rcv[k];
            }
            double s = dist > 0 ? (h.ws[c] - h.ws[k]) / dist : 0;
            h.setCurrent(c, g.col(r) - g.col(c), g.row(r) - g.row(c), wp.speed(Math.max(0, s)));
        }
        // Wide creeks take a second cell either side of the line.
        for (int c : h.creekOrder) {
            double w = h.width[c];
            if (w < wp.stampWidthM()) continue;
            int r = rcv[c];
            if (r < 0) continue;
            int dx = g.col(r) - g.col(c), dz = g.row(r) - g.row(c);
            int[][] sides = {{-dz, dx}, {dz, -dx}};
            for (int[] sd : sides) {
                int x = g.col(c) + Integer.signum(sd[0]), z = g.row(c) + Integer.signum(sd[1]);
                if (!g.in(x, z)) continue;
                int idx = g.idx(x, z);
                if (h.type[idx] != Hydro.DRY) continue;
                double depth = wp.depth(w) * 0.6;
                double bed = h.ws[c] - depth;
                double ground = h.h0[idx] - h.carveMain[idx];
                if (ground < h.ws[c]) continue; // would sit below the surface: leave it to the bank
                h.type[idx] = Hydro.CREEK;
                h.ws[idx] = h.ws[c];
                h.carveCreek[idx] = (float) Math.max(0, ground - bed);
                h.width[idx] = (float) w;
                h.body[idx] = h.body[c];
                h.catchment[idx] = h.catchment[c];
                h.dir[idx] = h.dir[c];
                h.speed[idx] = h.speed[c];
            }
        }
    }

    // --------------------------------------------------------------------------- banks, lines and flags

    /** Gravel bars, cut banks, flood lines, springs, fords, riffles, sweepers and the bog flats. */
    static void banks(Hydro h) {
        HydroParams.Water wp = h.hp.water();
        CoreGrid g = h.grid;
        Main m = h.main;
        int total = h.total;
        float[] ground = h.ground();

        // Main river banks: bends tighter than 5 widths.
        for (int idx = 0; idx < total; idx++) {
            int li = m.ownerLine()[idx];
            if (li < 0 || h.type[idx] != Hydro.DRY) continue;
            Line ln = m.lines().get(li);
            int i = m.ownerPoint()[idx];
            double lat = (g.lx(idx) - ln.x()[i]) * -ln.tz()[i] + (g.lz(idx) - ln.z()[i]) * ln.tx()[i];
            boolean tight = Math.abs(ln.kappa()[i]) * wp.cutBankMaxRadiusWidths() * ln.w()[i] > 1.0;
            double bankH = ground[idx] - ln.ws()[i];
            if (tight && lat * ln.kappa()[i] < 0 && bankH > wp.cutBankMinBankM()) {
                h.bank[idx] = Hydro.CUT_BANK;
            } else if (tight && lat * ln.kappa()[i] > 0 && bankH < 1.5) {
                h.bank[idx] = Hydro.BAR;
            }
        }
        // Inside bends reach further onto the floor than the stamp margin: grow bars across low ground.
        for (Line ln : m.lines()) {
            for (int i = 0; i < ln.size(); i += 2) {
                if (Math.abs(ln.kappa()[i]) * wp.cutBankMaxRadiusWidths() * ln.w()[i] <= 1.0) continue;
                double side = Math.signum(ln.kappa()[i]);
                for (double o = ln.w()[i] / 2 + 1; o < ln.w()[i] * 1.5; o += 2) {
                    int idx = g.cellAt(ln.x()[i] - ln.tz()[i] * o * side, ln.z()[i] + ln.tx()[i] * o * side);
                    if (idx < 0 || h.type[idx] != Hydro.DRY) continue;
                    if (ground[idx] - ln.ws()[i] < 1.5 && h.bank[idx] == Hydro.NONE) h.bank[idx] = Hydro.BAR;
                }
            }
        }
        // Braided flats: the dry, low ground of the belt between channels is gravel bar.
        if (m.braids() > 1) {
            Line ax = m.axis();
            float[] beltWs = new float[total];
            Arrays.fill(beltWs, Float.NaN);
            double r = m.beltM() / 2 + ax.w()[0];
            for (int i = 0; i < ax.size(); i += 3) {
                int i0 = (int) Math.floor((ax.x()[i] - r) / g.cell), i1 = (int) Math.floor((ax.x()[i] + r) / g.cell);
                int k0 = (int) Math.floor((ax.z()[i] - r) / g.cell), k1 = (int) Math.floor((ax.z()[i] + r) / g.cell);
                for (int k = Math.max(0, k0); k <= Math.min(g.n - 1, k1); k++) {
                    for (int c = Math.max(0, i0); c <= Math.min(g.n - 1, i1); c++) {
                        double dx = (c + 0.5) * g.cell - ax.x()[i], dz = (k + 0.5) * g.cell - ax.z()[i];
                        int idx = k * g.n + c;
                        if (dx * dx + dz * dz <= r * r && Float.isNaN(beltWs[idx])) beltWs[idx] = (float) ax.ws()[i];
                    }
                }
            }
            for (int idx = 0; idx < total; idx++) {
                if (h.type[idx] != Hydro.DRY || Float.isNaN(beltWs[idx])) continue;
                if (ground[idx] - beltWs[idx] < 2.5) h.bank[idx] = Hydro.BAR;
            }
        }
        // Creek mouths onto the main river build bars.
        for (WaterStage.Creek c : h.creeks) {
            if (!c.into().equals("main")) continue;
            int last = c.cells()[c.cells().length - 1];
            int ci = g.col(last), ck = g.row(last);
            for (int dz = -4; dz <= 4; dz++) for (int dx = -4; dx <= 4; dx++) {
                if (!g.in(ci + dx, ck + dz)) continue;
                int idx = g.idx(ci + dx, ck + dz);
                if (h.type[idx] == Hydro.DRY && h.bank[idx] == Hydro.NONE && ground[idx] - h.ws[last] < 1.5) h.bank[idx] = Hydro.BAR;
            }
        }

        // Flood lines, fords, riffles, sweepers.
        for (int idx = 0; idx < total; idx++) {
            byte t = h.type[idx];
            if (t == Hydro.MAIN) {
                h.flood[idx] = (byte) ((q4(wp.mainNormalM()) << 4) | q4(wp.mainFloodM()));
            } else if (t == Hydro.CREEK || t == Hydro.POOL) {
                h.flood[idx] = (byte) ((q4(wp.creekNormalM()) << 4) | q4(wp.creekFloodM()));
            }
            if (t == Hydro.MAIN || t == Hydro.CREEK) {
                double depth = h.depth(idx), v = h.speedAt(idx);
                if (wp.wadeable(depth, v) && (t == Hydro.MAIN || h.width[idx] >= 2)) h.flag(idx, Hydro.F_FORD);
                if (v >= 1.2 && depth < 0.6) h.flag(idx, Hydro.F_RIFFLE);
            }
            if (h.bank[idx] == Hydro.CUT_BANK) {
                // The water cells beside a cut bank are a sweeper reach.
                for (int k = 0; k < 8; k++) {
                    int x = g.col(idx) + CoreGrid.DX[k], z = g.row(idx) + CoreGrid.DZ[k];
                    if (g.in(x, z) && h.type[g.idx(x, z)] == Hydro.MAIN) h.flag(g.idx(x, z), Hydro.F_SWEEPER);
                }
            }
        }

        // Springs: 1–2 per creek longer than 500 m, in its upper half; the lead runs 50–200 m below.
        for (WaterStage.Creek c : h.creeks) {
            if (c.lengthM() < wp.springMinCreekM()) continue;
            int count = wp.springsPerCreek().drawInt(h.rng);
            for (int s = 0; s < count; s++) {
                int at = (int) (h.rng.nextDouble() * c.cells().length * 0.5);
                double leadM = 50 + 150 * h.rng.nextDouble();
                int cell = c.cells()[at];
                h.flag(cell, Hydro.F_SPRING);
                double run = 0;
                for (int k = at; k < c.cells().length && run <= leadM; k++) {
                    h.flag(c.cells()[k], Hydro.F_LEAD);
                    if (k > at) run += stepLen(g, c.cells()[k - 1], c.cells()[k]);
                }
                h.springs.add(new WaterStage.Spring(cell, c.bodyId(), leadM));
            }
        }

        // Bog flats: under 2°, within 200 m of water, over 1 m of fine sediment.
        if (!"gravel".equals(h.arch.floorSoil())) {
            boolean[] water = new boolean[total];
            for (int i = 0; i < total; i++) water[i] = h.type[i] != Hydro.DRY;
            float[] wd = g.distance(water);
            double maxTan = CoreGrid.tanDeg(wp.bogMaxSlopeDeg());
            byte[] sed = h.ero.sediment();
            for (int i = 0; i < total; i++) {
                if (h.type[i] != Hydro.DRY || h.bank[i] != Hydro.NONE) continue;
                if (wd[i] > wp.bogMaxWaterDistM() || (sed[i] & 0xFF) / 10.0 <= wp.bogMinSedimentM()) continue;
                if (g.slope(ground, i) < maxTan) h.bank[i] = Hydro.BOG;
            }
        }
    }

    private static int q4(double m) { return Math.max(0, Math.min(15, (int) Math.round(m / 0.25))); }

}

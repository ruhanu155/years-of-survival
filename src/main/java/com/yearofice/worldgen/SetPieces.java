package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Stage 5, set pieces (Blueprint › Set pieces; Minecraft development › Set pieces as templates). Each piece in
 * priority order takes its best-fitting site that keeps its spacing from every piece already placed. Fit rules
 * are hard and read only layers that exist by M1; preferences and a small seeded jitter rank what passes.
 *
 * <p>Pieces in the core are placed on the 2 m layers with a footprint mask (all dry, no cell over 25°, the mean
 * under the piece's own limit, no overlap). Pieces whose area lies outside the core (the hot spring, the camp
 * on the old mining creek, a second lookout) are placed on the 32 m coarse field and the coarse river net and
 * marked {@code coarse}; that region refines the spot when it streams in (M9). If no site passes, the rules
 * relax in two steps and the placement records how far; that keeps every attempt placeable without a reroll.
 */
public final class SetPieces {
    private SetPieces() {}

    public static final String[] ROTATIONS = {"none", "clockwise_90", "clockwise_180", "counterclockwise_90"};

    /**
     * One placed set piece. {@code x, z} are the world centre, {@code y} the flattened floor; {@code rotation}
     * turns the template's south door to face {@code facingDeg}; {@code relaxed} 0 means every rule held.
     */
    public record Placement(String id, int instance, String variant, String template, String area, String site,
                            double x, double z, double y, int rotation, double facingDeg, int footprintX,
                            int footprintZ, boolean coarse, int region, double score, int relaxed,
                            int reachStart, int reachEnd, int ponds) {
        public String rotationName() { return ROTATIONS[rotation]; }
    }

    /** Everything the fit rules read, built once per run. */
    static final class Sites {
        final WaterStage.Layers w;
        final CoreGrid g;
        final float[] ground, slopeTan, dWater, dMain, dLake, dCreek, mainLevel, localMax60, localMax100;
        final int[] faceAt;
        final double minElev, maxElev;
        final HeightField field;
        final Landform.Layers land;
        final SeedStage.SeedParams seed;
        // Coarse
        final int cn;
        final double cm, half;
        final float[] cSlope, cRiverDist, cLocalMax;
        final boolean[] cRiver;

        Sites(SeedStage.SeedParams seed, Landform.Layers land, Erosion.Layers ero, WaterStage.Layers w, WorldgenParams p) {
            this.seed = seed;
            this.land = land;
            this.w = w;
            this.g = new CoreGrid(p, seed.mirror());
            int total = g.n * g.n;
            ground = w.ground(ero.height());
            slopeTan = g.slopes(ground);
            boolean[] wet = new boolean[total], main = new boolean[total], lake = new boolean[total], creek = new boolean[total];
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            for (int i = 0; i < total; i++) {
                int t = w.type()[i];
                wet[i] = t != WaterStage.DRY;
                main[i] = t == WaterStage.MAIN;
                lake[i] = t == WaterStage.LAKE || t == WaterStage.OXBOW;
                creek[i] = t == WaterStage.CREEK || t == WaterStage.POOL;
                lo = Math.min(lo, ground[i]);
                hi = Math.max(hi, ground[i]);
            }
            minElev = lo;
            maxElev = hi;
            dWater = g.distance(wet);
            dMain = g.distance(main);
            dLake = g.distance(lake);
            dCreek = g.distance(creek);
            mainLevel = nearestLevel(g, w, main);
            localMax60 = maxFilter(ground, g.n, 30);
            localMax100 = maxFilter(ground, g.n, 50);
            faceAt = new int[total];
            Arrays.fill(faceAt, -1);
            List<WaterStage.Face> faces = w.faces();
            for (int f = 0; f < faces.size(); f++) {
                WaterStage.Face face = faces.get(f);
                if (face.heightM() < 6) continue;
                int ci = g.col(face.foot()), ck = g.row(face.foot());
                for (int dz = -6; dz <= 6; dz++) for (int dx = -6; dx <= 6; dx++) {
                    if (!g.in(ci + dx, ck + dz) || dx * dx + dz * dz > 36) continue;
                    int idx = g.idx(ci + dx, ck + dz);
                    if (faceAt[idx] < 0 || faces.get(faceAt[idx]).heightM() < face.heightM()) faceAt[idx] = f;
                }
            }
            field = new HeightField(p, g, ground, land);

            cn = land.coarseCells();
            cm = p.grid().coarseCellM();
            half = p.grid().worldSizeM() / 2.0;
            float[] ch = land.coarse();
            cSlope = new float[cn * cn];
            cRiver = new boolean[cn * cn];
            double minA = p.hydro().coarse().crossingMinM2();
            for (int c = 0; c < cn * cn; c++) {
                int i = c % cn, k = c / cn;
                int l = Math.max(0, i - 1), r = Math.min(cn - 1, i + 1), u = Math.max(0, k - 1), d = Math.min(cn - 1, k + 1);
                double gx = (ch[k * cn + r] - ch[k * cn + l]) / ((r - l) * cm), gz = (ch[d * cn + i] - ch[u * cn + i]) / ((d - u) * cm);
                cSlope[c] = (float) Math.sqrt(gx * gx + gz * gz);
                cRiver[c] = w.coarse().catchment()[c] >= minA;
            }
            cRiverDist = coarseDistance(cRiver, cn, cm);
            cLocalMax = maxFilter(ch, cn, 3);
        }

        double worldX(int idx) { return g.worldX(idx); }

        double worldZ(int idx) { return g.worldZ(idx); }

        double cWorldX(int c) { return -half + (c % cn + 0.5) * cm; }

        double cWorldZ(int c) { return -half + (c / cn + 0.5) * cm; }
    }

    /** Nearest main-river water surface for every cell (multi-source breadth-first search). */
    static float[] nearestLevel(CoreGrid g, WaterStage.Layers w, boolean[] main) {
        int total = g.n * g.n;
        float[] out = new float[total];
        Arrays.fill(out, Float.NaN);
        int[] q = new int[total];
        int qh = 0, qt = 0;
        for (int i = 0; i < total; i++) if (main[i]) { out[i] = w.surface()[i]; q[qt++] = i; }
        while (qh < qt) {
            int c = q[qh++];
            int ci = g.col(c), ck = g.row(c);
            for (int m = 0; m < 8; m += 2) {
                int x = ci + CoreGrid.DX[m], z = ck + CoreGrid.DZ[m];
                if (!g.in(x, z)) continue;
                int nb = g.idx(x, z);
                if (!Float.isNaN(out[nb])) continue;
                out[nb] = out[c];
                q[qt++] = nb;
            }
        }
        return out;
    }

    /** Square max filter of radius r cells (separable sliding maximum). */
    static float[] maxFilter(float[] a, int n, int r) {
        float[] tmp = new float[n * n], out = new float[n * n];
        int[] dq = new int[n];
        for (int k = 0; k < n; k++) slide(a, tmp, k * n, 1, n, r, dq);
        for (int i = 0; i < n; i++) slide(tmp, out, i, n, n, r, dq);
        return out;
    }

    private static void slide(float[] src, float[] dst, int base, int stride, int n, int r, int[] dq) {
        int head = 0, tail = 0;
        for (int j = 0; j < n + r; j++) {
            if (j < n) {
                float v = src[base + j * stride];
                while (tail > head && src[base + dq[tail - 1] * stride] <= v) tail--;
                dq[tail++] = j;
            }
            int out = j - r;
            if (out >= 0) {
                while (dq[head] < out - r) head++;
                dst[base + out * stride] = src[base + dq[head] * stride];
            }
        }
    }

    static float[] coarseDistance(boolean[] src, int cn, double cm) {
        float[] d = new float[cn * cn];
        Arrays.fill(d, Float.MAX_VALUE);
        boolean[] done = new boolean[cn * cn];
        CellHeap heap = new CellHeap(cn * 8);
        for (int i = 0; i < src.length; i++) if (src[i]) { d[i] = 0; heap.push(0, i); }
        while (!heap.isEmpty()) {
            int c = heap.pop();
            if (done[c]) continue;
            done[c] = true;
            int ci = c % cn, ck = c / cn;
            for (int m = 0; m < 8; m++) {
                int x = ci + CoreGrid.DX[m], z = ck + CoreGrid.DZ[m];
                if (x < 0 || z < 0 || x >= cn || z >= cn) continue;
                int nb = z * cn + x;
                float nd = (float) (d[c] + CoreGrid.DL[m] * cm);
                if (nd < d[nb]) { d[nb] = nd; heap.push(nd, nb); }
            }
        }
        return d;
    }

    /** A candidate: core cell or coarse cell, the site kind it matched, and its reach for beaver valleys. */
    private record Cand(int cell, boolean coarse, String site, int reachStart, int reachEnd) {}

    /** Places every set piece. Writes the footprint mask (piece index + 1 per core cell). */
    static List<Placement> run(Sites s, List<SetPieceDef> defs, WorldgenParams p, byte[] mask, List<String> notes) {
        com.yearofice.core.hash.Mulberry32 rng = SeedStage.rng(s.seed.attemptSeed(), PlacesStage.STAGE);
        int jitterSeed = (int) rng.nextUInt();
        List<Placement> out = new ArrayList<>();
        for (SetPieceDef d : defs) {
            int count = d.count().drawInt(rng);
            for (int k = 0; k < count; k++) {
                Placement best = null;
                boolean required = k < (int) d.count().min();
                // Each area in order, strict then a little relaxed, before the next area; a required piece
                // finally takes its first area with the rules relaxed further.
                for (int a = Math.min(k, d.areas().size() - 1); a < d.areas().size() && best == null; a++) {
                    for (int relax = 0; relax <= 1 && best == null; relax++) {
                        best = place(s, d, k, d.areas().get(a), relax, out, p, mask, jitterSeed);
                    }
                }
                if (best == null && required) {
                    best = place(s, d, k, d.areas().get(Math.min(k, d.areas().size() - 1)), 2, out, p, mask, jitterSeed);
                }
                if (best == null) {
                    if (required) notes.add("setpiece " + d.id() + " #" + k + ": no site");
                    continue;
                }
                if (best.relaxed() > 0) notes.add("setpiece " + d.id() + " #" + k + ": rules relaxed " + best.relaxed());
                stamp(s, best, out.size() + 1, mask);
                out.add(best);
            }
        }
        return out;
    }

    private static Placement place(Sites s, SetPieceDef d, int instance, String area, int relax, List<Placement> placed,
                                   WorldgenParams p, byte[] mask, int jitterSeed) {
        List<Cand> cands = area.equals("core") ? coreCandidates(s, d, relax) : coarseCandidates(s, d, area, relax);
        HydroParams.Places pp = p.hydro().places();
        Placement best = null;
        double bestScore = -Double.MAX_VALUE;
        Placement view = null;
        for (Placement q : placed) if (q.id().equals(d.prefer().viewOf())) { view = q; break; }
        for (Cand c : cands) {
            double wx = c.coarse() ? s.cWorldX(c.cell()) : s.worldX(c.cell());
            double wz = c.coarse() ? s.cWorldZ(c.cell()) : s.worldZ(c.cell());
            if (!spaced(s, d, wx, wz, placed, relax)) continue;
            SetPieceDef.Variant v = variantFor(d, c, jitterSeed, instance);
            if (v == null) continue;
            double facing = facing(s, d, c, wx, wz, view);
            int rot = rotationFor(facing);
            int fx = rot % 2 == 1 ? v.footprintZ() : v.footprintX(), fz = rot % 2 == 1 ? v.footprintX() : v.footprintZ();
            double meanSlope;
            double floor;
            if (c.coarse()) {
                double sl = CoreGrid.deg(s.cSlope[c.cell()]);
                if (sl > pp.coarseMaxSlopeDeg() * (relax > 0 ? 1.5 : 1) || s.cRiver[c.cell()]) continue;
                meanSlope = sl;
                floor = s.land.coarse()[c.cell()];
            } else {
                double[] fp = footprint(s, c.cell(), fx, fz, mask, pp.maxSlopeDeg());
                if (fp == null) continue;
                meanSlope = fp[0];
                floor = fp[1];
                if (meanSlope > Math.min(25, d.fit().maxSlopeDeg() * (relax > 0 ? 1.5 : 1))) continue;
            }
            double score = score(s, d, c, wx, wz, meanSlope, view, jitterSeed, pp.jitter());
            if (score > bestScore) {
                bestScore = score;
                best = new Placement(d.id(), instance, v.name(), "yearofice:setpiece/" + v.template(), area, c.site(), wx, wz,
                        Math.round(floor * 10) / 10.0, rot, Math.round(facing * 10) / 10.0, fx, fz, c.coarse(),
                        s.field.region(wx, wz), Math.round(score * 1000) / 1000.0, relax, c.reachStart(), c.reachEnd(),
                        0);
            }
        }
        if (best != null && d.path().equals("beaver_valley")) {
            int ponds = switch (best.variant()) {
                case "dam_staircase" -> 4 + Math.floorMod(Noise.hash(jitterSeed, (int) best.x(), (int) best.z()), 4);
                case "great_dam" -> 1;
                default -> 0;
            };
            best = new Placement(best.id(), best.instance(), best.variant(), best.template(), best.area(), best.site(),
                    best.x(), best.z(), best.y(), best.rotation(), best.facingDeg(), best.footprintX(), best.footprintZ(),
                    best.coarse(), best.region(), best.score(), best.relaxed(), best.reachStart(), best.reachEnd(), ponds);
        }
        return best;
    }

    private static boolean spaced(Sites s, SetPieceDef d, double wx, double wz, List<Placement> placed, int relax) {
        double sp = d.spacingM() * (relax > 1 ? 0.5 : 1);
        for (Placement q : placed) {
            double dx = q.x() - wx, dz = q.z() - wz;
            double need = Math.max(sp, 0.5 * (q.footprintX() + q.footprintZ()) + 8);
            if (dx * dx + dz * dz < need * need) return false;
        }
        return true;
    }

    private static SetPieceDef.Variant variantFor(SetPieceDef d, Cand c, int seed, int instance) {
        double total = 0;
        for (SetPieceDef.Variant v : d.variants()) if (v.sites().isEmpty() || v.sites().contains(c.site())) total += v.weight();
        if (total <= 0) return null;
        int hsh = Noise.hash(seed ^ (instance * 0x632BE5AB), c.cell(), d.priority());
        double u = (hsh >>> 8) / 16777216.0 * total;
        for (SetPieceDef.Variant v : d.variants()) {
            if (!(v.sites().isEmpty() || v.sites().contains(c.site()))) continue;
            u -= v.weight();
            if (u < 0) return v;
        }
        return d.variants().get(d.variants().size() - 1);
    }

    /** Bearing the door should face, in degrees (0 north, 90 east). */
    private static double facing(Sites s, SetPieceDef d, Cand c, double wx, double wz, Placement view) {
        double dx, dz;
        if (d.rotation().equals("face_landmark") && view != null) {
            dx = view.x() - wx;
            dz = view.z() - wz;
        } else if (c.coarse()) {
            int i = c.cell() % s.cn, k = c.cell() / s.cn;
            float[] ch = s.land.coarse();
            int l = Math.max(0, i - 1), r = Math.min(s.cn - 1, i + 1), u = Math.max(0, k - 1), dd = Math.min(s.cn - 1, k + 1);
            dx = -(ch[k * s.cn + r] - ch[k * s.cn + l]);
            dz = -(ch[dd * s.cn + i] - ch[u * s.cn + i]);
        } else if (d.rotation().equals("face_water") || d.rotation().equals("face_landmark")) {
            dx = -s.g.gradX(s.dWater, c.cell());
            dz = -s.g.gradZ(s.dWater, c.cell());
        } else {
            dx = -s.g.gradX(s.ground, c.cell());
            dz = -s.g.gradZ(s.ground, c.cell());
        }
        if (dx == 0 && dz == 0) dz = 1;
        return CoreGrid.bearing(dx, dz);
    }

    /** Template rotation turning its south-facing door to the bearing: none = south, cw90 = west, cw180 = north, ccw90 = east. */
    static int rotationFor(double bearing) {
        int q = (int) Math.floor(((bearing + 45) % 360) / 90.0); // 0 north, 1 east, 2 south, 3 west
        return switch (q) {
            case 0 -> 2;
            case 1 -> 3;
            case 2 -> 0;
            default -> 1;
        };
    }

    /** Footprint check on the core: all cells dry, inside, unmasked, none over the slope cap. Returns {mean slope°, floor}. */
    private static double[] footprint(Sites s, int centre, int fx, int fz, byte[] mask, double capDeg) {
        CoreGrid g = s.g;
        double cap = CoreGrid.tanDeg(capDeg);
        double lx0 = g.lx(centre) - fx / 2.0, lz0 = g.lz(centre) - fz / 2.0;
        int i0 = (int) Math.floor(lx0 / g.cell), i1 = (int) Math.floor((lx0 + fx - 0.01) / g.cell);
        int k0 = (int) Math.floor(lz0 / g.cell), k1 = (int) Math.floor((lz0 + fz - 0.01) / g.cell);
        if (i0 < 2 || k0 < 2 || i1 > g.n - 3 || k1 > g.n - 3) return null;
        double sum = 0;
        int cnt = 0;
        float[] hs = new float[(i1 - i0 + 1) * (k1 - k0 + 1)];
        for (int k = k0; k <= k1; k++) for (int i = i0; i <= i1; i++) {
            int idx = g.idx(i, k);
            if (s.w.type()[idx] != WaterStage.DRY || mask[idx] != 0 || s.slopeTan[idx] > cap) return null;
            sum += CoreGrid.deg(s.slopeTan[idx]);
            hs[cnt++] = s.ground[idx];
        }
        Arrays.sort(hs, 0, cnt);
        return new double[] {sum / cnt, hs[cnt / 2]};
    }

    static void stamp(Sites s, Placement q, int id, byte[] mask) {
        if (q.coarse()) return;
        CoreGrid g = s.g;
        double lx0 = q.x() - g.originX - q.footprintX() / 2.0, lz0 = q.z() - g.originZ - q.footprintZ() / 2.0;
        int i0 = (int) Math.floor(lx0 / g.cell), i1 = (int) Math.floor((lx0 + q.footprintX() - 0.01) / g.cell);
        int k0 = (int) Math.floor(lz0 / g.cell), k1 = (int) Math.floor((lz0 + q.footprintZ() - 0.01) / g.cell);
        for (int k = Math.max(0, k0); k <= Math.min(g.n - 1, k1); k++)
            for (int i = Math.max(0, i0); i <= Math.min(g.n - 1, i1); i++) mask[g.idx(i, k)] = (byte) id;
    }

    private static double score(Sites s, SetPieceDef d, Cand c, double wx, double wz, double meanSlope, Placement view,
                                int jitterSeed, double jitter) {
        SetPieceDef.Prefer pr = d.prefer();
        double u = (Noise.hash(jitterSeed, c.cell() + (c.coarse() ? 0x40000000 : 0), d.priority()) >>> 8) / 16777216.0;
        double sc = 1 + jitter * u;
        double elev = c.coarse() ? s.land.coarse()[c.cell()] : s.ground[c.cell()];
        sc += pr.highElev() * Noise.clamp01((elev - s.minElev) / Math.max(1, s.maxElev - s.minElev));
        sc += pr.lowSlope() * (1 - Noise.clamp01(meanSlope / Math.max(1, d.fit().maxSlopeDeg())));
        if (!c.coarse()) {
            sc += pr.nearMainRiver() * (1 - Noise.clamp01(s.dMain[c.cell()] / 300.0));
            sc += pr.nearWater() * (1 - Noise.clamp01(s.dWater[c.cell()] / 200.0));
            if (!Float.isNaN(s.mainLevel[c.cell()])) sc += pr.aboveRiver() * Noise.clamp01((s.ground[c.cell()] - s.mainLevel[c.cell()]) / 4.0);
            int f = s.faceAt[c.cell()];
            if (f >= 0) sc += pr.faceHeight() * Noise.clamp01(s.w.faces().get(f).heightM() / 40.0);
        } else {
            sc += pr.nearWater() * (1 - Noise.clamp01(s.cRiverDist[c.cell()] / 300.0));
        }
        if (view != null && pr.viewWeight() > 0) {
            double top = view.y() + 14;
            if (s.field.clear(wx, wz, 3.0, view.x(), view.z(), top, 8, 16, 0)) sc += pr.viewWeight();
        }
        return sc;
    }

    // ------------------------------------------------------------------------------------- candidates

    private static List<Cand> coreCandidates(Sites s, SetPieceDef d, int relax) {
        CoreGrid g = s.g;
        SetPieceDef.Fit f = d.fit();
        List<Cand> out = new ArrayList<>();
        double loose = relax > 0 ? 1.5 : 1, tight = relax > 0 ? 0.5 : 1;
        if (f.sites().contains("beaver_reach")) return beaverCandidates(s, d, relax);
        int step = Math.max(1, (int) Math.round(8 / g.cell));
        double slopeCap = CoreGrid.tanDeg(Math.min(25, f.maxSlopeDeg() * loose));
        for (int k = 16; k < g.n - 16; k += step) {
            for (int i = 16; i < g.n - 16; i += step) {
                int idx = g.idx(i, k);
                if (s.w.type()[idx] != WaterStage.DRY || s.slopeTan[idx] > slopeCap) continue;
                double e = s.ground[idx];
                if (e < f.minElevM() * tight || e > f.maxElevM()) continue;
                double dw = s.dWater[idx], dm = s.dMain[idx];
                if (dw < f.minWaterDistM() * tight || dw > f.maxWaterDistM() * loose) continue;
                if (dm < f.minMainRiverDistM() * tight || dm > f.maxMainRiverDistM() * loose) continue;
                String site = relax > 1 ? (f.sites().isEmpty() ? "any" : f.sites().get(0)) : siteOf(s, d, idx, relax);
                if (site == null) continue;
                out.add(new Cand(idx, false, site, -1, -1));
            }
        }
        return out;
    }

    /** The first of the fit's site kinds this cell is, or null. */
    private static String siteOf(Sites s, SetPieceDef d, int idx, int relax) {
        SetPieceDef.Fit f = d.fit();
        double tight = relax > 0 ? 0.6 : 1;
        for (String site : f.sites()) {
            boolean ok = switch (site) {
                case "riverbank" -> s.w.bank()[idx] == WaterStage.NO_BANK && !Float.isNaN(s.mainLevel[idx])
                        && s.ground[idx] - s.mainLevel[idx] >= f.aboveRiverM() * tight;
                case "gravel_bar" -> s.w.bank()[idx] == WaterStage.BAR;
                case "lake_shore" -> s.dLake[idx] >= 8 && s.dLake[idx] <= 20;
                case "ridge_clearing" -> s.ground[idx] >= s.localMax60[idx] - 2 && s.dWater[idx] > 40;
                case "ridge_top" -> s.ground[idx] >= s.localMax100[idx] - (relax > 0 ? 6 : 3);
                case "creek_bench" -> s.dCreek[idx] >= 15 && s.dCreek[idx] <= 80;
                case "creek_reach" -> s.dCreek[idx] >= 8 && s.dCreek[idx] <= 30;
                case "face_foot" -> faceOk(s, d, idx, tight);
                default -> false;
            };
            if (ok) return site;
        }
        return null;
    }

    private static boolean faceOk(Sites s, SetPieceDef d, int idx, double tight) {
        int f = s.faceAt[idx];
        if (f < 0) return false;
        WaterStage.Face face = s.w.faces().get(f);
        if (face.heightM() < d.fit().faceMinHeightM() * tight) return false;
        return aspectOk(d.fit().faceAspect(), face.aspectDeg());
    }

    static boolean aspectOk(String want, double aspect) {
        return switch (want) {
            case "north" -> aspect >= 300 || aspect <= 60;
            case "south" -> aspect >= 120 && aspect <= 240;
            default -> true;
        };
    }

    /** Beaver valleys: a creek reach of 400–1,200 m at 1–3%; the ruin stands on the bank of its lowest pond. */
    private static List<Cand> beaverCandidates(Sites s, SetPieceDef d, int relax) {
        CoreGrid g = s.g;
        SetPieceDef.Fit f = d.fit();
        double gLo = f.creekGradientPct().min() / 100 * (relax > 0 ? 0.5 : 1);
        double gHi = f.creekGradientPct().max() / 100 * (relax > 0 ? 1.5 : 1);
        double lenLo = f.creekReachM().min() * (relax > 0 ? 0.6 : 1), lenHi = f.creekReachM().max();
        List<Cand> out = new ArrayList<>();
        for (WaterStage.Creek c : s.w.creeks()) {
            int[] cs = c.cells();
            int m = cs.length;
            if (m < 20) continue;
            double[] sd = new double[m];
            for (int k = 1; k < m; k++) sd[k] = sd[k - 1] + Rivers.stepLen(g, cs[k - 1], cs[k]);
            if (sd[m - 1] < lenLo) continue;
            int bestA = -1, bestB = -1;
            double bestLen = 0;
            for (int a = 0; a < m; a += 5) {
                int b = a;
                while (b < m - 1 && sd[b] - sd[a] < lenLo) b++;
                if (sd[b] - sd[a] < lenLo) break;
                int lastOk = -1;
                for (; b < m && sd[b] - sd[a] <= lenHi; b++) {
                    double grad = (s.w.surface()[cs[a]] - s.w.surface()[cs[b]]) / (sd[b] - sd[a]);
                    if (grad >= gLo && grad <= gHi) lastOk = b;
                }
                if (lastOk >= 0 && sd[lastOk] - sd[a] > bestLen) {
                    bestLen = sd[lastOk] - sd[a];
                    bestA = a;
                    bestB = lastOk;
                }
            }
            if (bestA < 0) continue;
            // The ruin: dry bank cells 4–16 m from the lower third of the reach.
            int from = bestA + (bestB - bestA) * 2 / 3;
            for (int k = from; k <= bestB; k += 3) {
                int ci = g.col(cs[k]), ck = g.row(cs[k]);
                for (int dz = -8; dz <= 8; dz += 2) for (int dx = -8; dx <= 8; dx += 2) {
                    if (!g.in(ci + dx, ck + dz)) continue;
                    int idx = g.idx(ci + dx, ck + dz);
                    if (s.w.type()[idx] != WaterStage.DRY || s.dCreek[idx] < 4 || s.dCreek[idx] > 16) continue;
                    if (s.dMain[idx] < f.minMainRiverDistM() * (relax > 0 ? 0.5 : 1)) continue;
                    out.add(new Cand(idx, false, "beaver_reach", cs[bestA], cs[bestB]));
                }
            }
        }
        return out;
    }

    private static List<Cand> coarseCandidates(Sites s, SetPieceDef d, String area, int relax) {
        List<Cand> out = new ArrayList<>();
        SetPieceDef.Fit f = d.fit();
        float[] ch = s.land.coarse();
        int cn = s.cn;
        double tight = relax > 0 ? 0.6 : 1;
        for (int c = 0; c < cn * cn; c++) {
            int i = c % cn, k = c / cn;
            if (i < 2 || k < 2 || i > cn - 3 || k > cn - 3) continue;
            double wx = s.cWorldX(c), wz = s.cWorldZ(c);
            int region = s.field.region(wx, wz);
            if (region == SeedStage.CORE_REGION || s.field.inCore(wx, wz)) continue;
            if (area.startsWith("role:")) {
                String role = area.substring(5).toUpperCase(java.util.Locale.ROOT);
                if (!s.seed.regionRoles().get(region).name().equals(role)) continue;
            } else if (!area.equals("outside_core")) {
                continue;
            }
            double e = ch[c];
            if (e < f.minElevM() * tight || e > f.maxElevM()) continue;
            String site = null;
            for (String want : f.sites()) {
                boolean ok = switch (want) {
                    case "creek_reach", "beaver_reach" -> s.cRiverDist[c] > 0 && s.cRiverDist[c] <= 64 * (relax > 0 ? 2 : 1);
                    case "slope_break" -> CoreGrid.deg(s.cSlope[c]) < 8 && maxSlopeNear(s, c, 2) >= 20 * tight
                            && s.cRiverDist[c] <= 200 * (relax > 0 ? 2 : 1);
                    case "ridge_top" -> e >= s.cLocalMax[c] - 1;
                    case "face_foot" -> CoreGrid.deg(s.cSlope[c]) < 20 && northFaceNear(s, ch, c, tight);
                    default -> false;
                };
                if (ok) { site = want; break; }
            }
            if (site == null && relax > 1) site = f.sites().get(0);
            if (site != null) out.add(new Cand(c, true, site, -1, -1));
        }
        return out;
    }

    private static double maxSlopeNear(Sites s, int c, int r) {
        int cn = s.cn, ci = c % cn, ck = c / cn;
        double best = 0;
        for (int dz = -r; dz <= r; dz++) for (int dx = -r; dx <= r; dx++) {
            int x = ci + dx, z = ck + dz;
            if (x < 0 || z < 0 || x >= cn || z >= cn) continue;
            best = Math.max(best, CoreGrid.deg(s.cSlope[z * cn + x]));
        }
        return best;
    }

    /** A steep (over 30°) north-facing cell just south of this one, at least 10 m above it. */
    private static boolean northFaceNear(Sites s, float[] ch, int c, double tight) {
        int cn = s.cn, ci = c % cn, ck = c / cn;
        for (int dz = 1; dz <= 2; dz++) {
            int z = ck + dz;
            if (z >= cn) break;
            int nb = z * cn + ci;
            if (CoreGrid.deg(s.cSlope[nb]) >= 30 * tight && ch[nb] - ch[c] >= 10 * tight) return true;
        }
        return false;
    }
}

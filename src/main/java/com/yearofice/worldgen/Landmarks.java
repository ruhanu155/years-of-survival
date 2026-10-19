package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 5, landmarks (Blueprint › Landmarks; Minecraft engine › Landmarks from 2 km). Each of the 16 home
 * valley regions holds at least one landmark that passes the 2 km test: from 8 points on a 2 km circle at
 * 1.7 m eye height, the landmark's top 3 m clears the ground from at least 3. Set-piece landmarks are tried
 * first; a region without one gets a natural landmark: a white cascade (a fall on a rock face over 20 m), a
 * snowfield on a peak over 850 m, or a granite tor of 10–25 m on its best-seen ridge top. Burn ridges wait for
 * the fire history (M2). Sight lines read the core's carved 2 m ground and the 32 m coarse field elsewhere;
 * points beyond the home valley's edge stand in the Interior, on the border's height carried out (the Interior
 * plan replaces that at M10). {@code canopy_m} is 0 until cover exists (M2).
 */
public final class Landmarks {
    private Landmarks() {}

    public record Landmark(int region, String kind, String source, double x, double z, double baseM, double heightM,
                           int visiblePoints, int pointsInWorld, String silhouette) {
        public boolean passes(int minPoints) { return visiblePoints >= minPoints; }
    }

    private static final double R2 = 0.7071067811865476;
    private static final double[] PX = {1, R2, 0, -R2, -1, -R2, 0, R2};
    private static final double[] PZ = {0, R2, 1, R2, 0, -R2, -1, -R2};

    /**
     * How many of the circle's points see the top 3 m of something at (x, z) standing {@code height} over
     * {@code base}: {seen, points inside the home valley}. Each point's viewer stands on the highest ground within
     * {@code viewer_search_m} of it, as a player looking for the landmark steps onto the nearest rise. Stops early
     * once {@code beat} can no longer be exceeded (pass −1 to count every point).
     */
    static int[] visibility(HeightField f, HydroParams.LandmarkParams lp, double x, double z, double base, double height,
                            int beat) {
        int seen = 0, inWorld = 0, left = 0;
        double target = base + height - lp.topM();
        double[][] pts = new double[8][];
        for (int k = 0; k < lp.points() && k < 8; k++) {
            double px = x + lp.testRadiusM() * PX[k], pz = z + lp.testRadiusM() * PZ[k];
            pts[k] = new double[] {px, pz};
            left++;
            if (f.inWorld(px, pz)) inWorld++;
        }
        double r = lp.viewerSearchM(), st = r / 2;
        for (int k = 0; k < 8; k++) {
            if (pts[k] == null) continue;
            if (beat >= 0 && seen + left <= beat) break;
            left--;
            double bx = pts[k][0], bz = pts[k][1], bh = -Double.MAX_VALUE;
            for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
                if (dx * dx + dz * dz > 4) continue;
                double vx = pts[k][0] + dx * st, vz = pts[k][1] + dz * st;
                double hh = f.at(vx, vz);
                if (hh > bh) { bh = hh; bx = vx; bz = vz; }
            }
            if (f.clear(bx, bz, lp.eyeM(), x, z, target, lp.rayStepM(), lp.nearSkipM(), lp.canopyM())) seen++;
        }
        return new int[] {seen, inWorld};
    }

    static int[] visibility(HeightField f, HydroParams.LandmarkParams lp, double x, double z, double base, double height) {
        return visibility(f, lp, x, z, base, height, -1);
    }

    static List<Landmark> run(SeedStage.SeedParams seed, HeightField f, WorldgenParams p, List<SetPieceDef> defs,
                              List<SetPieces.Placement> placements, WaterStage.Layers w, CoreGrid g, float[] ground) {
        HydroParams.LandmarkParams lp = p.hydro().landmarks();
        Mulberry32 rng = SeedStage.rng(seed.attemptSeed(), PlacesStage.STAGE + ":landmarks");
        double rs = p.grid().worldSizeM() / 4.0, half = p.grid().worldSizeM() / 2.0;
        List<Landmark> out = new ArrayList<>();
        for (int region = 0; region < 16; region++) {
            double torH = lp.torHeightM().draw(rng);
            double x0 = -half + (region % 4) * rs, z0 = -half + (region / 4) * rs;
            Landmark best = null;
            // Set pieces first.
            for (SetPieces.Placement q : placements) {
                if (q.region() != region) continue;
                SetPieceDef d = def(defs, q.id());
                if (d == null || d.landmark() == null || !d.landmark().counts(q.variant())) continue;
                int[] v = visibility(f, lp, q.x(), q.z(), q.y(), d.landmark().heightM());
                Landmark l = new Landmark(region, "setpiece", q.id() + "/" + q.variant(), q.x(), q.z(), q.y(),
                        d.landmark().heightM(), v[0], v[1], d.landmark().silhouette());
                if (better(l, best)) best = l;
            }
            if (best != null && best.passes(lp.minPoints())) { out.add(best); continue; }

            List<Landmark> natural = new ArrayList<>();
            // White cascades: falls on rock faces over 20 m.
            for (WaterStage.Fall fall : w.falls()) {
                if (fall.faceHeightM() < lp.cascadeMinFaceM()) continue;
                double fx = g.worldX(fall.foot()), fz = g.worldZ(fall.foot());
                if (f.region(fx, fz) != region) continue;
                double base = ground[fall.foot()];
                double hgt = Math.max(fall.heightM(), fall.faceHeightM());
                int[] v = visibility(f, lp, fx, fz, base, hgt);
                natural.add(new Landmark(region, "white_cascade", "fall", fx, fz, base, hgt, v[0], v[1],
                        "yearofice:silhouette/cascade"));
            }
            // Ridge tops and knobs on a 64 m lattice, highest first: a tor of 10–25 m on the one seen from the
            // most points (search stops at 5). Tor height starts at the seed's draw and rises to 25 m if needed.
            int bestSeen = best == null ? -1 : best.visiblePoints();
            for (Landmark l : natural) bestSeen = Math.max(bestSeen, l.visiblePoints());
            // A coarse pass at twice the step first; the fine lattice only where it found nothing good enough.
            for (double step : new double[] {lp.candidateStepM() * 2, lp.candidateStepM()}) {
                if (bestSeen >= lp.minPoints() + 1) break;
                bestSeen = search(f, lp, region, x0, z0, rs, step, torH, bestSeen, natural);
            }
            Landmark pick = best;
            for (Landmark l : natural) if (better(l, pick)) pick = l;
            if (pick != null) out.add(pick);
        }
        return out;
    }

    /** Searches a region's lattice, highest cells first; adds each improvement to {@code natural}. */
    private static int search(HeightField f, HydroParams.LandmarkParams lp, int region, double x0, double z0, double rs,
                          double step, double torH, int bestSeen, List<Landmark> natural) {
        int cells = (int) (rs / step);
        int nc = cells * cells;
        double[] hs = new double[nc];
        Integer[] idx = new Integer[nc];
        for (int k = 0; k < cells; k++) for (int i = 0; i < cells; i++) {
            hs[k * cells + i] = f.at(x0 + (i + 0.5) * step, z0 + (k + 0.5) * step);
            idx[k * cells + i] = k * cells + i;
        }
        java.util.Arrays.sort(idx, (a, b) -> {
            int c = Double.compare(hs[b], hs[a]);
            return c != 0 ? c : Integer.compare(a, b);
        });
        for (int t = 0; t < nc && bestSeen < 5; t++) {
            int c = idx[t];
            double x = x0 + (c % cells + 0.5) * step, z = z0 + (c / cells + 0.5) * step, base = hs[c];
            int[] v = visibility(f, lp, x, z, base, lp.torHeightM().max(), bestSeen);
            if (v[0] <= bestSeen) continue;
            bestSeen = v[0];
            double hgt = lp.torHeightM().max();
            if (base >= lp.snowfieldMinM()) {
                int[] sv = visibility(f, lp, x, z, base, lp.topM());
                if (sv[0] >= v[0]) {
                    natural.add(new Landmark(region, "snowfield", "peak", x, z, base, lp.topM(), sv[0], sv[1],
                            "yearofice:silhouette/snowfield"));
                    continue;
                }
            }
            int[] dv = visibility(f, lp, x, z, base, torH);
            if (dv[0] >= v[0]) { hgt = torH; v = dv; }
            natural.add(new Landmark(region, "granite_tor", "ridge", x, z, base, hgt, v[0], v[1], "yearofice:silhouette/tor"));
        }
        return bestSeen;
    }

    /** More points seen wins; then a set piece over nature; then the shorter (more natural) landmark. */
    private static boolean better(Landmark a, Landmark b) {
        if (b == null) return true;
        if (a.visiblePoints() != b.visiblePoints()) return a.visiblePoints() > b.visiblePoints();
        return false;
    }

    static SetPieceDef def(List<SetPieceDef> defs, String id) {
        for (SetPieceDef d : defs) if (d.id().equals(id)) return d;
        return null;
    }
}

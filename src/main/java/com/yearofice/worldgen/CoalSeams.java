package com.yearofice.worldgen;

import com.yearofice.core.hash.Mulberry32;
import java.util.ArrayList;
import java.util.List;

/**
 * Coal seams on the rock shell (Every concept from the video › Coal; GameTest {@code worldgen/coal_seams}):
 * 3–6 a world, at least one 1.5–3 km from the start and none within 600 m, each a band of {@code coal_seam}
 * blocks across a few cells of a rock face. Only face cells (the shell) are picked, so no seam lies below the
 * surface. Faces at least {@code min_face_height_m} tall, nearest the middle of the ring first, so the far seam is
 * a walk worth making.
 */
public final class CoalSeams {
    public static final String STAGE = "coal";

    /** A coarse seam's bluff: radius and how far it stands above the ground. */
    static final double BLUFF_RADIUS_M = 5, BLUFF_RISE_M = 3;

    private CoalSeams() {}

    /**
     * One seam: its cells (rock cells of one face), the band's block y, and its centre. A seam beyond the core
     * ({@code coarse}) has no cells: it is a small rock bluff of {@code radiusM} raised on the coarse ground, with
     * the band in its side, until that region's own layers and faces are built.
     */
    public record Seam(int[] cells, int bandY, double x, double z, double fromStartM, boolean coarse, double radiusM) {}

    static List<Seam> run(ValleyWorld vw, double startX, double startZ, List<WaterStage.Face> faces) {
        TerrainParams.Coal cp = vw.p.terrain().coal();
        CoreGrid g = vw.g;
        Mulberry32 rng = SeedStage.rng(vw.seed.attemptSeed(), STAGE);
        int want = cp.count().drawInt(rng);
        double ringMid = (cp.ringM().min() + cp.ringM().max()) / 2;

        // Candidate faces: tall enough, not iron, far enough from the start.
        record Cand(WaterStage.Face f, double d, double key) {}
        List<Cand> cands = new ArrayList<>();
        for (WaterStage.Face f : faces) {
            if (f.heightM() < cp.minFaceHeightM()) continue;
            int c = f.foot();
            if (vw.node[c] != 0 || vw.node[f.top()] != 0) continue;
            double dx = g.worldX(c) - startX, dz = g.worldZ(c) - startZ;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < cp.minStartM()) continue;
            double u = rng.nextDouble();
            boolean inRing = cp.ringM().contains(d);
            double key = (inRing ? 0 : 1e6) + Math.abs(d - ringMid) * (0.75 + 0.5 * u);
            cands.add(new Cand(f, d, key));
        }
        cands.sort((a, b) -> Double.compare(a.key(), b.key()));

        List<Seam> out = new ArrayList<>();
        for (Cand c : cands) {
            if (out.size() >= want) break;
            boolean spaced = true;
            for (Seam s : out) {
                double dx = s.x() - g.worldX(c.f().foot()), dz = s.z() - g.worldZ(c.f().foot());
                if (dx * dx + dz * dz < cp.spacingM() * cp.spacingM()) { spaced = false; break; }
            }
            if (!spaced) continue;
            // The seam: rock cells of the face around its foot, walking up the face.
            int cells = cp.cells().drawInt(rng);
            List<Integer> pick = new ArrayList<>();
            int at = c.f().foot();
            for (int k = 0; k < cells * 4 && pick.size() < cells; k++) {
                if (vw.rock[at] == WaterStage.FACE_ROCK && vw.node[at] == 0 && !pick.contains(at)) pick.add(at);
                int best = -1;
                double bh = -1e9;
                for (int m = 0; m < 8; m++) {
                    int xx = g.col(at) + CoreGrid.DX[m], zz = g.row(at) + CoreGrid.DZ[m];
                    if (!g.in(xx, zz)) continue;
                    int nb = g.idx(xx, zz);
                    if (vw.rock[nb] != WaterStage.FACE_ROCK || pick.contains(nb)) continue;
                    double hgt = vw.ground[nb] + 0.01 * ((Noise.hash(vw.seed.attemptSeed(), nb, k) >>> 8) / 16777216.0);
                    // Along the face more than up it: prefer neighbours at a similar height.
                    double score = -Math.abs(hgt - vw.ground[at]);
                    if (score > bh) { bh = score; best = nb; }
                }
                if (best < 0) break;
                at = best;
            }
            if (pick.size() < (int) cp.cells().min()) continue;
            int[] arr = new int[pick.size()];
            double lo = Double.MAX_VALUE;
            for (int i = 0; i < arr.length; i++) { arr[i] = pick.get(i); lo = Math.min(lo, vw.ground[arr[i]]); }
            int band = (int) Math.floor(lo + 0.5);
            out.add(new Seam(arr, band, g.worldX(c.f().foot()), g.worldZ(c.f().foot()), Math.round(c.d() * 10) / 10.0, false, 0));
        }
        // The far seam: when no core face lies in the ring, a bluff on the steepest coarse ground in it.
        boolean far = false;
        for (Seam s : out) if (cp.ringM().contains(s.fromStartM())) far = true;
        if (!far) {
            Seam b = bluff(vw, startX, startZ, cp, out);
            if (b != null) {
                if (out.size() >= (int) cp.count().max()) out.remove(out.size() - 1);
                out.add(b);
            }
        }
        return List.copyOf(out);
    }

    private static Seam bluff(ValleyWorld vw, double sx, double sz, TerrainParams.Coal cp, List<Seam> placed) {
        double step = vw.p.grid().coarseCellM(), half = vw.p.grid().worldSizeM() / 2.0 - 2 * step;
        double best = -1;
        double bx = 0, bz = 0, bd = 0;
        for (double z = -half; z <= half; z += step) {
            for (double x = -half; x <= half; x += step) {
                if (vw.inCore(x, z)) continue;
                double dx = x - sx, dz = z - sz, d = Math.sqrt(dx * dx + dz * dz);
                if (!cp.ringM().contains(d)) continue;
                boolean spaced = true;
                for (Seam s : placed) {
                    double ex = s.x() - x, ez = s.z() - z;
                    if (ex * ex + ez * ez < cp.spacingM() * cp.spacingM()) spaced = false;
                }
                if (!spaced) continue;
                double gx = vw.coarseHeight(x + step, z) - vw.coarseHeight(x - step, z);
                double gz = vw.coarseHeight(x, z + step) - vw.coarseHeight(x, z - step);
                double key = Math.sqrt(gx * gx + gz * gz) + 1e-3 * ((Noise.hash(vw.seed.attemptSeed(), (int) x, (int) z) >>> 8) / 16777216.0);
                if (key > best) { best = key; bx = x; bz = z; bd = d; }
            }
        }
        if (best < 0) return null;
        int band = (int) Math.floor(vw.coarseHeight(bx, bz) + 0.5) + 1;
        return new Seam(new int[0], band, bx, bz, Math.round(bd * 10) / 10.0, true, BLUFF_RADIUS_M);
    }
}

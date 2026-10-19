package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 4, step 6: rock exposure (Terrain and hydrology step 6; Minecraft engine › Rock faces; Explore, don't
 * dig).
 *
 * <p>Cells over 40° on rock harder than 0.6 are rock: in the chunk at M2 their top and open sides become
 * {@code yearofice:face_rock} and everything behind {@code deep_rock}. Faces are the rock in each 64 m tile,
 * split into connected pieces; a piece taller than 6 m is a shelter candidate, and the top 20 by height and
 * aspect (south-facing first, warm in winter) are flagged.
 *
 * <p>Iron: 12–18 clusters of {@code yearofice:iron_outcrop} nodes on face rock above 250 m, spaced 100 m or
 * more, each with a rust stain 2–6 m down the scree below. Only shell cells are ever chosen, so no node can
 * lie behind the surface. Stage 6 (M2) places the blocks from this list.
 */
final class RockFaces {
    private RockFaces() {}

    static void run(Hydro h) {
        HydroParams.Rock rp = h.hp.rock();
        CoreGrid g = h.grid;
        int n = h.n, total = h.total;
        float[] ground = h.ground();
        double tan = CoreGrid.tanDeg(rp.minSlopeDeg());
        boolean[] rock = new boolean[total];
        for (int i = 0; i < total; i++) {
            if (h.type[i] != Hydro.DRY) continue;
            if (h.land.hardnessAt(i) > rp.minHardness() && g.slope(ground, i) > tan) {
                rock[i] = true;
                h.rock[i] = Hydro.FACE_ROCK;
            }
        }

        // Faces: connected rock within each tile.
        int tile = (int) (rp.tileM() / g.cell);
        boolean[] done = new boolean[total];
        int[] queue = new int[tile * tile];
        List<WaterStage.Face> faces = new ArrayList<>();
        for (int ty = 0; ty < n; ty += tile) {
            for (int tx = 0; tx < n; tx += tile) {
                for (int k = ty; k < Math.min(n, ty + tile); k++) {
                    for (int i = tx; i < Math.min(n, tx + tile); i++) {
                        int s = k * n + i;
                        if (!rock[s] || done[s]) continue;
                        int qh = 0, qt = 0;
                        queue[qt++] = s;
                        done[s] = true;
                        while (qh < qt) {
                            int c = queue[qh++];
                            int ci = c % n, ck = c / n;
                            for (int m = 0; m < 8; m++) {
                                int x = ci + CoreGrid.DX[m], z = ck + CoreGrid.DZ[m];
                                if (x < tx || z < ty || x >= Math.min(n, tx + tile) || z >= Math.min(n, ty + tile)) continue;
                                int nb = z * n + x;
                                if (!rock[nb] || done[nb]) continue;
                                done[nb] = true;
                                queue[qt++] = nb;
                            }
                        }
                        if (qt < 3) continue;
                        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE, gx = 0, gz = 0;
                        int foot = s, top = s;
                        for (int t = 0; t < qt; t++) {
                            int c = queue[t];
                            if (ground[c] < lo) { lo = ground[c]; foot = c; }
                            if (ground[c] > hi) { hi = ground[c]; top = c; }
                            gx += g.gradX(ground, c);
                            gz += g.gradZ(ground, c);
                        }
                        // Aspect: the way the face looks, down its slope.
                        double aspect = CoreGrid.bearing(-gx, -gz);
                        faces.add(new WaterStage.Face(foot, top, hi - lo, Math.round(aspect * 10) / 10.0, qt, false, 0));
                    }
                }
            }
        }
        // Shelter candidates, ranked by height times an aspect factor (south 1.0, north 0.7).
        List<Integer> shelters = new ArrayList<>();
        for (int i = 0; i < faces.size(); i++) if (faces.get(i).heightM() > rp.shelterMinHeightM()) shelters.add(i);
        shelters.sort((a, b) -> {
            int c = Double.compare(score(faces.get(b)), score(faces.get(a)));
            return c != 0 ? c : Integer.compare(faces.get(a).foot(), faces.get(b).foot());
        });
        for (int r = 0; r < shelters.size(); r++) {
            WaterStage.Face f = faces.get(shelters.get(r));
            faces.set(shelters.get(r), new WaterStage.Face(f.foot(), f.top(), f.heightM(), f.aspectDeg(), f.cells(), true,
                    r < rp.shelterFlagged() ? r + 1 : 0));
        }
        h.faces.addAll(faces);

        // Iron clusters: seeded order over shell cells above 250 m, spaced.
        List<Integer> cand = new ArrayList<>();
        for (int i = 0; i < total; i++) if (rock[i] && ground[i] > rp.ironMinElevM()) cand.add(i);
        int seed = SeedStage.stageSeed(h.seed.attemptSeed(), "water:iron");
        cand.sort((a, b) -> {
            int ka = Noise.hash(seed, a, 17), kb = Noise.hash(seed, b, 17);
            return ka != kb ? Integer.compareUnsigned(ka, kb) : Integer.compare(a, b);
        });
        int want = rp.ironClusters().drawInt(h.rng);
        double spacing = rp.ironSpacingM();
        List<Integer> centres = new ArrayList<>();
        while (centres.size() < want && spacing >= 20) {
            for (int c : cand) {
                if (centres.size() >= want) break;
                boolean far = true;
                for (int q : centres) {
                    double dx = g.lx(q) - g.lx(c), dz = g.lz(q) - g.lz(c);
                    if (dx * dx + dz * dz < spacing * spacing) { far = false; break; }
                }
                if (far) centres.add(c);
            }
            spacing *= 0.7;
        }
        for (int c : centres) {
            int nodes = rp.ironNodes().drawInt(h.rng);
            List<Integer> picked = new ArrayList<>();
            picked.add(c);
            int ci = g.col(c), ck = g.row(c);
            for (int r = 1; r <= 3 && picked.size() < nodes; r++) {
                for (int m = 0; m < 8 && picked.size() < nodes; m++) {
                    int x = ci + CoreGrid.DX[m] * r, z = ck + CoreGrid.DZ[m] * r;
                    if (!g.in(x, z)) continue;
                    int idx = g.idx(x, z);
                    if (rock[idx] && !picked.contains(idx)) picked.add(idx);
                }
            }
            int[] arr = new int[picked.size()];
            for (int k = 0; k < arr.length; k++) {
                arr[k] = picked.get(k);
                h.rock[arr[k]] = Hydro.IRON_OUTCROP;
            }
            // Rust stain down the scree below the cluster.
            double run = rp.rustRunM().draw(h.rng);
            int k = c;
            double s = 0;
            while (s < run) {
                int next = -1;
                double best = 0;
                for (int m = 0; m < 8; m++) {
                    int x = g.col(k) + CoreGrid.DX[m], z = g.row(k) + CoreGrid.DZ[m];
                    if (!g.in(x, z)) continue;
                    int nb = g.idx(x, z);
                    double d = (ground[k] - ground[nb]) / CoreGrid.DL[m];
                    if (d > best) { best = d; next = nb; }
                }
                if (next < 0) break;
                s += Rivers.stepLen(g, k, next);
                k = next;
                if (!rock[k] && h.type[k] == Hydro.DRY) h.rock[k] = Hydro.RUST_STAIN;
            }
            h.iron.add(new WaterStage.IronCluster(c, arr, ground[c]));
        }
    }

    private static double score(WaterStage.Face f) {
        double a = f.aspectDeg();
        double southness = 1 - Math.abs(a - 180) / 180; // 1 facing south, 0 facing north
        return f.heightM() * (0.7 + 0.3 * southness);
    }
}

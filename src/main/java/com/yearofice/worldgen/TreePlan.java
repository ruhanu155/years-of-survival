package com.yearofice.worldgen;

import java.util.ArrayList;
import java.util.List;

/**
 * Stage 6's trees, drawn from the cover (Registry table: plants, trees and fungi; the {@code cover} list's
 * {@code trees}, {@code tree_spacing_m} and {@code deadwood_pct}). Each 2 m cell of the world grid holds at most
 * one candidate at a hashed point; it stands with probability (2 / spacing)², spacing drawn between the class's
 * bounds by the patch noise, then hash-priority thinning removes any candidate with a stronger one closer than
 * {@code tree_gap_fraction} × its spacing. Everything is a hash of the cell, so any area's trees are the same
 * whoever asks and in whatever order, and no list needs storing for M2.
 *
 * <p>Deadwood: {@code deadwood_pct} of the stems are a stump, or a fallen log beside one ({@code fallen_share}).
 */
public final class TreePlan {
    private TreePlan() {}

    public enum Kind { LIVE, STUMP, FALLEN }

    /**
     * One tree. {@code x, z} is its base block column; {@code height} in blocks; {@code lean} 0–3 is the
     * direction a drunken black spruce leans on permafrost (−1 upright); {@code seed} drives its shape.
     */
    public record Tree(int x, int z, int species, int height, Kind kind, int lean, int seed) {}

    private record Cand(int bx, int bz, int species, double spacing, int priority, double elev, boolean perma, int h) {}

    /** Trees whose base lies in [x0, x1) × [z0, z1), in a fixed order. */
    public static List<Tree> treesIn(ValleyWorld vw, int x0, int z0, int x1, int z1) {
        Cover.Params cp = vw.p.cover();
        double maxGap = 0;
        for (Cover.ClassDef d : cp.classes()) if (!d.trees().isEmpty()) maxGap = Math.max(maxGap, d.spacingM().max() * cp.treeGapFraction());
        int m = (int) Math.ceil(maxGap / 2) + 1;
        int c0 = Math.floorDiv(x0, 2) - m, c1 = Math.floorDiv(x1 - 1, 2) + m;
        int k0 = Math.floorDiv(z0, 2) - m, k1 = Math.floorDiv(z1 - 1, 2) + m;
        int cw = c1 - c0 + 1, ckN = k1 - k0 + 1;
        Cand[] grid = new Cand[cw * ckN];
        for (int k = 0; k < ckN; k++) for (int i = 0; i < cw; i++) grid[k * cw + i] = candidate(vw, c0 + i, k0 + k);

        List<Tree> out = new ArrayList<>();
        for (int k = 0; k < ckN; k++) {
            for (int i = 0; i < cw; i++) {
                Cand c = grid[k * cw + i];
                if (c == null || c.bx() < x0 || c.bx() >= x1 || c.bz() < z0 || c.bz() >= z1) continue;
                double gap = c.spacing() * cp.treeGapFraction();
                int r = (int) Math.ceil(gap / 2);
                boolean keep = true;
                for (int dz = -r; dz <= r && keep; dz++) for (int dx = -r; dx <= r && keep; dx++) {
                    if (dx == 0 && dz == 0) continue;
                    int ii = i + dx, kk = k + dz;
                    if (ii < 0 || kk < 0 || ii >= cw || kk >= ckN) continue;
                    Cand o = grid[kk * cw + ii];
                    if (o == null) continue;
                    double ddx = o.bx() - c.bx(), ddz = o.bz() - c.bz();
                    if (ddx * ddx + ddz * ddz >= gap * gap) continue;
                    if (Integer.compareUnsigned(o.priority(), c.priority()) > 0) keep = false;
                }
                if (keep) out.add(finish(vw, c));
            }
        }
        return out;
    }

    private static Cand candidate(ValleyWorld vw, int ci, int ck) {
        int h = Noise.hash(vw.treeSeed, ci, ck);
        int bx = ci * 2 + ((h & 0xFF) >= 128 ? 1 : 0), bz = ck * 2 + (((h >>> 8) & 0xFF) >= 128 ? 1 : 0);
        int cls = vw.classAt(bx, bz);
        Cover.ClassDef d = vw.classDef(cls);
        if (d.trees().isEmpty()) return null;
        Cover.Params cp = vw.p.cover();
        double patch = Cover.patch(cp, vw.cover.patchSeed(), bx + 0.5, bz + 0.5);
        double spacing = d.spacingM().min() + (d.spacingM().max() - d.spacingM().min()) * patch;
        double prob = cp.treeFill() * 4.0 / (spacing * spacing);
        double u = ((h >>> 16) & 0xFFFF) / 65536.0;
        if (u >= prob) return null;
        // Ground: dry, not rock, not a pad, not too steep, not at the water's edge.
        int cell = vw.cellOf(bx, bz);
        boolean perma;
        if (cell >= 0) {
            if (vw.wType[cell] != WaterStage.DRY || (vw.rock[cell] != 0 && vw.rock[cell] != WaterStage.RUST_STAIN)) return null;
            if (vw.cover.waterDist()[cell] < cp.treeWaterGapM()) return null;
            perma = vw.cover.permafrost(cell);
        } else {
            perma = d.permafrost() == Cover.Permafrost.YES;
            if (vw.inBluff(bx + 0.5, bz + 0.5)) return null;
        }
        if (onPad(vw, bx, bz)) return null;
        double e = vw.groundAt(bx + 0.5, bz + 0.5);
        double sx = vw.groundAt(bx + 1.5, bz + 0.5) - vw.groundAt(bx - 0.5, bz + 0.5);
        double sz = vw.groundAt(bx + 0.5, bz + 1.5) - vw.groundAt(bx + 0.5, bz - 0.5);
        double slope = CoreGrid.deg(Math.sqrt(sx * sx + sz * sz) / 2);
        if (slope > cp.maxTreeSlopeDeg()) return null;
        int h2 = Noise.hash(vw.treeSeed ^ 0x5bd1e995, ci, ck);
        double tw = 0;
        for (Cover.TreeShare s : d.trees()) tw += s.weight();
        double pick = ((h2 >>> 8) / 16777216.0) * tw;
        int species = d.trees().get(d.trees().size() - 1).species();
        for (Cover.TreeShare s : d.trees()) {
            pick -= s.weight();
            if (pick < 0) { species = s.species(); break; }
        }
        return new Cand(bx, bz, species, spacing, Noise.hash(vw.treeSeed ^ 0x2545F491, ci, ck), e, perma, h2);
    }

    private static boolean onPad(ValleyWorld vw, int x, int z) {
        double skirt = vw.p.terrain().footprintSkirtM() + 1;
        for (SetPieces.Placement q : vw.placements) {
            int[] r = ValleyWorld.rect(q);
            if (x >= r[0] - skirt && z >= r[1] - skirt && x < r[0] + r[2] + skirt && z < r[1] + r[3] + skirt) return true;
        }
        return false;
    }

    private static Tree finish(ValleyWorld vw, Cand c) {
        Cover.Params cp = vw.p.cover();
        Cover.Species sp = cp.species().get(c.species());
        int h3 = Noise.hash(vw.treeSeed ^ 0x68E31DA4, c.bx(), c.bz());
        double u = (h3 >>> 8) / 16777216.0;
        double height = sp.heightBlocks().min() + (sp.heightBlocks().max() - sp.heightBlocks().min()) * u;
        double nearLine = Noise.clamp01((c.elev() - (cp.treelineM() - 150)) / 150.0);
        height *= 1 - sp.treelineShrink() * nearLine;
        int hb = Math.max(2, (int) Math.floor(height + 0.5));
        Kind kind = Kind.LIVE;
        int cls = vw.classAt(c.bx(), c.bz());
        double dead = vw.classDef(cls).deadwoodPct() / 100.0;
        int h4 = Noise.hash(vw.treeSeed ^ 0x1B873593, c.bx(), c.bz());
        if (c.species() != Cover.WILLOW && ((h4 & 0xFFFF) / 65536.0) < dead) {
            kind = ((h4 >>> 16) & 0xFF) / 256.0 < cp.fallenShare() ? Kind.FALLEN : Kind.STUMP;
        }
        int lean = -1;
        if (c.species() == Cover.BLACK_SPRUCE && c.perma() && ((h4 >>> 24) & 3) != 0) lean = (h3 & 3);
        return new Tree(c.bx(), c.bz(), c.species(), hb, kind, lean, h3);
    }
}

package com.yearofice.worldgen;

import com.yearofice.core.hash.StageHash;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stage 6, Cover ({@code stageName "cover"}), the first slice of it that M2 needs: for every 2 m core cell the
 * cover class (and so the Minecraft biome), the soil and surface blocks, the permafrost bit and the snow class.
 * The rules are the {@code cover} list of {@code worldgen_params/valley.json}, tested top to bottom; the first
 * match wins (Open world › Biome entry; RECONCILE 145). Beyond the core the same rules run on the 32 m coarse
 * field, with no water, so the 8 km valley has cover everywhere.
 *
 * <p>Trees are not a stored list: {@link TreePlan} draws them from the cover cell by cell with hashed jitter,
 * so any area's trees come out the same whichever chunk asks first. Plants, nodes, micro-climate and the active
 * layer depth join with their owners (M5, M7).
 */
public final class Cover {
    public static final String STAGE = "cover";

    private Cover() {}

    // ------------------------------------------------------------------ parameters

    /** Water types a rule can name, in {@link WaterStage} order. */
    static final List<String> WATER = List.of("dry", "main", "creek", "lake", "oxbow", "pool");
    static final List<String> BANKS = List.of("none", "bar", "cut_bank", "bog", "weed_shelf", "drop_off");
    public static final List<String> SPECIES = List.of("white_spruce", "black_spruce", "birch", "willow");
    public static final int WHITE_SPRUCE = 0, BLACK_SPRUCE = 1, BIRCH = 2, WILLOW = 3;

    /** Permafrost on a class: never, always, or on north-facing ground only. */
    public enum Permafrost { NO, YES, NORTH }

    /** One rule: any null part matches everything. Ranges are inclusive. */
    public record Rule(int waterMask, int bankMask, Boolean rock, GenJson.Range slopeDeg, GenJson.Range elevM,
                       GenJson.Range waterDistM, GenJson.Range aboveWaterM, GenJson.Range sedimentM, GenJson.Range patch,
                       String aspect) {

        boolean matches(Sample s) {
            if ((waterMask & (1 << s.water)) == 0) return false;
            if (bankMask != 0 && (bankMask & (1 << s.bank)) == 0) return false;
            if (rock != null && rock != s.rock) return false;
            if (slopeDeg != null && !slopeDeg.contains(s.slopeDeg)) return false;
            if (elevM != null && !elevM.contains(s.elevM)) return false;
            if (waterDistM != null && !waterDistM.contains(s.waterDistM)) return false;
            if (aboveWaterM != null && !aboveWaterM.contains(s.aboveWaterM)) return false;
            if (sedimentM != null && !sedimentM.contains(s.sedimentM)) return false;
            if (patch != null && !patch.contains(s.patch)) return false;
            if (aspect != null) {
                boolean south = s.slopeDeg >= 3 && s.aspectDeg >= 112.5 && s.aspectDeg <= 247.5;
                boolean north = s.slopeDeg >= 3 && (s.aspectDeg <= 67.5 || s.aspectDeg >= 292.5);
                if (aspect.equals("south") && !south) return false;
                if (aspect.equals("north") && !north) return false;
            }
            return true;
        }
    }

    /** A tree species share inside a class. */
    public record TreeShare(int species, double weight) {}

    /**
     * One cover class. Block names are {@code yearofice:} paths resolved by the chunk generator; {@code bed} is
     * the surface under water.
     */
    public record ClassDef(String id, String biome, Rule rule, String soil, String surface, String bed,
                           List<TreeShare> trees, GenJson.Range spacingM, double deadwoodPct, Permafrost permafrost) {
        public double treeWeight(int species) {
            double t = 0, w = 0;
            for (TreeShare s : trees) { t += s.weight(); if (s.species() == species) w += s.weight(); }
            return t > 0 ? w / t : 0;
        }
    }

    /** Per-species tree sizes, in blocks. */
    public record Species(String id, GenJson.Range heightBlocks, double treelineShrink) {}

    public record Params(double patchScaleM, int patchOctaves, double treelineM, double coarseRockSlopeDeg,
                         double coarseDetailScaleM, double coarseDetailReliefM, double maxTreeSlopeDeg,
                         double treeGapFraction, double treeFill, double fallenShare, double treeWaterGapM, List<Species> species,
                         List<ClassDef> classes) {

        public int index(String id) {
            for (int i = 0; i < classes.size(); i++) if (classes.get(i).id().equals(id)) return i;
            return -1;
        }

        public static Params from(GenJson j) {
            List<Species> sp = new ArrayList<>();
            GenJson t = j.obj("species");
            for (String name : SPECIES) {
                GenJson s = t.obj(name);
                sp.add(new Species(name, s.range("height_blocks"), s.num("treeline_shrink")));
                s.done();
            }
            t.done();
            List<ClassDef> cls = new ArrayList<>();
            for (GenJson c : j.objects("classes")) cls.add(classDef(c));
            if (cls.size() > 255) throw new IllegalArgumentException("cover: over 255 classes");
            GenJson coarse = j.obj("coarse");
            Params p = new Params(j.num("patch_scale_m"), j.integer("patch_octaves"), j.num("treeline_m"),
                    coarse.num("rock_slope_deg"), coarse.num("detail_scale_m"), coarse.num("detail_relief_m"),
                    j.num("max_tree_slope_deg"), j.num("tree_gap_fraction"), j.num("tree_fill"), j.num("fallen_share"),
                    j.num("tree_water_gap_m"), List.copyOf(sp), List.copyOf(cls));
            coarse.done();
            j.done();
            return p;
        }

        private static ClassDef classDef(GenJson c) {
            GenJson r = c.obj("rule");
            int wm = 0;
            for (String w : r.has("water") ? r.strings("water") : List.of("dry")) {
                int k = WATER.indexOf(w);
                if (k < 0) throw new IllegalArgumentException(r.path() + ": unknown water " + w);
                wm |= 1 << k;
            }
            int bm = 0;
            if (r.has("bank")) for (String b : r.strings("bank")) {
                int k = BANKS.indexOf(b);
                if (k < 0) throw new IllegalArgumentException(r.path() + ": unknown bank " + b);
                bm |= 1 << k;
            }
            Boolean rock = r.has("rock") ? r.bool("rock") : null;
            Rule rule = new Rule(wm, bm, rock, opt(r, "slope_deg"), opt(r, "elev_m"), opt(r, "water_dist_m"),
                    opt(r, "above_water_m"), opt(r, "sediment_m"), opt(r, "patch"), r.has("aspect") ? r.str("aspect") : null);
            r.done();
            List<TreeShare> trees = new ArrayList<>();
            for (GenJson tr : c.objects("trees")) {
                int k = SPECIES.indexOf(tr.str("species"));
                if (k < 0) throw new IllegalArgumentException(tr.path() + ": unknown species " + tr.str("species"));
                trees.add(new TreeShare(k, tr.num("pick_weight")));
                tr.done();
            }
            GenJson.Range spacing = trees.isEmpty() ? new GenJson.Range(0, 0) : c.range("tree_spacing_m");
            Permafrost pf = Permafrost.valueOf(c.str("permafrost").toUpperCase(java.util.Locale.ROOT));
            ClassDef d = new ClassDef(c.str("id"), c.str("biome"), rule, c.str("soil"), c.str("surface"), c.str("bed"),
                    List.copyOf(trees), spacing, trees.isEmpty() ? 0 : c.num("deadwood_pct"), pf);
            c.done();
            return d;
        }

        private static GenJson.Range opt(GenJson r, String key) {
            if (!r.has(key)) return null;
            double[] v = r.nums(key);
            if (v.length != 2 || v[0] > v[1]) throw new IllegalArgumentException(r.path() + "." + key + ": need [lo, hi]");
            return new GenJson.Range(v[0], v[1]);
        }
    }

    // ------------------------------------------------------------------ the stage

    /** What a rule reads about one place. */
    static final class Sample {
        int water, bank;
        boolean rock;
        double slopeDeg, aspectDeg, elevM, waterDistM, aboveWaterM, sedimentM, patch;
    }

    /** Flag bits per cell. */
    public static final int PERMAFROST = 1, SNOW_SHIFT = 1, SNOW_MASK = 3 << SNOW_SHIFT;
    /** Snow classes (Snow cover and permafrost blocks): open, under spruce, south slope, lee drift. */
    public static final int SNOW_OPEN = 0, SNOW_SPRUCE = 1, SNOW_SOUTH = 2, SNOW_LEE = 3;

    /**
     * Stage 6's layers over the core: cover class index, flags (permafrost, snow class), the distance to water
     * and the height above that water, which the tree plan reads too.
     */
    public record Layers(int cells, byte[] cover, byte[] flags, float[] waterDist, float[] aboveWater, int patchSeed) {
        public int classAt(int cell) { return cover[cell] & 0xFF; }

        public boolean permafrost(int cell) { return (flags[cell] & PERMAFROST) != 0; }

        public int snowClass(int cell) { return (flags[cell] & SNOW_MASK) >> SNOW_SHIFT; }

        public int hash() { return StageHash.of(Integer.toUnsignedString(StageHash.of(cover)) + ":" + StageHash.of(flags)); }

        /** Cells per class, in class order. */
        public int[] histogram(int classes) {
            int[] h = new int[classes];
            for (byte b : cover) h[b & 0xFF]++;
            return h;
        }
    }

    /** Patch noise in 0–1 at a world position: a slow field that varies spacing and splits look-alike classes. */
    static double patch(Params p, int seed, double wx, double wz) {
        return Noise.clamp01(0.5 + 0.5 * Noise.fbm(seed, wx / p.patchScaleM(), wz / p.patchScaleM(), p.patchOctaves()));
    }

    /** The first class whose rule matches (the last class should match anything). */
    static int classify(Params p, Sample s) {
        List<ClassDef> cls = p.classes();
        for (int i = 0; i < cls.size(); i++) if (cls.get(i).rule().matches(s)) return i;
        return cls.size() - 1;
    }

    static boolean permafrost(ClassDef d, Sample s) {
        return switch (d.permafrost()) {
            case YES -> true;
            case NO -> false;
            case NORTH -> s.slopeDeg >= 3 && (s.aspectDeg <= 67.5 || s.aspectDeg >= 292.5);
        };
    }

    static int snowClass(ClassDef d, Sample s) {
        if (d.treeWeight(WHITE_SPRUCE) + d.treeWeight(BLACK_SPRUCE) >= 0.5) return SNOW_SPRUCE;
        if (s.slopeDeg >= 5 && s.aspectDeg >= 112.5 && s.aspectDeg <= 247.5) return SNOW_SOUTH;
        return SNOW_OPEN;
    }

    public static Layers run(SeedStage.SeedParams seed, Landform.Layers land, Erosion.Layers ero, WaterStage.Layers w,
                             WorldgenParams p) {
        Params cp = p.cover();
        CoreGrid g = new CoreGrid(p, seed.mirror());
        int n = g.n, total = n * n;
        float[] ground = w.ground(ero.height());
        int patchSeed = SeedStage.stageSeed(seed.attemptSeed(), STAGE + ":patch");

        // Distance to water and the water surface there, by a chamfer that carries the source's surface along.
        float[] dist = new float[total];
        float[] surf = new float[total];
        nearestWater(g, w, dist, surf);

        byte[] cover = new byte[total];
        byte[] flags = new byte[total];
        float[] above = new float[total];
        Sample s = new Sample();
        for (int i = 0; i < total; i++) {
            s.water = w.type()[i];
            s.bank = w.bank()[i];
            s.rock = w.type()[i] == WaterStage.DRY && w.rock()[i] != 0 && w.rock()[i] != WaterStage.RUST_STAIN;
            double gx = g.gradX(ground, i), gz = g.gradZ(ground, i);
            double tan = Math.sqrt(gx * gx + gz * gz);
            s.slopeDeg = CoreGrid.deg(tan);
            s.aspectDeg = CoreGrid.bearing(-gx, -gz);
            s.elevM = ground[i];
            s.waterDistM = dist[i];
            s.aboveWaterM = dist[i] == Float.MAX_VALUE ? 1e6 : ground[i] - surf[i];
            s.sedimentM = (ero.sediment()[i] & 0xFF) * 0.1;
            s.patch = patch(cp, patchSeed, g.worldX(i), g.worldZ(i));
            int c = classify(cp, s);
            cover[i] = (byte) c;
            ClassDef d = cp.classes().get(c);
            int f = permafrost(d, s) ? PERMAFROST : 0;
            f |= snowClass(d, s) << SNOW_SHIFT;
            flags[i] = (byte) f;
            above[i] = (float) s.aboveWaterM;
        }
        return new Layers(n, cover, flags, dist, above, patchSeed);
    }

    /** Class of a place beyond the core, on the coarse field: dry, no bank, rock only on the steepest ground. */
    static int classifyCoarse(Params p, Sample s, double elev, double slopeDeg, double aspectDeg, double patch) {
        s.water = 0;
        s.bank = 0;
        s.rock = slopeDeg >= p.coarseRockSlopeDeg();
        s.slopeDeg = slopeDeg;
        s.aspectDeg = aspectDeg;
        s.elevM = elev;
        s.waterDistM = 1e6;
        s.aboveWaterM = 1e6;
        s.sedimentM = 0;
        s.patch = patch;
        return classify(p, s);
    }

    /**
     * Two-pass chamfer distance (5-7-11, as {@link CoreGrid#distance}) from every cell to the nearest wet cell,
     * carrying that cell's water surface with it.
     */
    static void nearestWater(CoreGrid g, WaterStage.Layers w, float[] distOut, float[] surfOut) {
        final int big = Integer.MAX_VALUE / 4;
        int n = g.n, pad = n + 4;
        int[] d = new int[pad * pad];
        float[] sv = new float[pad * pad];
        java.util.Arrays.fill(d, big);
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) {
            int c = k * n + i;
            if (w.type()[c] != WaterStage.DRY) {
                d[(k + 2) * pad + i + 2] = 0;
                sv[(k + 2) * pad + i + 2] = w.surface()[c];
            }
        }
        int[] off = {-pad - 1, -pad, -pad + 1, -1, -pad - 2, -2 * pad - 1, -2 * pad + 1, -pad + 2};
        int[] ow = {7, 5, 7, 5, 11, 11, 11, 11};
        for (int k = 2; k < n + 2; k++) {
            for (int i = 2, idx = k * pad + 2; i < n + 2; i++, idx++) {
                int v = d[idx];
                float s = sv[idx];
                for (int m = 0; m < 8; m++) {
                    int c = d[idx + off[m]] + ow[m];
                    if (c < v) { v = c; s = sv[idx + off[m]]; }
                }
                d[idx] = v;
                sv[idx] = s;
            }
        }
        for (int k = n + 1; k >= 2; k--) {
            for (int i = n + 1, idx = k * pad + n + 1; i >= 2; i--, idx--) {
                int v = d[idx];
                float s = sv[idx];
                for (int m = 0; m < 8; m++) {
                    int c = d[idx - off[m]] + ow[m];
                    if (c < v) { v = c; s = sv[idx - off[m]]; }
                }
                d[idx] = v;
                sv[idx] = s;
            }
        }
        for (int k = 0; k < n; k++) for (int i = 0; i < n; i++) {
            int v = d[(k + 2) * pad + i + 2];
            distOut[k * n + i] = v >= big ? Float.MAX_VALUE : (float) (v * g.cell / 5.0);
            surfOut[k * n + i] = sv[(k + 2) * pad + i + 2];
        }
    }

    /** Class ids in order, for reports. */
    public static List<String> ids(Params p) {
        List<String> out = new ArrayList<>();
        for (ClassDef d : p.classes()) out.add(d.id());
        return Collections.unmodifiableList(out);
    }
}

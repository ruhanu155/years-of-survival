package com.yearofice.worldgen;

import com.yearofice.worldgen.GenJson.Range;
import java.util.ArrayList;
import java.util.List;

/**
 * One set piece's data, {@code data/yearofice/setpiece/<id>.json} (Minecraft development › Set pieces and
 * Interior structures as templates › Fit rules stay data). Fit rules read only layers that exist by M1
 * (height, slope, aspect, water distance), never biomes. The list and its order live in
 * {@code worldgen_params/setpieces.json}; stage 5 places them by {@code priority}, highest first.
 */
public record SetPieceDef(String id, String name, String kind, List<Integer> ring, Range count, int priority,
                          double spacingM, int abandonedYear, List<String> areas, Fit fit, Prefer prefer,
                          String rotation, List<Variant> variants, Landmark landmark, List<String> loot,
                          List<String> notes, String story) {

    public static final String INDEX = "data/yearofice/worldgen_params/setpieces.json";

    /** The short id, the path under {@code setpiece/}. */
    public String path() { return id.substring(id.indexOf(':') + 1); }

    /**
     * Hard rules. {@code sites} names where it may stand: riverbank, gravel_bar, lake_shore, ridge_clearing,
     * creek_bench, creek_reach, beaver_reach, ridge_top, face_foot, slope_break.
     */
    public record Fit(List<String> sites, double maxSlopeDeg, double minElevM, double maxElevM, double minWaterDistM,
                      double maxWaterDistM, double minMainRiverDistM, double maxMainRiverDistM, double aboveRiverM,
                      double faceMinHeightM, String faceAspect, Range creekGradientPct, Range creekReachM) {}

    /** Soft preferences, each a weight on a 0–1 term. */
    public record Prefer(double highElev, double lowSlope, double nearMainRiver, double nearWater, double aboveRiver,
                         double faceHeight, String viewOf, double viewWeight) {}

    /** A template variant: {@code structure/setpiece/<template>.nbt}, its weight, and the site kinds it suits. */
    public record Variant(String name, String template, double weight, List<String> sites, int footprintX,
                          int footprintZ, int heightM) {}

    /**
     * Whether the piece counts for the 2 km landmark test ({@code test_km} 0: never, like the hot spring's winter-only
     * plume), for which variants, and its silhouette model.
     */
    public record Landmark(double testKm, double heightM, String silhouette, List<String> variants) {
        public boolean counts(String variant) { return testKm > 0 && (variants.isEmpty() || variants.contains(variant)); }
    }

    public static List<SetPieceDef> loadAll() {
        GenJson idx = GenJson.resource(INDEX);
        List<String> ids = idx.strings("setpieces");
        idx.done();
        List<SetPieceDef> out = new ArrayList<>();
        for (String id : ids) out.add(load(id));
        out.sort((a, b) -> a.priority() != b.priority() ? Integer.compare(b.priority(), a.priority()) : a.id().compareTo(b.id()));
        return List.copyOf(out);
    }

    public static String resource(String path) { return "data/yearofice/setpiece/" + path + ".json"; }

    public static SetPieceDef load(String path) {
        GenJson j = GenJson.resource(resource(path));
        SetPieceDef d = from(j);
        if (!d.path().equals(path)) throw new IllegalArgumentException(resource(path) + ": id " + d.id() + " does not match its file");
        return d;
    }

    public static SetPieceDef from(GenJson j) {
        String id = j.str("id");
        if (!id.matches("yearofice:[a-z0-9_]+")) throw new IllegalArgumentException(j.path() + ": bad id " + id);
        List<Integer> ring = new ArrayList<>();
        for (double r : j.nums("ring")) ring.add((int) r);

        GenJson f = j.obj("fit");
        Fit fit = new Fit(f.strings("sites"), f.num("max_slope_deg"), f.num("min_elev_m", 0), f.num("max_elev_m", 5000),
                f.num("min_water_dist_m", 0), f.num("max_water_dist_m", 1e9), f.num("min_main_river_dist_m", 0),
                f.num("max_main_river_dist_m", 1e9), f.num("above_river_m", 0), f.num("face_min_height_m", 0),
                f.str("face_aspect", "any"), f.has("creek_gradient_pct") ? f.range("creek_gradient_pct") : null,
                f.has("creek_reach_m") ? f.range("creek_reach_m") : null);
        f.done();
        if (fit.maxSlopeDeg() > 25) throw new IllegalArgumentException(j.path() + ": max_slope_deg over 25");

        Prefer prefer = new Prefer(0, 0, 0, 0, 0, 0, "", 0);
        if (j.has("prefer")) {
            GenJson p = j.obj("prefer");
            prefer = new Prefer(p.num("high_elev_weight", 0), p.num("low_slope_weight", 0), p.num("near_main_river_weight", 0),
                    p.num("near_water_weight", 0), p.num("above_river_weight", 0), p.num("face_height_weight", 0), p.str("view_of", ""),
                    p.num("view_weight", 0));
            p.done();
        }

        List<Variant> variants = new ArrayList<>();
        for (GenJson v : j.objects("variants")) {
            String template = v.str("template");
            double[] fp = v.nums("footprint_m");
            if (fp.length != 2) throw new IllegalArgumentException(v.path() + ": footprint_m needs [x, z]");
            List<String> sites = v.has("sites") ? v.strings("sites") : List.of();
            variants.add(new Variant(template.substring(template.lastIndexOf('/') + 1), template, v.num("pick_weight"), sites,
                    (int) fp[0], (int) fp[1], v.integer("height_m")));
            v.done();
            if (fp[0] > 48 || fp[1] > 48) throw new IllegalArgumentException(v.path() + ": footprint over 48 x 48");
        }
        if (variants.isEmpty()) throw new IllegalArgumentException(j.path() + ": no variants");

        Landmark landmark = null;
        if (j.has("landmark")) {
            GenJson l = j.obj("landmark");
            landmark = new Landmark(l.num("test_km"), l.num("height_m"), l.str("silhouette"),
                    l.has("variants") ? l.strings("variants") : List.of());
            l.done();
        }
        SetPieceDef d = new SetPieceDef(id, j.str("name"), j.str("kind"), List.copyOf(ring), j.range("count"),
                j.integer("priority"), j.num("spacing_m"), j.has("abandoned_year") ? j.integer("abandoned_year") : 0,
                j.strings("areas"), fit, prefer, j.str("rotation"), List.copyOf(variants), landmark,
                j.has("loot") ? j.strings("loot") : List.of(), j.has("notes") ? j.strings("notes") : List.of(),
                j.str("story"));
        j.done();
        return d;
    }
}

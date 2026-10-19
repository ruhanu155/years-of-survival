package com.yearofice.worldgen;

import com.yearofice.worldgen.GenJson.Range;

/**
 * The four valley archetypes (Terrain and hydrology › Archetype shaping). Stage 1 draws one with
 * equal odds; stage 2 reads its shaping numbers from {@code data/yearofice/archetype/<id>.json}.
 * Enum order is the draw order and must never change.
 */
public enum Archetype {
    BRAIDED_FLATS("braided_flats"),
    LAKE_VALLEY("lake_valley"),
    CANYON("canyon"),
    OXBOW_MARSH("oxbow_marsh");

    private final String path;

    Archetype(String path) { this.path = path; }

    public String id() { return "yearofice:" + path; }

    public String path() { return path; }

    public String resource() { return "data/yearofice/archetype/" + path + ".json"; }

    public Params load() { return Params.from(this, GenJson.resource(resource())); }

    /** Every archetype's shaping numbers, loaded once per pipeline. */
    public static Params[] loadAll() {
        Archetype[] all = values();
        Params[] out = new Params[all.length];
        for (int i = 0; i < all.length; i++) out[i] = all[i].load();
        return out;
    }

    /**
     * Shaping numbers for one archetype. Ranges are drawn by stage 2. Optional blocks are null when an
     * archetype lacks them.
     */
    public record Params(
            Archetype archetype,
            Range floorWidthM,
            Range floorGradientPct,
            Range riverSwingM,
            Range troughDepthM,
            double troughFlatFraction,
            double floorNoiseM,
            double mountainFraction,
            double floorSedimentMultiplier,
            Range channels,
            Range gravelBeltM,
            int minimumFalls,
            Basin basin,
            Canyon canyon,
            Oxbows oxbows) {

        /** Lake valley: an elliptical basin north of the river. */
        public record Basin(Range lengthM, Range widthM, Range depthM, double riverGapM) {}

        /** Canyon: a narrow, walled reach with rock steps on the river. */
        public record Canyon(Range reachM, double maxReachFraction, Range floorWidthM, Range wallHeightM,
                             double wallRunM, Range steps, Range stepHeightM, double outsideFloorWidthM) {}

        /** Oxbow marsh: cut-off loops. Carved by stage 4 at M1, which owns river widths; stored here. */
        public record Oxbows(Range lakes, Range depthM, double swingRetryFactor, int maxRetries) {}

        static Params from(Archetype a, GenJson j) {
            String id = j.str("id");
            if (!id.equals(a.id())) throw new IllegalArgumentException(a.resource() + ": id " + id + " != " + a.id());
            Basin basin = null;
            if (j.has("basin")) {
                GenJson b = j.obj("basin");
                basin = new Basin(b.range("length_m"), b.range("width_m"), b.range("depth_m"), b.num("river_gap_m"));
                b.done();
            }
            Canyon canyon = null;
            if (j.has("canyon")) {
                GenJson c = j.obj("canyon");
                canyon = new Canyon(c.range("reach_m"), c.num("max_reach_fraction"), c.range("floor_width_m"),
                        c.range("wall_height_m"), c.num("wall_run_m"), c.range("steps"), c.range("step_height_m"),
                        c.num("outside_floor_width_m"));
                c.done();
            }
            Oxbows oxbows = null;
            if (j.has("oxbows")) {
                GenJson o = j.obj("oxbows");
                oxbows = new Oxbows(o.range("lakes"), o.range("depth_m"), o.num("swing_retry_factor"),
                        o.integer("max_retries"));
                o.done();
            }
            Params p = new Params(a, j.range("floor_width_m"), j.range("floor_gradient_pct"),
                    j.range("river_swing_m"), j.range("trough_depth_m"), j.num("trough_flat_fraction"),
                    j.num("floor_noise_m"), j.num("mountain_fraction"), j.num("floor_sediment_multiplier"),
                    j.range("channels"), j.has("gravel_belt_m") ? j.range("gravel_belt_m") : null,
                    j.integer("minimum_falls"), basin, canyon, oxbows);
            j.done();
            return p;
        }
    }
}

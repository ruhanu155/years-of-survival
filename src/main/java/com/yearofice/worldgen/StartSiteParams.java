package com.yearofice.worldgen;

import java.util.List;

/**
 * {@code data/yearofice/worldgen_params/start_site.json}: every number the start-site search reads (Blueprint ›
 * Start site: the 12 checks, the score, the outpost bar), plus the stand-in life layers until {@code Trails} and
 * {@code Ranges} land. Kept apart from {@code valley.json} so tuning the start never moves the M0/M1 layers.
 */
public record StartSiteParams(
        double stepM, double edgeMarginM, double padClearanceM,
        double waterMaxM,
        double flatAreaM, double flatMaxSlopeDeg,
        double faceMinHeightM, double faceWithinM,
        double firewoodRadiusM, int treesMin, int deadwoodMin,
        double tinderRadiusM, int birchMin,
        double fishWithinM, double fishMinQ, double fishNoneAtOrAboveQ, double fishNoneWithinM,
        double trailWithinM, int trailMin,
        double ironMinM, double ironMaxM, double ironEyeM, double ironOutcropM, double ironForestHeightM, double ironForestBlockM, boolean ironPrune,
        double landmarkWithinM,
        double setPieceWithinM,
        double denClearM, int bearRangesMin,
        double mooseWithinM, int mooseRangesMin,
        Score score, SosReturn sosReturn, Life life, Site site) {

    public static final String RESOURCE = "data/yearofice/worldgen_params/start_site.json";

    /** The score S of Start site › Choosing among passing sites. */
    public record Score(double treesCap, double birchCap, double weightBirch, double weightFace, double weightFlood,
                        double weightAspect, double weightView, double floodLowMinM, double floodLowMaxM,
                        double floodHighMaxM, double floodHighValue, double viewTowerValue, double viewOtherValue,
                        double towerMinElevationDeg, String towerSource) {}

    /**
     * Where the outpost may stand (Blueprint › Start site): never on bar cover (a bar class under the pad, or more
     * than {@code barRingMax} of the ring around it, scores 0 and is skipped), a dry bank {@code aboveRiverMinM} to
     * {@code aboveRiverMaxM} over the water, and at least {@code woodMin} live spruce or birch within {@code woodWithinM}.
     */
    public record Site(List<String> barClasses, double padMarginM, double ringM, double barRingMax, double aboveRiverMinM,
                       double aboveRiverMaxM, double woodWithinM, int woodMin) {}

    /** The gravel-bar block the Endless SOS flight returns to (Registry: {@code yearofice_world.params.sos_return}). */
    public record SosReturn(double searchM, double aboveWaterMinM, double floodYearRiseM) {}

    public record Species(int count, double radiusM, List<String> habitat) {}

    /** Stand-in home ranges, dens and trails: replaced by the real stage-7 layers without touching the checks. */
    public record Life(double habitatStepM, Species moose, Species blackBear, Species grizzly, double denSlopeMinDeg,
                       double denSlopeMaxDeg, double denElevationMinM, double wolfSedimentMinM, double wolfSlopeMinDeg, double wolfSlopeMaxDeg,
                       double wolfWaterMaxM, double trailStepM, int trailWaterPoints, double trailWaterLatticeM) {}

    public static StartSiteParams load() { return from(GenJson.resource(RESOURCE)); }

    public static StartSiteParams from(GenJson j) {
        j.integer("version");
        GenJson s = j.obj("search");
        double step = s.num("step_m"), edge = s.num("edge_margin_m"), clear = s.num("pad_clearance_m");
        s.done();
        GenJson c = j.obj("checks");
        GenJson water = c.obj("water"), flat = c.obj("flat"), shelter = c.obj("shelter"), fire = c.obj("firewood"),
                tinder = c.obj("tinder"), fish = c.obj("fishing"), trail = c.obj("trail"), iron = c.obj("iron"),
                lm = c.obj("landmark"), sp = c.obj("set_piece"), bear = c.obj("bear"), moose = c.obj("moose");
        StartSiteParams p = new StartSiteParams(step, edge, clear,
                water.num("max_dist_m"),
                flat.num("area_m"), flat.num("max_slope_deg"),
                shelter.num("face_min_height_m"), shelter.num("within_m"),
                fire.num("radius_m"), fire.integer("trees_min"), fire.integer("deadwood_min"),
                tinder.num("radius_m"), tinder.integer("birch_min"),
                fish.num("within_m"), fish.num("min_q"), fish.num("none_at_or_above_q"), fish.num("none_within_m"),
                trail.num("within_m"), trail.integer("min_count"),
                iron.num("min_m"), iron.num("max_m"), iron.num("eye_m"), iron.num("outcrop_height_m"), iron.num("forest_height_m"), iron.num("forest_block_m"), iron.bool("prune_inside_min"),
                lm.num("within_m"),
                sp.num("within_m"),
                bear.num("den_clear_m"), bear.integer("min_overlapping_ranges"),
                moose.num("within_m"), moose.integer("min_ranges"),
                score(j.obj("score")), sos(j.obj("sos_return")), life(j.obj("life")), site(j.obj("site")));
        for (GenJson o : List.of(water, flat, shelter, fire, tinder, fish, trail, iron, lm, sp, bear, moose, c)) o.done();
        j.done();
        return p;
    }

    private static Score score(GenJson s) {
        Score r = new Score(s.num("trees_cap"), s.num("birch_cap"), s.num("coef_birch"), s.num("coef_face"),
                s.num("coef_flood"), s.num("coef_aspect"), s.num("coef_view"), s.num("flood_low_min_m"),
                s.num("flood_low_max_m"), s.num("flood_high_max_m"), s.num("flood_high_value"), s.num("view_tower_value"),
                s.num("view_other_value"), s.num("tower_min_elevation_deg"), s.str("tower_source"));
        s.done();
        return r;
    }

    private static Site site(GenJson s) {
        Site r = new Site(s.strings("bar_classes"), s.num("pad_margin_m"), s.num("ring_m"), s.num("bar_ring_max"), s.num("above_river_min_m"),
                s.num("above_river_max_m"), s.num("wood_within_m"), s.integer("wood_min"));
        s.done();
        return r;
    }

    private static SosReturn sos(GenJson s) {
        SosReturn r = new SosReturn(s.num("search_m"), s.num("above_water_min_m"), s.num("flood_year_rise_m"));
        s.done();
        return r;
    }

    private static Species species(GenJson s) {
        Species r = new Species(s.integer("count"), s.num("radius_m"), s.strings("habitat"));
        s.done();
        return r;
    }

    private static Life life(GenJson l) {
        GenJson den = l.obj("den"), wolf = l.obj("wolf_den");
        Life r = new Life(l.num("habitat_step_m"), species(l.obj("moose")), species(l.obj("black_bear")), species(l.obj("grizzly")),
                den.num("slope_min_deg"), den.num("slope_max_deg"), den.num("elevation_min_m"), wolf.num("sediment_min_m"), wolf.num("slope_min_deg"),
                wolf.num("slope_max_deg"), wolf.num("water_max_m"), l.num("trail_step_m"), l.integer("trail_water_points_per_range"),
                l.num("trail_water_lattice_m"));
        den.done();
        wolf.done();
        l.done();
        return r;
    }
}

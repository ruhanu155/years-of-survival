package com.yearofice.worldgen;

import com.yearofice.worldgen.GenJson.Range;
import java.util.ArrayList;
import java.util.List;

/**
 * The M1 blocks of {@code worldgen_params/valley.json}: every number stages 4 (Water and rock) and 5 (Places)
 * use. Read by {@link WorldgenParams} so the file stays strict; stages 1–3 never read these, so changing one
 * can never move a stage 1–3 hash.
 */
public record HydroParams(Coarse coarse, Water water, FallsParams falls, Spots spots, Rock rock, Places places,
                          LandmarkParams landmarks) {

    /** The 8 km coarse network at 32 m (Terrain and hydrology › stage 4: coarse world river network first). */
    public record Coarse(double thresholdM2, double crossingMinM2, double matchToleranceM) {}

    /** Rivers, lakes, bogs and banks. */
    public record Water(double riverThresholdM2, double widthMinM, double widthPerSqrtM2, double depthBaseM,
                        double depthPerWidth, double speedMinMS, double speedMaxMS, double speedBaseMS,
                        double speedPerSlope, double slopeWindowM, double wadeMaxDepthM, double wadeMaxSpeedMS,
                        double bankDropM, double creekBankDropM, double bankMarginM, double bendScour,
                        double lakeMinDepthM, double lakeMinAreaM2, double bogMinAreaM2, double bogMaxSlopeDeg,
                        double bogMaxWaterDistM, double bogMinSedimentM, double weedShelfMaxDepthM,
                        double weedShelfWidthM, double dropOffGradient, double dropOffMinDepthM,
                        double cutBankMaxRadiusWidths, double cutBankMinBankM, double mainNormalM, double mainFloodM,
                        double creekNormalM, double creekFloodM, Range springsPerCreek, double springMinCreekM,
                        double cutoffMinSeparationWidths, double cutoffMaxGapWidths, double oxbowPlugM,
                        double braidMaxGradientPct, Range braidNodeSpacingM, double edgePinM, double stampWidthM,
                        double lakeMaxDepthM) {

        /** W = max(0.8, 0.0015 · √A). */
        public double width(double catchmentM2) { return Math.max(widthMinM, widthPerSqrtM2 * Math.sqrt(catchmentM2)); }

        /** D = 0.3 + 0.08 · W. */
        public double depth(double widthM) { return depthBaseM + depthPerWidth * widthM; }

        /** V = min(3.0, max(0.3, 0.6 + 150 · S)). */
        public double speed(double slope) {
            return Math.min(speedMaxMS, Math.max(speedMinMS, speedBaseMS + speedPerSlope * slope));
        }

        public boolean wadeable(double depthM, double speedMS) { return depthM < wadeMaxDepthM && speedMS < wadeMaxSpeedMS; }
    }

    /** Falls and pools, and the archetype falls guarantee. */
    public record FallsParams(double minDropM, double runM, Range poolRadiusM, double poolExtraDepthM,
                              Range guaranteeDeepenM, double guaranteeRunM, double thinBelowM, double footMaxSlope,
                              double minSpacingM) {}

    /** One fishing-spot type (Wildlife and fish › Fishing spot quality; Fish stocks › Stock per spot). */
    public record SpotType(String id, double baseQ, Range band, double radiusM, boolean major, List<String> species,
                           double[] capacity) {}

    public record Spots(Range variation, double deepBonus, double deepBonusDepthM, double belowBonus,
                        double belowBonusM, Range count, double minSpacingM, Range seamSpacingM, double seamMinWidthM,
                        double deepHoleMinDepthM, List<SpotType> types) {
        public SpotType type(String id) {
            for (SpotType t : types) if (t.id().equals(id)) return t;
            throw new IllegalArgumentException("no fishing spot type " + id);
        }
    }

    /** Rock faces, shelter candidates and iron outcrops (Terrain step 6; Explore, don't dig). */
    public record Rock(double minSlopeDeg, double minHardness, double shelterMinHeightM, int shelterFlagged,
                       double tileM, int minShelters, double ironMinElevM, Range ironClusters, double ironSpacingM,
                       Range ironNodes, Range rustRunM) {}

    /** Stage 5 placement. */
    public record Places(double candidateStepM, double jitter, double maxSlopeDeg, double coarseMaxSlopeDeg) {}

    /** The 2 km landmark test (Landmarks) and the natural landmarks. */
    public record LandmarkParams(double testRadiusM, int points, double eyeM, double topM, int minPoints, double canopyM,
                                 double rayStepM, double nearSkipM, double viewerSearchM, Range torHeightM, double snowfieldMinM,
                                 double cascadeMinFaceM, double candidateStepM) {}

    static HydroParams from(GenJson j) {
        GenJson c = j.obj("coarse_rivers");
        Coarse coarse = new Coarse(c.num("threshold_m2"), c.num("crossing_min_m2"), c.num("match_tolerance_m"));
        c.done();

        GenJson w = j.obj("water");
        GenJson fl = w.obj("flood_lines");
        Water water = new Water(w.num("river_threshold_m2"), w.num("width_min_m"), w.num("width_per_sqrt_m2"),
                w.num("depth_base_m"), w.num("depth_m_per_width_m"), w.num("speed_min_m_s"), w.num("speed_max_m_s"),
                w.num("speed_base_m_s"), w.num("speed_m_s_per_slope"), w.num("slope_window_m"), w.num("wade_max_depth_m"),
                w.num("wade_max_speed_m_s"), w.num("bank_drop_m"), w.num("creek_bank_drop_m"), w.num("bank_margin_m"),
                w.num("bend_scour"), w.num("lake_min_depth_m"), w.num("lake_min_area_m2"), w.num("bog_min_area_m2"),
                w.num("bog_max_slope_deg"), w.num("bog_max_water_dist_m"), w.num("bog_min_sediment_m"),
                w.num("weed_shelf_max_depth_m"), w.num("weed_shelf_width_m"), w.num("drop_off_gradient"),
                w.num("drop_off_min_depth_m"), w.num("cut_bank_bend_widths"), w.num("cut_bank_min_bank_m"),
                fl.num("main_normal_m"), fl.num("main_flood_m"), fl.num("creek_normal_m"), fl.num("creek_flood_m"),
                w.range("springs_per_creek"), w.num("spring_min_creek_m"), w.num("cutoff_min_separation_widths"),
                w.num("cutoff_max_gap_widths"), w.num("oxbow_plug_m"), w.num("braid_max_gradient_pct"),
                w.range("braid_node_spacing_m"), w.num("edge_pin_m"), w.num("stamp_width_m"),
                w.num("lake_max_depth_m"));
        fl.done();
        w.done();

        GenJson f = j.obj("falls");
        FallsParams falls = new FallsParams(f.num("min_drop_m"), f.num("run_m"), f.range("pool_radius_m"),
                f.num("pool_extra_depth_m"), f.range("guarantee_deepen_m"), f.num("guarantee_run_m"),
                f.num("thin_below_m"), f.num("foot_max_slope"), f.num("min_spacing_m"));
        f.done();

        GenJson s = j.obj("fish_spots");
        List<SpotType> types = new ArrayList<>();
        GenJson ts = s.obj("types");
        for (String id : ts.keys()) {
            GenJson t = ts.obj(id);
            GenJson cap = t.obj("capacity");
            List<String> species = cap.keys();
            double[] k = new double[species.size()];
            for (int i = 0; i < k.length; i++) k[i] = cap.num(species.get(i));
            cap.done();
            types.add(new SpotType(id, t.num("base_q"), t.range("band"), t.num("radius_m"), t.bool("major"), species, k));
            t.done();
        }
        ts.done();
        Spots spots = new Spots(s.range("variation"), s.num("deep_bonus"), s.num("deep_bonus_depth_m"),
                s.num("below_bonus"), s.num("below_bonus_m"), s.range("count"), s.num("min_spacing_m"),
                s.range("seam_spacing_m"), s.num("seam_min_width_m"), s.num("deep_hole_min_depth_m"), List.copyOf(types));
        s.done();

        GenJson r = j.obj("rock_faces");
        Rock rock = new Rock(r.num("min_slope_deg"), r.num("min_hardness"), r.num("shelter_min_height_m"),
                r.integer("shelter_flagged"), r.num("tile_m"), r.integer("min_shelters"), r.num("iron_min_elev_m"),
                r.range("iron_clusters"), r.num("iron_spacing_m"), r.range("iron_nodes"), r.range("rust_run_m"));
        r.done();

        GenJson p = j.obj("places");
        Places places = new Places(p.num("candidate_step_m"), p.num("jitter"), p.num("max_slope_deg"),
                p.num("coarse_max_slope_deg"));
        p.done();

        GenJson l = j.obj("landmarks");
        LandmarkParams lm = new LandmarkParams(l.num("test_radius_m"), l.integer("points"), l.num("eye_m"), l.num("top_m"),
                l.integer("min_points"), l.num("canopy_m"), l.num("ray_step_m"), l.num("near_skip_m"), l.num("viewer_search_m"),
                l.range("tor_height_m"), l.num("snowfield_min_m"), l.num("cascade_min_face_m"), l.num("candidate_step_m"));
        l.done();
        return new HydroParams(coarse, water, falls, spots, rock, places, lm);
    }
}

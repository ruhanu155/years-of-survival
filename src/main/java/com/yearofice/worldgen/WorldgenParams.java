package com.yearofice.worldgen;

/**
 * {@code data/yearofice/worldgen_params/valley.json}: every number stages 1–3 use for the home valley, plus the
 * M1 blocks for stages 4–5 in {@link HydroParams}, and the M2 blocks: stage 6's {@code cover} list and the chunk
 * builder's {@code terrain} block.
 * Frozen into the world at creation, so a {@code /reload} never moves land.
 */
public record WorldgenParams(
        int generatorVersion,
        Grid grid,
        Elevation elevation,
        NoiseLayer baseNoise,
        NoiseLayer ridgedNoise,
        NoiseLayer detailNoise,
        Warp domainWarp,
        double wallRiseDistanceM,
        RiverPathParams riverPath,
        Hardness hardness,
        ErosionParams erosion,
        SeedRanges seed,
        Checks checks,
        HydroParams hydro,
        Cover.Params cover,
        TerrainParams terrain) {

    public static final String RESOURCE = "data/yearofice/worldgen_params/valley.json";

    /** Core and world geometry. The core is region B3: x −2,048 to −1, z 0 to 2,047. */
    public record Grid(int coreSizeM, int cellM, int coreOriginXM, int coreOriginZM,
                       int worldSizeM, int coarseCellM, int edgeBlendM) {
        public int coreCells() { return coreSizeM / cellM; }
        public int coarseCells() { return worldSizeM / coarseCellM; }
    }

    public record Elevation(double minM, double maxM, double floorM, double floorToleranceM,
                            double softCapKneeM, double softCapM, double softFloorKneeM, double softFloorM,
                            double worldCapM) {}

    /** A noise layer: horizontal scale, octaves, vertical relief, and a north–south stretch. */
    public record NoiseLayer(double scaleM, int octaves, double reliefM, double stretchNorthSouth) {}

    public record Warp(double amplitudeM, double scaleM, int octaves) {}

    public record RiverPathParams(double pointSpacingM, double longScaleM, double shortScaleM, double longWeight,
                                  double bandMinFraction, double bandMaxFraction, double maxTotalDropM) {}

    public record Hardness(double scaleM, int octaves, double base, double noise, double ridgedBoost,
                           double canyonWallBoost) {}

    /** Stage 3. Changing any of these must leave the stage 2 hash alone. */
    public record ErosionParams(int droplets, int dropletLifetimeSteps, double inertia, double sedimentCapacity,
                                double minSedimentCapacity, double erodeRate, double depositRate,
                                int brushRadiusCells, double evaporationPerStep, double gravity,
                                double maxErodePerStepM, double hardThreshold, double hardErodeFactor,
                                int thermalPasses, double talusDropPerCellM, double thermalMoveFraction,
                                double floorAlluviumM, double edgeFadeM) {
        public ErosionParams withDroplets(int n) {
            return new ErosionParams(n, dropletLifetimeSteps, inertia, sedimentCapacity, minSedimentCapacity,
                    erodeRate, depositRate, brushRadiusCells, evaporationPerStep, gravity, maxErodePerStepM,
                    hardThreshold, hardErodeFactor, thermalPasses, talusDropPerCellM, thermalMoveFraction,
                    floorAlluviumM, edgeFadeM);
        }

        public ErosionParams withThermalPasses(int n) {
            return new ErosionParams(droplets, dropletLifetimeSteps, inertia, sedimentCapacity, minSedimentCapacity,
                    erodeRate, depositRate, brushRadiusCells, evaporationPerStep, gravity, maxErodePerStepM,
                    hardThreshold, hardErodeFactor, n, talusDropPerCellM, thermalMoveFraction,
                    floorAlluviumM, edgeFadeM);
        }
    }

    public record SeedRanges(GenJson.Range virtualCatchmentKm2, int hareCycleYears, int maxAttempts) {}

    /** Thresholds for the stage checks that fail an attempt. */
    public record Checks(double massBalanceFraction, double hardFaceSlopeDeg, double hardFaceKeepFraction) {}

    public WorldgenParams withErosion(ErosionParams e) {
        return new WorldgenParams(generatorVersion, grid, elevation, baseNoise, ridgedNoise, detailNoise, domainWarp,
                wallRiseDistanceM, riverPath, hardness, e, seed, checks, hydro, cover, terrain);
    }

    public WorldgenParams withHydro(HydroParams h) {
        return new WorldgenParams(generatorVersion, grid, elevation, baseNoise, ridgedNoise, detailNoise, domainWarp,
                wallRiseDistanceM, riverPath, hardness, erosion, seed, checks, h, cover, terrain);
    }

    public static WorldgenParams load() { return from(GenJson.resource(RESOURCE)); }

    public static WorldgenParams from(GenJson j) {
        GenJson g = j.obj("grid");
        Grid grid = new Grid(g.integer("core_size_m"), g.integer("cell_m"), g.integer("core_origin_x_m"),
                g.integer("core_origin_z_m"), g.integer("world_size_m"), g.integer("coarse_cell_m"),
                g.integer("edge_blend_m"));
        g.done();

        GenJson e = j.obj("elevation");
        Elevation elev = new Elevation(e.num("min_m"), e.num("max_m"), e.num("floor_m"), e.num("floor_tolerance_m"),
                e.num("soft_cap_knee_m"), e.num("soft_cap_m"), e.num("soft_floor_knee_m"), e.num("soft_floor_m"),
                e.num("world_cap_m"));
        e.done();

        GenJson w = j.obj("domain_warp");
        Warp warp = new Warp(w.num("amplitude_m"), w.num("scale_m"), w.integer("octaves"));
        w.done();

        GenJson rp = j.obj("river_path");
        RiverPathParams river = new RiverPathParams(rp.num("point_spacing_m"), rp.num("long_scale_m"),
                rp.num("short_scale_m"), rp.num("long_weight"), rp.num("band_min_fraction"),
                rp.num("band_max_fraction"), rp.num("max_total_drop_m"));
        rp.done();

        GenJson h = j.obj("hardness");
        Hardness hard = new Hardness(h.num("scale_m"), h.integer("octaves"), h.num("base"), h.num("noise"),
                h.num("ridged_boost"), h.num("canyon_wall_boost"));
        h.done();

        GenJson er = j.obj("erosion");
        ErosionParams ero = new ErosionParams(er.integer("droplets"), er.integer("droplet_lifetime_steps"),
                er.num("inertia"), er.num("sediment_capacity"), er.num("min_sediment_capacity"),
                er.num("erode_rate"), er.num("deposit_rate"), er.integer("brush_radius_cells"),
                er.num("evaporation_per_step"), er.num("gravity"), er.num("max_erode_per_step_m"),
                er.num("hard_threshold"), er.num("hard_erode_factor"), er.integer("thermal_passes"),
                er.num("talus_drop_per_cell_m"), er.num("thermal_move_fraction"), er.num("floor_alluvium_m"),
                er.num("edge_fade_m"));
        er.done();

        GenJson s = j.obj("seed");
        SeedRanges seed = new SeedRanges(s.range("virtual_catchment_km2"), s.integer("hare_cycle_years"),
                s.integer("max_attempts"));
        s.done();

        GenJson c = j.obj("checks");
        Checks checks = new Checks(c.num("mass_balance_fraction"), c.num("hard_face_slope_deg"),
                c.num("hard_face_keep_fraction"));
        c.done();

        GenJson walls = j.obj("valley_walls");
        double rise = walls.num("rise_distance_m");
        walls.done();

        WorldgenParams p = new WorldgenParams(j.integer("generator_version"), grid, elev,
                layer(j, "base_noise"), layer(j, "ridged_noise"), layer(j, "detail_noise"), warp, rise,
                river, hard, ero, seed, checks, HydroParams.from(j), Cover.Params.from(j.obj("cover")),
                TerrainParams.from(j.obj("terrain")));
        PlacerParams.from(j.obj("placer"));
        j.done();
        return p;
    }

    private static NoiseLayer layer(GenJson j, String key) {
        GenJson n = j.obj(key);
        NoiseLayer l = new NoiseLayer(n.num("scale_m"), n.integer("octaves"), n.num("relief_m"),
                n.num("stretch_north_south"));
        n.done();
        return l;
    }
}

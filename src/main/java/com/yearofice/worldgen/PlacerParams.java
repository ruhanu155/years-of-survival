package com.yearofice.worldgen;

import com.yearofice.sim.food.Placer;

/**
 * The {@code placer} entry of {@code worldgen_params/valley.json} (video 2, A10 black sand): the grid cell, the share of
 * cells that are placer reaches, the least distance from the start and the black sand a reach holds each year. It only
 * feeds {@link Placer}; no terrain layer reads it, so the M0/M1 water hashes do not move.
 */
public final class PlacerParams {
    private PlacerParams() {}

    public static final String RESOURCE = "data/yearofice/worldgen_params/valley.json";

    public static Placer load() { return from(GenJson.resource(RESOURCE).obj("placer")); }

    public static Placer from(GenJson j) {
        Placer p = new Placer(j.integer("cell_m"), j.num("share"), j.num("min_start_m"), j.integer("quantity_kg"));
        j.done();
        return p;
    }
}

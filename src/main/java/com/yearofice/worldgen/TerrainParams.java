package com.yearofice.worldgen;

/**
 * {@code terrain} in {@code worldgen_params/valley.json}: the M2 numbers that turn layers into blocks (Minecraft
 * development › Terrain to blocks), the slice valley cut (M2.5's {@code yearofice:slice_valley}) and the coal
 * seams ({@code worldgen/coal_seams}).
 */
public record TerrainParams(int soilBlocks, double footprintSkirtM, double fallsFlowRadiusM, Slice slice, Coal coal) {

    /** The slice valley: a square cut of the core holding the start and the M2.5 features. */
    public record Slice(int sizeM, int stepM, int edgeMarginM, int minCreekCells, int minBarCells,
                        int minRiparianCells, int minSpruceCells, int minBirchCells) {}

    /** Coal seams on the rock shell: how many, how far from the start, how far apart, how many cells each. */
    public record Coal(GenJson.Range count, double minStartM, GenJson.Range ringM, double spacingM,
                       GenJson.Range cells, double minFaceHeightM) {}

    public static TerrainParams from(GenJson j) {
        GenJson s = j.obj("slice");
        Slice slice = new Slice(s.integer("size_m"), s.integer("step_m"), s.integer("edge_margin_m"),
                s.integer("min_creek_cells"), s.integer("min_bar_cells"), s.integer("min_riparian_cells"),
                s.integer("min_spruce_cells"), s.integer("min_birch_cells"));
        s.done();
        GenJson c = j.obj("coal");
        Coal coal = new Coal(c.range("count"), c.num("min_start_m"), c.range("ring_m"), c.num("spacing_m"),
                c.range("cells"), c.num("min_face_height_m"));
        c.done();
        TerrainParams t = new TerrainParams(j.integer("soil_blocks"), j.num("footprint_skirt_m"),
                j.num("falls_flow_radius_m"), slice, coal);
        j.done();
        return t;
    }
}

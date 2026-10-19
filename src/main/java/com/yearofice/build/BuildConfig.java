package com.yearofice.build;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [building]} table of {@code yearofice-common.toml}: the slice's camp work (Crafting and building in depth
 * › More Start recipes, "Cut boughs"; Blueprint › Making camp, "Bough bed"). Every preset uses the docs' numbers.
 */
public final class BuildConfig {
    private BuildConfig() {}

    static final PresetTable TABLE = new PresetTable("building")
            .dial("bough_cut_min", "Cut boughs from a living spruce: game minutes of hold-to-work.", 1, 0, 60)
            .dial("boughs_per_cut", "Spruce boughs one cut gives.", 10, 1, 64)
            .dial("bough_bed_boughs", "Spruce boughs a bough bed takes.", 40, 1, 256)
            .dial("bough_bed_min", "Build a bough bed: game minutes of hold-to-work on the ground.", 60, 0, 600)
            .dial("min_hold_ticks", "No hold-to-work job is shorter than this many real ticks, so its ring shows.", 10, 1, 200)
            .dial("work_reach_m", "How far from a job's spot the worker may stand, metres.", 5, 1, 16);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Building: camp work in the slice (cut boughs, the bough bed).");
    }

    public static double boughCutMin() { return TABLE.get("bough_cut_min"); }
    public static int boughsPerCut() { return (int) Math.round(TABLE.get("boughs_per_cut")); }
    public static int bedBoughs() { return (int) Math.round(TABLE.get("bough_bed_boughs")); }
    public static double bedMin() { return TABLE.get("bough_bed_min"); }
    public static int minHoldTicks() { return (int) Math.round(TABLE.get("min_hold_ticks")); }
    public static double reachM() { return TABLE.get("work_reach_m"); }
}

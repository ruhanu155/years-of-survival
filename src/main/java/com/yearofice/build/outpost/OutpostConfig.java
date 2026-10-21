package com.yearofice.build.outpost;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [outpost]} section: hot tent (B11), fence drifts (B9) and outpost ageing dials, Gentle/Standard/Brutal. */
public final class OutpostConfig {
    private OutpostConfig() {}

    public static final PresetTable TABLE = table();

    private static PresetTable table() {
        PresetTable t = new PresetTable("outpost");
        t.dial("tent_loss_canvas_wpc", "Canvas wall tent envelope loss, W per degC (B11).", 16, 18, 20, 1, 100);
        t.dial("tent_loss_hide_wpc", "Hide tent envelope loss, W per degC (B11).", 14, 16, 18, 1, 100);
        t.dial("tent_floor_boughs_wpc", "Tent floor loss on a spruce-bough floor, W per degC (B11).", 3, 0, 20);
        t.dial("tent_floor_snow_wpc", "Tent floor loss on bare snow, W per degC (B11).", 10, 0, 40);
        t.dial("tent_spark_hole_wpc", "Extra loss per spark hole in the canvas, W per degC (B11).", 4, 0, 20);
        t.dial("tent_heat_capacity_whc", "Tent air heat capacity, Wh per degC (B11).", 4, 0.5, 50);
        t.dial("tent_stove_kw", "Folding tin stove steady output, kW (B11).", 1.0, 0.1, 10);
        t.dial("can_stove_kw", "Can stove steady output, kW (B11).", 0.8, 0.1, 10);
        t.dial("tent_heat_kept", "Share of the stove's heat the tent keeps (B11).", 0.9, 0.1, 1);
        t.dial("tent_spark_hole_hot", "Chance a night burned on Hot holes a canvas tent (B11); hide is a quarter, a screen stops it.", 0.02, 0.04, 0.06, 0, 1);
        t.dial("fence_drift_boost", "A fence's lee drift is the lee snow pack's depth times this (B9).", 1.5, 0.1, 10);
        t.dial("fence_drift_reach", "Blocks downwind a fence's drift reaches (B9).", 7, 2, 16);
        t.dial("outpost_age_scale", "Years of decay a worldgen outpost gets, times this (Decay and time).", 0.5, 1.0, 1.5, 0, 5);
        return t;
    }

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Outposts, the hot tent and fence drifts (Open world in depth > Outposts; B9, B11).");
    }

    public static double get(String key) { return TABLE.get(key); }
}

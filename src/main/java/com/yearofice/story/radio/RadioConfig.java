package com.yearofice.story.radio;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [radio]} table of {@code yearofice-common.toml}: crank, battery item, the drop's fall and scatter. */
public final class RadioConfig {
    private RadioConfig() {}

    static final PresetTable TABLE = new PresetTable("radio")
            .dial("sked_cost_pct", "Battery a sked uses, percent (Open world in depth > The radio).", 3, 0, 100)
            .dial("crank_pct", "Battery one turn of the hand crank adds, percent.", 2, 0, 100)
            .dial("battery_item_pct", "Battery a spare radio battery adds, percent.", 30, 0, 100)
            .dial("panel_factor", "Solar charge multiplier (1 = the crate's panel, 3 = a 40 W catalogue panel).", 1, 0, 10)
            .dial("drop_height_m", "Metres above the ground the crate's parachute is released.", 40, 5, 200)
            .dial("drop_fall_mps", "Parachute descent speed, metres per second.", 5, 1, 20)
            .dial("scatter_m", "The crate lands within this many metres of a marked zone.", 50, 0, 300);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "The radio, skeds and supply drops (Open world in depth > The radio).");
    }

    public static double get(String key) { return TABLE.get(key); }
}

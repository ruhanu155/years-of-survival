package com.yearofice.build.craft;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [crafting]} table of {@code yearofice-common.toml}: station reach, the work scale, tool wear per job
 * and the lift-a-box dials (Crafting and building in depth › Stations, Practice, Tool durability; Minecraft
 * development › Lift a full box). Every number the code reads is here, with Gentle, Standard and Brutal columns.
 */
public final class CraftConfig {
    private CraftConfig() {}

    static final PresetTable TABLE = new PresetTable("crafting")
            .dial("work_time_scale", "Station recipes take their work minutes times this (before practice).", 0.8, 1.0, 1.25, 0.1, 10)
            .dial("workbench_reach_m", "How far from a workbench you may stand and still use it, in blocks.", 2, 1, 8)
            .dial("leave_m", "A craft stops if you walk this far from where you began it, in blocks.", 2.5, 1, 16)
            .dial("dark_light_level", "A job is in the dark (m_light x2) at this light level or less.", 4, 0, 15)
            .dial("minutes_per_tool_use", "A job charges its tool one use, plus one for each this many work minutes.", 15, 1, 1000)
            .dial("box_cap_kg", "The heaviest box, contents included, that can be lifted whole (A7).", 30, 1, 200)
            .dial("box_lift_s", "Real seconds Shift + use is held to lift a box (A7).", 1, 0.1, 10)
            .dial("box_chip_s", "Real seconds a frozen-down box takes to chip free with a hatchet or knife (A7).", 2, 0.1, 20)
            .dial("box_frozen_h", "Game hours a box stands on frozen ground in air below 0 C before it freezes down (A7).", 24, 0, 1000)
            .dial("box_reach_m", "How far from a box you may stand to lift or set it down, in blocks.", 4.5, 1, 8);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Crafting: stations, recipes, tool wear and lifting a box (Crafting and building in depth).");
    }

    public static double get(String key) { return TABLE.get(key); }
}

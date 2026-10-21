package com.yearofice.build.metal;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [metal]} table of {@code yearofice-common.toml}: smelting and forging times, the sluice, the fish wheel
 * and the canoe (Crafting and building in depth › Tier 4 and Homestead; Structures and buildables › Water power,
 * crossings and craft). Gentle, Standard and Brutal columns.
 */
public final class MetalConfig {
    private MetalConfig() {}

    static final PresetTable TABLE = new PresetTable("metal")
            .dial("smelt_hours", "Game hours of pumping one smelt takes at the bloomery.", 4, 6, 8, 0.1, 100)
            .dial("consolidate_hours", "Game hours consolidating a bloom into bar takes at the forge.", 1.5, 2, 3, 0.1, 100)
            .dial("work_reach_m", "How far you may stand from the bloomery, forge or sluice and keep working, in blocks.", 4, 1, 8)
            .dial("wheel_foul_per_h", "Debris a fish wheel gathers per game hour it turns.", 0.5, 1.0, 2.0, 0, 20)
            .dial("wheel_foul_max", "Debris at which a fish wheel stops until a player clears it (sneak and use).", 96, 1, 1000)
            .dial("canoe_capsize_wind_ms", "Wind speed at which an open canoe may capsize on open water.", 14, 11, 8, 1, 60)
            .dial("canoe_capsize_chance_per_s", "Chance each second of capsizing once the wind or rapids pass the limit.", 0.02, 0.05, 0.1, 0, 1)
            .dial("canoe_load_cap_kg", "Load a canoe carries before it rides too low to paddle.", 250, 1, 2000)
            .dial("canoe_portage_kg", "Weight of a carried canoe (the portage item).", 30, 1, 200);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Metal: bloomery, forge, sluice box, fish wheel and canoe (Tiers and gates; Water power, crossings and craft).");
    }

    public static double get(String key) { return TABLE.get(key); }
}

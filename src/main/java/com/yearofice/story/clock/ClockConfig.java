package com.yearofice.story.clock;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [clock]} table. The rates themselves are data ({@code clock/rates.json}) and the day length a world
 * option; what is left to tune is when a player counts as travelling.
 */
public final class ClockConfig {
    private ClockConfig() {}

    static final PresetTable TABLE = new PresetTable("clock")
            .dial("travel_from_m_per_s", "Horizontal speed at or above which a player asks for the Travel rate (m/s).", 0.5, 0, 20);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Clock and calendar (Layer 1). Rates live in data/yearofice/clock/rates.json.");
    }

    static double travelFromMps() { return TABLE.get("travel_from_m_per_s"); }
}

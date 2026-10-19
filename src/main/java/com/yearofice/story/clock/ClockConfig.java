package com.yearofice.story.clock;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [clock]} table. The rates themselves are data ({@code clock/rates.json}) and the day length a world
 * option; what is left to tune is when a player counts as travelling, and when Day 1's half-speed clock ends.
 */
public final class ClockConfig {
    private ClockConfig() {}

    static final PresetTable TABLE = new PresetTable("clock")
            .dial("travel_from_m_per_s", "Horizontal speed at or above which a player asks for the Travel rate (m/s).", 0.5, 0, 20)
            .dial("day_one_until_hour", "Day 1 (Blueprint > Start site > Day 1 rules): the half-speed clock ends at the first fire or at"
                    + " this hour of the guided night's first day, whichever comes first. The share itself is"
                    + " first_fire_world_pct in clock/rates.json.", 22, 0, 24);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Clock and calendar (Layer 1). Rates live in data/yearofice/clock/rates.json.");
    }

    static double travelFromMps() { return TABLE.get("travel_from_m_per_s"); }

    /** The hour of the guided night's first day at which the Day 1 half-speed clock ends without a fire. */
    public static int dayOneUntilHour() { return (int) Math.round(TABLE.get("day_one_until_hour")); }
}

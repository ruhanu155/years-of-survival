package com.yearofice.build.wood;

import com.yearofice.core.rules.PresetTable;
import com.yearofice.sim.fire.Seasoning;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [wood]} section of {@code yearofice-common.toml} (Trees, felling and firewood › Build: "Tuning lives in the
 * [wood] section (swing scale, mishap chances, drying rates)"), in the shared preset-table layout. The B4 seasoning
 * dials live here with the rest of the wood rather than in {@code [fire]}, so the fire package's table stays its own;
 * their values are B4's as written, the same in every preset.
 */
public final class WoodConfig {
    private WoodConfig() {}

    static final String[] MONTHS = {"jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"};

    public static final PresetTable TABLE = table();

    private static PresetTable table() {
        PresetTable t = new PresetTable("wood");
        t.dial("swing_scale", "Game minutes a felling swing takes, times this (the tool's minutes a swing come from data_maps/item/tool.json).",
                0.8, 1.0, 1.25, 0.1, 10);
        t.dial("work_scale", "Limbing, bucking, cutting and splitting take the tool's game minutes times this.", 0.8, 1.0, 1.25, 0.1, 10);
        t.dial("sneak_cut_time_mult", "Sneak + cut on a standing trunk takes the tool's buck time times this (A1).", 2.0, 0.1, 10);
        t.dial("work_reach_m", "Hold-to-work reach from the eye, in blocks.", 4.5, 1, 8);
        t.dial("fall_min_s", "Seconds the shortest tree takes to fall.", 1.5, 0.2, 10);
        t.dial("fall_max_s", "Seconds a 20 m tree takes to fall.", 3.0, 0.2, 10);
        t.dial("felled_log_settle_days", "A felled log not worked for this many game days settles into fallen_log blocks.", 7, 1, 365);
        t.dial("green_moisture_pct", "Moisture % of wood cut from a living tree.", 50, 0, 100);
        t.dial("placed_log_moisture_pct", "Moisture % of rounds cut out of a placed log (a wall log has dried).", 20, 0, 100);
        t.dial("chopping_block_set_min", "Game minutes to set a round on end as a chopping block (Sneak + hold use).", 2, 0, 60);
        // B4 seasoning by the calendar (Proposed; the current numbers).
        double[] std = Seasoning.Rates.STANDARD.monthly();
        for (int i = 0; i < 12; i++) {
            t.dial("seasoning_rate_" + MONTHS[i], "Moisture points a game day that split wood on a covered rack loses in this month (B4).", std[i], 0, 10);
        }
        t.dial("seasoning_april_from_day", "April's rate runs from this day (melt-out).", 15, 1, 30);
        t.dial("seasoning_october_to_day", "October's rate runs to this day (daily mean above 0 C).", 10, 0, 31);
        t.dial("woodshed_mult", "A woodshed dries split wood this many times the rack's rate (B4).", 1.3, 0, 10);
        t.dial("uncovered_mult", "A stack with no top cover dries at this share of the rate (B4).", 0.7, 0, 10);
        t.dial("rain_wet_points", "Moisture points an uncovered stack gains a rainy day (B4; the woodpile, M8).", 1.5, 0, 50);
        t.dial("unsplit_mult", "Unsplit rounds dry at this share of the rate (B4).", 0.4, 0, 10);
        t.dial("log_mult", "Whole logs dry at this share of the rate (B4).", 0.25, 0, 10);
        t.dial("seasoning_floor_pct", "Air-dried wood dries no lower than this moisture %.", 12, 0, 50);
        return t;
    }

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Trees, felling and firewood: swings, work times, falls and seasoning (Crafting and building in depth).");
    }

    public static double get(String key) { return TABLE.get(key); }

    /** The seasoning dials in force. */
    public static Seasoning.Rates rates() {
        double[] m = new double[12];
        for (int i = 0; i < 12; i++) m[i] = get("seasoning_rate_" + MONTHS[i]);
        return new Seasoning.Rates(m, (int) get("seasoning_april_from_day"), (int) get("seasoning_october_to_day"), get("woodshed_mult"),
                get("uncovered_mult"), get("unsplit_mult"), get("log_mult"), get("seasoning_floor_pct"));
    }
}

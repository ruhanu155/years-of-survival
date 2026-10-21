package com.yearofice.build.pieces;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [pieces]} table: rooms, heat, paths and stamping (Crafting and building in depth › Building pieces, Warm rooms). */
public final class PiecesConfig {
    private PiecesConfig() {}

    static final PresetTable TABLE = new PresetTable("pieces")
            .dial("instant_place", "1 places a building piece at a tap; 0 makes placing a hold-to-work job of the piece's labour_min.", 1, 0, 0, 0, 1)
            .dial("room_max_m3", "Largest enclosure counted as a room, cubic metres; bigger fills are the open air.", 600, 8, 4096)
            .dial("room_scans_per_tick", "Dirty room scans run per server tick.", 4, 1, 64)
            .dial("room_cache_ticks", "How long a room's heat balance is trusted before it is recomputed, ticks.", 200, 1, 24000)
            .dial("path_dirt_pct", "Walking speed bonus on a dirt path, percent.", 6, 0, 50)
            .dial("path_gravel_pct", "Walking speed bonus on a gravel path, percent.", 10, 0, 50)
            .dial("path_plank_pct", "Walking speed bonus on a plank path, percent.", 14, 0, 50)
            .dial("path_stone_pct", "Walking speed bonus on a stone path, percent.", 18, 0, 50)
            .dial("stamp_labour_pct", "Player labour of stamping a saved structure, percent of the first build's.", 5, 0, 100)
            .dial("stamp_setup_min", "Game minutes to mark a stamp's anchor.", 5, 0, 120)
            .dial("stamp_pieces_per_tick", "Queued stamp pieces placed per server tick.", 4, 1, 64);

    public static void define(ModConfigSpec.Builder b) { TABLE.define(b, "Building pieces, rooms, heat, paths and stamping (M8)."); }

    public static boolean instantPlace() { return TABLE.get("instant_place") >= 0.5; }
    public static int roomMax() { return (int) Math.round(TABLE.get("room_max_m3")); }
    public static int scansPerTick() { return (int) Math.round(TABLE.get("room_scans_per_tick")); }
    public static int cacheTicks() { return (int) Math.round(TABLE.get("room_cache_ticks")); }
    public static double pathPct(String surface) { return TABLE.get("path_" + surface + "_pct"); }
    public static double stampPct() { return TABLE.get("stamp_labour_pct"); }
    public static double stampSetupMin() { return TABLE.get("stamp_setup_min"); }
    public static int stampPerTick() { return (int) Math.round(TABLE.get("stamp_pieces_per_tick")); }
}

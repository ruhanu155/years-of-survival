package com.yearofice.body;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [sos]} table of {@code yearofice-common.toml}: the collapse and the flight out (all presets alike). */
public final class SosConfig {
    private SosConfig() {}

    static final PresetTable TABLE = new PresetTable("sos")
            .dial("collapse_ticks", "Ticks the collapse takes before the SOS goes out (Blueprint: 4 real seconds).", 80, 0, 600)
            .dial("flight_ticks", "Ticks the SOS flight screen stays up before the jump (about 2 real seconds).", 60, 0, 600)
            .dial("away_days", "Game days the flight out takes (Blueprint: 14).", 14, 1, 60)
            .dial("line_max_chars", "Longest 'why I left' line.", 60, 0, 200);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "SOS: the collapse and the Endless flight out (Minecraft development > Collapse, SOS and the flight).");
    }

    public static int collapseTicks() { return (int) Math.round(TABLE.get("collapse_ticks")); }

    public static int flightTicks() { return (int) Math.round(TABLE.get("flight_ticks")); }

    /** {@code /yoi sos stayout} (M9): game days that replace the table's away_days while above 0. */
    public static volatile int stayoutDays = 0;

    public static int awayDays() { return stayoutDays > 0 ? stayoutDays : (int) Math.round(TABLE.get("away_days")); }

    public static int lineMax() { return (int) Math.round(TABLE.get("line_max_chars")); }
}

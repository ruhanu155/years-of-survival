package com.yearofice.body.sleep;

import com.yearofice.core.rules.PresetTable;
import com.yearofice.sim.body.SleepRules;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [sleep]} table of {@code yearofice-common.toml}: when a player may lie down and what wakes them
 * (Blueprint › Survival tuning › Sleep). Every preset uses the Blueprint's numbers; the night's chill is already
 * scaled by {@code [body] cold}. Beds' own numbers (quality, insulation) are data: {@code data_maps/block/bed.json}.
 */
public final class SleepConfig {
    private SleepConfig() {}

    static final PresetTable TABLE = new PresetTable("sleep")
            .dial("tired_energy", "Below this many bolts a player may sleep at any hour.", 7, 0, 10)
            .dial("night_from_hour", "The clock's night begins at this hour; a player may sleep from then.", 21, 0, 23)
            .dial("morning_hour", "The clock's night ends at this hour, and a night sleeper wakes.", 6, 0, 23)
            .dial("wake_warmth", "Warmth below which the cold wakes a sleeper.", 40, 0, 100)
            .dial("wake_wetness_pct", "Clothing wetness above which a sleeper wakes, percent.", 60, 0, 100)
            .dial("wake_water", "Water meter below which thirst wakes a sleeper.", 15, 0, 100)
            .dial("large_animal_m", "Any large animal (#yearofice:large_animals) this near wakes a sleeper, metres.", 30, 0, 200)
            .dial("predator_m", "Any predator (#yearofice:predators) this near wakes a sleeper, metres.", 60, 0, 200)
            .dial("ground_quality", "Sleep quality on bare ground, with no bed (Blueprint: 0.70).", 0.70, 0.01, 1);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Sleep: when a player may lie down and what wakes them (Blueprint > Survival tuning > Sleep).",
                "Bed quality and insulation are data: data/yearofice/data_maps/block/bed.json.");
    }

    public static SleepRules.Tuning tuning() {
        return new SleepRules.Tuning(TABLE.get("tired_energy"), hour("night_from_hour"), hour("morning_hour"),
                TABLE.get("wake_warmth"), TABLE.get("wake_wetness_pct") / 100.0, TABLE.get("wake_water"),
                TABLE.get("large_animal_m"), TABLE.get("predator_m"), TABLE.get("ground_quality"));
    }

    private static int hour(String key) { return (int) Math.round(TABLE.get(key)) % 24 * 60; }
}

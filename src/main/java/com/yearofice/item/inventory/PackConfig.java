package com.yearofice.item.inventory;

import com.yearofice.core.rules.PresetTable;
import com.yearofice.sim.pack.LoadBands;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [pack]} table: load bands, the no-pack limit, the pickup reach and the dropped-item rule. */
public final class PackConfig {
    private PackConfig() {}

    static final PresetTable TABLE = new PresetTable("pack")
            .dial("band_light_kg", "Upper edge of the full-speed band at Strength multiplier 1 (kg).", 15, 1, 200)
            .dial("band_heavy_kg", "Upper edge of the 85% band (kg).", 30, 1, 200)
            .dial("band_loaded_kg", "Upper edge of the 65% band; above it drag only (kg).", 45, 1, 200)
            .dial("speed_heavy", "Speed factor 15-30 kg.", 0.85, 0, 1)
            .dial("speed_loaded", "Speed factor 30-45 kg (no running).", 0.65, 0, 1)
            .dial("speed_overloaded", "Speed factor over 45 kg (drag only).", 0.30, 0, 1)
            .dial("kcal_heavy", "Calorie factor 15-30 kg.", 1.2, 1, 5)
            .dial("kcal_loaded", "Calorie factor over 30 kg.", 1.5, 1, 5)
            .dial("strength_low", "Band multiplier at Strength 0 (video 2, A11).", 0.8, 0.1, 2)
            .dial("strength_high", "Band multiplier at Strength 100 (video 2, A11).", 1.15, 0.1, 2)
            .dial("hauling_bonus", "Extra band multiplier at full Hauling practice (A11).", 0.10, 0, 1)
            .dial("hauling_reps", "Hauling reps to Practiced (A11).", 300, 1, 10_000)
            .dial("hauling_rep_ticks", "Ticks of walking under 15 kg or more per Hauling rep.", 400, 1, 100_000)
            .dial("no_pack_kg", "Carry limit with no pack: the belt and pockets (kg).", 5, 0, 100)
            .dial("refuse_over_kg", "A pickup that would pass this total is refused: drag it instead (kg).", 45, 1, 500)
            .dial("default_item_g", "Weight of an item with no weight map entry (g).", 100, 0, 100_000)
            .dial("pickup_reach_m", "How far the pickup ray reaches (m).", 3, 1, 8)
            .dial("dropped_item_days", "Game days a dropped item lasts (reconcile 36).", 7, 1, 365);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Pack and weight (Inventory and slots; Weight and carrying). Pack rows and limits live in data_maps/item/pack.json.");
    }

    public static double get(String key) { return TABLE.get(key); }

    public static LoadBands bands() {
        return new LoadBands(new double[] {get("band_light_kg"), get("band_heavy_kg"), get("band_loaded_kg")},
                new double[] {1.0, get("speed_heavy"), get("speed_loaded"), get("speed_overloaded")},
                new double[] {1.0, get("kcal_heavy"), get("kcal_loaded"), get("kcal_loaded")},
                get("strength_low"), get("strength_high"), get("hauling_bonus"), get("hauling_reps"));
    }
}

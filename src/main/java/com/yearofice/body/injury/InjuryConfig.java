package com.yearofice.body.injury;

import com.yearofice.core.rules.PresetTable;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [injury]} table of {@code yearofice-common.toml}: stumbles, back strain, cauterizing, pocket stones. */
public final class InjuryConfig {
    private InjuryConfig() {}

    static final PresetTable TABLE = new PresetTable("injury")
            .dial("stumble_base_pct", "Chance of a stumble roll per 100 m on rough ground, percent (Blueprint 0.3).",
                    0.15, 0.3, 0.45, 0, 20)
            .dial("back_strain_chance", "Chance a lift of 90% or more of the carry cap strains the back (B15). 0 is off.",
                    0, 0.03, 0.045, 0, 1)
            .dial("back_strain_squat_mult", "Multiplier on that chance for a squat lift.", 0.3, 0, 1)
            .dial("back_strain_cooldown_days", "Game days between two back strains (the tedium guard).", 10, 0, 100)
            .dial("pocket_stone_fade_min", "Game minutes a heated pocket stone takes to fade to nothing.", 90, 1, 1440)
            .dial("cauterize_loss", "Condition lost to cauterizing a deep cut.", 8, 0, 100)
            .dial("cauterize_blackout_below", "Condition under which a cauterize may black the player out.", 15, 0, 100)
            .dial("cauterize_blackout_chance", "Chance of that blackout.", 0.5, 0, 1)
            .dial("bleed_window_min", "Game minutes a burn may be cooled within.", 10, 0, 1440);

    public static void define(ModConfigSpec.Builder b) {
        TABLE.define(b, "Injury: stumbles, back strain, cauterizing and pocket stones (Body, injury and sickness).");
    }

    public static double get(String key) { return TABLE.get(key); }
}

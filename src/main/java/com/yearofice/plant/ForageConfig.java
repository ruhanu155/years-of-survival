package com.yearofice.plant;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.plant.ForageTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [forage]} section of {@code yearofice-common.toml}, with the Gentle, Standard and Brutal presets. */
public final class ForageConfig {
    private ForageConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("yieldScale", "x units per clump.");
        NOTES.put("workTimeScale", "x game minutes per action.");
        NOTES.put("regrowScale", "x regrow and refruit days.");
        NOTES.put("strippedThreshold", "Picked share that halves next year.");
        NOTES.put("poisonScale", "x Condition lost to plant poisons.");
        NOTES.put("windowJitterDays", "Most a year can shift a ripeness window, in days.");
        NOTES.put("sapScale", "x litres a tap a day.");
        NOTES.put("spoilScale", "x spoil_days_at_5c of plant food.");
        NOTES.put("lickRegrowKgPerWeek", "kg of crust a mineral lick regrows a week.");
        NOTES.put("patchDensity", "Share of 24 m cells that hold a plant patch at world generation.");
    }

    public static final PresetConfig<ForageTuning> FORAGE = new PresetConfig<>(ForageTuning.class, p -> switch (p) {
        case GENTLE -> ForageTuning.Preset.GENTLE.tuning;
        case STANDARD -> ForageTuning.Preset.STANDARD.tuning;
        case BRUTAL -> ForageTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Plants, foraging and mineral licks (Plants and fungi > Debug commands and tuning).").push("forage");
        FORAGE.define(b);
        b.pop();
    }

    public static ForageTuning tuning() { return FORAGE.tuning(); }
}

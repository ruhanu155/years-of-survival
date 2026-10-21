package com.yearofice.ecosystem.livestock;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.livestock.LivestockTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [livestock]} section of {@code yearofice-common.toml}: four dials with Gentle, Standard and Brutal presets. */
public final class LivestockConfig {
    private LivestockConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("needDecayScale", "x how fast fed, water and warmth fall.");
        NOTES.put("predatorScale", "x every night-raid chance on a pen.");
        NOTES.put("escapeScale", "x every chance a penned animal slips out.");
        NOTES.put("growthScale", "x how fast young animals grow.");
    }

    public static final PresetConfig<LivestockTuning> LIVESTOCK = new PresetConfig<>(LivestockTuning.class, p -> switch (p) {
        case GENTLE -> LivestockTuning.Preset.GENTLE.tuning;
        case STANDARD -> LivestockTuning.Preset.STANDARD.tuning;
        case BRUTAL -> LivestockTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Livestock: goats, reindeer and hens (Raising animals).").push("livestock");
        LIVESTOCK.define(b);
        b.pop();
    }

    public static LivestockTuning tuning() { return LIVESTOCK.tuning(); }
}

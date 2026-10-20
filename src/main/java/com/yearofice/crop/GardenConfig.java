package com.yearofice.crop;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.crop.GardenTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [garden]} section of {@code yearofice-common.toml}: six dials with Gentle, Standard and Brutal presets. */
public final class GardenConfig {
    private GardenConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("germinationScale", "x the rolled germination chance, capped at 0.95.");
        NOTES.put("yieldScale", "x yield per block.");
        NOTES.put("pestScale", "x every pest chance.");
        NOTES.put("wornBedLoss", "Yield lost a year without feeding.");
        NOTES.put("workTimeScale", "x game minutes per garden job.");
    }

    public static final PresetConfig<GardenTuning> GARDEN = new PresetConfig<>(GardenTuning.class, p -> switch (p) {
        case GENTLE -> GardenTuning.Preset.GENTLE.tuning;
        case STANDARD -> GardenTuning.Preset.STANDARD.tuning;
        case BRUTAL -> GardenTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    private static ModConfigSpec.DoubleValue frost;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("The garden: beds, crops, germination, frost (Land and seasons in play > Crop farming in play).").push("garden");
        GARDEN.define(b);
        frost = b.comment("Added to every night low over a bed, in C. 0 follows the preset (Gentle +1, Standard 0, Brutal -1).")
                .defineInRange("frost_offset_c", 0.0, -5.0, 5.0);
        b.pop();
    }

    public static GardenTuning tuning() { return GARDEN.tuning(); }

    public static double frostOffsetC() {
        if (frost == null || !YoiConfig.SPEC.isLoaded()) return 0.0;
        double v = frost.get();
        if (v != 0.0) return v;
        return switch (YoiConfig.PRESET.get()) {
            case GENTLE -> GardenTuning.Preset.GENTLE.frostOffsetC;
            case STANDARD -> GardenTuning.Preset.STANDARD.frostOffsetC;
            case BRUTAL -> GardenTuning.Preset.BRUTAL.frostOffsetC;
        };
    }
}

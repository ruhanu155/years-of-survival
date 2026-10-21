package com.yearofice.world.longterm;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.world.LongWorldTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [longworld]} section of {@code yearofice-common.toml}: wildfire years, regrowth and decay, with presets. */
public final class LongWorldConfig {
    private LongWorldConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("wildfireYearPct", "Percent of years drawn as wildfire years.");
        NOTES.put("escalateWildfirePct", "Percent chance a lit tree becomes a wildfire in a wildfire year.");
        NOTES.put("escalateExtremePct", "Percent chance a lit tree becomes a wildfire at Extreme danger.");
        NOTES.put("regrowthTimeScale", "x the regrowth calendar (above 1 is slower).");
        NOTES.put("decayRateScale", "x every rot rate of an abandoned camp.");
    }

    public static final PresetConfig<LongWorldTuning> LONG_WORLD = new PresetConfig<>(LongWorldTuning.class, p -> switch (p) {
        case GENTLE -> LongWorldTuning.Preset.GENTLE.tuning;
        case STANDARD -> LongWorldTuning.Preset.STANDARD.tuning;
        case BRUTAL -> LongWorldTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static void define(ModConfigSpec.Builder b) {
        b.comment("The long world: regrowth, wildfire, beaver ponds and old camps (Land and seasons in play).").push("longworld");
        LONG_WORLD.define(b);
        b.pop();
    }

    public static LongWorldTuning tuning() { return LONG_WORLD.tuning(); }
}

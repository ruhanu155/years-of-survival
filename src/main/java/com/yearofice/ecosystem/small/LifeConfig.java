package com.yearofice.ecosystem.small;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.life.LifeTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [life]} section of {@code yearofice-common.toml}: one key per {@link LifeTuning} field, with Gentle, Standard and Brutal presets. */
public final class LifeConfig {
    private LifeConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("lemmingBubbleM", "Lemmings and voles exist as bodies only within this many metres of a player.");
        NOTES.put("insectRangeM", "Biting-insect swarm entities exist only within this many metres of a player.");
        NOTES.put("insectCap", "Swarm entities alive near the players at once.");
        NOTES.put("biteIntervalS", "Game seconds between bites at a bug pressure of 1.");
        NOTES.put("biteScale", "Multiplier on the Condition one bite takes.");
        NOTES.put("chorusVoices", "The dawn chorus mixes at most this many voices.");
        NOTES.put("oxygenStartMgL", "Dissolved oxygen at freeze-over (mg/L).");
        NOTES.put("oxygenDeathBelowMgL", "Fish begin to die below this oxygen (mg/L).");
        NOTES.put("oxygenDeathPerMg", "Share of a stock that dies a day per mg/L below the line.");
        NOTES.put("oxygenDemandMult", "Scales every water type's winter oxygen demand.");
        NOTES.put("salmonDieoffDays", "Spawned-out salmon die this many days after their run's peak.");
        NOTES.put("salmonCarcassMax", "Spent salmon bodies shown at one spot at once.");
        NOTES.put("waveJitterDays", "A species draws its spring arrival within this many days of its mean.");
        NOTES.put("winterkillCarcassMax", "Winterkill bodies shown at one spot at breakup.");
    }

    public static final PresetConfig<LifeTuning> LIFE = new PresetConfig<>(LifeTuning.class, p -> switch (p) {
        case GENTLE -> LifeTuning.Preset.GENTLE.tuning;
        case STANDARD -> LifeTuning.Preset.STANDARD.tuning;
        case BRUTAL -> LifeTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Life of the year: M7 lemmings, biting insects, the dawn chorus, migration and winterkill.").push("life");
        LIFE.define(b);
        b.pop();
    }

    public static LifeTuning tuning() { return LIFE.tuning(); }

    public static void invalidate() { LIFE.invalidate(); }
}

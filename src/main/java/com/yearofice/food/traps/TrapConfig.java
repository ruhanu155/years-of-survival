package com.yearofice.food.traps;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.ecosystem.TrapTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [traps]} section of {@code yearofice-common.toml} (Blueprint › Traps). Rolls fall at the sun's dawn and
 * dusk from the clock; the retired keys {@code dawn_min} and {@code dusk_min} are dropped from an older file on load.
 */
public final class TrapConfig {
    private TrapConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("hareP", "p_s: a hare's catch chance per roll in a trail snare.");
        NOTES.put("trailMult", "Catch multiplier for a snare on a trail.");
        NOTES.put("coverMult", "Catch multiplier beside cover.");
        NOTES.put("funnelMult", "Catch multiplier for a funnel of brush, sticks or logs on both sides.");
        NOTES.put("placeCap", "Place multipliers multiply but cap at this.");
        NOTES.put("baitMult", "Catch multiplier when baited.");
        NOTES.put("scentFloor", "Scent multipliers never fall below this.");
        NOTES.put("theftAfterH", "A catch left this many game hours may be taken.");
        NOTES.put("theftPerDayPct", "Then this % chance at each later dawn.");
        NOTES.put("catchOffsetMaxMin", "A catch happens 0 to this many game minutes after its dawn or dusk roll.");
        NOTES.put("wireCatches", "A wire snare lasts this many catches.");
        NOTES.put("respawnDays", "A hare caught from a snare's range is replaced after this many game days.");
        NOTES.put("tellRangeM", "A snared hare is heard within this many metres when it is caught.");
        NOTES.put("setMinutes", "Game minutes setting a snare takes (time-lapse).");
    }

    public static final PresetConfig<TrapTuning> TRAPS = new PresetConfig<>(TrapTuning.class, p -> switch (p) {
        case GENTLE -> TrapTuning.Preset.GENTLE.tuning;
        case STANDARD -> TrapTuning.Preset.STANDARD.tuning;
        case BRUTAL -> TrapTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Traps: the trail snare's dawn and dusk rolls, wear and theft (Blueprint > Traps).").push("traps");
        TRAPS.define(b);
        b.pop();
    }

    public static TrapTuning tuning() { return TRAPS.tuning(); }
}

package com.yearofice.food;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.food.RackTuning;
import com.yearofice.sim.food.RoastTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [cooking]} section of {@code yearofice-common.toml} (Blueprint › Cooking). */
public final class FoodConfig {
    private FoodConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("spitMinPerKg", "Spit or stick roast: game minutes per kg of meat.");
        NOTES.put("spitWholeHareMin", "Spit roast: game minutes for a whole hare's meat (a smaller load takes its share).");
        NOTES.put("wholeHareG", "Grams of dressed meat that count as one whole hare.");
        NOTES.put("spitFishMin", "Spit roast: game minutes for a whole fish under spit_fish_max_g.");
        NOTES.put("spitFishMaxG", "Whole fish at or over this weight (g) roast at the per-kg rate.");
        NOTES.put("spitCapacityG", "The most a spit holds at once, in grams (one whole fish always fits).");
        NOTES.put("speedLow", "Cooking speed on a Low fire (1 = the table's cook time).");
        NOTES.put("speedSteady", "Cooking speed on a Steady fire.");
        NOTES.put("speedHot", "Cooking speed on a Hot fire (a Hot fire also burns sooner: 112.5% of cook time).");
        NOTES.put("speedCoals", "Cooking speed over coals.");
        NOTES.put("eatTicks", "Ticks one bite takes to eat (a 100 g unit or one whole piece).");
    }

    public static final PresetConfig<RoastTuning> COOKING = new PresetConfig<>(RoastTuning.class, p -> switch (p) {
        case GENTLE -> RoastTuning.Preset.GENTLE.tuning;
        case STANDARD -> RoastTuning.Preset.STANDARD.tuning;
        case BRUTAL -> RoastTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    private static final Map<String, String> RACK_NOTES = new LinkedHashMap<>();
    static {
        RACK_NOTES.put("dryHours", "Dry game hours a drying-rack batch of 1 cm strips needs.");
        RACK_NOTES.put("rainPenaltyH", "Hours an uncovered rain hour adds to a drying batch.");
        RACK_NOTES.put("rainResetH", "Uncovered rain hours in a row that restart a drying batch.");
        RACK_NOTES.put("smokeMinH", "Shortest hot-smoke run, game hours.");
        RACK_NOTES.put("smokeMaxH", "Longest hot-smoke run, game hours.");
        RACK_NOTES.put("smokeWindMs", "Wind (m/s) above which the smoke rack loses heat and smoke.");
        RACK_NOTES.put("dryCapacityKg", "Fresh strips a drying rack holds, kg.");
        RACK_NOTES.put("smokeCapacityKg", "Meat or fish a smoke rack holds, kg.");
    }

    public static final PresetConfig<RackTuning> RACKS = new PresetConfig<>(RackTuning.class, p -> switch (p) {
        case GENTLE -> RackTuning.Preset.GENTLE.tuning;
        case STANDARD -> RackTuning.Preset.STANDARD.tuning;
        case BRUTAL -> RackTuning.Preset.BRUTAL.tuning;
    }, RACK_NOTES);

    public static RackTuning racks() { return RACKS.tuning(); }

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Cooking: the spit over a fire ring, doneness and eating (Blueprint > Cooking).").push("cooking");
        COOKING.define(b);
        b.pop();
        b.comment("Drying rack and smoke rack (Blueprint > Preserving in detail).").push("racks");
        RACKS.define(b);
        b.pop();
    }

    public static RoastTuning tuning() { return COOKING.tuning(); }
}

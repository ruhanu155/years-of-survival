package com.yearofice.food;

import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.food.BlackSand;
import com.yearofice.sim.food.DeadfallTuning;
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

    private static final Map<String, String> DEADFALL_NOTES = new LinkedHashMap<>();
    static {
        DEADFALL_NOTES.put("bigP", "p_s per animal per roll for the figure-4 deadfall's target.");
        DEADFALL_NOTES.put("bigN", "Animals of the target in range of a figure-4 deadfall.");
        DEADFALL_NOTES.put("mouseP", "p_s per mouse per winter-night roll of a mouse deadfall.");
        DEADFALL_NOTES.put("mouseN", "Mice in range of a mouse deadfall in winter.");
        DEADFALL_NOTES.put("killRight", "Kill chance when the rock fits the animal.");
        DEADFALL_NOTES.put("killHeavy", "Kill chance for an animal heavier than a tenth of the rock.");
        DEADFALL_NOTES.put("slipP", "Chance the rock slips while setting, at practice 0.");
        DEADFALL_NOTES.put("sticksLife", "Triggers the deadfall's sticks last; the rock never wears out.");
        DEADFALL_NOTES.put("soundRangeM", "Metres a falling deadfall rock is heard.");
        DEADFALL_NOTES.put("mouseSoundRangeM", "Metres a mouse deadfall's stone is heard.");
    }

    public static final PresetConfig<DeadfallTuning> DEADFALL = new PresetConfig<>(DeadfallTuning.class, p -> switch (p) {
        case GENTLE -> DeadfallTuning.Preset.GENTLE.tuning;
        case STANDARD -> DeadfallTuning.Preset.STANDARD.tuning;
        case BRUTAL -> DeadfallTuning.Preset.BRUTAL.tuning;
    }, DEADFALL_NOTES);

    private static final Map<String, String> PAN_NOTES = new LinkedHashMap<>();
    static {
        PAN_NOTES.put("richKgH", "Black sand per game hour from a gold pan on placer gravel.");
        PAN_NOTES.put("plainKgH", "Black sand per game hour from a gold pan on any other gravel.");
        PAN_NOTES.put("birchSpeed", "A birch pan's speed against the gold pan.");
        PAN_NOTES.put("sluiceMult", "A sluice box against the pan.");
        PAN_NOTES.put("firstMonth", "First month of open water for panning.");
        PAN_NOTES.put("lastMonth", "Last month of open water for panning.");
    }

    public static final PresetConfig<BlackSand> PAN = new PresetConfig<>(BlackSand.class, p -> BlackSand.standard(), PAN_NOTES);

    public static DeadfallTuning deadfall() { return DEADFALL.tuning(); }

    public static BlackSand pan() { return PAN.tuning(); }

    public static RackTuning racks() { return RACKS.tuning(); }

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Cooking: the spit over a fire ring, doneness and eating (Blueprint > Cooking).").push("cooking");
        COOKING.define(b);
        b.pop();
        b.comment("Drying rack and smoke rack (Blueprint > Preserving in detail).").push("racks");
        RACKS.define(b);
        b.pop();
        b.comment("Deadfalls: the figure-4 and the mouse deadfall (Blueprint > Traps).").push("deadfall");
        DEADFALL.define(b);
        b.pop();
        b.comment("Black sand in the pan (video 2, A10).").push("pan");
        PAN.define(b);
        b.pop();
    }

    public static RoastTuning tuning() { return COOKING.tuning(); }
}

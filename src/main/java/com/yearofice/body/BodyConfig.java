package com.yearofice.body;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.body.BodyTuning;
import java.util.EnumMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [body]} table of {@code yearofice-common.toml} (Blueprint › Realism presets; Body tab › Tuning and
 * debug). Each preset has its own sub-table with every body dial ({@code [body.gentle]}, {@code [body.standard]},
 * {@code [body.brutal]}), so the presets themselves are editable; {@code general.preset} picks the column. A dial
 * set in {@code [body]} itself (anything but −1) overrides the preset's value, which is what the Blueprint calls a
 * "Custom" preset.
 */
public final class BodyConfig {
    private BodyConfig() {}

    private static final String[] KEYS = {"calorie_burn", "kcal_per_kg", "cold", "water_need", "unboiled_illness",
            "bleed_scale", "infection_scale", "stomach_kg", "sos_bmi", "return_bmi", "sip_l"};
    private static final String[] COMMENTS = {
            "Calorie burn: x on every activity rate (0.25-1.5).",
            "kcal per kg of body weight change (RECONCILE 135: 2,400; Gentle 3,500).",
            "Cold: x on warmth loss (0.25-1.5).",
            "Water need: x on every water drain (0.5-1.5).",
            "Chance one drink of untreated water makes the player ill (0-0.5).",
            "Bleeding: x Condition a game minute (M4).",
            "Infection: x stage speed (M4).",
            "Stomach capacity, kg.",
            "BMI at or below which the Trail Messenger sends SOS.",
            "BMI the player returns at after a flight out.",
            "Litres in one drink from open water."};

    private static ModConfigSpec.DoubleValue[] overrides;
    private static ModConfigSpec.ConfigValue<String> digitLossOverride;
    private static final Map<YoiConfig.Preset, ModConfigSpec.DoubleValue[]> PRESET_VALUES =
            new EnumMap<>(YoiConfig.Preset.class);
    private static final Map<YoiConfig.Preset, ModConfigSpec.BooleanValue> PRESET_DIGITS =
            new EnumMap<>(YoiConfig.Preset.class);

    private static double[] values(BodyTuning t) {
        return new double[] {t.calorieBurn(), t.kcalPerKg(), t.coldScale(), t.waterNeed(), t.unboiledIllness(),
                t.bleedScale(), t.infectionScale(), t.stomachKg(), t.sosBmi(), t.returnBmi(), t.sipLitres()};
    }

    /** Called from {@link YoiConfig}'s builder, so the table lives in the one common config file. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Body: calories, weight, energy, warmth and water. -1 takes the active preset's value from the",
                "matching sub-table below; any other number overrides it (a 'Custom' preset).").push("body");
        overrides = new ModConfigSpec.DoubleValue[KEYS.length];
        for (int i = 0; i < KEYS.length; i++) {
            overrides[i] = b.comment(COMMENTS[i]).defineInRange(KEYS[i], -1.0, -1.0, 10_000.0);
        }
        digitLossOverride = b.comment("Deep frostbite can take digits: preset, true or false (M4).")
                .define("digit_loss", "preset");
        for (YoiConfig.Preset p : YoiConfig.Preset.values()) {
            BodyTuning t = BodyTuning.of(p.name());
            double[] v = values(t);
            b.comment("The " + p.name().toLowerCase(java.util.Locale.ROOT) + " preset's body dials.")
                    .push(p.name().toLowerCase(java.util.Locale.ROOT));
            ModConfigSpec.DoubleValue[] vals = new ModConfigSpec.DoubleValue[KEYS.length];
            for (int i = 0; i < KEYS.length; i++) vals[i] = b.defineInRange(KEYS[i], v[i], 0.0, 10_000.0);
            PRESET_VALUES.put(p, vals);
            PRESET_DIGITS.put(p, b.define("digit_loss", t.digitLoss()));
            b.pop();
        }
        b.pop();
    }

    /** The tuning in force: Standard until the config file has loaded. */
    public static BodyTuning tuning() {
        if (overrides == null || !YoiConfig.SPEC.isLoaded()) return BodyTuning.STANDARD;
        YoiConfig.Preset p = YoiConfig.PRESET.get();
        ModConfigSpec.DoubleValue[] base = PRESET_VALUES.get(p);
        double[] v = new double[KEYS.length];
        boolean custom = false;
        for (int i = 0; i < KEYS.length; i++) {
            double o = overrides[i].get();
            custom |= o >= 0;
            v[i] = o >= 0 ? o : base[i].get();
        }
        String d = digitLossOverride.get();
        boolean digits = "preset".equals(d) ? PRESET_DIGITS.get(p).get() : Boolean.parseBoolean(d);
        custom |= !"preset".equals(d);
        String name = p.name().toLowerCase(java.util.Locale.ROOT) + (custom ? "+custom" : "");
        return new BodyTuning(name, v[0], v[1], v[2], v[3], v[4], v[5], v[6], digits, v[7], v[8], v[9], v[10]);
    }
}

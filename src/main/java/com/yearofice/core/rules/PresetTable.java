package com.yearofice.core.rules;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * One table of {@code yearofice-common.toml} in the body's layout: {@code [<section>]} holds a −1 override per
 * dial, and {@code [<section>.gentle|standard|brutal]} hold each preset's own values; {@code general.preset} picks
 * the column (Blueprint › Realism presets). The shared layers' tables use it so every tuning number has a home.
 */
public final class PresetTable {
    private final String section;
    private final Map<String, Dial> dials = new LinkedHashMap<>();
    private final Map<String, ModConfigSpec.DoubleValue> overrides = new LinkedHashMap<>();
    private final Map<YoiConfig.Preset, Map<String, ModConfigSpec.DoubleValue>> presets = new EnumMap<>(YoiConfig.Preset.class);

    private record Dial(String comment, double gentle, double standard, double brutal, double min, double max) {}

    public PresetTable(String section) { this.section = section; }

    /** Adds a dial with its three preset values. */
    public PresetTable dial(String key, String comment, double gentle, double standard, double brutal, double min, double max) {
        dials.put(key, new Dial(comment, gentle, standard, brutal, min, max));
        return this;
    }

    /** A dial with one value in every preset. */
    public PresetTable dial(String key, String comment, double all, double min, double max) {
        return dial(key, comment, all, all, all, min, max);
    }

    public void define(ModConfigSpec.Builder b, String... sectionComment) {
        b.comment(sectionComment).push(section);
        for (Map.Entry<String, Dial> e : dials.entrySet()) {
            overrides.put(e.getKey(), b.comment(e.getValue().comment(), "-1 takes the active preset's value.")
                    .defineInRange(e.getKey(), -1.0, -1.0, Math.max(e.getValue().max(), 0)));
        }
        for (YoiConfig.Preset p : YoiConfig.Preset.values()) {
            b.push(p.name().toLowerCase(Locale.ROOT));
            Map<String, ModConfigSpec.DoubleValue> vals = new LinkedHashMap<>();
            for (Map.Entry<String, Dial> e : dials.entrySet()) {
                Dial d = e.getValue();
                double v = switch (p) {
                    case GENTLE -> d.gentle();
                    case STANDARD -> d.standard();
                    case BRUTAL -> d.brutal();
                };
                vals.put(e.getKey(), b.defineInRange(e.getKey(), v, d.min(), d.max()));
            }
            presets.put(p, vals);
            b.pop();
        }
        b.pop();
    }

    /** The value in force; the Standard default until the config file has loaded (GameTests run on Standard). */
    public double get(String key) {
        Dial d = dials.get(key);
        if (d == null) throw new IllegalArgumentException(section + "." + key);
        if (overrides.isEmpty() || !YoiConfig.SPEC.isLoaded()) return d.standard();
        double o = overrides.get(key).get();
        if (o >= 0) return o;
        return presets.get(YoiConfig.PRESET.get()).get(key).get();
    }
}

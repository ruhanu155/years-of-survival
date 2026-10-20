package com.yearofice.hunt;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.hunt.HuntPreset;
import com.yearofice.sim.hunt.HuntTuning;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [hunt]} section of {@code yearofice-common.toml}: one key per {@link HuntTuning} field, Standard by default; untouched keys follow the world's realism preset. */
public final class HuntConfig {
    private HuntConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("gravityMps2", "Arrow gravity (m/s2).");
        NOTES.put("dragPerS", "Arrow drag toward the moving air (1/s); this is how wind pushes a shaft.");
        NOTES.put("swayCmAt20m", "Aim sway radius at 20 m before modifiers (cm).");
        NOTES.put("holdBoltsPerS", "Energy a held draw costs (bolts a second).");
        NOTES.put("rangeFalloffPerM", "Share of arrow energy lost per metre past the effective range.");
        NOTES.put("cappedDrawPct", "Full draw cap (0-1) when Strength is under the bow's requirement.");
        NOTES.put("lowEnergyDrawX", "Draw time x this below 3 bolts of energy.");
        NOTES.put("bloodLifeMin", "Game minutes a blood drop lasts.");
        NOTES.put("bloodRainX", "Blood life x this in rain.");
        NOTES.put("bloodSnowX", "Blood life x this in snow.");
        NOTES.put("pushExtraDeathMin", "Game minutes added to a pushed animal's death timer.");
        NOTES.put("pushTrailX", "Blood drops x this after the animal is pushed.");
        NOTES.put("callRutPeak", "Cow moan base odds at the rut peak.");
        NOTES.put("blindSightX", "Sight range factor against a still player in a ground blind.");
        NOTES.put("blindFreshSightX", "Sight factor for a blind under one game day old.");
        NOTES.put("standPreySightX", "Tree stand: prey see the player at this factor.");
        NOTES.put("standBearSightX", "Tree stand: bears see the player at this factor.");
        NOTES.put("standScentClearM", "Tree stand: scent does not reach the ground within this many metres.");
        NOTES.put("bluffShare", "Share of an unwounded black bear's rushes that are bluffs.");
    }

    private static final List<RecordComponent> FIELDS = List.of(HuntTuning.class.getRecordComponents());
    private static final List<ModConfigSpec.ConfigValue<? extends Number>> VALUES = new ArrayList<>();
    private static volatile HuntTuning cached;
    private static volatile YoiConfig.Preset cachedFor;

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Hunting: weapons, the hit-zone shot, blood trails, calls, blinds and bears (Blueprint > Hunting in depth).").push("hunt");
        HuntTuning std = HuntPreset.STANDARD.tuning;
        for (RecordComponent c : FIELDS) {
            Number s = value(c, std);
            String comment = NOTES.getOrDefault(c.getName(), c.getName()) + " Gentle " + value(c, HuntPreset.GENTLE.tuning)
                    + ", Standard " + s + ", Brutal " + value(c, HuntPreset.BRUTAL.tuning) + ".";
            String key = snake(c.getName());
            if (c.getType() == int.class) VALUES.add(b.comment(comment).defineInRange(key, s.intValue(), 0, 1_000_000));
            else VALUES.add(b.comment(comment).defineInRange(key, s.doubleValue(), 0, 1_000_000));
        }
        b.pop();
    }

    /** The tuning in force: config values, with untouched keys following the world's preset. Standard when unloaded. */
    public static HuntTuning tuning() {
        if (VALUES.isEmpty() || !YoiConfig.SPEC.isLoaded()) return HuntTuning.standard();
        YoiConfig.Preset p = YoiConfig.PRESET.get();
        HuntTuning t = cached;
        if (t != null && cachedFor == p) return t;
        HuntTuning std = HuntPreset.STANDARD.tuning;
        HuntTuning preset = switch (p) {
            case GENTLE -> HuntPreset.GENTLE.tuning;
            case STANDARD -> std;
            case BRUTAL -> HuntPreset.BRUTAL.tuning;
        };
        Object[] args = new Object[FIELDS.size()];
        Class<?>[] types = new Class<?>[FIELDS.size()];
        for (int i = 0; i < FIELDS.size(); i++) {
            RecordComponent c = FIELDS.get(i);
            Number v = VALUES.get(i).get();
            Number s = value(c, std);
            if (v.doubleValue() == s.doubleValue()) v = value(c, preset);
            types[i] = c.getType();
            args[i] = c.getType() == int.class ? (Object) v.intValue() : (Object) v.doubleValue();
        }
        try {
            t = HuntTuning.class.getDeclaredConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("HuntTuning constructor", e);
        }
        cached = t;
        cachedFor = p;
        return t;
    }

    /** Drops the cached tuning after a config load or reload. */
    public static void invalidate() { cached = null; }

    private static Number value(RecordComponent c, HuntTuning t) {
        try {
            return (Number) c.getAccessor().invoke(t);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(c.getName(), e);
        }
    }

    static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char ch : camel.toCharArray()) {
            if (Character.isUpperCase(ch)) sb.append('_').append(Character.toLowerCase(ch));
            else sb.append(ch);
        }
        return sb.toString();
    }
}

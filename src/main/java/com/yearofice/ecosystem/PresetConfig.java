package com.yearofice.ecosystem;

import com.yearofice.core.rules.YoiConfig;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * One {@code yearofice-common.toml} section built from a tuning record, as the fire section is: one key per
 * record field (snake_case), defaulting to Standard. While a key still holds the Standard number, Gentle and Brutal
 * worlds read their preset's number instead; a moved slider wins.
 */
public final class PresetConfig<T extends Record> {
    private final Class<T> type;
    private final Function<YoiConfig.Preset, T> preset;
    private final Map<String, String> notes;
    private final List<RecordComponent> fields;
    private final List<ModConfigSpec.ConfigValue<? extends Number>> values = new ArrayList<>();
    private volatile T cached;
    private volatile YoiConfig.Preset cachedFor;

    public PresetConfig(Class<T> type, Function<YoiConfig.Preset, T> preset, Map<String, String> notes) {
        this.type = type;
        this.preset = preset;
        this.notes = notes;
        this.fields = List.of(type.getRecordComponents());
    }

    /** Defines the keys inside the section the caller pushed. */
    public void define(ModConfigSpec.Builder b) {
        T std = preset.apply(YoiConfig.Preset.STANDARD);
        for (RecordComponent c : fields) {
            Number s = value(c, std);
            String comment = notes.getOrDefault(c.getName(), c.getName()) + " Gentle " + value(c, preset.apply(YoiConfig.Preset.GENTLE))
                    + ", Standard " + s + ", Brutal " + value(c, preset.apply(YoiConfig.Preset.BRUTAL)) + ".";
            String key = snake(c.getName());
            if (c.getType() == int.class) values.add(b.comment(comment).defineInRange(key, s.intValue(), 0, 1_000_000));
            else values.add(b.comment(comment).defineInRange(key, s.doubleValue(), 0, 1_000_000));
        }
    }

    public T tuning() {
        if (values.isEmpty() || !YoiConfig.SPEC.isLoaded()) return preset.apply(YoiConfig.Preset.STANDARD);
        YoiConfig.Preset p = YoiConfig.PRESET.get();
        T t = cached;
        if (t != null && cachedFor == p) return t;
        T std = preset.apply(YoiConfig.Preset.STANDARD);
        T pre = preset.apply(p);
        Object[] args = new Object[fields.size()];
        Class<?>[] types = new Class<?>[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            RecordComponent c = fields.get(i);
            Number v = values.get(i).get();
            if (v.doubleValue() == value(c, std).doubleValue()) v = value(c, pre);
            types[i] = c.getType();
            args[i] = c.getType() == int.class ? (Object) v.intValue() : (Object) v.doubleValue();
        }
        try {
            t = type.getDeclaredConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(type.getSimpleName() + " constructor", e);
        }
        cached = t;
        cachedFor = p;
        return t;
    }

    public void invalidate() { cached = null; }

    private static Number value(RecordComponent c, Object t) {
        try {
            return (Number) c.getAccessor().invoke(t);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(c.getName(), e);
        }
    }

    public static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char ch : camel.toCharArray()) {
            if (Character.isUpperCase(ch)) sb.append('_').append(Character.toLowerCase(ch));
            else sb.append(ch);
        }
        return sb.toString();
    }
}

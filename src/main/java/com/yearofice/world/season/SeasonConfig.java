package com.yearofice.world.season;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.world.IceTuning;
import com.yearofice.sim.world.SeasonPreset;
import com.yearofice.sim.world.SeasonTuning;
import com.yearofice.sim.world.SnowTuning;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [season]}, {@code [ice]} and {@code [snow]} sections of {@code yearofice-common.toml}: one key per
 * tuning field (snake_case), Standard by default; a key still at the Standard number follows the world's realism
 * preset (Gentle or Brutal), a moved slider wins.
 */
public final class SeasonConfig {
    private SeasonConfig() {}

    private static final List<RecordComponent> ICE = List.of(IceTuning.class.getRecordComponents());
    private static final List<RecordComponent> SNOW = List.of(SnowTuning.class.getRecordComponents());
    private static final List<RecordComponent> SEASON = List.of(SeasonTuning.class.getRecordComponents());
    private static final List<ModConfigSpec.ConfigValue<? extends Number>> ICE_V = new ArrayList<>(), SNOW_V = new ArrayList<>(),
            SEASON_V = new ArrayList<>();

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        section(b, "season", "Calendar colours, ground freeze and the terrain queue's budgets (days are days of the year, 0 = May 1).",
                SEASON, SEASON_V, SeasonPreset.STANDARD.season, SeasonPreset.GENTLE.season, SeasonPreset.BRUTAL.season);
        section(b, "ice", "Degree-day ice per water body class: onset, Stefan growth, melt, rot, breakup, flood and ice holes.",
                ICE, ICE_V, SeasonPreset.STANDARD.ice, SeasonPreset.GENTLE.ice, SeasonPreset.BRUTAL.ice);
        section(b, "snow", "Snow pack: fall, settling, melt, crust and the four snow classes.",
                SNOW, SNOW_V, SeasonPreset.STANDARD.snow, SeasonPreset.GENTLE.snow, SeasonPreset.BRUTAL.snow);
    }

    private static void section(ModConfigSpec.Builder b, String name, String comment, List<RecordComponent> fields,
                                List<ModConfigSpec.ConfigValue<? extends Number>> out, Object std, Object gentle, Object brutal) {
        b.comment(comment).push(name);
        for (RecordComponent c : fields) {
            Number s = value(c, std);
            String note = c.getName() + ". Gentle " + value(c, gentle) + ", Standard " + s + ", Brutal " + value(c, brutal) + ".";
            String key = snake(c.getName());
            if (c.getType() == int.class) out.add(b.comment(note).defineInRange(key, s.intValue(), -1_000_000, 1_000_000));
            else out.add(b.comment(note).defineInRange(key, s.doubleValue(), -1_000_000.0, 1_000_000.0));
        }
        b.pop();
    }

    private static volatile IceTuning iceC;
    private static volatile SnowTuning snowC;
    private static volatile SeasonTuning seasonC;
    private static volatile YoiConfig.Preset cachedFor;

    /** Test and debug hook: forces tuning (null clears). */
    public static void force(IceTuning i, SnowTuning s, SeasonTuning t) { iceC = i; snowC = s; seasonC = t; cachedFor = null; forced = i != null; }

    private static volatile boolean forced;

    private static YoiConfig.Preset preset() {
        try { return YoiConfig.PRESET.get(); } catch (IllegalStateException | NullPointerException e) { return YoiConfig.Preset.STANDARD; }
    }

    private static void refresh() {
        if (forced) return;
        YoiConfig.Preset p = preset();
        if (cachedFor == p && iceC != null) return;
        SeasonPreset sp = SeasonPreset.valueOf(p.name());
        iceC = build(IceTuning.class, ICE, ICE_V, SeasonPreset.STANDARD.ice, sp.ice);
        snowC = build(SnowTuning.class, SNOW, SNOW_V, SeasonPreset.STANDARD.snow, sp.snow);
        seasonC = build(SeasonTuning.class, SEASON, SEASON_V, SeasonPreset.STANDARD.season, sp.season);
        cachedFor = p;
    }

    public static IceTuning ice() { refresh(); return iceC; }

    public static SnowTuning snow() { refresh(); return snowC; }

    public static SeasonTuning season() { refresh(); return seasonC; }

    @SuppressWarnings("unchecked")
    private static <T> T build(Class<T> type, List<RecordComponent> fields, List<ModConfigSpec.ConfigValue<? extends Number>> vals,
                               Object std, Object preset) {
        try {
            Object[] args = new Object[fields.size()];
            for (int i = 0; i < args.length; i++) {
                RecordComponent c = fields.get(i);
                Number cfg;
                try { cfg = vals.get(i).get(); } catch (IllegalStateException e) { cfg = value(c, std); }
                Number s = value(c, std);
                Number use = cfg.doubleValue() == s.doubleValue() ? value(c, preset) : cfg;
                args[i] = c.getType() == int.class ? (Object) use.intValue() : (Object) use.doubleValue();
            }
            Constructor<?> k = type.getDeclaredConstructors()[0];
            return (T) k.newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Number value(RecordComponent c, Object rec) {
        try { return (Number) c.getAccessor().invoke(rec); } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    private static String snake(String camel) {
        StringBuilder sb = new StringBuilder();
        for (char ch : camel.toCharArray()) {
            if (Character.isUpperCase(ch)) sb.append('_').append(Character.toLowerCase(ch)); else sb.append(ch);
        }
        return sb.toString();
    }
}

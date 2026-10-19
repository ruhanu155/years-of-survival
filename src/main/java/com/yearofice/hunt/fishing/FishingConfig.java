package com.yearofice.hunt.fishing;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.fishing.FishingPreset;
import com.yearofice.sim.fishing.FishingTuning;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [fishing]} section of {@code yearofice-common.toml}. One key per {@link FishingTuning} field (its name
 * in snake_case), defaulting to the Standard value. As with {@code [fire]}, the realism preset sets every slider's
 * default: while a key still holds the Standard number, Gentle and Brutal worlds read their preset's number; a moved
 * slider wins.
 */
public final class FishingConfig {
    private FishingConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("spotQ", "Quality q of the slice's fishing spot (creek mouth, deep, below the lake, r 1.2).");
        NOTES.put("spotMinDepthM", "Slice spot rule: every column of the square at least this deep (m).");
        NOTES.put("spotMinWidth", "Slice spot rule: water at least this many blocks across both ways.");
        NOTES.put("spotRadiusM", "The spot's radius (m).");
        NOTES.put("graylingCapacity", "Grayling a creek mouth holds at q = 1.");
        NOTES.put("spawnCapacityX", "Creek-mouth capacity x this during the grayling spawn (Apr 25 - May 31).");
        NOTES.put("stockX", "Stock capacity x this (Realism presets > Animal numbers).");
        NOTES.put("baitDepthM", "Depth the bobber rig hangs the bait (m); over 2 m draws bigger fish.");
        NOTES.put("wormBites", "Bites a worm lasts on the hook.");
        NOTES.put("wormMinutes", "Game minutes a worm lasts in the water.");
        NOTES.put("castRangeM", "Willow rod cast range at full charge (m).");
        NOTES.put("castChargeS", "Real seconds of right mouse for a full cast.");
        NOTES.put("castScatterPct", "Cast lands scattered +/- this % in range.");
        NOTES.put("castScatterDeg", "Cast lands scattered +/- this many degrees.");
        NOTES.put("strikeGraceS", "Seconds the server adds to the hook's strike window for the network round trip.");
        NOTES.put("missBaitLossPct", "A strike outside the window leaves the bait gone this % of the time.");
        NOTES.put("spookMin", "Game minutes a strike at a nibble spooks the spot (bites x0.5).");
        NOTES.put("nibbleShare", "Chance of a false nibble in a minute with no bite, as a share of P_bite.");
        NOTES.put("leadMinS", "Least real seconds the rod tip twitches before a bite's dip.");
        NOTES.put("leadMaxS", "Most real seconds the rod tip twitches before a bite's dip.");
        NOTES.put("nibbleS", "Real seconds a nibble shakes the bobber.");
        NOTES.put("lineWearPct", "Line condition % one fight costs (x3 if the arc reached red).");
        NOTES.put("snapWearPct", "Line condition % a snap costs (the line is re-tied from the winder).");
        NOTES.put("lineWetMin", "Game minutes in use before the line is wet (rain wets it at once).");
        NOTES.put("closeClockRate", "World clock share during a fight (the close clock).");
        NOTES.put("inputTimeoutTicks", "The server steps idle input for a client this many ticks late.");
        NOTES.put("assist", "1 turns on the fishing assist: safe band shaded, side pressure set, fish tire 30% faster.");
        NOTES.put("wormDigMax", "Worms one broken riparian soil block may give (0 to this).");
        NOTES.put("wormDigRange", "Soil within this many blocks of water is riparian.");
        NOTES.put("waterTempC", "Water temperature until the environment field lands (C).");
        NOTES.put("turbidity", "Turbidity until the environment field lands (0 clear to 1 muddy).");
    }

    private static final List<RecordComponent> FIELDS = List.of(FishingTuning.class.getRecordComponents());
    private static final List<ModConfigSpec.ConfigValue<? extends Number>> VALUES = new ArrayList<>();
    private static volatile FishingTuning cached;
    private static volatile YoiConfig.Preset cachedFor;

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Fishing: the rod, the bite at the spot and the fight (Blueprint > Fishing in depth).").push("fishing");
        FishingTuning std = FishingPreset.STANDARD.tuning;
        for (RecordComponent c : FIELDS) {
            Number s = value(c, std);
            String comment = NOTES.getOrDefault(c.getName(), c.getName()) + " Gentle " + value(c, FishingPreset.GENTLE.tuning)
                    + ", Standard " + s + ", Brutal " + value(c, FishingPreset.BRUTAL.tuning) + ".";
            String key = snake(c.getName());
            if (c.getType() == int.class) VALUES.add(b.comment(comment).defineInRange(key, s.intValue(), 0, 1_000_000));
            else VALUES.add(b.comment(comment).defineInRange(key, s.doubleValue(), 0, 1_000_000));
        }
        b.pop();
    }

    /** The tuning in force: config values, with untouched keys following the world's preset. Standard when unloaded. */
    public static FishingTuning tuning() {
        if (VALUES.isEmpty() || !YoiConfig.SPEC.isLoaded()) return FishingTuning.standard();
        YoiConfig.Preset p = YoiConfig.PRESET.get();
        FishingTuning t = cached;
        if (t != null && cachedFor == p) return t;
        FishingTuning std = FishingPreset.STANDARD.tuning;
        FishingTuning preset = switch (p) {
            case GENTLE -> FishingPreset.GENTLE.tuning;
            case STANDARD -> std;
            case BRUTAL -> FishingPreset.BRUTAL.tuning;
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
            t = FishingTuning.class.getDeclaredConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FishingTuning constructor", e);
        }
        cached = t;
        cachedFor = p;
        return t;
    }

    /** Drops the cached tuning after a config load or reload. */
    public static void invalidate() { cached = null; }

    private static Number value(RecordComponent c, FishingTuning t) {
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

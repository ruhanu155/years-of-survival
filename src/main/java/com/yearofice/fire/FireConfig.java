package com.yearofice.fire;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.fire.FirePreset;
import com.yearofice.sim.fire.FireTuning;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [fire]} section of {@code yearofice-common.toml}. One key per {@link FireTuning} field (its name in
 * snake_case), defaulting to the Standard value. The realism preset sets every slider's default: while a key still
 * holds the Standard number, Gentle and Brutal worlds read their preset's number instead; a moved slider wins.
 * Wind at a fire comes from the Environment field ({@link FireEnv}); the M2.5 stand-in keys {@code calm_wind_ms},
 * {@code rain_wind_ms} and {@code storm_wind_ms} are retired, and NeoForge drops them from an older file on load.
 */
public final class FireConfig {
    private FireConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("ringArmloadMin", "Plain ring: game minutes one armload of a x1.0 wood burns at Steady (wood table multiplies it).");
        NOTES.put("armloadCatchMin", "Game minutes for an armload to catch from a small fire or coals.");
        NOTES.put("emberLifeS", "Real seconds an ember or match flame lives before it dies.");
        NOTES.put("emberWindMs", "Wind (m/s) over which the ember's life is halved unless the player is crouched in shelter.");
        NOTES.put("tinderFlameMin", "Game minutes a tinder flame lasts.");
        NOTES.put("smallFireMin", "Game minutes a small fire lasts on its kindling.");
        NOTES.put("coalsMin", "Game minutes coals last unfed.");
        NOTES.put("bankedMin", "Game minutes banked coals keep.");
        NOTES.put("kindlingNeeded", "Kindling pieces a lay needs before the tinder catches it.");
        NOTES.put("heavyRainStepMin", "Heavy rain drops an uncovered fire one state per this many game minutes.");
        NOTES.put("lightRainStepMin", "Light rain drops an uncovered fire one state per this many game minutes.");
        NOTES.put("snowLowOutMin", "Snowfall puts out a Low fire in this many game minutes.");
        NOTES.put("snowBurnFactor", "Snowfall's fuel-use factor on other fires.");
        NOTES.put("windBurnMs", "Wind (m/s) over which an uncovered fire eats fuel faster.");
        NOTES.put("windBurnFactor", "Fuel-use factor in that wind.");
        NOTES.put("tinderMaxMoisture", "Tinder, kindling and the bow-drill board catch only at this moisture % or less.");
        NOTES.put("matchMaxMoisture", "A match above this moisture % never strikes.");
        NOTES.put("matchCalmPct", "Match success % in calm air.");
        NOTES.put("matchWindPct", "Match success % in wind.");
        NOTES.put("matchWindMs", "Wind (m/s) that counts as wind for matches.");
        NOTES.put("drillFreshPct", "Bow-drill success % for a fresh player (Automatic assist and bots).");
        NOTES.put("drillPracticedPct", "Bow-drill success % at Practiced.");
        NOTES.put("drillSkillReps", "Fire skill reps to Practiced (then the drill is one 6 s hold).");
        NOTES.put("drillKcal", "kcal one bow-drill try costs.");
        NOTES.put("drillMinEnergy", "Energy bolts (0-10) below which a bow-drill attempt ends.");
        NOTES.put("drillWindPct", "Bow-drill success x this % in strong wind without shelter.");
        NOTES.put("packedArmloads", "More armloads than this packs a fire too full.");
        NOTES.put("packedHotArmloads", "More armloads than this packs a Hot fire too full.");
        NOTES.put("maxArmloads", "Most armloads a fire ring holds.");
        NOTES.put("rainWetsPerHour", "Moisture points an unlit lay gains per game hour in uncovered rain.");
        NOTES.put("teepeeArmloadMin", "Teepee: game minutes one armload of a x1.0 wood burns at Steady.");
        NOTES.put("longArmloadMin", "Long fire: game minutes one armload of a x1.0 wood burns at Steady.");
        NOTES.put("dakotaArmloadMin", "Dakota hole: game minutes one armload of a x1.0 wood burns at Steady.");
        NOTES.put("ferroCalmPct", "Ferro rod success % in calm air.");
        NOTES.put("ferroWindPct", "Ferro rod success % in wind.");
        NOTES.put("spreadLowPct", "Packed-too-full fire: % chance per game hour to start a flame within 2 m, fire danger Low.");
        NOTES.put("spreadModeratePct", "The same at fire danger Moderate.");
        NOTES.put("spreadHighPct", "The same at fire danger High.");
        NOTES.put("spreadExtremePct", "The same at fire danger Extreme.");
        NOTES.put("spreadAwayM", "A packed fire spreads only while no player is within this many metres.");
        NOTES.put("potTipPct", "% chance a pot set on a burning fire (not coals) tips and spills.");
        NOTES.put("containerSipL", "Litres one drink from a water container takes.");
        NOTES.put("flameBurnMin", "Game minutes a flame burns a piece before it is gone.");
    }

    private static final List<RecordComponent> FIELDS = List.of(FireTuning.class.getRecordComponents());
    private static final List<ModConfigSpec.ConfigValue<? extends Number>> VALUES = new ArrayList<>();
    private static volatile FireTuning cached;
    private static volatile YoiConfig.Preset cachedFor;

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Fire: states, fuel, weather, matches and the bow drill (Blueprint > Fire, water and light).").push("fire");
        FireTuning std = FirePreset.STANDARD.tuning;
        for (RecordComponent c : FIELDS) {
            Number s = value(c, std);
            String comment = NOTES.getOrDefault(c.getName(), c.getName()) + " Gentle " + value(c, FirePreset.GENTLE.tuning)
                    + ", Standard " + s + ", Brutal " + value(c, FirePreset.BRUTAL.tuning) + ".";
            String key = snake(c.getName());
            if (c.getType() == int.class) VALUES.add(b.comment(comment).defineInRange(key, s.intValue(), 0, 1_000_000));
            else VALUES.add(b.comment(comment).defineInRange(key, s.doubleValue(), 0, 1_000_000));
        }
        b.pop();
    }

    /** The tuning in force: config values, with untouched keys following the world's preset. Standard when unloaded. */
    public static FireTuning tuning() {
        if (VALUES.isEmpty() || !YoiConfig.SPEC.isLoaded()) return FireTuning.standard();
        YoiConfig.Preset p = YoiConfig.PRESET.get();
        FireTuning t = cached;
        if (t != null && cachedFor == p) return t;
        FireTuning std = FirePreset.STANDARD.tuning;
        FireTuning preset = switch (p) {
            case GENTLE -> FirePreset.GENTLE.tuning;
            case STANDARD -> std;
            case BRUTAL -> FirePreset.BRUTAL.tuning;
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
            t = FireTuning.class.getDeclaredConstructor(types).newInstance(args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FireTuning constructor", e);
        }
        cached = t;
        cachedFor = p;
        return t;
    }

    /** Drops the cached tuning after a config load or reload. */
    public static void invalidate() { cached = null; }

    private static Number value(RecordComponent c, FireTuning t) {
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

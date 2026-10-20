package com.yearofice.ecosystem;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.food.traps.TrapConfig;
import com.yearofice.sim.ecosystem.EcoTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [ecosystem]} section of {@code yearofice-common.toml}, and through {@link TrapConfig} the
 * {@code [traps]} section. Called once from {@link YoiConfig} while it builds the spec.
 */
public final class EcoConfig {
    private EcoConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("hareDensityPerKm2", "Snowshoe hares per km2 of willow and spruce edge at K (times the hare cycle's year multiplier).");
        NOTES.put("ptarmiganGroupsPerKm2", "Willow ptarmigan groups (2-5 birds) per km2 of open shrub.");
        NOTES.put("mooseDensityPerKm2", "Moose per km2 of riparian willow.");
        NOTES.put("activeRadiusM", "Wild bodies appear within this many metres of a player (Active tier).");
        NOTES.put("demoteRadiusM", "Wild bodies leave beyond this many metres from every player.");
        NOTES.put("activeCap", "Most wild bodies in one level at once.");
        NOTES.put("workMinPerS", "The Work time-lapse: game minutes a real second while a hold-to-work ring fills.");
        NOTES.put("smallGameDensityMult", "Multiplies the species-file densities of the small game, furbearers and grouse (red squirrel to rock ptarmigan).");
        NOTES.put("theftMult", "Multiplies the hourly theft chances at the camp's stores (marten, jay, raven, fox).");
        NOTES.put("minWorkEnergy", "Hold-to-work (dressing, setting a snare) needs at least this many energy bolts.");
    }

    public static final PresetConfig<EcoTuning> ECO = new PresetConfig<>(EcoTuning.class, p -> switch (p) {
        case GENTLE -> EcoTuning.Preset.GENTLE.tuning;
        case STANDARD -> EcoTuning.Preset.STANDARD.tuning;
        case BRUTAL -> EcoTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static ModConfigSpec.BooleanValue MOOSE_HUNTABLE;
    public static ModConfigSpec.IntValue HARE_CYCLE_YEAR;
    public static ModConfigSpec.BooleanValue RACK_THIEVES;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Ecosystem: small game, the passive moose and the slot spawner (Minecraft development > Entities and AI).").push("ecosystem");
        ECO.define(b);
        MOOSE_HUNTABLE = b.comment("Moose take player damage. Off on every preset until big game lands in M6.")
                .define("moose_huntable", false);
        HARE_CYCLE_YEAR = b.comment("Pin the hare cycle to a year 0-9 (multipliers 0.3, 0.5, 0.9, 1.5, 2.2, 2.5, 1.8, 0.9, 0.4, 0.25); -1 draws the phase from the seed.")
                .defineInRange("hare_cycle_year", -1, -1, 9);
        RACK_THIEVES = b.comment("Animals raid the camp's stores and racks (Structures and buildables > Thieves at the rack; RECONCILE 170). On in every preset; "
                + "the world's own copy (yearofice_world) wins once world options land, and /yoi theft toggle flips it at runtime.")
                .define("rack_thieves", true);
        b.pop();
        TrapConfig.define(b);
    }

    public static EcoTuning tuning() { return ECO.tuning(); }

    public static boolean mooseHuntable() { return YoiConfig.SPEC.isLoaded() && MOOSE_HUNTABLE != null && MOOSE_HUNTABLE.get(); }

    public static boolean rackThievesDefault() { return !YoiConfig.SPEC.isLoaded() || RACK_THIEVES == null || RACK_THIEVES.get(); }

    public static int hareCycleYear() { return YoiConfig.SPEC.isLoaded() && HARE_CYCLE_YEAR != null ? HARE_CYCLE_YEAR.get() : -1; }

    public static void invalidate() {
        ECO.invalidate();
        TrapConfig.TRAPS.invalidate();
    }
}

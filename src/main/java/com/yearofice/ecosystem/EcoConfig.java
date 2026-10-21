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
        NOTES.put("bigGameDensityMult", "Multiplies the species-file densities of caribou and the other big game (M6).");
        NOTES.put("predatorDensityMult", "Multiplies the species-file densities of bears, wolves, coyote, fox, lynx and wolverine (M6).");
        NOTES.put("minWorkEnergy", "Hold-to-work (dressing, setting a snare) needs at least this many energy bolts.");
    }

    public static final PresetConfig<EcoTuning> ECO = new PresetConfig<>(EcoTuning.class, p -> switch (p) {
        case GENTLE -> EcoTuning.Preset.GENTLE.tuning;
        case STANDARD -> EcoTuning.Preset.STANDARD.tuning;
        case BRUTAL -> EcoTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    public static ModConfigSpec.BooleanValue MOOSE_HUNTABLE;
    public static ModConfigSpec.IntValue HARE_CYCLE_YEAR;
    public static ModConfigSpec.BooleanValue DEER_FLAG;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Ecosystem: small game, the passive moose and the slot spawner (Minecraft development > Entities and AI).").push("ecosystem");
        ECO.define(b);
        MOOSE_HUNTABLE = b.comment("Moose take player damage. Off on every preset until big game lands in M6.")
                .define("moose_huntable", false);
        HARE_CYCLE_YEAR = b.comment("Pin the hare cycle to a year 0-9 (multipliers 0.3, 0.5, 0.9, 1.5, 2.2, 2.5, 1.8, 0.9, 0.4, 0.25); -1 draws the phase from the seed.")
                .defineInRange("hare_cycle_year", -1, -1, 9);
        DEER_FLAG = b.comment("Content flag yearofice:deer (RECONCILE 152): the mule deer straggler and the deer species. On in dev and beta, off in stable.")
                .define("deer", true);
        b.pop();
        com.yearofice.ecosystem.encounter.EncounterConfig.define(b); // M6 lane D: [world], door entry, talk
        TrapConfig.define(b);
        com.yearofice.ecosystem.companion.CompanionConfig.define(b); // M9 lane A
    }

    public static EcoTuning tuning() { return ECO.tuning(); }

    public static boolean mooseHuntable() { return YoiConfig.SPEC.isLoaded() && MOOSE_HUNTABLE != null && MOOSE_HUNTABLE.get(); }

    /** A species file's {@code content_flag}: empty is always on; {@code yearofice:deer} follows {@code [ecosystem] deer}. */
    public static boolean flagOn(String flag) {
        if (flag == null || flag.isEmpty()) return true;
        if (flag.equals("yearofice:deer")) return !YoiConfig.SPEC.isLoaded() || DEER_FLAG == null || DEER_FLAG.get();
        return true;
    }

    public static boolean rackThievesDefault() { return com.yearofice.ecosystem.encounter.WorldToggles.rackThieves(); }

    public static int hareCycleYear() { return YoiConfig.SPEC.isLoaded() && HARE_CYCLE_YEAR != null ? HARE_CYCLE_YEAR.get() : -1; }

    public static void invalidate() {
        ECO.invalidate();
        com.yearofice.ecosystem.encounter.EncounterConfig.invalidate();
        TrapConfig.TRAPS.invalidate();
        com.yearofice.ecosystem.companion.CompanionConfig.invalidate();
    }
}

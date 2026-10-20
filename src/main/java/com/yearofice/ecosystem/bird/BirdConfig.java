package com.yearofice.ecosystem.bird;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.bird.BirdTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [birds]} section of {@code yearofice-common.toml}: one key per {@link BirdTuning} field. */
public final class BirdConfig {
    private BirdConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("flockCohesionM", "A flock member farther than this from its flock's centre steers back (m).");
        NOTES.put("flockSeparationM", "A flock member nearer than this to a neighbour steers apart (m).");
        NOTES.put("dabblerMin", "Ducks in a ground flock, least.");
        NOTES.put("dabblerMax", "Ducks in a ground flock, most.");
        NOTES.put("gullMin", "Gulls in a flock, least.");
        NOTES.put("gullMax", "Gulls in a flock, most.");
        NOTES.put("scavengeRangeM", "A raven, jay, gull or eagle notices a carcass within this many metres.");
        NOTES.put("corvidArriveMin", "Corvids arrive 0 to this many game minutes after a death.");
        NOTES.put("ravenKgPerDay", "What one raven eats from a carcass a game day (kg).");
        NOTES.put("eagleKgPerDay", "What one eagle eats a game day (kg).");
        NOTES.put("jayGramsPerVisit", "What a gray jay carries off a visit (g).");
        NOTES.put("eagleFishPerDay", "Fish an eagle takes a game day from a spot at a full stock.");
        NOTES.put("eagleSpotRangeM", "An eagle fishes spots within this many metres of itself.");
        NOTES.put("thiefRangeM", "A jay or raven sees the camp's stores within this many metres.");
        NOTES.put("activeCap", "Single birds alive near the players at once.");
        NOTES.put("flockCap", "Ground flocks alive near the players at once.");
        NOTES.put("spawnRadiusM", "Birds appear within this many metres of a player.");
    }

    public static final PresetConfig<BirdTuning> BIRDS = new PresetConfig<>(BirdTuning.class, p -> switch (p) {
        case GENTLE -> BirdTuning.Preset.GENTLE.tuning;
        case STANDARD -> BirdTuning.Preset.STANDARD.tuning;
        case BRUTAL -> BirdTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Birds: the M6 raven, jays, eagle, owl, ducks, loon and gull (Birds and the air).").push("birds");
        BIRDS.define(b);
        b.pop();
    }

    public static BirdTuning tuning() { return BIRDS.tuning(); }

    public static void invalidate() { BIRDS.invalidate(); }
}

package com.yearofice.story;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.ecosystem.PresetConfig;
import com.yearofice.sim.story.OnboardingTuning;
import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [onboarding]} section of {@code yearofice-common.toml}: the guided first night's switch, the beacon's
 * switch and the numbers in {@link OnboardingTuning}. Presets follow the house rule (a key still at its Standard
 * value reads the world's preset instead), for the switches too.
 */
public final class StoryConfig {
    private StoryConfig() {}

    private static final Map<String, String> NOTES = new LinkedHashMap<>();
    static {
        NOTES.put("nearFireRadiusM", "Sleep by the fire: the most a sleeping player may be from a warm fire, metres.");
        NOTES.put("duskLeadMin", "The no-fire dusk warning opens this many game minutes before sunset (the clock's sun at 64N).");
        NOTES.put("reachM", "A ring or a lit fire this near the player counts as theirs for the ring and light steps, metres.");
        NOTES.put("searchRadius", "How far the server looks for a beacon target (a birch, the creek), blocks.");
        NOTES.put("beaconFadeNearM", "The beacon is invisible within this distance of its target, metres.");
        NOTES.put("beaconFadeFullM", "The beacon reaches its full, soft strength from this distance, metres.");
        NOTES.put("beaconAlphaPct", "The beacon's strongest opacity, percent.");
    }

    public static final PresetConfig<OnboardingTuning> TUNING = new PresetConfig<>(OnboardingTuning.class, p -> switch (p) {
        case GENTLE -> OnboardingTuning.Preset.GENTLE.tuning;
        case STANDARD -> OnboardingTuning.Preset.STANDARD.tuning;
        case BRUTAL -> OnboardingTuning.Preset.BRUTAL.tuning;
    }, NOTES);

    /** The guided first night for new players. Gentle on, Standard on, Brutal on (Blueprint: on by default on a first world). */
    static ModConfigSpec.BooleanValue ENABLED;
    /** The objective beacon. Gentle on, Standard on, Brutal off (the prompts still run). */
    static ModConfigSpec.BooleanValue BEACON;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Onboarding: the guided first night, its six prompts and the objective beacon (Blueprint > Start site).")
                .push("onboarding");
        ENABLED = b.comment("Run the guided first night for a player's first join of a world; returning players never get it."
                + " Gentle true, Standard true, Brutal true.").define("enabled", true);
        BEACON = b.comment("Draw the objective beacon, the soft light column toward the current step."
                + " Gentle true, Standard true, Brutal false.").define("beacon", true);
        TUNING.define(b);
        b.pop();
    }

    public static OnboardingTuning tuning() { return TUNING.tuning(); }

    public static boolean enabled() {
        if (ENABLED == null || !YoiConfig.SPEC.isLoaded()) return true;
        return ENABLED.get();
    }

    public static boolean beacon() {
        if (BEACON == null || !YoiConfig.SPEC.isLoaded()) return true;
        boolean v = BEACON.get();
        // Still at Standard's true: the preset decides.
        if (v) return YoiConfig.PRESET.get() != YoiConfig.Preset.BRUTAL;
        return false;
    }

    static void invalidate() { TUNING.invalidate(); }
}

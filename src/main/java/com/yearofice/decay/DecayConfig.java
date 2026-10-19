package com.yearofice.decay;

import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.decay.Fresh;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The {@code [decay]} section of {@code yearofice-common.toml}. The Blueprint gives no preset differences for
 * decay (the Spoilage model is one formula), so every preset reads the same defaults; the realism preset changes
 * food through the body, not through rot. The air every stack decays at comes from the Environment field
 * ({@link DecayAir}); the M2.5 stand-in key {@code stand_in_air_c} is retired, and NeoForge drops it from an older
 * file on load.
 */
public final class DecayConfig {
    private DecayConfig() {}

    public static ModConfigSpec.IntValue BANDS;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Decay and freshness (Minecraft development > Layer 5; Blueprint > Spoilage model).").push("decay");
        BANDS = b.comment("Freshness bands per life: due points floor to life/bands so stacks merge (2.5 points at 40)."
                        + " Gentle 40, Standard 40, Brutal 40.")
                .defineInRange("bands", Fresh.DEFAULT_BANDS, 10, 400);
        b.pop();
    }

    public static int bands() {
        return BANDS != null && YoiConfig.SPEC.isLoaded() ? BANDS.get() : Fresh.DEFAULT_BANDS;
    }
}

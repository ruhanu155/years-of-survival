package com.yearofice.food.grease;

import com.yearofice.core.rules.YoiConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The {@code [food]} keys of bone grease (Items › B5): grams per kg of bone, crushing minutes per kg, the hot-fire penalty. */
public final class GreaseConfig {
    private GreaseConfig() {}

    private static ModConfigSpec.DoubleValue gPerKg, crushMin, penalty;

    public static void define(ModConfigSpec.Builder b) {
        b.comment("Food: bone grease (Items > B5 Bone grease).").push("food");
        gPerKg = b.comment("Grams of bone_grease per kg of crushed marrow bone, simmered on Low.").defineInRange("bone_grease_g_per_kg", 50.0, 0.0, 500.0);
        crushMin = b.comment("Game minutes of Heavy work to crush 1 kg of marrow bone.").defineInRange("bone_crush_min_per_kg", 20.0, 0.0, 600.0);
        penalty = b.comment("Yield multiplier when the pot boils on Steady or Hot.").defineInRange("bone_grease_boil_penalty", 0.5, 0.0, 1.0);
        b.pop();
    }

    private static boolean ok() { return YoiConfig.SPEC.isLoaded() && gPerKg != null; }

    public static double gPerKg() { return ok() ? gPerKg.get() : 50; }

    public static double crushMinPerKg() { return ok() ? crushMin.get() : 20; }

    public static double boilPenalty() { return ok() ? penalty.get() : 0.5; }
}

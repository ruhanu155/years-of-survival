package com.yearofice.env.weather;

import com.yearofice.core.rules.YoiConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * {@code [weather]} in yearofice-common.toml (Weather and storms › Tuning and presets). A key that still holds the Standard number
 * follows the world's preset; a key moved by hand wins.
 */
public final class WeatherConfig {
    private WeatherConfig() {}

    public record Tuning(double stormFrequency, double stormSeverity, double forecastBonus, double lightningPlayerRisk,
                         double blowdownScale, double whiteoutSightM, double coldSnapDepthC, boolean decadeStorm,
                         double crustMortality) {
        public static final Tuning GENTLE = new Tuning(0.6, 0.8, 0.10, 0.5, 0.5, 40, 0, true, 1.5);
        public static final Tuning STANDARD = new Tuning(1.0, 1.0, 0, 1.0, 1.0, 20, 0, true, 2.0);
        public static final Tuning BRUTAL = new Tuning(1.3, 1.2, -0.10, 1.0, 1.5, 15, -3, true, 2.5);

        double[] arr() {
            return new double[] {stormFrequency, stormSeverity, forecastBonus, lightningPlayerRisk, blowdownScale, whiteoutSightM,
                    coldSnapDepthC, crustMortality};
        }
    }

    private static final String[] KEYS = {"storm_frequency", "storm_severity", "forecast_bonus", "lightning_player_risk",
            "blowdown_scale", "whiteout_sight_m", "cold_snap_depth_c", "crust_mortality"};
    private static final String[] NOTES = {"x catalogue odds", "x peak wind, glaze and amounts", "added to forecast hit rates",
            "x strike chance on the player", "x windthrow and limb chances", "snowing-blizzard sight in metres",
            "added to cold-snap anomalies", "x caribou and muskox winter deaths under a crust"};
    private static final ModConfigSpec.DoubleValue[] VALUES = new ModConfigSpec.DoubleValue[KEYS.length];
    private static ModConfigSpec.BooleanValue decade;

    /** Called from {@link YoiConfig} while it builds the spec. */
    public static void define(ModConfigSpec.Builder b) {
        b.comment("Weather and storms: the storm table, glaze, hail, lightning and the decade storm.").push("weather");
        double[] std = Tuning.STANDARD.arr(), g = Tuning.GENTLE.arr(), br = Tuning.BRUTAL.arr();
        for (int i = 0; i < KEYS.length; i++)
            VALUES[i] = b.comment(NOTES[i] + ". Gentle " + g[i] + ", Standard " + std[i] + ", Brutal " + br[i] + ".")
                    .defineInRange(KEYS[i], std[i], -1000.0, 1000.0);
        decade = b.comment("Allow the storm of the decade.").define("decade_storm", true);
        b.pop();
    }

    /** The tuning in force; Standard when the config is not loaded (GameTests, JUnit). */
    public static Tuning tuning() {
        if (VALUES[0] == null || !YoiConfig.SPEC.isLoaded()) return Tuning.STANDARD;
        Tuning p = switch (YoiConfig.PRESET.get()) {
            case GENTLE -> Tuning.GENTLE;
            case STANDARD -> Tuning.STANDARD;
            case BRUTAL -> Tuning.BRUTAL;
        };
        double[] std = Tuning.STANDARD.arr(), pre = p.arr(), v = new double[KEYS.length];
        for (int i = 0; i < v.length; i++) v[i] = VALUES[i].get() == std[i] ? pre[i] : VALUES[i].get();
        return new Tuning(v[0], v[1], v[2], v[3], v[4], v[5], v[6], decade.get(), v[7]);
    }
}

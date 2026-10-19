package com.yearofice.sim.fire;

import com.yearofice.sim.env.FireSetting;

/**
 * The fire's lookup rules: heat, light, smoke, fuel use and what catches from what. Numbers are the Blueprint's
 * (Fire, water and light; Minecraft engine › Fire) unless marked as a slice rule.
 */
public final class FireRules {
    private FireRules() {}

    /** Moisture at which wood counts as green (Blueprint › Seasoning). */
    public static final int GREEN_MOISTURE = 50;
    /** Seasoned means under 20%. */
    public static final int SEASONED_MOISTURE = 20;
    /** Over this, the readout says "Smoky: wet wood" (Video › Fire time and the fire roster). */
    public static final int SMOKY_MOISTURE = 35;
    /** kWh in one armload (Crafting › Warm rooms › Heat sources). */
    public static final double KWH_PER_ARMLOAD = 11.8;

    /** Fuel use per setting: Low ×0.4, Steady ×1, Hot ×3 (Settings table). */
    public static double fuelUse(FireSetting s) {
        return switch (s) {
            case LOW -> 0.4;
            case STEADY -> 1.0;
            case HOT -> 3.0;
        };
    }

    /**
     * Heat × from moisture: seasoned (under 20%) gives full heat, green (50% or more) half, linear between. The
     * Blueprint gives the two ends (seasoned full heat, green half); the ramp between them is ours.
     */
    public static double moistureHeat(int moisturePct) {
        if (moisturePct < SEASONED_MOISTURE) return 1.0;
        if (moisturePct >= GREEN_MOISTURE) return 0.5;
        return 1.0 - 0.5 * (moisturePct - SEASONED_MOISTURE) / (double) (GREEN_MOISTURE - SEASONED_MOISTURE);
    }

    /** True when an armload catches from a small fire. Green wood catches only on a Hot fire. */
    public static boolean catchesFromSmall(Armload a) {
        return !a.green() && a.wood.lightsFrom == Wood.LightsFrom.SMALL_FIRE;
    }

    /** True when an armload catches from a burning fire at this setting. */
    public static boolean catchesFromFire(Armload a, FireSetting s) {
        return !a.green() || s == FireSetting.HOT;
    }

    /** True when an armload catches from coals: small-fire woods and birch ("Needs a Fire or coals"). */
    public static boolean catchesFromCoals(Armload a) {
        return !a.green() && a.wood.lightsFrom != Wood.LightsFrom.FIRE;
    }

    /**
     * Block light level by state and setting (Minecraft engine › Fire › Light): small fire 9, Low 9, Steady 12,
     * Hot 15, coals 6, banked 0. The tinder flame lights 2 m (Blueprint state table), which is level 3 as light
     * falls 1 a block; ember and out give none.
     */
    public static int light(FireState state, FireSetting setting) {
        return switch (state) {
            case OUT, EMBER, BANKED -> 0;
            case TINDER -> 3;
            case SMALL -> 9;
            case COALS -> 6;
            case FIRE -> switch (setting) {
                case LOW -> 9;
                case STEADY -> 12;
                case HOT -> 15;
            };
        };
    }

    /** Heat within 3 m in °C by state (state table; the setting table for a fire), before wood and moisture. */
    public static double baseHeatC(FireState state, FireSetting setting) {
        return switch (state) {
            case OUT, EMBER, BANKED -> 0;
            case TINDER -> 2;
            case SMALL -> 10;
            case COALS -> 15;
            case FIRE -> setting.bonusC;
        };
    }

    /** Smoke shown: 0 none, 1 thin (tinder, small fire, coals), 2 a column (a fire), 3 heavy (green or wet fuel). */
    public static int smoke(FireState state, Armload burning) {
        return switch (state) {
            case OUT, EMBER -> 0;
            case BANKED -> 0;
            case TINDER, SMALL, COALS -> 1;
            case FIRE -> burning != null && burning.moisturePct > SMOKY_MOISTURE ? 3 : 2;
        };
    }
}

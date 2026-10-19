package com.yearofice.sim.body;

import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.HeatIndex;
import com.yearofice.sim.env.HeatSource;
import com.yearofice.sim.env.RoomHeat;
import java.util.List;

/**
 * Warmth: the feels-like formula and the per-minute warmth step (Blueprint › Warmth).
 *
 * <pre>
 *   T_feel = T_air − C_wind − 10w + I_clothing + S_shelter + F_fire + A_activity + B_body
 *   C_wind = min(16, 0.8 · v) · shelter wind factor · (1 − wind block), only when T_air &lt; 10 °C
 * </pre>
 *
 * {@code I_clothing} already carries each garment's wet loss ({@link Clothing#warmthC}); the formula's
 * {@code (1 − 0.7w)} is that same loss for garments with no kept-warmth value, so it is not applied twice.
 * Inside a closed room the room temperature is the air ({@link EnvSample#airC}), wind drops out and no
 * shelter bonus applies.
 */
public final class Warmth {
    private Warmth() {}

    public static final double WIND_CHILL_PER_MS = 0.8, WIND_CHILL_MAX = 16, WIND_CHILL_BELOW_C = 10;
    public static final double WET_PENALTY_MAX = 10;
    /** The soaked rule: at least this loss a minute while soaked below 0 °C and away from fire. */
    public static final double SOAKED_LOSS = 6;
    public static final double FIRE_RESCUE_M = 3;

    public static FeelsLike feelsLike(EnvSample env, ClothingState clothing, List<HeatSource> heatSources) {
        return feelsLike(env, clothing, heatSources, WarmthContext.RESTING);
    }

    /**
     * @param heatSources the sources around the player; {@code null} reads the env layer's own
     *                    {@code heat_bonus_c} instead, so the two are never added together.
     */
    public static FeelsLike feelsLike(EnvSample env, ClothingState clothing, List<HeatSource> heatSources,
                                      WarmthContext ctx) {
        boolean indoors = env.indoors();
        double windChill = indoors ? 0 : windChillC(env.airC(), env.windMs(), ctx.shelter().windFactor(),
                Clothing.windBlock(clothing));
        double wet = WET_PENALTY_MAX * Clothing.wetness(clothing);
        double fire = heatSources == null ? env.heatBonusC() : HeatIndex.bonusC(heatSources);
        double shelter = indoors ? 0 : ctx.shelter().bonusC();
        return new FeelsLike(env.airC(), windChill, wet, Clothing.warmthC(clothing), shelter, fire,
                ctx.activity().warmthC, ctx.bodyModC());
    }

    public static double windChillC(double airC, double windMs, double shelterFactor, double windBlock) {
        if (airC >= WIND_CHILL_BELOW_C) return 0;
        return Math.min(WIND_CHILL_MAX, WIND_CHILL_PER_MS * Math.max(0, windMs)) * shelterFactor * (1 - windBlock);
    }

    /**
     * A zone's exposure temperature for frost: air, less wind chill through that zone's own wind block,
     * plus shelter and fire. Clothing warmth is left out because the zone's "safe to" limit already
     * accounts for its gear; wetness is left out because it moves the limit to −10 °C instead.
     */
    public static double zoneExposureC(EnvSample env, ClothingState clothing, List<HeatSource> heatSources,
                                       WarmthContext ctx, Zone zone) {
        boolean indoors = env.indoors();
        double windChill = indoors ? 0 : windChillC(env.airC(), env.windMs(), ctx.shelter().windFactor(),
                Clothing.zoneWindBlock(clothing, zone));
        double fire = heatSources == null ? env.heatBonusC() : HeatIndex.bonusC(heatSources);
        return env.airC() - windChill + (indoors ? 0 : ctx.shelter().bonusC()) + fire;
    }

    /** dW/dt per game minute from feels-like alone. */
    public static double rateFromFeel(double feelC) {
        if (feelC < 10) return -0.02 * (10 - feelC);
        if (feelC > 18) return 0.05 * (feelC - 18);
        return 0;
    }

    /**
     * Everything one warmth step needs.
     *
     * @param nearFireOrWarmRoom within 3 m of a fire, or in a warm room: ends the soaked rule
     * @param coldScale          the preset's Cold slider (warmth loss ×); gains are not scaled
     */
    public record Step(double feelsLikeC, double wetness, double airC, boolean nearFireOrWarmRoom,
                       double waterC, ColdWater.Depth depth, double coldScale) {

        public static Step of(FeelsLike f, ClothingState c, EnvSample env, List<HeatSource> heat, double coldScale) {
            boolean rescue = HeatIndex.withinFire(heat, FIRE_RESCUE_M)
                    || (env.indoors() && env.airC() >= RoomHeat.WARM_ROOM_C);
            return new Step(f.totalC(), Clothing.wetness(c), env.airC(), rescue, env.waterC(), ColdWater.Depth.NONE,
                    coldScale);
        }

        public Step inWater(double waterC, ColdWater.Depth d) {
            return new Step(feelsLikeC, wetness, airC, nearFireOrWarmRoom, waterC, d, coldScale);
        }
    }

    /** dW for one game minute, before clamping. */
    public static double ratePerMinute(Step s) {
        if (s.depth() == ColdWater.Depth.SWIM) {
            double r = ColdWater.swimWarmthPerMin(s.waterC());
            return r < 0 ? r * s.coldScale() : r;
        }
        double r = rateFromFeel(s.feelsLikeC());
        if (r < 0) r *= s.coldScale();
        if (s.wetness() > Clothing.SOAKED && s.airC() < 0 && !s.nearFireOrWarmRoom()) {
            r = Math.min(r, -SOAKED_LOSS * s.coldScale());
        }
        r += ColdWater.contactWarmthPerMin(s.waterC(), s.depth()) * s.coldScale();
        return r;
    }

    /** One game minute of warmth, clamped to 0–100. */
    public static double step(double warmth, Step s) {
        return clamp(warmth + ratePerMinute(s));
    }

    /** {@code minutes} steps at constant inputs (the body tick's closed form above 10 minutes uses hourly samples). */
    public static double step(double warmth, Step s, int minutes) {
        double w = warmth;
        for (int i = 0; i < minutes; i++) w = step(w, s);
        return w;
    }

    /** Instant changes from what is eaten or drunk. */
    public enum Intake { HOT_DRINK, HOT_MEAL, RAW_SNOW_HALF_LITRE, VERY_COLD_WATER_HALF_LITRE }

    public static double afterIntake(double warmth, Intake i) {
        return clamp(switch (i) {
            case HOT_DRINK -> warmth + 5;
            case HOT_MEAL -> warmth + 3;
            case RAW_SNOW_HALF_LITRE -> warmth - 8;
            case VERY_COLD_WATER_HALF_LITRE -> warmth < 50 ? warmth - 2 : warmth;
        });
    }

    static double clamp(double w) { return w < 0 ? 0 : (w > 100 ? 100 : w); }
}

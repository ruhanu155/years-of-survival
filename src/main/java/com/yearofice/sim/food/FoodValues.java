package com.yearofice.sim.food;

import com.yearofice.sim.decay.DecayCore;
import com.yearofice.sim.decay.FoodState;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * One row of {@code data_maps/item/food.json} (Items › Food values): per 100 g.
 *
 * <p>Carbs are not stored: they are the kcal left after protein and fat, divided by 4, never below 0, so
 * kcal = 4P + 9F + 4C holds within rounding. Vitamin C is stored as 10 mg per C-day, a game value. Life is the
 * safe life at 5 °C by state; the curve stretches it with temperature.
 *
 * <p>Pure Java. The JSON adapter that fills these lives in {@code food}.
 */
public record FoodValues(String id, double kcalPer100g, double proteinGPer100g, double fatGPer100g,
                         double vitCMgPer100g, String curve, Map<FoodState, Double> spoilDaysAt5c) {
    public static final double MG_PER_C_DAY = 10.0;

    public FoodValues {
        if (kcalPer100g < 0 || proteinGPer100g < 0 || fatGPer100g < 0 || vitCMgPer100g < 0) {
            throw new IllegalArgumentException(id + ": negative nutrient");
        }
        EnumMap<FoodState, Double> copy = new EnumMap<>(FoodState.class);
        copy.putAll(spoilDaysAt5c);
        spoilDaysAt5c = Collections.unmodifiableMap(copy);
    }

    /** Carbohydrate in g per 100 g: (kcal − 4P − 9F) ÷ 4, floored at 0. */
    public double carbsGPer100g() {
        return Math.max(0.0, (kcalPer100g - 4.0 * proteinGPer100g - 9.0 * fatGPer100g) / 4.0);
    }

    /** 4P + 9F + 4C, which must match kcal within 2 (GameTest food/units_match_kcal). */
    public double kcalFromMacros() {
        return 4.0 * proteinGPer100g + 9.0 * fatGPer100g + 4.0 * carbsGPer100g();
    }

    public double kcal(double grams) { return kcalPer100g * grams / 100.0; }

    public double cDays(double grams) { return vitCMgPer100g / MG_PER_C_DAY * grams / 100.0; }

    /** The safe life at 5 °C for a state, or NaN if the food has no such state. */
    public double safeLifeDays(FoodState state) {
        Double d = spoilDaysAt5c.get(state);
        return d == null ? Double.NaN : d;
    }

    /**
     * The reference life in clock hours from p = 0 to p = 1. Living-rot states: τ = safe life ÷ 0.7, so the safe
     * life ends at Turning. Fixed-life states (smoked, dried): the shelf life itself, after which the food is
     * Stale (Blueprint › Preserving in detail).
     */
    public double refLifeDh(FoodState state) {
        double days = safeLifeDays(state);
        return fixedLife(state) ? days * 24.0 : DecayCore.tauFromSafeLifeDays(days);
    }

    /** True when this state counts down a fixed shelf life: a preserved state, or any curve but living rot. */
    public boolean fixedLife(FoodState state) {
        return state.fixedLife() || !LIVING_ROT.equals(curve);
    }

    public static final String LIVING_ROT = "living_rot";

    /** Stew with bones, heads or frames multiplies calories by 1.15 (Blueprint › Cooking). */
    public static final double STEW_WITH_BONES = 1.15;
    /** Rendering: 1 kg raw fat gives 0.7 kg lard and 0.1 kg cracklings (Items › Food values). */
    public static final double RENDER_LARD_SHARE = 0.7;
    public static final double RENDER_CRACKLINGS_SHARE = 0.1;
}

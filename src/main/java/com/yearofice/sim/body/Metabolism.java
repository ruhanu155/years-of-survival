package com.yearofice.sim.body;

/**
 * Calories, weight, energy and water: the rate tables of Blueprint › Survival tuning (Calories and weight, Energy,
 * Sleep, Water) and the body tick's burn formula (Minecraft development › The body tick, step 2).
 *
 * <pre>
 *   BMR        = 10 · kg + 6.25 · cm − 5 · age + 5        (−161 for sex 1)
 *   kcal_min   = rate_activity / 60 · BMR / 1794 · m_preset + kcal_shiver / 60
 *   Δkg        = (absorbed − burned) / kcal_per_kg        (2,400 Standard, RECONCILE 135)
 * </pre>
 *
 * The activity rates are totals that already include the base burn at the default body (RECONCILE 17), so a
 * thinner body burns less and starvation slows near the end.
 */
public final class Metabolism {
    private Metabolism() {}

    public static final double DEFAULT_BMR = 1794;
    public static final int AGE = 25;
    public static final int DEFAULT_HEIGHT_CM = 183;
    public static final double DEFAULT_KG = 77;

    /** Macro daily targets in grams: carbs, fat, protein (Blueprint › Macros and strength). */
    public static final double[] MACRO_TARGET_G = {150, 90, 75};
    public static final int CARBS = 0, FAT = 1, PROTEIN = 2;

    /** Water: 1 point is 30 mL; 100 is a 3 L reserve. */
    public static final double ML_PER_POINT = 30;

    public static double bmr(double kg, int heightCm, int sex) {
        return 10 * kg + 6.25 * heightCm - 5 * AGE + (sex == 1 ? -161 : 5);
    }

    public static double bmi(double kg, int heightCm) {
        double m = heightCm / 100.0;
        return kg / (m * m);
    }

    public static double kgForBmi(double bmi, int heightCm) {
        double m = heightCm / 100.0;
        return bmi * m * m;
    }

    /**
     * kcal per game hour by activity, at the default body. Sprinting has no row; it is read as heavy work
     * (450), the highest land rate, until the Blueprint gives it one.
     */
    public static double kcalPerHour(Activity a) {
        return switch (a) {
            case SLEEP -> 60;
            case REST -> 75;
            case WALK -> 250;
            case HEAVY, RUN -> 450;
            case SWIM -> 550;
        };
    }

    /** Burn for one game minute, shivering included. */
    public static double burnPerMinute(Activity a, double kg, int heightCm, int sex, double warmth, double burnScale) {
        return kcalPerHour(a) / 60.0 * bmr(kg, heightCm, sex) / DEFAULT_BMR * burnScale
                + Hypothermia.shiverKcalPerHour(warmth) / 60.0;
    }

    /** Energy change per game hour awake (negative) by activity; sprint is −0.05 a minute. */
    public static double energyPerHourAwake(Activity a) {
        return switch (a) {
            case SLEEP, REST -> -0.2;
            case WALK -> -0.4;
            case HEAVY -> -0.6;
            case SWIM -> -1.0;
            case RUN -> -3.0;
        };
    }

    /** Sleep restores 1.25 bolts a game hour times quality, so 8 hours in a good bed refill 10. */
    public static final double SLEEP_BOLTS_PER_H = 1.25;

    /** Water drain per game minute, before the Water need slider. */
    public static double waterDrainPerMinute(Activity a, double airC, boolean gutIll) {
        double perH = 3.5 + (a == Activity.HEAVY ? 5 : 0) + (gutIll ? 1.4 : 0);
        if (airC > 20) perH *= 1.2;
        return perH / 60.0;
    }

    /** Points of water for a drink of {@code litres}: a 0.25 L sip restores about 8. */
    public static double waterPoints(double litres) {
        return litres * 1000 / ML_PER_POINT;
    }

    /**
     * The energy cap: the lowest of the cap table's causes that apply (Blueprint › Energy cap). Gut illness caps
     * at 7. Lean-meat sickness and scurvy are M4.
     */
    /** Cause ids for {@link #capCause}: what holds the cap down (0 nothing). */
    public static final int CAP_NONE = 0, CAP_HUNGRY = 1, CAP_THIN = 2, CAP_THIRST = 3, CAP_GUT = 4, CAP_AWAKE = 5, CAP_ILL = 6;

    /** The cause of the lowest cap in the table (the first of equals); {@code illCap} is the illness stage's cap. */
    public static int capCause(double balance24h, double bmi, double water, int awakeMin, boolean gutIll, double illCap) {
        double cap = energyCap(balance24h, bmi, water, awakeMin, gutIll);
        if (illCap < cap) return CAP_ILL;
        if (cap >= 10) return CAP_NONE;
        if (energyCap(0, 22, 100, awakeMin, false) == cap) return CAP_AWAKE;
        if (energyCap(balance24h, 22, 100, 0, false) == cap) return CAP_HUNGRY;
        if (energyCap(0, bmi, 100, 0, false) == cap) return CAP_THIN;
        if (energyCap(0, 22, water, 0, false) == cap) return CAP_THIRST;
        return CAP_GUT;
    }

    public static double energyCap(double balance24h, double bmi, double water, int awakeMin, boolean gutIll) {
        double cap = 10;
        if (balance24h < -1500) cap = Math.min(cap, 8);
        if (bmi < 18.5) cap = Math.min(cap, 7);
        if (water < 15) cap = Math.min(cap, 4);
        else if (water < 30) cap = Math.min(cap, 7);
        if (gutIll) cap = Math.min(cap, 7);
        if (awakeMin >= 72 * 60) cap = Math.min(cap, 3);
        else if (awakeMin >= 48 * 60) cap = Math.min(cap, 5);
        else if (awakeMin >= 24 * 60) cap = Math.min(cap, 7);
        return cap;
    }

    /** The BMI band's feels-like term (Calories and weight › Body weight bands). */
    public static double bmiWarmthC(double bmi) {
        if (bmi >= 25) return 1;
        if (bmi < 18.5) return -2;
        if (bmi < 20) return -1;
        return 0;
    }

    /** The word the vitals page shows for a BMI. */
    public static String bmiWord(double bmi) {
        if (bmi >= 25) return "heavy";
        if (bmi >= 20) return "healthy";
        if (bmi >= 18.5) return "thin";
        return "underweight";
    }

    /** Strength = Condition · clamp((BMI − 17) / 6, 0, 1). */
    public static double strength(double condition, double bmi) {
        return condition * Math.max(0, Math.min(1, (bmi - 17) / 6));
    }

    /** Body heat drops in sleep: feels-like counts 2 °C lower while asleep (Blueprint › Sleep). */
    public static final double SLEEP_FEEL_C = -2;

    /** The stomach empties 7% of capacity a game hour awake and 4% asleep. */
    public static double stomachEmptyPerMinute(boolean asleep, double capacityKg) {
        return capacityKg * (asleep ? 0.04 : 0.07) / 60.0;
    }

    /** Gut illness halves the calories absorbed for two days (Blueprint › Water). */
    public static final double GUT_ILL_FACTOR = 0.5;
    public static final int GUT_ILL_MINUTES = 2 * 1440;
}

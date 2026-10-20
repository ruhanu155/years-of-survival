package com.yearofice.sim.food;

/**
 * Bone grease (Items › B5; research round B). Crush marrow bone, simmer 1 L of water per kg on Low for 3 game hours,
 * cool, skim. 50 g of grease per kg of bone; a Steady or Hot fire emulsifies the fat and halves the yield. The water
 * left is {@code skimmed_broth}: 10 kcal per 100 g. Pure Java. Original Year of Ice code.
 */
public final class BoneGrease {
    private BoneGrease() {}

    public enum Fire { LOW, STEADY, HOT }

    public static final double KCAL_PER_100G = 900;
    public static final double BROTH_KCAL_PER_100G = 10;

    public static double gramsPerKg(double tuned, double boilPenalty, Fire fire) { return fire == Fire.LOW ? tuned : tuned * boilPenalty; }

    /** Kilograms of grease from {@code boneKg} of crushed bone. */
    public static double yieldKg(double boneKg, double gPerKg, double boilPenalty, Fire fire) {
        return boneKg * gramsPerKg(gPerKg, boilPenalty, fire) / 1000.0;
    }

    /** Game minutes of Heavy work to crush {@code boneKg}. */
    public static double crushMinutes(double boneKg, double minPerKg) { return boneKg * minPerKg; }

    /** Litres of skimmed broth left (1 L of water per kg of bone, less the cake). */
    public static double brothL(double boneKg) { return boneKg * 0.75; }

    public static double kcal(double greaseKg) { return greaseKg * 10 * KCAL_PER_100G; }
}

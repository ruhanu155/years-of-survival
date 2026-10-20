package com.yearofice.sim.food;

/**
 * What a stack of food gives the body (Items › Food values: the {@code yearofice:nutrients} component): kcal, protein
 * and fat in grams, vitamin C in mg (10 mg is one C-day) and water in mL. Pure Java; original Year of Ice code.
 */
public record Nutrients(double kcal, double proteinG, double fatG, double vitCMg, double waterMl) {
    public static final Nutrients ZERO = new Nutrients(0, 0, 0, 0, 0);

    public Nutrients {
        if (kcal < 0 || proteinG < 0 || fatG < 0 || vitCMg < 0 || waterMl < 0) throw new IllegalArgumentException("negative nutrient");
    }

    /** The row's per-100 g values taken for {@code grams}. */
    public static Nutrients of(FoodValues v, double grams, double waterMlPer100g) {
        double f = grams / 100.0;
        return new Nutrients(v.kcalPer100g() * f, v.proteinGPer100g() * f, v.fatGPer100g() * f, v.vitCMgPer100g() * f, waterMlPer100g * f);
    }

    public Nutrients plus(Nutrients o) {
        return new Nutrients(kcal + o.kcal, proteinG + o.proteinG, fatG + o.fatG, vitCMg + o.vitCMg, waterMl + o.waterMl);
    }

    public Nutrients times(double k) { return new Nutrients(kcal * k, proteinG * k, fatG * k, vitCMg * k, waterMl * k); }

    /** The share {@code k} of this stack (a bite of a dish), all fields together. */
    public Nutrients share(double k) { return times(k); }

    public double cDays() { return vitCMg / FoodValues.MG_PER_C_DAY; }

    /** Carbohydrate g: the kcal left after protein and fat, over 4, never below 0. */
    public double carbsG() { return Math.max(0, (kcal - 4 * proteinG - 9 * fatG) / 4.0); }
}

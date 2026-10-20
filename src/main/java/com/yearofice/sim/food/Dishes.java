package com.yearofice.sim.food;

import java.util.List;

/**
 * Cooked dishes and dried food (Items › Food values: "Cooked dishes are one item"; Blueprint › Preserving in detail:
 * "Dried meat weighs 40% of fresh and keeps every calorie").
 *
 * <p>A dish sums its ingredients' {@link Nutrients} when it finishes, times the method's factors: stew with bones or
 * heads gives 1.15 of the energy and protein, and each method keeps a share of the vitamin C (a hard boil destroys
 * most of it). Pure Java; original Year of Ice code.
 */
public final class Dishes {
    private Dishes() {}

    /** Dried meat and fish weigh this share of the fresh weight and keep every calorie. */
    public static final double DRIED_WEIGHT_FRACTION = 0.4;

    /** @param energy factor on kcal, protein and fat; @param vitCKept share of the vitamin C that survives. */
    public enum Method {
        RAW(1.0, 1.0), STONE_FRY(1.0, 0.7), COAL_BAKE(1.0, 1.0), STEW(1.0, 0.5), STEW_BONES(1.15, 0.5), STEEP(1.0, 1.0), WHIP(1.0, 1.0);

        public final double energy, vitCKept;

        Method(double energy, double vitCKept) {
            this.energy = energy;
            this.vitCKept = vitCKept;
        }
    }

    /** The dish's nutrients: the ingredients summed, then the method's factors. */
    public static Nutrients cook(List<Nutrients> ingredients, Method m) {
        Nutrients sum = Nutrients.ZERO;
        for (Nutrients n : ingredients) sum = sum.plus(n);
        return new Nutrients(sum.kcal() * m.energy, sum.proteinG() * m.energy, sum.fatG() * m.energy, sum.vitCMg() * m.vitCKept, sum.waterMl());
    }

    /** Grams after drying: 40% of the fresh weight. */
    public static double driedGrams(double freshGrams) { return freshGrams * DRIED_WEIGHT_FRACTION; }

    /** kcal per kg of the dried product: the fresh kcal packed into 40% of the weight. */
    public static double driedKcalPerKg(double freshKcalPerKg) { return freshKcalPerKg / DRIED_WEIGHT_FRACTION; }
}

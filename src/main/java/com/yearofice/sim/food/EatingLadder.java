package com.yearofice.sim.food;

/**
 * Eating decayed food (Decay and time › Eating decayed food › Chances by food and stage).
 *
 * <p>Decayed food pays now and bills later through the existing illnesses: gut illness, nausea and parasites.
 * One ladder per food family; Fresh and Aging follow the Spoilage model, the other cells are the tab's.
 * Weakness (Condition under 50 or BMI under 18.5) multiplies any gut-illness chance by 1.25, to at most 95%,
 * and lengthens the illness by 12 game hours.
 *
 * <p>Stages are {@link com.yearofice.sim.decay.DecayLadder#FOOD} indices: 0 Fresh, 1 Aging, 2 Turning,
 * 3 Spoiled, 4 Rotten. Pure Java. Original Year of Ice code.
 */
public final class EatingLadder {
    private EatingLadder() {}

    public static final int FRESH = 0, AGING = 1, TURNING = 2, SPOILED = 3, ROTTEN = 4;

    public static final double WEAK_MULTIPLIER = 1.25;
    public static final double WEAK_CAP = 0.95;
    public static final double WEAK_CONDITION_BELOW = 50.0;
    public static final double WEAK_BMI_BELOW = 18.5;
    public static final double WEAK_EXTRA_HOURS = 12.0;

    public enum Family { GAME_MEAT, FISH, BEAR, COOKED_DISH, EGGS, BERRIES, GREENS, MUSHROOMS, ROOTS }

    /** How it is eaten. STEWED is cooked in a stew or fish-head soup. */
    public enum Prep { RAW, COOKED, STEWED }

    /**
     * What a meal risks.
     *
     * @param gutIllness       chance of gut illness from this meal
     * @param nauseaNow        nausea added at once
     * @param parasiteMultiplier multiplier on the food's base parasite chance (0 when cooking removes it)
     * @param vitCMultiplier   share of the food's vitamin C that counts
     * @param trimShare        share of the weight cut away as rot first (soft potatoes)
     * @param removed          the food is gone and cannot be eaten
     */
    public record Risk(double gutIllness, double nauseaNow, double parasiteMultiplier, double vitCMultiplier,
                       double trimShare, boolean removed) {
        static final Risk SAFE = new Risk(0, 0, 0, 1, 0, false);
        static final Risk GONE = new Risk(0, 0, 0, 0, 0, true);

        static Risk gut(double g) { return new Risk(g, 0, 0, 1, 0, false); }
    }

    /** The meal's risk before weakness. */
    public static Risk risk(Family family, Prep prep, int stage) {
        if (stage >= ROTTEN) return Risk.GONE;
        boolean raw = prep == Prep.RAW;
        boolean stewed = prep == Prep.STEWED;
        switch (family) {
            case GAME_MEAT, BEAR, FISH -> {
                if (!raw) {
                    return switch (stage) {
                        case FRESH, AGING -> Risk.SAFE;
                        case TURNING -> Risk.gut(stewed ? 0.15 : 0.30);
                        default -> Risk.gut(0.80);
                    };
                }
                return switch (stage) {
                    case FRESH -> new Risk(0, 0, 1, 1, 0, false);
                    case AGING -> new Risk(0.10, 0, 2, 1, 0, false);
                    case TURNING -> new Risk(0.50, 40, 2, 1, 0, false);
                    default -> new Risk(0.80, 60, 2, 1, 0, false);
                };
            }
            case COOKED_DISH, EGGS -> {
                return switch (stage) {
                    case FRESH, AGING -> Risk.SAFE;
                    case TURNING -> Risk.gut(0.30);
                    default -> Risk.gut(0.80);
                };
            }
            case BERRIES -> {
                return switch (stage) {
                    case FRESH, AGING -> Risk.SAFE;
                    case TURNING -> new Risk(0.20, 40, 0, 1, 0, false); // fermented
                    default -> Risk.gut(0.50); // mouldy
                };
            }
            case GREENS -> {
                return switch (stage) {
                    case FRESH -> Risk.SAFE;
                    case AGING -> new Risk(0, 0, 0, 0.5, 0, false);
                    case TURNING -> new Risk(0.30, 0, 0, 0, 0, false);
                    default -> Risk.GONE; // slimy
                };
            }
            case MUSHROOMS -> {
                return switch (stage) {
                    case FRESH -> Risk.SAFE; // the lookalike rules sit elsewhere
                    case AGING -> raw ? Risk.gut(0.10) : Risk.SAFE;
                    case TURNING -> Risk.gut(0.30); // even cooked
                    default -> Risk.gut(0.80);
                };
            }
            case ROOTS -> {
                return switch (stage) {
                    case FRESH, AGING -> Risk.SAFE;
                    case TURNING -> new Risk(0, 0, 0, 1, 0.20, false);
                    default -> Risk.GONE;
                };
            }
            default -> throw new IllegalStateException();
        }
    }

    public static boolean weak(double condition, double bmi) {
        return condition < WEAK_CONDITION_BELOW || bmi < WEAK_BMI_BELOW;
    }

    /** The gut-illness chance after weakness: ×1.25, capped at 95%. */
    public static double gutIllnessChance(Family family, Prep prep, int stage, double condition, double bmi) {
        double g = risk(family, prep, stage).gutIllness();
        if (g > 0 && weak(condition, bmi)) g = Math.min(WEAK_CAP, g * WEAK_MULTIPLIER);
        return g;
    }

    /** Roll one meal against a uniform draw u in [0, 1). */
    public static boolean rollGutIllness(Family family, Prep prep, int stage, double condition, double bmi, double u) {
        return u < gutIllnessChance(family, prep, stage, condition, bmi);
    }

    /** Auto-eat never picks Turning or worse unless the ration is Starvation. */
    public static boolean autoEatAllowed(int stage, boolean starvationRation) {
        return stage < TURNING || (starvationRation && stage < ROTTEN);
    }
}

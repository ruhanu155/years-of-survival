package com.yearofice.sim.food;

import com.yearofice.sim.decay.FoodState;

/**
 * One bite of food, decided before the body books it (Blueprint › Cooking › Doneness; Decay and time › Eating
 * decayed food). A bite is one item: a 100 g unit, or a whole piece at its own {@code weight_g}.
 *
 * <ul>
 *   <li>Rotten food is gone: it cannot be eaten.</li>
 *   <li>Burnt food keeps 70% of its calories and costs 2 Spirits.</li>
 *   <li>Raw food is eaten as {@link EatingLadder.Prep#RAW}, anything with the cooked state as
 *       {@link EatingLadder.Prep#COOKED}; the ladder gives the gut-illness chance, nausea and parasite multiplier by
 *       stage, and weakness multiplies the gut-illness chance by 1.25 to at most 95%.</li>
 *   <li>Parasites: the food's base chance (raw game 5%, fish 2%; Blueprint › Cooking) times the ladder's multiplier
 *       times the doneness share (Rare halves it, Done removes it).</li>
 * </ul>
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class Meal {
    private Meal() {}

    /** Base parasite chance of a raw meal (Blueprint › Cooking, the Raw row). Bear's 25% arrives with bears. */
    public static final double PARASITE_GAME = 0.05, PARASITE_FISH = 0.02;
    /** Spirits a burnt meal costs. */
    public static final double BURNT_SPIRITS = -2.0;

    /**
     * What the bite does.
     *
     * @param refused      true if it cannot be eaten (rotten)
     * @param kcalShare    share of the food's calories that count (burnt 0.7)
     * @param gutIllness   chance of gut illness from this bite
     * @param gutIll       the roll's verdict
     * @param nauseaNow    nausea added at once
     * @param parasites    chance of a parasite from this bite
     * @param spirits      Spirits change
     */
    public record Outcome(boolean refused, double kcalShare, double gutIllness, boolean gutIll, double nauseaNow,
                          double parasites, double spirits) {
        static final Outcome REFUSED = new Outcome(true, 0, 0, false, 0, 0, 0);
    }

    /**
     * Decide one bite.
     *
     * @param state     the stack's {@code yearofice:fresh} state (raw or cooked in the slice)
     * @param ring      the doneness ring a cooked stack carries, or null
     * @param stage     the {@link EatingLadder} stage, 0 Fresh to 4 Rotten
     * @param condition Condition 0–100; {@code bmi} the player's BMI (weakness)
     * @param u         a uniform draw in [0, 1) for the gut-illness roll
     */
    public static Outcome bite(EatingLadder.Family family, FoodState state, Doneness.Ring ring, int stage,
                               double condition, double bmi, double u) {
        if (stage >= EatingLadder.ROTTEN) return Outcome.REFUSED;
        EatingLadder.Prep prep = state == FoodState.RAW ? EatingLadder.Prep.RAW : EatingLadder.Prep.COOKED;
        EatingLadder.Risk risk = EatingLadder.risk(family, prep, stage);
        if (risk.removed()) return Outcome.REFUSED;
        double gut = EatingLadder.gutIllnessChance(family, prep, stage, condition, bmi);
        Doneness.Ring r = ring == null ? (prep == EatingLadder.Prep.RAW ? Doneness.Ring.RAW : Doneness.Ring.DONE) : ring;
        double base = family == EatingLadder.Family.FISH ? PARASITE_FISH : PARASITE_GAME;
        double parasites = prep == EatingLadder.Prep.RAW
                ? base * risk.parasiteMultiplier()
                : base * Math.max(1.0, risk.parasiteMultiplier()) * Doneness.parasiteShare(r);
        return new Outcome(false, Doneness.calorieShare(r), gut, u < gut, risk.nauseaNow(), parasites,
                r == Doneness.Ring.BURNT ? BURNT_SPIRITS : 0.0);
    }
}

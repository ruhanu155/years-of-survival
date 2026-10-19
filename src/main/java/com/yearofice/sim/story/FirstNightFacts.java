package com.yearofice.sim.story;

/**
 * What the server saw about one player at one check, as plain booleans: the state machine never reads the world
 * itself. Each comes from a real game event or a check of the player's things (never from a timer).
 *
 * @param tinder        tinder in the pack
 * @param kindling      kindling in the pack
 * @param ring          a fire ring set near the player (placed, or found within reach)
 * @param fireLit       a fire caught near the player ({@code FireStateEvent} to a flaming state, or a warm fire in
 *                      the heat lookup within reach)
 * @param food          food in the pack (any item with a {@code yearofice:food} row)
 * @param ate           the body booked a meal since the last check ({@code BodyService} readout rose)
 * @param asleepByFire  asleep within the near-fire radius of a warm fire
 */
public record FirstNightFacts(boolean tinder, boolean kindling, boolean ring, boolean fireLit, boolean food,
                              boolean ate, boolean asleepByFire) {

    public static final FirstNightFacts NONE = new FirstNightFacts(false, false, false, false, false, false, false);

    public FirstNightFacts withTinder(boolean t, boolean k) { return new FirstNightFacts(t, k, ring, fireLit, food, ate, asleepByFire); }

    public FirstNightFacts withRing(boolean r) { return new FirstNightFacts(tinder, kindling, r, fireLit, food, ate, asleepByFire); }

    public FirstNightFacts withFireLit(boolean l) { return new FirstNightFacts(tinder, kindling, ring, l, food, ate, asleepByFire); }

    public FirstNightFacts withFood(boolean f) { return new FirstNightFacts(tinder, kindling, ring, fireLit, f, ate, asleepByFire); }

    public FirstNightFacts withAte(boolean a) { return new FirstNightFacts(tinder, kindling, ring, fireLit, food, a, asleepByFire); }

    public FirstNightFacts withAsleepByFire(boolean s) { return new FirstNightFacts(tinder, kindling, ring, fireLit, food, ate, s); }
}

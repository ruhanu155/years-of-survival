package com.yearofice.sim.hunt;

/**
 * Blinds and tree stands (Blueprint › Setting up the hunt › Build sheet): what they do to what animals see, hear and
 * smell. The factors multiply the sight range from {@link com.yearofice.sim.ecosystem.Awareness#sightRange}.
 */
public final class Blinds {
    private Blinds() {}

    public enum Kind { NONE, GROUND_BLIND, TREE_STAND }

    /** @param sightX sight range factor; @param noiseX noise factor; @param scentX scent factor; @param scentClearM scent does not reach the ground within this; @param viewX the player's view distance factor */
    public record Effect(double sightX, double noiseX, double scentX, double scentClearM, double viewX) {
        public static final Effect NONE = new Effect(1, 1, 1, 0, 1);
    }

    /**
     * @param still        the player is not moving
     * @param drawing      a bow is being drawn
     * @param animalDistM  distance to the animal
     * @param inFront      the animal is in front of the player
     * @param ageDays      game days since the blind was built
     * @param bigPrey      moose, caribou or sheep (a fresh blind only halves their sight)
     * @param bear         the animal is a bear
     * @param snowCm       snow depth on the blind; 40 cm collapses it
     */
    public static Effect effect(Kind kind, boolean still, boolean drawing, double animalDistM, boolean inFront, double ageDays,
                                boolean bigPrey, boolean bear, double snowCm, HuntTuning t) {
        switch (kind) {
            case GROUND_BLIND -> {
                if (snowCm >= 40) return Effect.NONE;
                double sight = t.blindSightX();
                if (ageDays < 1 && bigPrey) sight = t.blindFreshSightX();
                else if (ageDays < 1) sight = Math.max(sight, t.blindFreshSightX());
                if (!still) sight = Math.max(sight, 0.6);
                if (drawing && animalDistM <= 15 && inFront) sight = 0.5;
                return new Effect(sight, 0.5, 1, 0, 1);
            }
            case TREE_STAND -> {
                return new Effect(bear ? t.standBearSightX() : t.standPreySightX(), 1, 0, t.standScentClearM(), 1.5);
            }
            default -> { return Effect.NONE; }
        }
    }
}

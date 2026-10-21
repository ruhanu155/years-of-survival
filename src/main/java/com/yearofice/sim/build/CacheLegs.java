package com.yearofice.sim.build;

/**
 * Raised cache legs (Every concept › Raised cache legs). Who can reach a cache platform by how its legs are dressed and
 * how high the platform stands. Pure; the ecosystem asks through {@code api.build}.
 */
public final class CacheLegs {
    private CacheLegs() {}

    public enum Leg { BARKED, PEELED, BANDED }

    /** A grizzly cannot climb but tears a platform lower than this (metres). */
    public static final double GRIZZLY_REACH_M = 3.5;

    /** Chance the animal gets up the legs (or to the platform): 0 to 1. Unknown species: 1, the safe side for the player's food. */
    public static double reach(String species, Leg leg, double platformM) {
        return switch (species) {
            case "grizzly_bear", "grizzly" -> platformM < GRIZZLY_REACH_M ? 1.0 : 0.0;
            case "black_bear" -> 1.0;
            case "wolverine" -> switch (leg) { case BARKED -> 1.0; case PEELED -> 0.7; case BANDED -> 0.2; };
            case "squirrel", "marten", "red_squirrel", "pine_marten" -> switch (leg) { case BARKED -> 1.0; case PEELED -> 0.5; case BANDED -> 0.0; };
            default -> 1.0;
        };
    }

    /** True when no roll can put this animal on the platform. */
    public static boolean proof(String species, Leg leg, double platformM) { return reach(species, leg, platformM) <= 0; }
}

package com.yearofice.sim.body;

/**
 * Cold water by temperature band (Waters and fish › Swimming, wading and cold water). Banded, not a flat
 * 6 a minute (RECONCILE 123); the flat figure is the coldest band. An injured torso shortens every timed
 * band: ×0.8 Hurt, ×0.6 Badly hurt (RECONCILE 120 C11, 123).
 */
public final class ColdWater {
    private ColdWater() {}

    public enum Depth { NONE, ANKLE, KNEE, THIGH, SWIM }

    /** Unlimited useful time. */
    public static final double UNLIMITED = Double.POSITIVE_INFINITY;

    /**
     * Hot-spring pools: the source is 42 °C and the main pool 38 °C; a soak restores warmth to full in 10 game
     * minutes (Blueprint › Set pieces). Water at or above this warms instead of cooling.
     */
    public static final double HOT_POOL_C = 35;

    /** Warmth change per game minute while swimming; negative is loss. */
    public static double swimWarmthPerMin(double waterC) {
        if (waterC >= HOT_POOL_C) return +10;
        if (waterC > 15) return -1;
        if (waterC >= 12) return -2;
        if (waterC >= 8) return -3;
        if (waterC >= 4) return -4.5;
        return -6;
    }

    /** Useful time in water, game minutes, before the torso factor. */
    public static double usefulMinutes(double waterC) {
        if (waterC > 15) return UNLIMITED;
        if (waterC >= 8) return 20;
        return 10;
    }

    /** Useful time with the torso's band factor applied (1, 0.8 or 0.6). */
    public static double usefulMinutes(double waterC, double torsoFactor) {
        double t = usefulMinutes(waterC);
        return Double.isInfinite(t) ? t : t * torsoFactor;
    }

    /** Share of the swim rate a wader pays, by depth. */
    public static double wadingShare(double waterC, Depth d) {
        int col = switch (d) {
            case ANKLE -> 0;
            case KNEE -> 1;
            case THIGH -> 2;
            default -> -1;
        };
        if (d == Depth.SWIM) return 1;
        if (col < 0) return 0;
        double[] row;
        if (waterC >= HOT_POOL_C) return 0;
        if (waterC > 15) row = new double[] {0, 0, 0.1};
        else if (waterC >= 12) row = new double[] {0.05, 0.1, 0.25};
        else if (waterC >= 8) row = new double[] {0.1, 0.2, 0.35};
        else if (waterC >= 4) row = new double[] {0.1, 0.25, 0.5};
        else row = new double[] {0.15, 0.3, 0.6};
        return row[col];
    }

    /** Warmth change per minute from water contact at {@code depth}: the full rate swimming, a share wading. */
    public static double contactWarmthPerMin(double waterC, Depth depth) {
        if (depth == Depth.NONE) return 0;
        double rate = swimWarmthPerMin(waterC);
        if (depth == Depth.SWIM) return rate;
        return rate < 0 ? rate * wadingShare(waterC, depth) : 0;
    }
}

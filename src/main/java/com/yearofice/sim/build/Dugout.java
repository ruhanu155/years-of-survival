package com.yearofice.sim.build;

/**
 * Dugout rules (Every concept › Dugouts and rock shelters, Dugout hearth and chimney). No Minecraft imports.
 * Interiors are capped by kind; a hearth needs a flue or the room fills with smoke; deep ground is cleared only inside
 * a validated dugout (capped 4 x 2 x 6 m).
 */
public final class Dugout {
    private Dugout() {}

    public enum Kind {
        HILLSIDE(4, 2, 6), CUT_BANK(3, 2, 4), ROCK_SHELTER(5, 3, 4);
        public final int maxWide, maxHigh, maxDeep;
        Kind(int w, int h, int d) { maxWide = w; maxHigh = h; maxDeep = d; }
    }

    /** Why a dugout fails its check, or null when it passes. */
    public static String verdict(Kind k, int wide, int high, int deep, double coverBlocks, boolean sealed, boolean hearth, boolean flue) {
        if (wide < 1 || high < 1 || deep < 1) return "empty";
        if (wide > k.maxWide || high > k.maxHigh || deep > k.maxDeep) return "too_big";
        if (!sealed) return "open_shell";
        if (k != Kind.ROCK_SHELTER && coverBlocks < 1) return "no_cover";
        if (hearth && !flue) return "hearth_without_flue";
        return null;
    }

    /** Heat-loss coefficient W/(m2 K) of earth cover (1 m or more) and of a plank or log face. */
    public static final double U_EARTH = 0.3, U_WOOD = 1.2;

    /**
     * Steady interior temperature: earth faces see the ground, wood faces the air, and the stove's watts lift the
     * balance by W / K. A slice model until lane A's room model replaces it ({@code build.rooms}).
     */
    public static double interiorC(double airC, double groundC, double stoveW, double earthM2, double woodM2) {
        double k = U_EARTH * earthM2 + U_WOOD * woodM2;
        if (k <= 0) return airC;
        return (U_EARTH * earthM2 * groundC + U_WOOD * woodM2 * airC + stoveW) / k;
    }
}

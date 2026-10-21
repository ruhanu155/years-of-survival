package com.yearofice.sim.world;

/**
 * Calendar-driven looks and flags (Seasonal variants by colour remap; Blueprint › Calendar). Days are days of the
 * year, 0 = May 1. Colours are 0xRRGGBB.
 */
public final class SeasonModel {
    private SeasonModel() {}

    public static final int LEAF_GREEN = 0x4E7D32, LEAF_FALL = 0xC9801E, LEAF_BARE = 0x6B5A42, ASPEN_GOLD = 0xE2B92B,
            GRASS_GREEN = 0x6FA03E, GRASS_CURED = 0xA79A58, GRASS_DEAD = 0x8A7C5A, GRASS_SPRING = 0x9BB05A;

    /** 0 = green, 1 = full fall colour: the turn from {@code turnDoy} over {@code turnDays}. */
    public static double turn(int doy, SeasonTuning t) { return clamp01((doy - t.turnDoy()) / (double) t.turnDays()); }

    /** 1 = fully leafed, 0 = bare: out in spring, dropping after the turn. */
    public static double leafed(int doy, SeasonTuning t) {
        if (doy < t.turnDoy()) return clamp01((doy - t.leafOutDoy() + 1) / (double) t.leafOutDays());
        return 1 - clamp01((doy - t.dropDoy()) / (double) t.dropDays());
    }

    public static int leafColour(int doy, SeasonTuning t, boolean birchAspenWillow) {
        double l = leafed(doy, t);
        if (l <= 0) return LEAF_BARE;
        int fall = birchAspenWillow ? ASPEN_GOLD : LEAF_FALL;
        int c = lerp(LEAF_GREEN, fall, turn(doy, t));
        if (doy < t.leafOutDoy() + t.leafOutDays()) c = lerp(GRASS_SPRING, c, l);
        return c;
    }

    public static int grassColour(int doy, SeasonTuning t) {
        if (doy >= t.groundFrozenDoy() || doy < t.leafOutDoy()) return GRASS_DEAD;
        double cure = clamp01((doy - t.turnDoy()) / (double) (t.groundFrozenDoy() - t.turnDoy()));
        int c = lerp(GRASS_GREEN, GRASS_CURED, cure);
        return doy < t.leafOutDoy() + t.leafOutDays() ? lerp(GRASS_DEAD, c, clamp01((doy - t.leafOutDoy()) / (double) t.leafOutDays())) : c;
    }

    /** Tamarack gold from the dates ({@code tamarackFromDoy}..{@code tamarackToDoy}). */
    public static boolean tamarackGold(int doy, SeasonTuning t) { return doy >= t.tamarackFromDoy() && doy <= t.tamarackToDoy(); }

    /** The level-wide frozen-ground flag: freeze-up to thaw, by date. */
    public static boolean groundFrozen(int doy, SeasonTuning t) { return doy >= t.groundFrozenDoy() && doy < t.groundThawDoy(); }

    static int lerp(int a, int b, double f) {
        int r = (int) Math.round(((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = (int) Math.round((a & 255) * (1 - f) + (b & 255) * f);
        return r << 16 | g << 8 | bl;
    }

    static double clamp01(double v) { return v < 0 ? 0 : Math.min(1, v); }
}

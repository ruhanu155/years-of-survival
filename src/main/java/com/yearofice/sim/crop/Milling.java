package com.yearofice.sim.crop;

/** Threshing and milling by hand (Crop farming; Structures and buildables › Grist mill): pure numbers, grams and minutes. */
public final class Milling {
    private Milling() {}

    /** A hand quern grinds 1 kg an hour at light work. */
    public static final double QUERN_G_PER_HOUR = 1000.0;
    /** A flail takes 6 minutes a kilogram of sheaf. */
    public static final double FLAIL_MIN_PER_KG = 6.0;
    /** A dry sheaf is 40 % grain and 60 % straw by weight. */
    public static final double SHEAF_GRAIN_SHARE = 0.4;

    public static int millMinutes(double grams) { return (int) Math.ceil(grams / QUERN_G_PER_HOUR * 60.0); }

    public static int threshMinutes(double sheafG) { return (int) Math.ceil(sheafG / 1000.0 * FLAIL_MIN_PER_KG); }

    public static int grainG(double sheafG) { return (int) Math.round(sheafG * SHEAF_GRAIN_SHARE); }

    public static int strawG(double sheafG) { return (int) Math.round(sheafG) - grainG(sheafG); }

    /** Whole years a seed lot has aged: the year now less the year it was harvested (never negative). */
    public static int lotYears(int harvestYear, int yearNow) { return Math.max(0, yearNow - harvestYear); }
}

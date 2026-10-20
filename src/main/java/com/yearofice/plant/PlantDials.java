package com.yearofice.plant;

/** Debug and test dials for the year: the berry index (f_year) and the season's warmth factor. Reset to 1.0 on world load. */
public final class PlantDials {
    private PlantDials() {}

    public static volatile double berryIndex = 1.0;
    public static volatile double coneCrop = 1.0;
    /** Scales the normal-year degree-days (0.9 cold, 1.0 normal, 1.1 warm). */
    public static volatile double warmth = 1.0;

    public static void reset() { berryIndex = 1.0; coneCrop = 1.0; warmth = 1.0; }
}

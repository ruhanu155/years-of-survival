package com.yearofice.sim.plant;

/**
 * Mineral lick crust (Open world in depth › Salt from the mineral lick). A lick holds up to {@code capG} of crust
 * (seep 1000 g, clay bank 500 g). It regrows 0.2 kg a week, doubled in the first dry week after rain, only while the
 * ground is thawed (May to September). A scrape takes 50 g in 15 game minutes; 2 kg of crust boiled down with 1 armload
 * of wood gives 150 g of salt.
 */
public final class LickModel {
    private LickModel() {}

    public static final int SCRAPE_G = 50, SCRAPE_MIN = 15, STEP_G = 250, BOIL_CRUST_G = 2000, BOIL_SALT_G = 150;
    public static final int SEEP_CAP_G = 1000, CLAY_CAP_G = 500;
    /** Day of year (0 = May 1) at which the ground freezes again: the thaw runs May to September. */
    public static final int THAW_END_DOY = 153;

    public static boolean thawed(int doy) { return doy >= 0 && doy < THAW_END_DOY; }

    /** The crust state 0-4 (a step is 250 g, rounded up so a trace still shows). */
    public static int crustState(double grams) { return (int) Math.min(4, Math.ceil(grams / STEP_G - 1e-9)); }

    /**
     * Regrows {@code grams} over one day. {@code sinceRain} is the dry days since the last rain day before this one (0 on the day after rain);
     * a rain day regrows nothing, and each of the 7 dry days after rain regrows double. Returns the new grams; the
     * caller advances {@code sinceRain}.
     */
    public static double dayRegrow(double grams, int capG, int doy, boolean rainy, int sinceRain, double kgPerWeek) {
        if (!thawed(doy) || rainy) return Math.min(grams, capG);
        double rate = kgPerWeek * 1000.0 / 7.0 * (sinceRain <= 6 ? 2.0 : 1.0);
        return Math.min(capG, grams + rate);
    }
}

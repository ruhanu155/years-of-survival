package com.yearofice.sim.crop;

/**
 * The garden's pure rules (Blueprint › Crop farming; Land and seasons in play › Crop farming in play): germination,
 * growth units, frost and stand yield. No Minecraft imports; the block entity feeds it numbers.
 */
public final class CropModel {
    private CropModel() {}

    /** Seed depth classes (Land › Soil temperature and germination). */
    public enum Depth {
        RIGHT(1.0), SHALLOW(0.7), BURIED(0.6);
        public final double factor;
        Depth(double f) { factor = f; }
    }

    /** Soil temperature factor: under 5 C the seed rots (x0.3), 5-10 C x0.7, 10 C and over x1.0. */
    public static double soilFactor(double soilC) {
        if (soilC < 5.0) return 0.3;
        if (soilC < 10.0) return 0.7;
        return 1.0;
    }

    /** Moisture 0 dry x0.6, 1-2 right x1.0, 3 waterlogged x0.5. */
    public static double moistureFactor(int moisture) {
        return switch (Math.max(0, Math.min(3, moisture))) { case 0 -> 0.6; case 3 -> 0.5; default -> 1.0; };
    }

    /** Seed age: -10% for each year saved, and -30% more when damp. */
    public static double ageFactor(int yearsOld, boolean damp) {
        double f = 1.0 - 0.10 * Math.max(0, yearsOld);
        if (damp) f -= 0.30;
        return Math.max(0.0, f);
    }

    /** P(germ) = g * f_soil * f_moisture * f_depth * f_age, scaled by the dial and capped at 0.95. */
    public static double germination(double base, double soilC, int moisture, Depth depth, int yearsOld, boolean damp, double scale) {
        double p = base * soilFactor(soilC) * moistureFactor(moisture) * depth.factor * ageFactor(yearsOld, damp) * scale;
        return Math.max(0.0, Math.min(0.95, p));
    }

    /** Soil temperature: the air mean plus the bed offset (tilled bare bed +1 C). */
    public static double soilC(double airMeanC, double bedOffsetC) { return airMeanC + bedOffsetC; }

    /** A day's growth units: mean air above 5 C, x1.2 in June and July for the midnight sun (months 6 and 7). */
    public static double growthUnits(double meanAirC, int month, double warmth) {
        double u = Math.max(0.0, meanAirC - 5.0) * warmth;
        return (month == 6 || month == 7) ? u * 1.2 : u;
    }

    /** Stage 0 sown, 1 sprout, 2 leaf, 3 head, 4 ripe from growth units; 5 is dead and is set by frost, not here. */
    public static int stage(double units, double sproutAt, double ripeAt) {
        if (units < sproutAt) return 0;
        if (units >= ripeAt) return 4;
        double f = (units - sproutAt) / (ripeAt - sproutAt);
        return f < 0.45 ? 1 : f < 0.8 ? 2 : 3;
    }

    /** A grain block's yield multiplier: clamp(0.3 + 0.7 s / g, 0, 1.2); thin stands tiller. */
    public static double standYield(double stand, double baseGermination) {
        return Math.max(0.0, Math.min(1.2, 0.3 + 0.7 * stand / baseGermination));
    }

    /** Head frost: a night at or under {@code headMinC} between the sensitive units cuts the yield to {@code mult}. */
    public static double headFrostMult(double nightLowC, double units, double sensFrom, double sensTo, double headMinC, double mult) {
        return (units >= sensFrom && units <= sensTo && nightLowC <= headMinC) ? mult : 1.0;
    }

    /** The hard freeze ends the season (-5 C). */
    public static boolean hardFreeze(double nightLowC, double hardC) { return nightLowC <= hardC; }

    /** Days until ripe at {@code unitsPerDay}, for the crouch card ("ripe in about 12 days"). */
    public static int daysToRipe(double units, double ripeAt, double unitsPerDay) {
        if (units >= ripeAt) return 0;
        return unitsPerDay <= 0 ? Integer.MAX_VALUE : (int) Math.ceil((ripeAt - units) / unitsPerDay);
    }

    /** True while the sowing window is open; days are days of the year (0 = May 1), inclusive. */
    public static boolean inWindow(int doy, int from, int to) { return doy >= from && doy <= to; }
}

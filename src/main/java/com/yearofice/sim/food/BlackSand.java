package com.yearofice.sim.food;

/**
 * Black sand in the pan (video 2, A10): magnetite iron sand from creek gravel. A gold pan on rust-stained placer gravel
 * gives 0.3 kg per game hour, on other gravel 0.1; a crafted birch pan works at half speed; a sluice box triples the
 * pan. 4 kg of black sand equals one 3 kg iron nodule in the bloomery. Open water only, May to October. Pure Java;
 * original Year of Ice code.
 *
 * @param richKgH     kg per game hour on placer gravel (0.3)
 * @param plainKgH    kg per game hour on any other gravel (0.1)
 * @param birchSpeed  a birch pan's speed against the gold pan (0.5)
 * @param sluiceMult  a sluice box against the pan (3)
 * @param firstMonth  open-water season, first month (5)
 * @param lastMonth   open-water season, last month (10)
 */
public record BlackSand(double richKgH, double plainKgH, double birchSpeed, double sluiceMult, int firstMonth, int lastMonth) {

    public enum Tool { GOLD_PAN, BIRCH_PAN, SLUICE }

    public static BlackSand standard() { return new BlackSand(0.3, 0.1, 0.5, 3, 5, 10); }

    public boolean openWater(int month) { return month >= firstMonth && month <= lastMonth; }

    /** Kg of black sand per game hour; 0 outside the open-water months. */
    public double kgPerHour(Tool tool, boolean placer, int month) {
        if (!openWater(month)) return 0;
        double base = placer ? richKgH : plainKgH;
        return switch (tool) {
            case GOLD_PAN -> base;
            case BIRCH_PAN -> base * birchSpeed;
            case SLUICE -> base * sluiceMult;
        };
    }

    /** Kg of black sand that make one 3 kg iron nodule in the bloomery. */
    public static final double KG_PER_NODULE = 4.0;
}

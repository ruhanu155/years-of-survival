package com.yearofice.sim.ecosystem;

/**
 * Field care for big game (Blueprint › Field care): gutting within an hour keeps all the meat, later costs a
 * quarter of it; missing the quartering deadline turns 30% of the meat (the deep hindquarter); missing it by
 * double turns the whole carcass. Deadlines count from the kill and depend on the air.
 */
public final class FieldCare {
    private FieldCare() {}

    public static final double GUT_BY_H = 1.0;
    public static final double LATE_GUT_LOSS = 0.25;
    public static final double LATE_QUARTER_TURNING = 0.30;

    /** Hours from the kill by which the quarters must be off. */
    public static double quarterByHours(double airC) {
        if (airC > 10) return 4;
        if (airC >= 0) return 12;
        if (airC >= -10) return 8;
        return 4;
    }

    /** Hours until a carcass of this class freezes solid below freezing (Field care; moose 18, caribou 10, sheep 8, hare 2). */
    public static double freezesSolidHours(String species, double airC) {
        if (airC >= 0) return Double.POSITIVE_INFINITY;
        return switch (species) {
            case "yearofice:moose" -> 18;
            case "yearofice:caribou" -> 10;
            case "yearofice:snowshoe_hare" -> 2;
            default -> 8;
        };
    }

    /**
     * What dressing a body at {@code hoursSinceDeath} gives: the share of meat lost for late gutting, and the share
     * that has turned for late quartering. {@code gutHours} is the hours from the kill to the gut step.
     *
     * @param lost    share of the meat not recovered (late gut)
     * @param turning share of what is recovered that comes out Turning
     */
    public record Result(double lost, double turning) {
        public double usable() { return (1 - lost) * (1 - turning); }
    }

    public static Result dress(double gutHours, double hoursSinceDeath, double airC) {
        double lost = gutHours > GUT_BY_H ? LATE_GUT_LOSS : 0;
        double by = quarterByHours(airC);
        double turning = hoursSinceDeath > 2 * by ? 1.0 : hoursSinceDeath > by ? LATE_QUARTER_TURNING : 0;
        return new Result(lost, turning);
    }
}

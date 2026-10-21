package com.yearofice.sim.life;

/**
 * Life under the ice (Waters and fish › Winterkill): each still water body tracks dissolved oxygen from freeze-over,
 * DO(t+1) = min(12, DO - c (1 + snow/50) / max(0.1, Dmean - ice) + inflow), and below 3 mg/L each species dies at
 * 5% x (3 - DO) of its stock a day. The ice and snow curves are a stand-in for the world lane's IceService and
 * SnowService (one hardness number scales both); {@link #simulate} takes them as functions so the real ones drop in.
 * Pure Java. Original Year of Ice code.
 */
public final class Winter {
    private Winter() {}

    /** Days from freeze-over (Nov 1) to breakup (about Apr 25). */
    public static final int DAYS = 175;

    /** The type's oxygen demand c, mg/L x m a day. */
    public static double demand(String type) {
        return switch (type) {
            case "bog_pool", "bog" -> 0.024;
            case "beaver_pond", "pond" -> 0.018;
            case "oxbow_lake", "oxbow", "weed_shelf" -> 0.015;
            case "tundra_pond", "tarn" -> 0.012;
            case "big_lake", "lake" -> 0.006;
            default -> type.startsWith("tarn") ? 0.012 : 0;
        };
    }

    /** Stand-in ice thickness (m) {@code d} days after freeze-over in a winter of the given hardness (1 is normal). */
    public static double iceM(int d, double hard) {
        double g = Math.min(1, Math.sqrt(Math.max(d, 0) / 120.0)) * 0.62 * hard;
        return d > 150 ? g * Math.max(0, (DAYS - d) / 25.0) : g;
    }

    /** Stand-in snow on the ice (cm). */
    public static double snowCm(int d, double hard) {
        return d < 150 ? Math.min(50, d * 0.35) * hard : 50 * hard * Math.max(0, (DAYS - d) / 25.0);
    }

    public static double step(double doMgL, double c, double dMeanM, double iceM, double snowCm, double inflow, double start) {
        double next = doMgL - c * (1 + snowCm / 50.0) / Math.max(0.1, dMeanM - iceM) + inflow;
        return Math.max(0, Math.min(start, next));
    }

    /** The share of a stock that dies in a day at this DO. {@code speciesFactor}: 1 most fish, 1/3 stickleback, 0 blackfish. */
    public static double dailyDeath(double doMgL, double speciesFactor, double below, double perMg) {
        if (doMgL >= below) return 0;
        return Math.min(1, perMg * (below - doMgL)) * speciesFactor;
    }

    public static double speciesFactor(String species) {
        return switch (species) {
            case "alaska_blackfish" -> 0;
            case "ninespine_stickleback" -> 1.0 / 3.0;
            default -> 1;
        };
    }

    /** What a winter did to one water body. */
    public record Result(double finalDo, double minDo, double lostShare, int firstDeathDay) {}

    /**
     * Runs the whole winter day by day. Closed-form per body and year, so a body that loads after months away reads the
     * same answer as one that was watched (Catch-up on chunk load).
     */
    public static Result simulate(String type, double dMeanM, boolean fed, double hard, double speciesFactor, LifeTuning t) {
        return simulate(type, dMeanM, fed, speciesFactor, t, d -> iceM(d, hard), d -> snowCm(d, hard));
    }

    /** As above with the ice thickness (m) and snow (cm) read from a day since freeze-over: the seam for the world lane's services. */
    public static Result simulate(String type, double dMeanM, boolean fed, double speciesFactor, LifeTuning t,
                                  java.util.function.IntToDoubleFunction ice, java.util.function.IntToDoubleFunction snow) {
        double c = demand(type) * t.oxygenDemandMult(), in = fed ? 0.15 : 0;
        double doMg = t.oxygenStartMgL(), min = doMg, alive = 1;
        int first = -1;
        for (int d = 0; d < DAYS; d++) {
            doMg = step(doMg, c, dMeanM, ice.applyAsDouble(d), snow.applyAsDouble(d), in, t.oxygenStartMgL());
            min = Math.min(min, doMg);
            double death = dailyDeath(doMg, speciesFactor, t.oxygenDeathBelowMgL(), t.oxygenDeathPerMg());
            if (death > 0 && first < 0) first = d;
            alive *= 1 - death;
        }
        return new Result(doMg, min, 1 - alive, first);
    }

    /** The winter's hardness for a year, 0.8 to 1.5 from a uniform draw (a hard winter is the rare tail). */
    public static double hardness(double u) { return 0.8 + 0.7 * u * u; }
}

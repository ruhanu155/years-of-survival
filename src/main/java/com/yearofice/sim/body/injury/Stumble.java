package com.yearofice.sim.body.injury;

/**
 * Stumbles, trips and falls on rough ground (Every concept from the video › Stumbles, trips and falls; Body tab ›
 * Falls, stumbles and slips). Pure: the chance per 100 m and the outcome of the roll.
 *
 * <pre>p = base * m_run * m_wet * m_load * m_energy * m_dark * m_injured / (pole ? 2 : 1)</pre>
 */
public final class Stumble {
    private Stumble() {}

    /** Everything that raises the chance; the adapter fills it from the world. */
    public record Factors(boolean running, boolean wet, boolean loaded, boolean tired, boolean dark, boolean pole,
                          double injuredFactor, boolean onTrail) {
        public static Factors calm() { return new Factors(false, false, false, false, false, false, 1, false); }
    }

    public enum Outcome { STUMBLE, FOOT_INJURY, FALL, SPRAIN }

    /** Chance of a stumble roll going off over 100 m, a fraction (0.003 = 0.3%). {@code basePct} is the dial. */
    public static double chance(double basePct, Factors f) {
        if (f.onTrail()) return 0;
        double p = basePct / 100.0;
        if (f.running()) p *= 3;
        if (f.wet()) p *= 2;
        if (f.loaded()) p *= 2;
        if (f.tired()) p *= 2;
        if (f.dark()) p *= 2;
        p *= Math.max(1, f.injuredFactor());
        if (f.pole()) p /= 2;
        return Math.min(1, p);
    }

    /** The outcome of one roll u in [0,1): 70% stumble, 20% foot injury, 8% fall, 2% sprain. */
    public static Outcome outcome(double u) {
        if (u < 0.70) return Outcome.STUMBLE;
        if (u < 0.90) return Outcome.FOOT_INJURY;
        if (u < 0.98) return Outcome.FALL;
        return Outcome.SPRAIN;
    }

    /** A fall costs 5 to 20 Condition. */
    public static double fallLoss(double u) { return 5 + 15 * u; }

    /** The injury_source id each outcome uses, or null for a plain stumble. */
    public static String source(Outcome o) {
        return switch (o) {
            case STUMBLE -> null;
            case FOOT_INJURY -> "stumble_foot";
            case FALL -> "stumble_fall";
            case SPRAIN -> "stumble_sprain";
        };
    }
}

package com.yearofice.sim.hunt;

import com.yearofice.sim.hunt.Shot.Channel;

/**
 * What a wound does and how it bleeds (Blueprint › The hit-zone model › Blood trail data; Every concept › Wounds that
 * last). Pure arithmetic on game minutes; the adapter in {@code hunt.wound} keeps one {@link Wound} per animal.
 *
 * <pre>
 *   zone          dies in        runs / beds                 drops per 10 m   wait
 *   heart, lungs  under 1 min    50-150 m, then drops        30               15 min
 *   liver         1-3 h          beds after 200-400 m        10               2 h
 *   gut           6-12 h         beds every ~300 m           3                6 h
 *   leg           survives       limps; heals in 10-20 days  5 for 100 m      tracks only
 * </pre>
 * {@code bleedPerMin} is the share of the animal's blood lost a minute, so the animal dies when the lost share reaches 1.
 */
public final class WoundModel {
    private WoundModel() {}

    public static final double VITALS_DIE_MIN = 0.8, LIVER_DIE_MIN = 120, GUT_DIE_MIN = 540;
    public static final double LEG_TRAIL_M = 100;

    /** Immutable wound state; {@code pushed} returns the pushed copy. */
    public record Wound(Channel channel, double hitMin, double dieAfterMin, double dropsPer10m, double trailLimitM, double waitMin,
                        double runMinM, double runMaxM, double bedEveryM, double healDays, int pushes, double trailMult, String look) {

        public double bleedPerMin() { return Double.isInfinite(dieAfterMin) ? 0 : 1.0 / dieAfterMin; }

        public double deathMin() { return hitMin + dieAfterMin; }

        public boolean lethal() { return !Double.isInfinite(dieAfterMin); }

        /** Speed factor while a leg wound heals (-15%). */
        public double speedX() { return channel == Channel.LEG ? 0.85 : 1.0; }

        /** Drops per metre now (pushing halves the trail). */
        public double dropsPerM(double travelledM) {
            if (travelledM >= trailLimitM) return 0;
            return dropsPer10m / 10.0 * trailMult;
        }

        /** Pushed before {@code hitMin + waitMin}: runs again 300-800 m, death timer +2 h, trail x0.5. */
        public Wound pushed(HuntTuning t) {
            return new Wound(channel, hitMin, lethal() ? dieAfterMin + t.pushExtraDeathMin() : dieAfterMin, dropsPer10m, trailLimitM,
                    waitMin, 300, 800, bedEveryM, healDays, pushes + 1, trailMult * t.pushTrailX(), look);
        }

        public boolean waitOver(double nowMin) { return nowMin >= hitMin + waitMin; }
    }

    /**
     * Opens a wound.
     *
     * @param bleedFactor the head's cut (bone 0.8 bleeds slower and so trails longer, steel 1.2)
     * @param roll        uniform [0,1) picking inside the blueprint ranges
     */
    public static Wound open(Channel c, double nowMin, double bleedFactor, double roll) {
        double bf = bleedFactor <= 0 ? 1 : bleedFactor;
        return switch (c) {
            case VITALS -> new Wound(c, nowMin, VITALS_DIE_MIN / bf, 30, Double.MAX_VALUE, 15, 50, 150, 0, 0, 0, 1, "bright red, pink froth");
            case LIVER -> new Wound(c, nowMin, (60 + 120 * roll) / bf, 10, Double.MAX_VALUE, 120, 200, 400, 0, 0, 0, 1, "dark red");
            case GUT -> new Wound(c, nowMin, (360 + 360 * roll) / bf, 3, Double.MAX_VALUE, 360, 150, 300, 300, 0, 0, 1, "sparse, with green stomach matter");
            case LEG -> new Wound(c, nowMin, Double.POSITIVE_INFINITY, 5, LEG_TRAIL_M, 0, 0, 0, 0, 10 + 10 * roll, 0, 1, "spotty");
            case NONE -> throw new IllegalArgumentException("no wound for NONE");
        };
    }

    /** Distance the animal runs before it drops or first beds, for a roll in [0,1). */
    public static double runM(Wound w, double roll) { return w.runMinM() + (w.runMaxM() - w.runMinM()) * roll; }

    /** Blood drops to place for {@code dM} metres moved when the animal has already travelled {@code travelledM}; fractions carry. */
    public static final class Emitter {
        private double carry;

        public int drops(Wound w, double travelledM, double dM) {
            if (dM <= 0) return 0;
            double mid = travelledM + dM / 2;
            carry += w.dropsPerM(mid) * dM;
            int n = (int) Math.floor(carry);
            carry -= n;
            return n;
        }
    }

    public enum Weather { CLEAR, RAIN, SNOW }

    /** Game minutes a drop lasts: 2 h, halved in rain, doubled in snow. */
    public static double dropLifeMin(Weather w, HuntTuning t) {
        return t.bloodLifeMin() * switch (w) { case CLEAR -> 1.0; case RAIN -> t.bloodRainX(); case SNOW -> t.bloodSnowX(); };
    }

    /** A wound met again heals after {@code healDays}; true when it is over. */
    public static boolean healed(Wound w, double nowMin) { return w.healDays() > 0 && nowMin >= w.hitMin() + w.healDays() * 1440.0; }
}

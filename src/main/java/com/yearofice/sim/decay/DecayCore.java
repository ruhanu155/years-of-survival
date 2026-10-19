package com.yearofice.sim.decay;

/**
 * The decay formula family (Decay and time › The decay clock).
 *
 * <p>Every decaying thing carries one number p from 0 (new) to 1 (become something else). Three drivers move
 * it (living rot, chemical, physical) and seven factors scale the rate:
 *
 * <pre>
 *   dp/dt = g(p) / τ · f_T(T) · f_W · f_S · f_G · f_B · f_P        g(p) = 1 (linear) or 1 − p (proportional)
 *   Δp_use = 1 / N_uses
 * </pre>
 *
 * Time is never ticked. The temperature factor is already integrated in the climate prefix sums
 * ({@link DecayClimate}), so a settle from t0 to t1 takes D = D(t1) − D(t0) decay-hours and the factor
 * product F, and has a closed form for both shapes. Settling hourly and settling once over the whole interval
 * agree, which is the Catch-up and determinism rule.
 *
 * <p>Units: τ is in decay-hours (one hour at 5 °C, damp, shade, off the ground, no pests, unprotected).
 * A valley year is about 390 decay-days. Structures' "r a year" for proportional classes is per valley year.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class DecayCore {
    private DecayCore() {}

    public enum Driver { LIVING_ROT, CHEMICAL, PHYSICAL }

    public enum Shape {
        /** Loses the same amount each hour: food, hides, hay, metal, clay, snow. */
        LINEAR,
        /** Loses a share of what is left: wood, bark, bone, antler. Never quite reaches 1. */
        PROPORTIONAL
    }

    /** The Standard valley year in decay-days (Decay and time › The decay-day). */
    public static final double VALLEY_YEAR_DECAY_DAYS = 390.0;
    public static final double VALLEY_YEAR_DECAY_HOURS = VALLEY_YEAR_DECAY_DAYS * 24.0;
    /** A proportional class's last stage starts here, since it never reaches 1. */
    public static final double PROPORTIONAL_LAST_STAGE = 0.97;
    /** The Spoilage model's safe life L ends at freshness 30, which is p = 0.7. */
    public static final double SAFE_P = 0.7;

    /**
     * The seven factors; f_T comes from the clock. Each class lists which apply, and every factor not listed
     * is 1. f_U (use) is separate: see {@link #use}.
     */
    public record Factors(double moisture, double sun, double ground, double biota, double protection) {
        public static final Factors REFERENCE = new Factors(1, 1, 1, 1, 1);

        public Factors {
            if (moisture < 0 || sun < 0 || ground < 0 || biota < 0 || protection < 0) {
                throw new IllegalArgumentException("factors must be non-negative");
            }
        }

        public double product() { return moisture * sun * ground * biota * protection; }

        public Factors withMoisture(double v) { return new Factors(v, sun, ground, biota, protection); }
        public Factors withGround(double v) { return new Factors(moisture, sun, v, biota, protection); }
        public Factors withProtection(double v) { return new Factors(moisture, sun, ground, biota, v); }
        public Factors withBiota(double v) { return new Factors(moisture, sun, ground, v, protection); }
        public Factors withSun(double v) { return new Factors(moisture, v, ground, biota, protection); }
    }

    /**
     * Settle p over {@code decayHours} of clock time (the f_T integral) at factor product {@code factor}.
     *
     * @param tauDh the class's reference life from p = 0 to p = 1 in decay-hours (for proportional classes
     *              the e-folding time, 1 ÷ r)
     */
    public static double settle(Shape shape, double p0, double decayHours, double factor, double tauDh) {
        if (tauDh <= 0) throw new IllegalArgumentException("tau must be positive");
        if (decayHours < 0) throw new IllegalArgumentException("time runs forward");
        double x = decayHours * factor / tauDh;
        double p = switch (shape) {
            case LINEAR -> p0 + x;
            case PROPORTIONAL -> 1.0 - (1.0 - p0) * StrictMath.exp(-x);
        };
        return DecayLadder.clamp01(p);
    }

    /** Decay-hours at factor F needed to take p from p0 to pTarget; infinite if never (F = 0 or p never reaches it). */
    public static double decayHoursTo(Shape shape, double p0, double pTarget, double factor, double tauDh) {
        if (pTarget <= p0) return 0.0;
        if (factor <= 0) return Double.POSITIVE_INFINITY;
        return switch (shape) {
            case LINEAR -> (pTarget - p0) * tauDh / factor;
            case PROPORTIONAL -> pTarget >= 1.0 ? Double.POSITIVE_INFINITY
                    : StrictMath.log((1.0 - p0) / (1.0 - pTarget)) * tauDh / factor;
        };
    }

    /** One use takes 1 ÷ durability (Crafting › Tool durability). */
    public static double use(double p, int durabilityUses) {
        if (durabilityUses <= 0) throw new IllegalArgumentException("durability must be positive");
        return DecayLadder.clamp01(p + 1.0 / durabilityUses);
    }

    /** Food: the safe life L (to p = 0.7) at 5 °C gives τ = L ÷ 0.7, in decay-hours. */
    public static double tauFromSafeLifeDays(double safeLifeDaysAt5c) {
        return safeLifeDaysAt5c * 24.0 / SAFE_P;
    }

    /** Proportional classes: r per valley year gives τ = 1 ÷ r valley years, in decay-hours. */
    public static double tauFromRatePerValleyYear(double r) {
        if (r <= 0) throw new IllegalArgumentException("rate must be positive");
        return VALLEY_YEAR_DECAY_HOURS / r;
    }

    /** Linear classes given as a share lost per valley year (metal 3% a year: τ = 33 valley years). */
    public static double tauFromLinearSharePerValleyYear(double share) {
        return tauFromRatePerValleyYear(share);
    }

    /**
     * A settled mark: p0 at clock reading d0 under a fixed factor product. Reading it at any later D recomputes
     * from the origin, so an hourly read and one read after three years give identical bits; rebase only when
     * the factors change (the thing moved, lost its bark, got a roof). This is what {@code yearofice:decay_marks}
     * and the {@code yearofice:decay} component store.
     */
    public record Mark(Shape shape, double p0, double d0, double factor, double tauDh) {
        public double progress(double dNow) { return settle(shape, p0, Math.max(0.0, dNow - d0), factor, tauDh); }

        public Mark rebase(double dNow, double newFactor) {
            return new Mark(shape, progress(dNow), dNow, newFactor, tauDh);
        }

        /** The clock reading at which p reaches a threshold u (a roof block's fall), or +∞. */
        public double dueAt(double u) { return d0 + decayHoursTo(shape, p0, u, factor, tauDh); }
    }

    /** Decay-hours to valley years. */
    public static double valleyYears(double decayHours) { return decayHours / VALLEY_YEAR_DECAY_HOURS; }
}

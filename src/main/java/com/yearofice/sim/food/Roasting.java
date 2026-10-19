package com.yearofice.sim.food;

import com.yearofice.sim.env.FireSetting;
import com.yearofice.sim.fire.FireState;

/**
 * Spit or stick roast over a fire ring (Blueprint › Cooking, the Spit row and Doneness), the slice's one cooking
 * method. A spit holds one load; its cook time comes from the load, and its progress (a share of the cook time)
 * accrues only while the ring below is in a cooking state, at that state's speed. The ring is the {@link Doneness}
 * colour ring: Raw, Rare from 50%, Done from 90%, Burnt at 150% (112.5% while the fire is Hot). Burnt is latched:
 * turning the fire down does not unburn the meat.
 *
 * <p>Load rules (which rule an item uses is data, {@code data/yearofice/cooking/spit.json}; the minutes are
 * tuning, {@link RoastTuning}):
 * <ul>
 *   <li>{@link Rule#WHOLE_HARE}: "whole hare 25". A hare's dressed meat (9 units, 900 g) is the whole hare; a smaller
 *       load takes its share of the time.</li>
 *   <li>{@link Rule#PER_KG}: "30 game minutes per kg" of meat.</li>
 *   <li>{@link Rule#WHOLE_FISH}: "fish 12" for a whole fish under 1 kg (Items › Food values: "Spit roast 12 min a
 *       fish under 1 kg"); a heavier fish roasts at the per-kg rate.</li>
 * </ul>
 * Bear meat's double time is {@link Doneness#cookMinutes}'s rule and arrives with bears in M6.
 *
 * <p>Pure Java. Original Year of Ice code.
 */
public final class Roasting {
    private Roasting() {}

    public enum Rule { WHOLE_HARE, PER_KG, WHOLE_FISH }

    /** Game minutes to cook a load to 100% of its cook time on a fire at speed 1. */
    public static double spitMinutes(Rule rule, double grams, RoastTuning t) {
        if (!(grams > 0)) throw new IllegalArgumentException("a load weighs something");
        return switch (rule) {
            case WHOLE_HARE -> t.spitWholeHareMin() * grams / t.wholeHareG();
            case PER_KG -> t.spitMinPerKg() * grams / 1000.0;
            case WHOLE_FISH -> grams < t.spitFishMaxG() ? t.spitFishMin() : t.spitMinPerKg() * grams / 1000.0;
        };
    }

    /** The fire as a cook sees it: Low, Steady and Hot are settings of a burning fire. */
    public static Doneness.Fire fireOf(FireState state, FireSetting setting) {
        if (state == null) return Doneness.Fire.OUT;
        return switch (state) {
            case OUT -> Doneness.Fire.OUT;
            case EMBER -> Doneness.Fire.EMBER;
            case TINDER -> Doneness.Fire.TINDER;
            case SMALL -> Doneness.Fire.SMALL_FIRE;
            case COALS -> Doneness.Fire.COALS;
            case BANKED -> Doneness.Fire.BANKED;
            case FIRE -> switch (setting == null ? FireSetting.STEADY : setting) {
                case LOW -> Doneness.Fire.LOW;
                case STEADY -> Doneness.Fire.STEADY;
                case HOT -> Doneness.Fire.HOT;
            };
        };
    }

    /** Cook-time share gained per game minute of cook time: 0 unless the fire cooks. */
    public static double speed(Doneness.Fire f, RoastTuning t) {
        if (!Doneness.cooks(f)) return 0.0;
        return switch (f) {
            case LOW -> t.speedLow();
            case STEADY -> t.speedSteady();
            case HOT -> t.speedHot();
            case COALS -> t.speedCoals();
            default -> 0.0;
        };
    }

    /** Where a piece taken off a spit earlier starts when it goes back on: the start of its ring's band. */
    public static double startProgress(Doneness.Ring r) {
        if (r == null) return 0.0;
        return switch (r) {
            case RAW -> 0.0;
            case RARE -> Doneness.RARE_FROM;
            case DONE -> Doneness.DONE_FROM;
            case BURNT -> Doneness.BURNT_AT;
        };
    }

    /**
     * A load on a spit: its progress as a share of cook time and the burnt latch.
     *
     * @param cookMinutes the load's cook time at speed 1, {@link #spitMinutes}
     */
    public record Spit(double cookMinutes, double progress, boolean burnt) {
        public Spit {
            if (!(cookMinutes > 0)) throw new IllegalArgumentException("cook time must be positive");
            if (progress < 0 || Double.isNaN(progress)) throw new IllegalArgumentException("progress");
        }

        public static Spit load(double cookMinutes, Doneness.Ring already) {
            return new Spit(cookMinutes, startProgress(already), already == Doneness.Ring.BURNT);
        }

        /** After {@code minutes} game minutes over a fire held in state {@code f} (progress is monotonic, so one step equals many). */
        public Spit after(double minutes, Doneness.Fire f, RoastTuning t) {
            if (minutes < 0) throw new IllegalArgumentException("time runs forward");
            double rate = speed(f, t) / cookMinutes;
            double p = progress + rate * minutes;
            boolean b = burnt || (rate > 0 && Doneness.ring(p, Doneness.Method.SPIT, f) == Doneness.Ring.BURNT);
            return new Spit(cookMinutes, p, b);
        }

        /** The colour ring now, with the fire in state {@code f}. */
        public Doneness.Ring ring(Doneness.Fire f) {
            if (burnt) return Doneness.Ring.BURNT;
            Doneness.Ring r = Doneness.ring(progress, Doneness.Method.SPIT, f);
            // Off the heat, a piece past the burn line it never crossed still reads Done.
            return r == Doneness.Ring.BURNT ? Doneness.Ring.DONE : r;
        }

        /** Game minutes from now until the ring reaches {@code target} on a steady fire state, or ∞. */
        public double minutesUntil(double target, Doneness.Fire f, RoastTuning t) {
            double rate = speed(f, t) / cookMinutes;
            if (progress >= target) return 0.0;
            return rate > 0 ? (target - progress) / rate : Double.POSITIVE_INFINITY;
        }
    }

    /** True once a ring means the food is cooked: Rare, Done or Burnt (the stack's state becomes cooked). */
    public static boolean cooked(Doneness.Ring r) { return r != null && r != Doneness.Ring.RAW; }
}

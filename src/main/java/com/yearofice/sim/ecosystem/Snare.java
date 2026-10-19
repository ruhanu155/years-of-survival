package com.yearofice.sim.ecosystem;

import com.yearofice.sim.decay.Thresholds;
import java.util.ArrayList;
import java.util.List;

/**
 * The trail snare, settled in closed form (Blueprint › Traps; Minecraft engine › Traps and nets; Every concept
 * from the video › Snare funnels, Trap sounds).
 *
 * <pre>
 *   P_catch = 1 − (1 − p_s · m)^n        m = min(6, place) · bait · max(0.35, scent) · weather
 * </pre>
 *
 * Rolls happen at dawn and dusk only ({@link SliceCalendar#rollMinute}). Each roll draws from the stream
 * (seed, "snare", x, y, z, k), so the outcome depends only on the trap and the roll, never on whether the chunk
 * was loaded: {@link #advance} over any split of a span gives the same state as one call over the whole span, and
 * chunk-load catch-up is that one call.
 *
 * <ul>
 *   <li>n is the hares whose home ranges cover the trap ({@link Env#hares}), less the hares this snare caught
 *       within the respawn window: "a trap whose last hare was just caught waits out the species'
 *       respawn_days".</li>
 *   <li>A snare holds one animal and stops rolling while it holds it.</li>
 *   <li>The catch happens 0–120 game minutes after its roll, drawn from the same stream.</li>
 *   <li>A catch left over 48 hours is taken at each later dawn with 35%; a taken snare is sprung until it is
 *       reset.</li>
 *   <li>A wire snare wears out after 20 catches.</li>
 * </ul>
 * Pure Java.
 */
public final class Snare {
    private Snare() {}

    public enum Status { SET, CAUGHT, TAKEN, BROKEN }

    /** Where and how a snare is set; the multipliers of the catch formula. */
    public record SetQuality(boolean onTrail, boolean besideCover, boolean funnel, boolean baited, double scent,
                             double weather) {
        public static final SetQuality PLAIN = new SetQuality(false, false, false, false, 1, 1);

        public double place(TrapTuning t) {
            double m = (onTrail ? t.trailMult() : 1) * (besideCover ? t.coverMult() : 1) * (funnel ? t.funnelMult() : 1);
            return Math.min(t.placeCap(), m);
        }

        public double multiplier(TrapTuning t) {
            return place(t) * (baited ? t.baitMult() : 1) * Math.max(t.scentFloor(), scent) * weather;
        }
    }

    /** A catch: the species, the roll that made it, and the game minute it happened. */
    public record Catch(String species, long roll, long minute) {}

    /**
     * @param setMinute  when the snare was set
     * @param lastRoll   the last roll index settled
     * @param held       the animal in the loop, or null
     * @param catches    catches over the snare's life (wear)
     * @param recent     game minutes of this snare's catches still inside the respawn window
     */
    public record State(long setMinute, long lastRoll, Status status, Catch held, int catches, List<Long> recent) {
        public State {
            recent = List.copyOf(recent);
        }

        public static State set(long minute, TrapTuning t) {
            return new State(minute, SliceCalendar.rollAtOrBefore(minute, t.dawnMin(), t.duskMin()), Status.SET, null, 0, List.of());
        }

        /** The held catch, once its minute has come. */
        public Catch visible(long now) { return held != null && held.minute() <= now ? held : null; }
    }

    /** What the world says at each roll. */
    public interface Env {
        long seed();
        int x();
        int y();
        int z();
        /** Hares whose home ranges cover the trap at roll k (before this snare's own catches). */
        double hares(long roll);
        SetQuality quality();
        TrapTuning tuning();
    }

    public static double pCatch(double pS, double m, double n) {
        if (n <= 0) return 0;
        double p = Math.min(1, Math.max(0, pS * m));
        return 1 - StrictMath.pow(1 - p, n);
    }

    /** Hares covering the trap at roll k, after this snare's catches within the respawn window. */
    public static double effectiveHares(State s, Env env, long roll) {
        TrapTuning t = env.tuning();
        long at = SliceCalendar.rollMinute(roll, t.dawnMin(), t.duskMin());
        double window = t.respawnDays() * SliceCalendar.DAY_MIN;
        int taken = 0;
        for (long m : s.recent()) if (m <= at && at - m < window) taken++;
        return Math.max(0, env.hares(roll) - taken);
    }

    /** Settles every roll up to and including {@code toMinute}. */
    public static State advance(State s, long toMinute, Env env) {
        TrapTuning t = env.tuning();
        long last = SliceCalendar.rollAtOrBefore(toMinute, t.dawnMin(), t.duskMin());
        for (long k = s.lastRoll() + 1; k <= last; k++) s = roll(s, k, env);
        return s;
    }

    static State roll(State s, long k, Env env) {
        TrapTuning t = env.tuning();
        long at = SliceCalendar.rollMinute(k, t.dawnMin(), t.duskMin());
        List<Long> recent = new ArrayList<>();
        for (long m : s.recent()) if (at - m < t.respawnDays() * SliceCalendar.DAY_MIN) recent.add(m);
        switch (s.status()) {
            case BROKEN, TAKEN -> {
                return new State(s.setMinute(), k, s.status(), s.held(), s.catches(), recent);
            }
            case CAUGHT -> {
                Catch c = s.held();
                if (SliceCalendar.isDawn(k) && at - c.minute() > t.theftAfterH() * 60) {
                    double u = Thresholds.stream(env.seed(), "snare_theft", env.x(), env.y(), env.z(), k).nextDouble();
                    if (u < t.theftPerDayPct() / 100.0) return new State(s.setMinute(), k, Status.TAKEN, null, s.catches(), recent);
                }
                return new State(s.setMinute(), k, Status.CAUGHT, c, s.catches(), recent);
            }
            default -> {
                State base = new State(s.setMinute(), k, Status.SET, null, s.catches(), recent);
                double p = pCatch(t.hareP(), env.quality().multiplier(t), effectiveHares(base, env, k));
                var r = Thresholds.stream(env.seed(), "snare", env.x(), env.y(), env.z(), k);
                if (r.nextDouble() >= p) return base;
                long minute = at + (long) Math.floor(r.nextDouble() * (t.catchOffsetMaxMin() + 1));
                recent.add(minute);
                return new State(s.setMinute(), k, Status.CAUGHT, new Catch("yearofice:snowshoe_hare", k, minute), s.catches() + 1, recent);
            }
        }
    }

    /** The player checks the snare: takes a visible catch, or resets a taken one. A worn wire breaks. */
    public static State check(State s, long now, TrapTuning t) {
        return switch (s.status()) {
            case CAUGHT -> s.visible(now) == null ? s
                    : new State(s.setMinute(), s.lastRoll(), s.catches() >= t.wireCatches() ? Status.BROKEN : Status.SET, null, s.catches(), s.recent());
            case TAKEN -> new State(s.setMinute(), s.lastRoll(), Status.SET, null, s.catches(), s.recent());
            default -> s;
        };
    }

    /** P per roll right now, for /yoi snare info. */
    public static double pNow(State s, Env env, long now) {
        TrapTuning t = env.tuning();
        long next = SliceCalendar.rollAtOrBefore(now, t.dawnMin(), t.duskMin()) + 1;
        return pCatch(t.hareP(), env.quality().multiplier(t), effectiveHares(s, env, next));
    }
}

package com.yearofice.sim.fishing;

import com.yearofice.core.hash.Mulberry32;
import com.yearofice.sim.decay.Thresholds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set lines, gill nets, weirs and traps settled in closed form (Blueprint › Fishing methods › Passive gear;
 * Waters and fish › Passive gear while away, reviewer C's section).
 *
 * <p>A settle covers the h game hours since the last one, at dawn, at dusk, when the player checks the gear and
 * for every dawn and dusk missed while unloaded:
 * <ol>
 *   <li>Catch: the count is Poisson with mean equal to the sum of the gear's hourly means over those hours.
 *       Each hour reads its own m_season, water, turbidity and stock N/K ({@link Hour}).</li>
 *   <li>Catch hours: each fish gets a catch hour in the period; the holds cap turns away fish caught after it
 *       fills. The hour is drawn in proportion to the hourly means, which is uniform when they are constant and
 *       keeps catches out of hours when the bait was gone.</li>
 *   <li>Losses since the catch hour t: a fish on a set line stays with 0.9^t (0.95^t on a gorge); where an
 *       otter's or bear's range covers the gear, each fish survives theft with 0.95^t; a fish dead in a net over
 *       12 hours is eaten 25% of the time.</li>
 * </ol>
 * All rolls come from the stream (seed, "trap", pos, day, period), so a chunk loaded all month and one loaded at
 * its end give the same catch. Pure Java. Original Year of Ice code.
 */
public final class PassiveGear {
    private PassiveGear() {}

    public static final int NET_MIN_SEGMENTS = 4;
    public static final double NET_SEGMENT_M = 3.0;
    public static final double LAKE_SPAN_M = 30.0;
    public static final int HOLDS_HOOK = 1, HOLDS_NET_SEGMENT = 8, HOLDS_WEIR = 15, HOLDS_MINNOW = 10,
            HOLDS_WHEEL = 30, HOLDS_BLACKFISH = 60;
    public static final double LINE_STAY = 0.9, LINE_STAY_GORGE = 0.95, THEFT_STAY = 0.95;
    public static final double NET_DEAD_HOURS = 12.0, NET_DEAD_EATEN = 0.25;
    public static final double SET_LINE_MAX = 0.5, SET_LINE_PER_BITE = 30.0;

    public enum Kind { SET_LINE, GILL_NET, WEIR, MINNOW_TRAP, FISH_WHEEL }

    public enum Fate { KEPT, TURNED_AWAY, ESCAPED, STOLEN, EATEN_IN_NET }

    /** One game hour's mean catch per species (index order fixed by the gear's species list). */
    public record Hour(double[] meanPerSpecies) {
        public Hour {
            meanPerSpecies = meanPerSpecies.clone();
        }

        public double total() {
            double t = 0;
            for (double m : meanPerSpecies) t += m;
            return t;
        }
    }

    public record Catch(int species, double catchHour, Fate fate) {}

    /** Set line, per baited hook and game hour: min(0.5, 30 × P_bite). */
    public static double setLineHourly(double pBitePerMinute) {
        return Math.min(SET_LINE_MAX, SET_LINE_PER_BITE * pBitePerMinute);
    }

    /**
     * Gill-net coverage: joined length (3 m a segment, plus any wing) over the channel's width, or over 30 m in
     * a lake, capped at 1. Fewer than 4 joined segments never roll.
     */
    public static double netCoverage(int joinedSegments, double wingM, double channelWidthM, boolean lake) {
        if (joinedSegments < NET_MIN_SEGMENTS) return 0.0;
        double span = lake ? LAKE_SPAN_M : channelWidthM;
        if (span <= 0) throw new IllegalArgumentException("span must be positive");
        return Math.min(1.0, (joinedSegments * NET_SEGMENT_M + wingM) / span);
    }

    /** Gill net hourly mean for one species: c × q × coverage × m_season × N/K. */
    public static double netHourly(double c, double q, double coverage, double mSeason, double stockFrac) {
        return c * q * coverage * mSeason * stockFrac;
    }

    /** Weir and basket hourly mean: 0.2 × q × m_season × N/K. */
    public static double weirHourly(double q, double mSeason, double stockFrac) {
        return 0.2 * q * mSeason * stockFrac;
    }

    /** Minnow trap hourly mean: 0.15 × q sculpin. */
    public static double minnowHourly(double q) { return 0.15 * q; }

    /** Fish wheel hourly mean: 1.0 × q × today's passage ÷ a mean run's peak passage. */
    public static double wheelHourly(double q, double passage, double meanPeakPassage) {
        return meanPeakPassage <= 0 ? 0 : q * passage / meanPeakPassage;
    }

    /** True if a fish on a set line stays for t hours, given a uniform draw. */
    public static boolean staysOnLine(double hoursLeft, boolean gorge, double u) {
        return u < StrictMath.pow(gorge ? LINE_STAY_GORGE : LINE_STAY, hoursLeft);
    }

    /**
     * Settle one period.
     *
     * @param holds       the gear's cap (hooks × 1, segments × 8, ...)
     * @param hours       the period's hours, oldest first
     * @param thiefCovers an otter's or bear's range covers the gear
     * @param period      0 for the dawn settle, 1 for dusk, 2+ for checks the same day
     * @return every fish caught, in catch-hour order, with its fate
     */
    public static List<Catch> settle(long seed, int x, int y, int z, long day, int period, Kind kind, int holds,
                                     List<Hour> hours, boolean gorge, boolean thiefCovers) {
        if (hours.isEmpty() || holds <= 0) return List.of();
        Mulberry32 r = Thresholds.stream(seed, "trap", x, y, z, day, period);
        int nSpecies = hours.get(0).meanPerSpecies().length;
        int h = hours.size();
        List<Catch> raw = new ArrayList<>();
        for (int sp = 0; sp < nSpecies; sp++) {
            double[] cum = new double[h + 1];
            for (int i = 0; i < h; i++) cum[i + 1] = cum[i] + hours.get(i).meanPerSpecies()[sp];
            int count = poisson(cum[h], r.nextDouble());
            for (int f = 0; f < count; f++) raw.add(new Catch(sp, catchHour(cum, r.nextDouble()), Fate.KEPT));
        }
        // Earliest first; species index then draw order break ties, so the order never depends on hashing.
        List<Catch> sorted = new ArrayList<>(raw);
        sorted.sort((a, b) -> a.catchHour() != b.catchHour() ? Double.compare(a.catchHour(), b.catchHour())
                : Integer.compare(a.species(), b.species()));
        List<Catch> out = new ArrayList<>(sorted.size());
        int held = 0;
        for (Catch c : sorted) {
            if (held >= holds) {
                out.add(new Catch(c.species(), c.catchHour(), Fate.TURNED_AWAY));
                continue;
            }
            held++;
            double left = h - c.catchHour();
            Fate fate = Fate.KEPT;
            if (kind == Kind.SET_LINE && !staysOnLine(left, gorge, r.nextDouble())) fate = Fate.ESCAPED;
            if (fate == Fate.KEPT && thiefCovers && r.nextDouble() >= StrictMath.pow(THEFT_STAY, left)) fate = Fate.STOLEN;
            if (fate == Fate.KEPT && kind == Kind.GILL_NET && left > NET_DEAD_HOURS && r.nextDouble() < NET_DEAD_EATEN) {
                fate = Fate.EATEN_IN_NET;
            }
            out.add(new Catch(c.species(), c.catchHour(), fate));
        }
        return Collections.unmodifiableList(out);
    }

    /** Count kept fish per species, for {@code EcologyService.harvest}. */
    public static int[] kept(List<Catch> catches, int nSpecies) {
        int[] k = new int[nSpecies];
        for (Catch c : catches) {
            if (c.fate() == Fate.KEPT) k[c.species()]++;
        }
        return k;
    }

    /** Inverse-transform Poisson draw; exact for the small means passive gear produces. */
    static int poisson(double mean, double u) {
        if (mean <= 0) return 0;
        double p = StrictMath.exp(-mean);
        double cdf = p;
        int k = 0;
        while (u >= cdf && k < 10_000) {
            k++;
            p *= mean / k;
            cdf += p;
            if (p == 0.0 && cdf < u) break; // tail underflow: stop at the mode's far side
        }
        return k;
    }

    /** A catch hour in [0, h), drawn in proportion to the cumulative hourly mean. */
    static double catchHour(double[] cum, double u) {
        int h = cum.length - 1;
        double total = cum[h];
        if (total <= 0) return u * h;
        double target = u * total;
        for (int i = 0; i < h; i++) {
            if (target < cum[i + 1]) {
                double w = cum[i + 1] - cum[i];
                return i + (w > 0 ? (target - cum[i]) / w : 0);
            }
        }
        return h - 1e-9;
    }
}

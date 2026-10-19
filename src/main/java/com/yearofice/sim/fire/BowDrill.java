package com.yearofice.sim.fire;

/**
 * The bow-drill minigame's numbers and its heat meter (Blueprint › The bow drill, step by step). The client's
 * screen drives a {@link HeatMeter} from real input; JUnit and the server's judge drive the same meter, so the
 * screen, the bots and the plausibility check share one model.
 *
 * <ul>
 *   <li>Stroke: a beat pulses at 1.5 strokes a second; a stroke within 0.15 s of the beat adds full heat, off the
 *       beat a third.</li>
 *   <li>Press: right mouse holds pressure; the green band is 40–70%. Under 40% a stroke adds nothing; over 85% the
 *       bow stalls and the string slips (−15 heat, 2 s to reset).</li>
 *   <li>Heat fills 0 to 100 and cools 8 points a second without strokes (none for two beats). Thin smoke at 40, thick at 75, an ember
 *       at 100. A board below 60 dryness caps the heat at 60: plenty of smoke, never an ember.</li>
 *   <li>Full heat per stroke is 100 ÷ 90, so a perfect rhythm takes 60 s and an average one (70% of strokes on the
 *       beat) about 75 s: the Blueprint's 60–90 s of steady strokes.</li>
 *   <li>Ember to flame: tip it into the tinder (2 s), then 3 good puffs of 1–1.5 s each; a puff past 1.5 s scatters
 *       the ember.</li>
 *   <li>At Practiced (10 fires) stroke and press merge into one 6-second hold that fails only on wet wood.</li>
 * </ul>
 */
public final class BowDrill {
    private BowDrill() {}

    public static final double STROKES_PER_S = 1.5;
    public static final double BEAT_S = 1 / STROKES_PER_S;
    public static final double BEAT_WINDOW_S = 0.15;
    public static final double FULL_HEAT = 100.0 / 90.0;
    public static final double OFF_BEAT_HEAT = FULL_HEAT / 3;
    public static final double COOL_PER_S = 8;
    /** "Without strokes" means no stroke for two beats (1.33 s), so a late or missed stroke never cools. */
    public static final double COOL_AFTER_S = 2 * BEAT_S;
    public static final double PRESS_LOW = 0.40;
    public static final double PRESS_HIGH = 0.70;
    public static final double PRESS_STALL = 0.85;
    public static final double STALL_HEAT = 15;
    public static final double STALL_RESET_S = 2;
    public static final double WET_BOARD_CAP = 60;
    public static final double SMOKE_THIN = 40;
    public static final double SMOKE_THICK = 75;
    public static final double EMBER_AT = 100;
    public static final double SEAT_S = 2;
    public static final double TIP_S = 2;
    public static final double PUFF_MIN_S = 1.0;
    public static final double PUFF_MAX_S = 1.5;
    public static final int PUFFS_FOR_FLAME = 3;
    public static final double PRACTICED_HOLD_S = 6;
    /** Close clock: 6 game seconds a real second (Minecraft development › Layer 1), so 60–90 s charge 6–9 game minutes. */
    public static final double CLOSE_GAME_S_PER_REAL_S = 6;

    /** The least real time a stroked ember can take: seating plus 90 perfect strokes. */
    public static double fastestStrokedS() { return SEAT_S + Math.ceil(EMBER_AT / FULL_HEAT) / STROKES_PER_S; }

    /** The least real time the Practiced hold can take. */
    public static double fastestHeldS() { return SEAT_S + PRACTICED_HOLD_S; }

    /** Game minutes the close clock charges for {@code realS} seconds of drilling. */
    public static double gameMinutes(double realS) { return realS * CLOSE_GAME_S_PER_REAL_S / 60.0; }

    /** True when {@code reps} make the Fire skill Practiced. */
    public static boolean practiced(double reps, FireTuning t) { return reps >= t.drillSkillReps(); }

    /** The most heat a stroke list can have built, ignoring cooling and stalls (the judge's ceiling). */
    public static double heatCeiling(int strokes, int onBeat) {
        int on = Math.max(0, Math.min(onBeat, strokes));
        return on * FULL_HEAT + (strokes - on) * OFF_BEAT_HEAT;
    }

    /**
     * The Automatic-assist chance (a hold with the same success chance): 70% fresh rising to 95% at Practiced, in a
     * straight line with reps, × 0.85 in wind over 6 m/s without shelter. Wet wood, wet tinder and low energy are
     * the judge's, not part of the chance.
     */
    public static double autoChance(double reps, double windMs, boolean sheltered, FireTuning t) {
        double f = Math.min(reps, t.drillSkillReps()) / t.drillSkillReps();
        double p = (t.drillFreshPct() + (t.drillPracticedPct() - t.drillFreshPct()) * f) / 100.0;
        if (windMs > t.emberWindMs() && !sheltered) p *= t.drillWindPct() / 100.0;
        return p;
    }

    /** True when a stroke at time {@code s} since the start falls within the beat window. */
    public static boolean onBeat(double s) {
        double phase = s % BEAT_S;
        return phase <= BEAT_WINDOW_S || BEAT_S - phase <= BEAT_WINDOW_S;
    }

    /** The heat meter: one per attempt. Time is real seconds since the strokes started. */
    public static final class HeatMeter {
        private final double cap;
        private double heat;
        private double sinceStroke = 1e9;
        private double stallLeft;
        private int strokes;
        private int onBeat;
        private int stalls;
        private double peak;

        /** @param boardWet a board below 60 dryness: heat stops at 60. */
        public HeatMeter(boolean boardWet) { this.cap = boardWet ? WET_BOARD_CAP : EMBER_AT; }

        public double heat() { return heat; }
        public double peak() { return peak; }
        public int strokes() { return strokes; }
        public int onBeatStrokes() { return onBeat; }
        public int stalls() { return stalls; }
        public boolean stalled() { return stallLeft > 0; }
        public boolean ember() { return heat >= EMBER_AT - 1e-9; }
        public int smoke() { return heat >= SMOKE_THICK ? 2 : heat >= SMOKE_THIN ? 1 : 0; }

        /**
         * One stroke of the bow at {@code timeS} with pressure {@code pressure} (0–1). Returns true when the stroke
         * stalled the bow.
         */
        public boolean stroke(double timeS, double pressure) {
            if (stallLeft > 0) return false;
            strokes++;
            sinceStroke = 0;
            if (pressure > PRESS_STALL) {
                stalls++;
                heat = Math.max(0, heat - STALL_HEAT);
                stallLeft = STALL_RESET_S;
                return true;
            }
            boolean beat = onBeat(timeS);
            if (beat) onBeat++;
            if (pressure < PRESS_LOW) return false;
            heat = Math.min(cap, heat + (beat ? FULL_HEAT : OFF_BEAT_HEAT));
            peak = Math.max(peak, heat);
            return false;
        }

        /** Time passing: cooling when no stroke came within a beat, and the stall reset. */
        public void tick(double dtS) {
            if (stallLeft > 0) stallLeft = Math.max(0, stallLeft - dtS);
            sinceStroke += dtS;
            if (sinceStroke > COOL_AFTER_S) heat = Math.max(0, heat - COOL_PER_S * dtS);
        }
    }
}

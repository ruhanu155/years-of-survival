package com.yearofice.sim.companion;

/**
 * The {@code [companion]} numbers (Taming a wolf, the sled team, the lead fence), three presets. Pure data:
 * {@code ecosystem.companion.CompanionConfig} builds the config keys from this record's fields.
 *
 * <p>{@code wolfLeaves}: 0 never (Gentle), 1 recoverable (Standard), 2 permanent (Brutal) -- the dial
 * {@code companion.wolf_leaves}.
 */
public record CompanionTuning(
        int wolfLeaves, double leaveBase, double leaveDivisor, int returnMinDays, int returnMaxDays,
        int capLow, int capMid, int capHigh, int neglectGraceDays, double neglectDecay,
        int sledMaxTeam, double sledPullKg, double sledTrotMps, double sledLaneFactor, double sledSnowFactor,
        double sledIceFactor, double sledBareFactor,
        double leadFenceFollowShort, double leadFenceFollowLong, double caribouSnareHold, int fenceLearnKills) {

    public static final int NEVER = 0, RECOVERABLE = 1, PERMANENT = 2;

    public enum Preset {
        GENTLE(make(NEVER, 3, 3, 2, 1.0, 4)),
        STANDARD(make(RECOVERABLE, 3, 2, 1, 1.0, 3)),
        BRUTAL(make(PERMANENT, 3, 2, 1, 2.0, 2));

        public final CompanionTuning tuning;

        Preset(CompanionTuning t) { tuning = t; }

        private static CompanionTuning make(int leaves, int low, int mid, int high, double decay, int graceDays) {
            return new CompanionTuning(leaves, 80, 400, 10, 20, low, mid, high, graceDays, decay,
                    6, 40, 3.5, 1.0, 0.8, 1.1, 0.35, 0.5, 0.8, 0.15, 3);
        }
    }

    public static CompanionTuning standard() { return Preset.STANDARD.tuning; }
}

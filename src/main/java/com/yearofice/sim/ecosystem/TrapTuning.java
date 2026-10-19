package com.yearofice.sim.ecosystem;

/**
 * Every snare number, as the {@code [traps]} section of {@code yearofice-common.toml} holds them (Blueprint ›
 * Traps; Every concept from the video › Snare funnels, Trap sounds). Sources per field.
 *
 * @param hareP          p_s, a hare's chance per roll in a trail snare (0.025)
 * @param trailMult      on a trail ×4
 * @param coverMult      beside cover ×1.5
 * @param funnelMult     a funnel of brush, sticks or logs on both sides ×1.3
 * @param placeCap       place multipliers multiply but cap at ×6
 * @param baitMult       baited ×1.5
 * @param scentFloor     scent multipliers floor at ×0.35
 * @param theftAfterH    a catch left this long may be taken (48 h)
 * @param theftPerDayPct then this chance per extra game day (35%)
 * @param catchOffsetMaxMin the moment of a catch falls 0–this many minutes after its roll (120)
 * @param wireCatches    a wire snare lasts this many catches (20)
 * @param respawnDays    a caught hare's place in the population refills after this many days (species
 *                       respawn_days, 6)
 * @param tellRangeM     a caught hare is heard within this range ("Snare on a hare: thrash and a short squeal,
 *                       40 m")
 * @param setMinutes     setting a snare takes this many game minutes (2)
 *
 * <p>Rolls fall at the sun's dawn and dusk ({@link Snare.Rolls}, Layer 1); the M2.5 stand-in keys {@code dawn_min}
 * and {@code dusk_min} (06:00 and 18:00) are retired.
 */
public record TrapTuning(double hareP, double trailMult, double coverMult, double funnelMult, double placeCap,
                         double baitMult, double scentFloor, double theftAfterH, double theftPerDayPct,
                         int catchOffsetMaxMin, int wireCatches, double respawnDays, double tellRangeM,
                         int setMinutes) {

    public static TrapTuning standard() { return Preset.STANDARD.tuning; }

    /** Gentle, Standard and Brutal (Blueprint › Realism presets). GameTests run on Standard. */
    public enum Preset {
        GENTLE(new TrapTuning(0.035, 4, 1.5, 1.3, 6, 1.5, 0.35, 72, 20, 120, 25, 4, 50, 2)),
        STANDARD(new TrapTuning(0.025, 4, 1.5, 1.3, 6, 1.5, 0.35, 48, 35, 120, 20, 6, 40, 2)),
        BRUTAL(new TrapTuning(0.02, 4, 1.5, 1.3, 6, 1.5, 0.35, 36, 50, 120, 15, 8, 40, 2));

        public final TrapTuning tuning;

        Preset(TrapTuning t) { this.tuning = t; }
    }
}

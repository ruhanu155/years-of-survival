package com.yearofice.sim.hud;

import java.util.Random;

/**
 * Which piece may play now (Art and audio production › Music tied to Spirits). Music is rare: Bright (over 70) gets
 * the dawn theme at dawn and day pieces 15-30 real minutes apart; Steady (30-70) day pieces 25-45 minutes apart;
 * Low (under 30) only the fire piece, only within 4 m of a lit fire. Danger fades music in 3 s and holds it off
 * for 5 real minutes. Pure; the client supplies the facts and plays the answer.
 */
public final class MusicPlan {
    private MusicPlan() {}

    public enum Piece { NONE, DAWN, DAY, FIRE }

    public static final long DANGER_HOLD_TICKS = 5L * 60 * 20;
    public static final int FADE_TICKS = 3 * 20;
    public static final double FIRE_RADIUS_M = 4.0;

    public enum Band { BRIGHT, STEADY, LOW }

    public static Band band(float spirits) { return spirits > 70 ? Band.BRIGHT : spirits >= 30 ? Band.STEADY : Band.LOW; }

    /** Minimum real minutes between pieces for the band, drawn from the doc's range by {@code r} in [0,1). */
    public static double gapMinutes(Band b, double r) {
        return switch (b) {
            case BRIGHT -> 15 + 15 * r;
            case STEADY -> 25 + 20 * r;
            case LOW -> 25 + 20 * r;
        };
    }

    /**
     * @param dawnNow         the clock is in the dawn window
     * @param fireDistM       distance to the nearest lit fire, metres (large when none)
     * @param ticksSinceLast  real ticks since the last piece ended
     * @param ticksSinceDanger real ticks since the last danger cue or close clock ended
     * @param inDanger        a danger cue or the close clock is on now
     */
    public static Piece choose(float spirits, boolean dawnNow, double fireDistM, long ticksSinceLast,
                               long ticksSinceDanger, boolean inDanger, double gapRoll) {
        if (inDanger || ticksSinceDanger < DANGER_HOLD_TICKS) return Piece.NONE;
        Band b = band(spirits);
        boolean gapOk = ticksSinceLast >= (long) (gapMinutes(b, gapRoll) * 60 * 20);
        return switch (b) {
            case BRIGHT -> dawnNow && ticksSinceLast >= 60 * 20 ? Piece.DAWN : gapOk ? Piece.DAY : Piece.NONE;
            case STEADY -> gapOk && !dawnNow ? Piece.DAY : Piece.NONE;
            case LOW -> gapOk && fireDistM <= FIRE_RADIUS_M ? Piece.FIRE : Piece.NONE;
        };
    }

    public static double roll(Random r) { return r.nextDouble(); }
}

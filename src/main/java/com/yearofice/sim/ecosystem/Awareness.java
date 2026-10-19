package com.yearofice.sim.ecosystem;

/**
 * The awareness score toward one source, 0–100 (Minecraft development › The shared state machine; Blueprint ›
 * How animals act, Senses by species, How birds react to the player).
 *
 * <pre>
 *   Δa = Σ g_s · (1 − d / R_s) · m_s · Δt        g_sight = 40, g_hearing = 25, g_scent = 15 per second at d = 0
 *   decay 2 a second with no sense firing (5 in cover)
 *   a plume reaching the animal sets a to at least 60 at once ("the moose winds you")
 * </pre>
 *
 * Thresholds: 30 Alert (or Freeze), 70 the species response, 100 the exact position; under 20 back to Routine.
 *
 * <p>Flush distance (Birds and the air › How birds react to the player), used for every animal that freezes:
 * {@code d = d_base · m_posture · m_approach · m_habit}, m_approach 1.5 head-on and 0.7 on a tangent (1.0 for a
 * still player), m_habit 0.2 once Habituated. Pure Java.
 */
public final class Awareness {
    private Awareness() {}

    public static final double GAIN_SIGHT = 40, GAIN_HEARING = 25, GAIN_SCENT = 15;
    public static final double DECAY_PER_S = 2, DECAY_COVER_PER_S = 5;
    public static final double SCENT_FLOOR = 60;
    public static final double MAX = 100;
    public static final double HEAD_ON = 1.5, TANGENT = 0.7, HABITUATED = 0.2;
    /** cos(45°): an approach within 45° of straight at the animal is head-on. */
    public static final double HEAD_ON_COS = Math.sqrt(0.5);

    /** s = 1 − d ÷ R, never below 0; 0 when the range is 0. */
    public static double stimulus(double distanceM, double rangeM) {
        if (rangeM <= 0) return 0;
        return Math.max(0, 1 - distanceM / rangeM);
    }

    public static double sightRange(Senses s, Posture p, double lightFactor, double coverFactor) {
        return s.sightM() * p.sightRange * lightFactor * coverFactor;
    }

    public static double hearingRange(Senses s, Posture p) { return s.hearingM() * p.noise; }

    /** Awareness after one sense sample of {@code dtS} seconds. */
    public static double gain(double a, double gainPerS, double stimulus, Posture p, double dtS) {
        return clamp(a + gainPerS * stimulus * p.gain * dtS);
    }

    /** Scent: at least {@link #SCENT_FLOOR} at once, then the normal rise. */
    public static double smell(double a, double stimulus, Posture p, double dtS) {
        if (stimulus <= 0) return a;
        return gain(Math.max(a, SCENT_FLOOR), GAIN_SCENT, stimulus, p, dtS);
    }

    public static double decay(double a, double dtS, boolean inCover) {
        return Math.max(0, a - (inCover ? DECAY_COVER_PER_S : DECAY_PER_S) * dtS);
    }

    /**
     * True when the bearing to the source lies inside the field of view. Angles in degrees, yaw as Minecraft
     * measures it; only the difference matters.
     */
    public static boolean inView(double fovDeg, double facingDeg, double bearingDeg) {
        double diff = Math.abs(wrap(bearingDeg - facingDeg));
        return diff <= fovDeg / 2.0;
    }

    /**
     * Flush distance in metres.
     *
     * @param approachCos cosine between the source's motion and the line from it to the animal (1 = straight at it)
     */
    public static double flushDistance(double baseM, Posture p, double approachCos, boolean habituated) {
        double approach = !p.moving() ? 1.0 : approachCos >= HEAD_ON_COS ? HEAD_ON : TANGENT;
        return baseM * p.flush * approach * (habituated ? HABITUATED : 1.0);
    }

    static double wrap(double deg) {
        double d = deg % 360.0;
        if (d > 180) d -= 360;
        if (d < -180) d += 360;
        return d;
    }

    static double clamp(double a) { return Math.max(0, Math.min(MAX, a)); }
}

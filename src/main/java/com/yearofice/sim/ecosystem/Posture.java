package com.yearofice.sim.ecosystem;

/**
 * How the player (or any threat) is moving, as the senses read it (Blueprint › Senses by species, How birds react
 * to the player; Minecraft development › The shared state machine).
 *
 * <ul>
 *   <li>{@link #sightRange}: a still player is seen at half range, a crouching one at a third, both at 0.17.</li>
 *   <li>{@link #gain}: m_s, 1.5 for a running player and 0.33 crouched.</li>
 *   <li>{@link #noise}: footstep noise relative to walking (sprint 30 m, walk 10 m, crouch 3 m; a still player makes
 *       none), which scales the species' "hears walking at" range.</li>
 *   <li>{@link #flush}: m_posture of the flush formula, 1.0 walking, 1.6 running, 0.6 crouched, 0.3 still.</li>
 * </ul>
 */
public enum Posture {
    STILL(0.5, 1.0, 0.0, 0.3),
    WALK(1.0, 1.0, 1.0, 1.0),
    RUN(1.0, 1.5, 3.0, 1.6),
    CROUCH(0.33, 0.33, 0.3, 0.6),
    CROUCH_STILL(0.17, 0.33, 0.0, 0.3);

    public final double sightRange;
    public final double gain;
    public final double noise;
    public final double flush;

    Posture(double sightRange, double gain, double noise, double flush) {
        this.sightRange = sightRange;
        this.gain = gain;
        this.noise = noise;
        this.flush = flush;
    }

    /** Under this ground speed a player counts as still. */
    public static final double STILL_BELOW_MPS = 0.3;

    public static Posture of(double speedMps, boolean crouching, boolean sprinting) {
        boolean still = speedMps < STILL_BELOW_MPS;
        if (crouching) return still ? CROUCH_STILL : CROUCH;
        if (still) return STILL;
        return sprinting || speedMps > 4.5 ? RUN : WALK;
    }

    public boolean moving() { return this != STILL && this != CROUCH_STILL; }
}

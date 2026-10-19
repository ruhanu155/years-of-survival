package com.yearofice.build.work;

import net.minecraft.server.level.ServerPlayer;

/**
 * One hold-to-work job (Player systems and client › Hold-to-work and the use key): it advances in game deciseconds
 * at the Clock's Work rate while the player holds use on its target, and says how far it is for the ring.
 */
public interface WorkJob {
    /** Whether the job may go on this tick: the target is still there, in reach, with the right thing in hand. */
    boolean valid(ServerPlayer p);

    /** Advances the job by {@code ds} game deciseconds of work. */
    void work(ServerPlayer p, long ds);

    /** True once the job has done all it will do. */
    boolean finished();

    /** Progress for the ring, 0–1. */
    float share();

    /** The share gained per game decisecond, for the client's prediction. */
    float sharePerDs();

    WorkProgressPayload.Phase phase();

    /** Counted steps done and needed, for the ring's label. */
    int done();

    int of();

    /** Called once when the hold ends before the job finished. */
    default void stopped(ServerPlayer p) {}
}

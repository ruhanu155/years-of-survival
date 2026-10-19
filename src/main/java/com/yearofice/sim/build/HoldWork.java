package com.yearofice.sim.build;

/**
 * Hold-to-work timing (Minecraft development › Player systems and client › Hold-to-work and the use key), pure
 * Java. A job is a number of game minutes; while the use key is held the clock runs the Work rate, so the job takes
 * {@code minutes × 600 ÷ work_ds} real ticks (5 game minutes a real second at the shipped 150 ds a tick), and never
 * fewer than {@code minTicks}, so even a one-minute job shows its ring. A held use key repeats every 4 ticks on a
 * block; a gap longer than {@link #MAX_GAP_TICKS} means the key was let go, and the job starts over.
 */
public final class HoldWork {
    private HoldWork() {}

    /** Game deciseconds in a game minute. */
    public static final int DS_PER_MINUTE = 600;
    /** The client repeats a held use every 4 ticks; up to 6 between two repeats still counts as held. */
    public static final int MAX_GAP_TICKS = 6;

    /** Real ticks a {@code minutes}-minute job takes at {@code workDsPerTick}, at least {@code minTicks}. */
    public static int ticksFor(double minutes, int workDsPerTick, int minTicks) {
        if (workDsPerTick <= 0) throw new IllegalArgumentException("work rate must be positive");
        int t = (int) Math.ceil(minutes * DS_PER_MINUTE / workDsPerTick - 1e-9);
        return Math.max(Math.max(1, minTicks), t);
    }

    /** One player's hold on one target: when it began and when the use key was last seen. */
    public record Hold(long target, long startTick, long lastTick) {

        /** The hold after a use at {@code tick} on {@code target}: continued, or a fresh one. */
        public Hold seen(long target, long tick) {
            if (target != this.target || tick - lastTick > MAX_GAP_TICKS || tick < lastTick) return start(target, tick);
            return new Hold(target, startTick, tick);
        }

        public static Hold start(long target, long tick) { return new Hold(target, tick, tick); }

        /** Ticks held so far. */
        public long held(long tick) { return tick - startTick; }

        /** Still held at {@code tick} (the key was seen within the gap). */
        public boolean live(long tick) { return tick - lastTick <= MAX_GAP_TICKS && tick >= lastTick; }
    }

    /** Whole items a job yields from {@code have} toward a stack limit: what fits, the rest is dropped. */
    public static int fits(int yield, int have, int stackMax) { return Math.max(0, Math.min(yield, stackMax - have)); }
}

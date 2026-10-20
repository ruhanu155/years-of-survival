package com.yearofice.sim.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * The field-note card gate (Creative concepts › Field-note cards): at most one card per 10 real minutes, later
 * triggers queue and show in order, a card queued more than 1 game day files to the notebook unseen, and nothing
 * shows during the close clock or a danger alert. Pure: the caller passes real time (server ticks) and game minutes.
 */
public final class FieldNoteQueue {
    public static final long GAP_TICKS = 10L * 60 * 20;
    public static final long STALE_GAME_MIN = 24 * 60;

    private record Queued(String id, long gameMin) {}

    private final Deque<Queued> queue = new ArrayDeque<>();
    private final Set<String> known = new HashSet<>();
    private long lastShownTick = Long.MIN_VALUE / 2;

    /** Marks a note as already filed (loaded from the save) so it never queues again. */
    public void markKnown(String id) { known.add(id); }

    public boolean isKnown(String id) { return known.contains(id); }

    /** A trigger fired. Returns true when the note is new (it is now filed and queued). */
    public boolean trigger(String id, long gameMin) {
        if (!known.add(id)) return false;
        queue.addLast(new Queued(id, gameMin));
        return true;
    }

    /** Queued notes older than a game day leave the queue: the caller keeps them in the notebook, marked new. */
    public int dropStale(long gameMin) {
        int n = 0;
        while (!queue.isEmpty() && gameMin - queue.peekFirst().gameMin > STALE_GAME_MIN) { queue.removeFirst(); n++; }
        return n;
    }

    /**
     * The card to show now, or empty. {@code blocked} is the close clock, a danger alert or a messenger that does
     * not work; the queue keeps its place while blocked.
     */
    public Optional<String> poll(long nowTick, long gameMin, boolean blocked) {
        dropStale(gameMin);
        if (blocked || queue.isEmpty() || nowTick - lastShownTick < GAP_TICKS) return Optional.empty();
        lastShownTick = nowTick;
        return Optional.of(queue.removeFirst().id);
    }

    public int pending() { return queue.size(); }
}

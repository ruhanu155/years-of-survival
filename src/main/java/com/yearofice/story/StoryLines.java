package com.yearofice.story;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * STAND-IN for Layer 8's {@code HudBus} (Minecraft development › Layer 8: Messenger and HUD bus), which lands with
 * the HUD stack in M2 lane B. Story lines go out the way every slice line already does, as the player's overlay
 * line ({@code displayClientMessage(..., true)}, where the pickup and warn lines show today), under the bus's own
 * rules: lang keys {@code messenger.yearofice.<type>.<id>}, a queue of 3, each line held 4 s plus 0.05 s a
 * character and at most 8 s before the next. When HudBus exists, {@link #post} becomes
 * {@code HudBus.post(player, HudLine.prompt|warn(...))} and the queue here is deleted.
 */
public final class StoryLines {
    private StoryLines() {}

    static final int QUEUE = 3;

    private record Line(Component text, int ticks) {}

    private static final class Out {
        final Deque<Line> queue = new ArrayDeque<>();
        long freeAt;
    }

    private static final Map<UUID, Out> OUT = new ConcurrentHashMap<>();

    /** How long a line holds the channel: 4 s + 0.05 s a character, at most 8 s, in ticks. */
    static int holdTicks(String text) {
        double s = Math.min(8.0, 4.0 + 0.05 * text.length());
        return (int) Math.round(s * 20);
    }

    /** Queues one line. A full queue drops the oldest waiting line (the bus keeps 3). */
    public static void post(ServerPlayer p, Component line) {
        Out o = OUT.computeIfAbsent(p.getUUID(), k -> new Out());
        synchronized (o) {
            if (o.queue.size() >= QUEUE) o.queue.pollFirst();
            o.queue.addLast(new Line(line, holdTicks(line.getString())));
        }
        drain(p, p.serverLevel().getGameTime());
    }

    /** Sends the next line when the channel is free. Called on post and every server tick. */
    static void drain(ServerPlayer p, long gameTime) {
        Out o = OUT.get(p.getUUID());
        if (o == null) return;
        synchronized (o) {
            if (o.queue.isEmpty() || gameTime < o.freeAt) return;
            Line l = o.queue.pollFirst();
            p.displayClientMessage(l.text(), true);
            o.freeAt = gameTime + l.ticks();
        }
    }

    /** Lines still waiting (the tests read it). */
    public static int waiting(ServerPlayer p) {
        Out o = OUT.get(p.getUUID());
        if (o == null) return 0;
        synchronized (o) {
            return o.queue.size();
        }
    }

    static void forget(UUID id) { OUT.remove(id); }

    static void clear() { OUT.clear(); }
}

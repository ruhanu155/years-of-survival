package com.yearofice.client.hud;

import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.client.ui.Units;
import com.yearofice.net.payload.HudLinePayload;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The client's copy of the bus's lines (Layer 8; Messenger screens › Messenger lines): one messenger line at a
 * time for 4 s plus 0.05 s a character, at most 8 s, with a queue of 3; the pickup line for 2 s; the prompt row
 * until it is withdrawn (or 6 s with no refresh); the readout row for 3 s. Journal lines never reach the client.
 */
public final class ClientHud {
    private ClientHud() {}

    public record Shown(Component text, int priority, long untilMs, String key) {}

    static final int QUEUE = 3;
    static final long PICKUP_MS = 2000, PROMPT_MS = 6000, READOUT_MS = 3000;

    private static final Deque<Shown> messengerQueue = new ArrayDeque<>();
    private static @Nullable Shown messenger, pickup, prompt, readout;

    public static synchronized void accept(HudLinePayload p) {
        long now = Util.getMillis();
        HudLineType type = p.lineType();
        if (p.clear()) {
            if (prompt != null && prompt.key().equals(p.key())) prompt = null;
            return;
        }
        Component text = text(p);
        switch (type.channel) {
            case PICKUP -> pickup = new Shown(text, p.priority(), now + PICKUP_MS, p.key());
            case PROMPT -> prompt = new Shown(text, p.priority(), now + PROMPT_MS, p.key());
            case READOUT -> readout = new Shown(text, p.priority(), now + READOUT_MS, p.key());
            case MESSENGER -> {
                Shown s = new Shown(text, p.priority(), 0, p.key());
                if (messengerQueue.size() >= QUEUE) messengerQueue.removeFirst();
                messengerQueue.addLast(s);
            }
            case JOURNAL -> { }
        }
    }

    static Component text(HudLinePayload p) {
        if (p.lineType() == HudLineType.PROMPT && p.args().size() == 1 && p.args().get(0).kind() == HudArg.Kind.TEXT) {
            return Component.literal(p.args().get(0).text());
        }
        Object[] args = p.args().stream().map(Units::arg).toArray();
        return Component.translatable(p.key(), args);
    }

    /** Duration a messenger line stays: 4 s plus 0.05 s a character, at most 8 s. */
    public static long messengerMs(String text) { return Math.min(8000, 4000 + 50L * text.length()); }

    public static synchronized @Nullable Shown messenger() {
        long now = Util.getMillis();
        if (messenger != null && now > messenger.untilMs()) messenger = null;
        if (messenger == null && !messengerQueue.isEmpty()) {
            Shown s = messengerQueue.removeFirst();
            messenger = new Shown(s.text(), s.priority(), now + messengerMs(s.text().getString()), s.key());
        }
        return messenger;
    }

    public static synchronized @Nullable Shown pickup() { return live(pickup); }

    public static synchronized @Nullable Shown prompt() { return live(prompt); }

    public static synchronized @Nullable Shown readout() { return live(readout); }

    private static @Nullable Shown live(@Nullable Shown s) { return s != null && Util.getMillis() <= s.untilMs() ? s : null; }

    /** A client-side prompt (the item-pickup ray): shown like a server prompt under {@code key}. */
    public static synchronized void localPrompt(String key, Component text) {
        prompt = new Shown(text, 2, Util.getMillis() + 150, key);
    }

    public static synchronized void localPickup(Component text) {
        pickup = new Shown(text, 1, Util.getMillis() + PICKUP_MS, "local");
    }

    public static synchronized void clear() {
        messengerQueue.clear();
        messenger = pickup = prompt = readout = null;
    }
}

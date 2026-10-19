package com.yearofice.api.hud;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;

/**
 * Layer 8, Messenger and HUD bus: every word a server system tells the player goes through here
 * (Minecraft development › Layer 8). Lines are flushed to the owner at the end of the tick.
 *
 * <p>Rules: messenger lines need the Trail Messenger in a neck slot or a hand with battery above 0, else they go
 * to the journal only; during the player's close clock only priority 3 messenger lines show and the rest wait;
 * the same key and arguments within 10 game minutes are dropped; any line from a trap is refused (an exception
 * in development, a drop in release).
 */
public interface HudBus {

    void post(ServerPlayer player, HudLine line);

    /** Offers the prompt row near the crosshair: {@code key} names the offer, {@code text} is its words. */
    void offerPrompt(ServerPlayer player, String key, String text, int priority);

    /** Clears the prompt row offered under {@code key}. */
    void clearPrompt(ServerPlayer player, String key);

    /** The journal lines kept this session for {@code player}, oldest first (the journal attachment is M4). */
    List<HudLine> journal(ServerPlayer player);

    /** The lines sent to {@code player}'s screen, newest last, at most 32 (debug and tests). */
    List<HudLine> sent(ServerPlayer player);

    /** Lines held for {@code player} until their close clock ends. */
    List<HudLine> held(ServerPlayer player);

    static HudBus get() {
        HudBus s = Holder.service;
        if (s == null) throw new IllegalStateException("HudBus not installed yet");
        return s;
    }

    static boolean installed() { return Holder.service != null; }

    static void install(HudBus bus) {
        if (Holder.service != null) throw new IllegalStateException("HudBus already installed");
        Holder.service = bus;
    }

    final class Holder {
        private static volatile HudBus service;

        private Holder() {}
    }
}

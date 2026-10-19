package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** A system offers a prompt to a player (Events and hooks); cancelling it keeps the prompt off the screen. */
public final class PromptOfferEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final String key;
    private final String text;
    private final int priority;

    public PromptOfferEvent(ServerPlayer player, String key, String text, int priority) {
        this.player = player;
        this.key = key;
        this.text = text;
        this.priority = priority;
    }

    public ServerPlayer player() { return player; }

    public String key() { return key; }

    public String text() { return text; }

    public int priority() { return priority; }
}

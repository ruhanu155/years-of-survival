package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Posted on the game bus by {@code hunt} when a bear or moose encounter changes the player's posture (RECONCILE 171).
 * The client plays the matching first-person hand clip and camera kick; the server already decided the state.
 */
public final class EncounterStateEvent extends Event {
    public enum State { PLAY_DEAD, PINNED, STRIKE, GET_UP, ENDED }

    private final ServerPlayer player;
    private final State state;

    public EncounterStateEvent(ServerPlayer player, State state) { this.player = player; this.state = state; }

    public ServerPlayer player() { return player; }

    public State state() { return state; }
}

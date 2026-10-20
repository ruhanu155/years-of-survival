package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * A player stumbled, tripped or fell (Body tab › Falls, stumbles and slips): posted on the game bus by
 * {@code body.injury}. The client animates the {@code stagger} hand clip and the camera dip from it (lane D); the
 * server decided the outcome already.
 *
 * @param outcome {@code stumble}, {@code foot_injury}, {@code fall} or {@code sprain}
 */
public final class StaggerEvent extends Event {
    private final ServerPlayer player;
    private final String outcome;

    public StaggerEvent(ServerPlayer player, String outcome) {
        this.player = player;
        this.outcome = outcome;
    }

    public ServerPlayer player() { return player; }

    public String outcome() { return outcome; }
}

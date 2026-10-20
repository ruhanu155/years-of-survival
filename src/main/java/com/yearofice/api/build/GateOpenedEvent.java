package com.yearofice.api.build;

import com.yearofice.sim.build.Tier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Posted on {@code NeoForge.EVENT_BUS} when a crafting gate opens for a player (owner {@code build}; readers: the notebook, story). */
public class GateOpenedEvent extends Event {
    private final ServerPlayer player;
    private final Tier tier;
    private final String cause;

    public GateOpenedEvent(ServerPlayer player, Tier tier, String cause) {
        this.player = player;
        this.tier = tier;
        this.cause = cause;
    }

    public ServerPlayer player() { return player; }
    public Tier tier() { return tier; }
    public String cause() { return cause; }
}

package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * {@code companion.wolf_leaves}: a tamed wolf left to join a pack in February. Posted on the game bus by
 * {@code ecosystem.companion}. {@code returnInDays} is 0 when it is gone for good (Brutal).
 */
public final class WolfLeavesEvent extends Event {
    private final ServerPlayer player;
    private final String name;
    private final int returnInDays;

    public WolfLeavesEvent(ServerPlayer player, String name, int returnInDays) {
        this.player = player;
        this.name = name;
        this.returnInDays = returnInDays;
    }

    public ServerPlayer player() { return player; }

    public String name() { return name; }

    public int returnInDays() { return returnInDays; }

    public boolean permanent() { return returnInDays <= 0; }
}

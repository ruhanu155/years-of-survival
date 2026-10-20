package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/** Posted when a player hangs a trophy mount (A6); the ambition tracker and the record book read it. */
public final class TrophyHungEvent extends Event {
    private final ServerPlayer player;
    private final String species, cls;
    private final int mountsHung;

    public TrophyHungEvent(ServerPlayer player, String species, String cls, int mountsHung) {
        this.player = player;
        this.species = species;
        this.cls = cls;
        this.mountsHung = mountsHung;
    }

    public ServerPlayer player() { return player; }

    public String species() { return species; }

    public String trophyClass() { return cls; }

    /** How many mounts this player has hung, this one included. */
    public int mountsHung() { return mountsHung; }
}

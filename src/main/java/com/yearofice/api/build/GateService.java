package com.yearofice.api.build;

import com.yearofice.sim.build.Tier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The crafting gates (Crafting and building in depth › Tiers and gates): Start is open in a new world, and the other
 * four tiers open on events in the world. Gates open independently, never close, and show one Trail Messenger line
 * when they open. Written only by {@code build.craft}; story and fire open theirs through here.
 */
public interface GateService {

    boolean isOpen(Player player, Tier tier);

    /** The gate mask, one bit per tier ({@link com.yearofice.sim.build.Gates}). */
    int mask(Player player);

    /**
     * Opens {@code tier} for the player, posting its line and a {@link GateOpenedEvent}; returns false if it was
     * already open. {@code cause} is for the log ("first fire", "command").
     */
    boolean open(ServerPlayer player, Tier tier, String cause);

    static GateService get() {
        GateService s = Holder.service;
        if (s == null) throw new IllegalStateException("GateService not installed yet");
        return s;
    }

    static boolean installed() { return Holder.service != null; }

    static void install(GateService service) {
        if (Holder.service != null) throw new IllegalStateException("GateService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile GateService service;

        private Holder() {}
    }
}

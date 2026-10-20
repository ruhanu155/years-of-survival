package com.yearofice.api.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * An animal strike tore the outer garment of a body zone (Body tab › What garments add; injury_source
 * {@code tears_outer}). Posted on the game bus by {@code body}; the clothing system subscribes and takes
 * {@code amount} off the worn Curios outer layer of {@code zone}. The body never touches the garment itself.
 *
 * @param zone   {@code head}, {@code torso}, {@code hands}, {@code legs} or {@code feet}
 * @param amount condition taken off the garment, 0 to 1 (0.15 for a bear or wolf)
 */
public final class GarmentTearEvent extends Event {
    private final ServerPlayer player;
    private final String zone, source;
    private final double amount;

    public GarmentTearEvent(ServerPlayer player, String zone, double amount, String source) {
        this.player = player;
        this.zone = zone;
        this.amount = amount;
        this.source = source;
    }

    public ServerPlayer player() { return player; }

    public String zone() { return zone; }

    public double amount() { return amount; }

    public String source() { return source; }
}

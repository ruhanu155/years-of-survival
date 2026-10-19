package com.yearofice.api.fire;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;

/**
 * Posted on {@code NeoForge.EVENT_BUS} when fire work costs the body: a bow-drill try charges 60 kcal and 6–9 game
 * minutes of time-lapse at the close clock (Blueprint › Units in these sections), whether it works or not. The body
 * owns the calorie ledger and the clock owns the rate; fire only reports the cost.
 */
public class FireEffortEvent extends Event {
    private final Player player;
    private final String activity;
    private final int kcal;
    private final double gameMinutes;

    public FireEffortEvent(Player player, String activity, int kcal, double gameMinutes) {
        this.player = player;
        this.activity = activity;
        this.kcal = kcal;
        this.gameMinutes = gameMinutes;
    }

    public Player player() { return player; }
    /** {@code "bow_drill"} for now. */
    public String activity() { return activity; }
    public int kcal() { return kcal; }
    public double gameMinutes() { return gameMinutes; }
}

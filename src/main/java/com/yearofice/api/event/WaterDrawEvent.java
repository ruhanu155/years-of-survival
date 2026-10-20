package com.yearofice.api.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.Event;

/**
 * Posted on the NeoForge bus each time the water-draw service samples a water edge near a player (Water, calls and
 * encounters › Water draws animals). Land drives subscribe and call {@link #draw} to name the species that come to
 * drink or fish here now; the service moves nobody itself.
 *
 * <p>{@code strength} is the drinking schedule at this minute of the day, 0 to 1 (peaks at dawn and dusk, low at
 * midday). {@code runStrength} is the strongest salmon run passing the spot today, 0 to 1: bears gather at a spot
 * with a run (RECONCILE 168, Bears in water). {@code spotType} is null at an edge with no generated fishing spot.
 *
 * @param edge        the water edge sampled (a water block next to land)
 * @param minuteOfDay game minute of the day, 0 to 1439
 * @param strength    drinking schedule, 0 to 1
 * @param runStrength salmon run strength at the spot today, 0 to 1
 */
public final class WaterDrawEvent extends Event {
    private final ServerLevel level;
    private final BlockPos edge;
    private final int minuteOfDay;
    private final double strength, runStrength;
    private final String spotType;
    private final List<String> drawn = new ArrayList<>();

    public WaterDrawEvent(ServerLevel level, BlockPos edge, int minuteOfDay, double strength, double runStrength, String spotType) {
        this.level = level;
        this.edge = edge;
        this.minuteOfDay = minuteOfDay;
        this.strength = strength;
        this.runStrength = runStrength;
        this.spotType = spotType;
    }

    public ServerLevel level() { return level; }
    public BlockPos edge() { return edge; }
    public int minuteOfDay() { return minuteOfDay; }
    public double strength() { return strength; }
    public double runStrength() { return runStrength; }
    public String spotType() { return spotType; }

    /** A subscriber names a species ({@code yearofice:moose}) that is drawn to this edge now. */
    public void draw(String speciesId) { drawn.add(speciesId); }

    public List<String> drawn() { return Collections.unmodifiableList(drawn); }
}

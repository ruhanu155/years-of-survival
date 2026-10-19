package com.yearofice.api.fire;

import com.yearofice.sim.fire.FireHeat;
import com.yearofice.sim.fire.FireState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * Posted on {@code NeoForge.EVENT_BUS} when a fire changes state (Minecraft development › Events: {@code FireStateEvent},
 * owner {@code fire}; readers: heat index, fire roster, scent). {@code line} is the reason in words, or null.
 */
public class FireStateEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final FireState from;
    private final FireState to;
    private final FireHeat heat;
    private final String line;

    public FireStateEvent(Level level, BlockPos pos, FireState from, FireState to, FireHeat heat, String line) {
        this.level = level;
        this.pos = pos.immutable();
        this.from = from;
        this.to = to;
        this.heat = heat;
        this.line = line;
    }

    public Level level() { return level; }
    public BlockPos pos() { return pos; }
    public FireState from() { return from; }
    public FireState to() { return to; }
    /** The fire as a heat source after the change. */
    public FireHeat heat() { return heat; }
    public String line() { return line; }
}

package com.yearofice.fire;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.sim.time.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Fire rings run on the game clock (Minecraft development › Layer 1, Clock events: "fires and rooms HIGH"): each
 * {@link ClockEvent.Minute} steps every loaded ring in a ticking chunk by the minutes it crossed, so a ring burns
 * one game minute per game minute at every rate (Close slows it with the clock, Work and Sleep speed it up). A
 * {@link ClockEvent.Jump} forward (an admin time set, the SOS flight) steps the rings through the jump in closed
 * form, at most {@link #MAX_JUMP_MIN}; a jump back steps nothing.
 */
public final class FireClock {
    private FireClock() {}

    /** The longest jump a ring steps through: 30 game days, well past any fire's fuel. */
    public static final int MAX_JUMP_MIN = 30 * Calendar.MINUTES_PER_DAY;

    static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FireClock::onMinute);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FireClock::onJump);
    }

    private static void onMinute(ClockEvent.Minute e) { stepLoaded(e.count()); }

    private static void onJump(ClockEvent.Jump e) {
        long minutes = Calendar.minute(e.toDs()) - Calendar.minute(e.fromDs());
        if (minutes > 0) stepLoaded((int) Math.min(minutes, MAX_JUMP_MIN));
    }

    static void stepLoaded(int minutes) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (BlockPos pos : FireIndex.INSTANCE.rings(level)) {
                if (!level.shouldTickBlocksAt(pos)) continue;
                if (level.getBlockEntity(pos) instanceof FireRingBlockEntity be && !be.isRemoved()) be.clockMinutes(minutes);
            }
        }
    }
}

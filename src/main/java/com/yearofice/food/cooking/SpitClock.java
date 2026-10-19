package com.yearofice.food.cooking;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.sim.time.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Spits run on the game clock, like the rings under them: each {@link ClockEvent.Minute} steps every loaded spit in a
 * ticking chunk by the minutes it crossed, at normal priority, after the rings (high) have stepped, so the spit reads
 * the fire as it is at the end of those minutes. A jump forward steps through the jump as the rings do (at most
 * 30 game days); a jump back steps nothing. Server thread only; in memory, rebuilt as spits load.
 */
public final class SpitClock {
    private SpitClock() {}

    static final int MAX_JUMP_MIN = 30 * Calendar.MINUTES_PER_DAY;

    private static final Map<ResourceKey<Level>, Set<BlockPos>> SPITS = new HashMap<>();

    public static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, SpitClock::onMinute);
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, SpitClock::onJump);
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> SPITS.clear());
    }

    static void add(Level level, BlockPos pos) { SPITS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable()); }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> s = SPITS.get(level.dimension());
        if (s != null) s.remove(pos);
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
            Set<BlockPos> s = SPITS.get(level.dimension());
            if (s == null || s.isEmpty()) continue;
            for (BlockPos pos : List.copyOf(s)) {
                if (!level.shouldTickBlocksAt(pos)) continue;
                if (level.getBlockEntity(pos) instanceof SpitBlockEntity be && !be.isRemoved()) be.clockMinutes(minutes);
            }
        }
    }
}

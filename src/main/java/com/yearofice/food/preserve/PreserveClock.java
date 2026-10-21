package com.yearofice.food.preserve;

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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Racks and caches run on the game clock: each {@link ClockEvent.Hour} steps every loaded one in a ticking chunk by an
 * hour; a forward {@link ClockEvent.Jump} steps through the jump (at most 30 game days). Caches settle their mice
 * weekly on {@link ClockEvent.Day}. Server thread only; in memory, rebuilt as the blocks load.
 */
public final class PreserveClock {
    private PreserveClock() {}

    static final int MAX_JUMP_H = 30 * 24;
    private static final Map<ResourceKey<Level>, Set<BlockPos>> POS = new HashMap<>();

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClockEvent.Hour e) -> stepLoaded(1));
        NeoForge.EVENT_BUS.addListener((ClockEvent.Jump e) -> {
            long h = (Calendar.minute(e.toDs()) - Calendar.minute(e.fromDs())) / 60;
            if (h > 0) stepLoaded((int) Math.min(h, MAX_JUMP_H));
        });
        NeoForge.EVENT_BUS.addListener((ClockEvent.Day e) -> dayLoaded(e.absDay()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> POS.clear());
    }

    static void add(Level level, BlockPos pos) { POS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable()); }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> s = POS.get(level.dimension());
        if (s != null) s.remove(pos);
    }

    private static void stepLoaded(int hours) {
        each(be -> { if (be instanceof RackBlockEntity r) r.clockHours(hours); else if (be instanceof FoodCacheBlockEntity c) c.spoilHours(hours); });
    }

    private static void dayLoaded(long absDay) {
        each(be -> { if (be instanceof FoodCacheBlockEntity c) c.day(absDay); });
    }

    private static void each(java.util.function.Consumer<net.minecraft.world.level.block.entity.BlockEntity> f) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            Set<BlockPos> s = POS.get(level.dimension());
            if (s == null || s.isEmpty()) continue;
            for (BlockPos pos : List.copyOf(s)) {
                if (!level.shouldTickBlocksAt(pos)) continue;
                var be = level.getBlockEntity(pos);
                if (be != null && !be.isRemoved()) f.accept(be);
            }
        }
    }
}

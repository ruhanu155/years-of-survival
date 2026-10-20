package com.yearofice.food.traps;

import com.yearofice.api.event.ClockEvent;
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
 * Snares roll on the clock's dawn and dusk (Minecraft development › Layer 1: "traps, birds and the chorus key off"
 * {@link ClockEvent.Dawn}): each {@code Dawn} and {@code Dusk} settles every loaded snare in a ticking chunk, and a
 * {@link ClockEvent.Jump} settles them through the jump in closed form. A snare in an unloaded chunk settles the
 * same rolls when it loads ({@link SnareBlockEntity#onLoad}). Server thread only; in memory, rebuilt as snares load.
 */
public final class TrapClock {
    private TrapClock() {}

    private static final Map<ResourceKey<Level>, Set<BlockPos>> SNARES = new HashMap<>();

    public static void init() {
        NeoForge.EVENT_BUS.addListener((ClockEvent.Dawn e) -> settleLoaded());
        NeoForge.EVENT_BUS.addListener((ClockEvent.Dusk e) -> settleLoaded());
        NeoForge.EVENT_BUS.addListener((ClockEvent.Jump e) -> settleLoaded());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> SNARES.clear());
    }

    static void add(Level level, BlockPos pos) { SNARES.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(pos.immutable()); }

    static void remove(Level level, BlockPos pos) {
        Set<BlockPos> s = SNARES.get(level.dimension());
        if (s != null) s.remove(pos);
    }

    static void settleLoaded() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            Set<BlockPos> s = SNARES.get(level.dimension());
            if (s == null || s.isEmpty()) continue;
            for (BlockPos pos : List.copyOf(s)) {
                if (!level.shouldTickBlocksAt(pos)) continue;
                var be = level.getBlockEntity(pos);
                if (be == null || be.isRemoved()) continue;
                if (be instanceof SnareBlockEntity sn) sn.settle(true);
                else if (be instanceof DeadfallBlockEntity d) d.settle(true);
            }
        }
    }
}

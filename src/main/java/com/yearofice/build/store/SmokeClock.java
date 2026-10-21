package com.yearofice.build.store;

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

/** Smokehouses step an hour on every game hour and through a forward jump (at most 30 game days). Server thread only. */
final class SmokeClock {
    private SmokeClock() {}

    private static final Map<ResourceKey<Level>, Set<BlockPos>> POS = new HashMap<>();

    static void init() {
        NeoForge.EVENT_BUS.addListener((ClockEvent.Hour e) -> step(1));
        NeoForge.EVENT_BUS.addListener((ClockEvent.Jump e) -> {
            long h = (Calendar.minute(e.toDs()) - Calendar.minute(e.fromDs())) / 60;
            if (h > 0) step((int) Math.min(h, 30 * 24));
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> POS.clear());
    }

    static void add(Level l, BlockPos p) { POS.computeIfAbsent(l.dimension(), k -> new HashSet<>()).add(p.immutable()); }

    static void remove(Level l, BlockPos p) {
        Set<BlockPos> s = POS.get(l.dimension());
        if (s != null) s.remove(p);
    }

    private static void step(int hours) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            Set<BlockPos> s = POS.get(level.dimension());
            if (s == null) continue;
            for (BlockPos p : List.copyOf(s)) {
                if (level.shouldTickBlocksAt(p) && level.getBlockEntity(p) instanceof SmokehouseBlockEntity be && !be.isRemoved()) be.stepHours(hours);
            }
        }
    }
}

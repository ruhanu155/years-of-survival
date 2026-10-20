package com.yearofice.fire;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudArg;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.sim.fire.FireRosterMath;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The roster's warning (Every concept from the video › Fire time and the fire roster › Sound and first use): a fire
 * with a pot on it that drops under 15 minutes of fuel while its builder is more than 30 m away sends one line,
 * "Fire: 12 min of fuel. Pot on it.", at most once per fire per game day. Within 30 m the crackle is the cue.
 */
public final class FireRosterWatch {
    private FireRosterWatch() {}

    private record Key(ResourceKey<Level> dim, BlockPos pos) {}

    private static final Map<Key, Long> WARNED = new HashMap<>();

    static void clear() { WARNED.clear(); }

    /** Called each game minute from {@link FireClock}. */
    static void minute(MinecraftServer server) {
        if (!HudBus.installed() || !ClockService.installed()) return;
        long day = Math.floorDiv(ClockService.get().nowMinute(), 1440L);
        for (ServerLevel level : server.getAllLevels()) {
            for (BlockPos pos : FireIndex.INSTANCE.rings(level)) {
                if (!(level.getBlockEntity(pos) instanceof FireRingBlockEntity be) || be.isRemoved() || be.builder() == null) continue;
                ServerPlayer p = server.getPlayerList().getPlayer(be.builder());
                if (p == null) continue;
                double d = Math.sqrt(pos.distToCenterSqr(p.getX(), p.getY(), p.getZ()));
                if (p.level() != level || d <= FireRosterMath.HEARD_M) continue;
                FireRosterMath.Row row = be.rosterRow(d);
                if (!row.amber()) continue;
                Key k = new Key(level.dimension(), pos.immutable());
                Long was = WARNED.get(k);
                if (was != null && was == day) continue;
                WARNED.put(k, day);
                HudBus.get().post(p, HudLine.of(HudLineType.WARN, "fire_low", "fire", HudArg.text(row.kind()), HudArg.of(Math.round(row.minutesLeft()))));
            }
        }
    }
}

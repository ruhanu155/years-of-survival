package com.yearofice.ecosystem.companion;

import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.sim.time.Calendar;
import com.yearofice.world.WorldHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The taming path's start (Blueprint: takes a wolf pup from a den while the pack is out hunting): once a summer (June to
 * August) a den pup is left alone 120-200 m from each player, tameable and unowned; trust does the rest (WolfEntity).
 * The pup comes from the den of the nearest pack range (worldgen.Ranges): it wanders out along the bearing from the player
 * toward that den and is left 120-200 m away (no den, e.g. a test world: any bearing).
 */
public final class DenPups {
    private DenPups() {}

    static final String KEY = "yoi_den_pup_year";

    public static void onDay(ClockEvent.Day e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int month = Calendar.month(Calendar.dayOfYearOfDay(e.absDay()));
        if (month < 6 || month > 8) return;
        int year = (int) Math.floorDiv(e.absDay(), 365L);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) leave(p, year);
    }

    /** Leaves a pup for this player if none came this year; false when the ground was not loaded (it tries again tomorrow). */
    public static boolean leave(ServerPlayer p, int year) {
        var vw = WorldHooks.world(p.serverLevel());
        return vw == null || leave(p, year, vw.life());
    }

    /** As {@link #leave(ServerPlayer, int)} with the home-range layers given (tests pass their own). */
    public static boolean leave(ServerPlayer p, int year, com.yearofice.worldgen.Ranges.Layers life) {
        ServerLevel level = p.serverLevel();
        CompoundTag root = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (root.contains(KEY) && root.getInt(KEY) >= year) return true;
        var den = life == null ? null : life.nearestWolfDen(p.getX(), p.getZ());
        double ang = den == null ? level.random.nextDouble() * Math.PI * 2
                : Math.atan2(den.z() - p.getZ(), den.x() - p.getX()) + (level.random.nextDouble() - 0.5) * 0.5;
        double rad = 120 + level.random.nextDouble() * 80;
        int x = p.getBlockX() + (int) Math.round(Math.cos(ang) * rad), z = p.getBlockZ() + (int) Math.round(Math.sin(ang) * rad);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return false;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (!level.getFluidState(new BlockPos(x, y - 1, z)).isEmpty()) return false;
        WolfEntity w = EcosystemRegistry.WOLF.get().create(level);
        if (w == null) return true;
        w.moveTo(x + 0.5, y, z + 0.5);
        w.makeTameable(0, 60);
        level.addFreshEntity(w);
        root.putInt(KEY, year);
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, root);
        HudBus.get().post(p, HudLine.of(HudLineType.EVENT, "den_pup", "ecosystem.companion"));
        return true;
    }
}

package com.yearofice.fire;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.yearofice.api.hud.HudBus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Fire time on the look row (Every concept from the video › Fire time and the fire roster): looking at a fire within 6 m
 * for 0.5 s offers a second prompt row, "Fire, Low. Fuel 52 min.", read from the same ring record as the roster. The server
 * decides what is looked at (a ray from the eye every 10 ticks, so the cost is one ray per player per half second); the
 * client only draws the row. The row clears when the player looks away.
 */
public final class FireLook {
    private FireLook() {}

    public static final String KEY = "fire_time";
    public static final double REACH_M = 6;
    /** The hold, in the 10-tick checks: one check apart is 0.5 s. */
    public static final int HOLD_CHECKS = 2;
    public static final int CHECK_TICKS = 10;

    private record Look(BlockPos pos, int checks, String shown) {}

    private static final Map<UUID, Look> LOOKS = new HashMap<>();

    static void init() {
        NeoForge.EVENT_BUS.register(new FireLook.Events());
    }

    static void clear() { LOOKS.clear(); }

    public static final class Events {
        @SubscribeEvent
        public void onTick(PlayerTickEvent.Post e) {
            if (e.getEntity() instanceof ServerPlayer p && p.tickCount % CHECK_TICKS == 0) check(p);
        }

        @SubscribeEvent
        public void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { LOOKS.remove(e.getEntity().getUUID()); }
    }

    /** One look check: the ring under the crosshair within reach, held for {@link #HOLD_CHECKS} checks, shows its row. */
    static void check(ServerPlayer p) {
        BlockPos at = null;
        HitResult hit = p.pick(REACH_M, 0f, false);
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK
                && p.serverLevel().getBlockState(b.getBlockPos()).getBlock() instanceof FireRingBlock) at = b.getBlockPos();
        Look was = LOOKS.get(p.getUUID());
        if (at == null) {
            if (was != null) {
                if (was.shown() != null && HudBus.installed()) HudBus.get().clearPrompt(p, KEY);
                LOOKS.remove(p.getUUID());
            }
            return;
        }
        int checks = was != null && was.pos().equals(at) ? was.checks() + 1 : 1;
        String shown = was != null && was.pos().equals(at) ? was.shown() : null;
        if (checks >= HOLD_CHECKS && p.serverLevel().getBlockEntity(at) instanceof FireRingBlockEntity be) {
            String line = be.readout(FireConfig.tuning());
            if (!line.equals(shown) && HudBus.installed()) HudBus.get().offerPrompt(p, KEY, line, 1);
            shown = line;
        }
        LOOKS.put(p.getUUID(), new Look(at.immutable(), checks, shown));
    }
}

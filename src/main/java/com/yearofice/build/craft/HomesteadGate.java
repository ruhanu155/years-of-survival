package com.yearofice.build.craft;

import com.yearofice.api.build.GateService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.sim.build.Homestead;
import com.yearofice.sim.build.Tier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens Homestead: the first river breakup the player lives through (the window around April 26) while standing in a
 * warm room, a roofed spot with a lit stove or fire close by. M8's RoomService.roomAt replaces the roof-and-fire test
 * with the real room when it lands (one line in {@link #warmRoom}).
 */
public final class HomesteadGate {
    private HomesteadGate() {}

    /** Ticks between looks at a player. */
    static final int SCAN_TICKS = 100;

    public static boolean warmRoom(ServerPlayer p) {
        var level = p.serverLevel();
        if (level.canSeeSky(p.blockPosition().above())) return false;
        return !FireHeatLookup.INSTANCE.sourcesNear(level, p.getX(), p.getY(), p.getZ()).isEmpty();
    }

    /** One look: opens the gate if the window and a warm room agree. Returns true when it opened now. */
    public static boolean scan(ServerPlayer p, int doy) {
        GateService g = GateService.get();
        if (g.isOpen(p, Tier.HOMESTEAD) || !Homestead.inWindow(doy)) return false;
        return Homestead.qualifies(doy, warmRoom(p)) && g.open(p, Tier.HOMESTEAD, "first breakup in a warm room");
    }

    static void onTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % SCAN_TICKS == 31 && !p.isSpectator() && ClockService.installed()) {
            scan(p, ClockService.get().dayOfYear());
        }
    }
}

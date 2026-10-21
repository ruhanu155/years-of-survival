package com.yearofice.hunt.fishing;

import com.yearofice.api.event.WaterDrawEvent;
import com.yearofice.api.world.FishSpotService;
import com.yearofice.sim.bird.DrinkSchedule;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Water draws animals (Water, calls and encounters › Water draws animals). Every {@link #PERIOD} ticks the service
 * samples up to three fishing spots near each player and posts a {@link WaterDrawEvent} for each: the drinking
 * schedule at this minute and the salmon run passing, for lane A's drives to answer with the species that come.
 */
public final class WaterDrawService {
    private WaterDrawService() {}

    public static final int PERIOD = 200;
    private static int posted;

    public static int posted() { return posted; }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % PERIOD != 11) return;
        for (ServerPlayer p : level.players()) {
            List<FishSpotService.Spot> near = WaterLife.spotsNear(level, p.blockPosition(), 64);
            for (int i = 0; i < Math.min(3, near.size()); i++) post(level, near.get(i));
        }
    }

    /** Posts the event for one spot now and returns it with whatever the subscribers drew. */
    public static WaterDrawEvent post(ServerLevel level, FishSpotService.Spot spot) {
        int mod = FishingClock.minuteOfDay(FishingClock.minute(level.getServer()));
        WaterDrawEvent ev = new WaterDrawEvent(level, spot.centre(), mod, DrinkSchedule.strength(mod), WaterLife.runStrength(level, spot), spot.type());
        NeoForge.EVENT_BUS.post(ev);
        posted++;
        return ev;
    }
}

package com.yearofice.build.camp;

import com.yearofice.api.clock.ClockService;
import com.yearofice.build.BuildConfig;
import com.yearofice.sim.build.HoldWork;
import com.yearofice.sim.time.Rate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/**
 * The building package's side of hold-to-work: how long a job is, and asking the clock for the Work rate while it
 * runs (Layer 1: a hold-to-work ring is filling, 5 game minutes a real second). Only a player in the player list asks:
 * a GameTest's detached bot never moves the shared clock, so parallel tests keep their own time.
 */
public final class Work {
    private Work() {}

    /** Real ticks a job of {@code minutes} game minutes takes at the Work rate. */
    public static int ticks(double minutes) {
        int ds = ClockService.installed() ? ClockService.get().dsPerTick(Rate.WORK) : 150;
        return HoldWork.ticksFor(minutes, ds, BuildConfig.minHoldTicks());
    }

    /** True for a player the server lists (a real player), false for a test's detached bot. */
    public static boolean listed(ServerPlayer p) {
        return p.server != null && p.server.getPlayerList().getPlayer(p.getUUID()) == p;
    }

    /** One tick of a job: the Work rate for this tick, and a swing every 10 ticks so the body books heavy work. */
    public static void tick(ServerPlayer p, long n, String why) {
        if (!listed(p) || !ClockService.installed()) return;
        ClockService.get().requestRate(p, Rate.WORK, why);
        if (n % 10 == 0) p.swing(InteractionHand.MAIN_HAND, true);
    }
}

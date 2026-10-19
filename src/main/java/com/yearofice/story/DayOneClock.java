package com.yearofice.story;

import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.FirstNight;
import com.yearofice.sim.time.Rate;
import com.yearofice.story.clock.ClockConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Blueprint › Start site › Day 1 rules: "The clock runs at half speed until the first fire is lit or 22:00, whichever
 * comes first." A player on the guided night (the rules hold during it only) with no fire caught yet asks the clock
 * for {@link Rate#DAY_ONE} every tick, through {@link ClockService#requestRate}, until their night's first day reaches
 * {@code [clock] day_one_until_hour}. The first fire is the onboarding's own: the {@code api.fire.FireStateEvent} it
 * saw at the player's ring, or the night's fire minute once its check booked it.
 *
 * <p>Any other rate the player asks wins (walking is Travel, a work ring is Work, sleep is Sleep), and across players
 * the clock runs at the slowest, so Day 1 only slows what would otherwise be the World rate. The ticker runs at normal
 * priority on {@code ServerTickEvent.Post}, before the clock's low-priority step.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class DayOneClock {
    private DayOneClock() {}

    static final String REASON = "day 1, no fire yet";

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ClockService.installed()) return;
        long minute = ClockService.get().nowMinute();
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            if (!p.isSpectator()) ask(p, minute);
        }
    }

    /** True while the Day 1 rule holds for {@code p} at absolute game minute {@code minute}. */
    public static boolean applies(ServerPlayer p, long minute) {
        FirstNight n = Onboarding.of(p).night();
        if (!n.active() || n.fireLitMinute() >= 0 || Onboarding.fireSeen(p)) return false;
        long firstDay = Math.floorDiv(n.startMinute(), 1440L);
        return minute >= n.startMinute() && minute < firstDay * 1440L + ClockConfig.dayOneUntilHour() * 60L;
    }

    /** One tick's ask: requests Day 1 for {@code p} when the rule holds; returns whether it asked. */
    public static boolean ask(ServerPlayer p, long minute) {
        if (!applies(p, minute)) return false;
        ClockService.get().requestRate(p, Rate.DAY_ONE, REASON);
        return true;
    }
}

package com.yearofice.story.ambition;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.api.event.TrophyHungEvent;
import com.yearofice.core.YearOfIce;
import com.yearofice.story.Journal;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Wires the calendar-driven ambitions and the ambition page ({@code /yoi ambitions}). */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class AmbitionEvents {
    private AmbitionEvents() {}

    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && ClockService.installed()) Ambitions.refresh(p, ClockService.get().absDay());
    }

    @SubscribeEvent
    public static void day(ClockEvent.Day e) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Ambitions.refresh(p, e.absDay());
            if (e.absDay() == 365) {
                Ambitions.fire(p, "calendar.year2_may1", e.absDay() * 1440L);
                if (Journal.flights(p) == 0) Ambitions.fire(p, "calendar.year2_may1_no_sos", e.absDay() * 1440L);
            }
        }
    }

    @SubscribeEvent
    public static void hung(TrophyHungEvent e) { Ambitions.fire(e.player(), "trophy.hung", ClockService.installed() ? ClockService.get().nowMinute() : 0); }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("ambitions")
                .executes(c -> {
                    var lines = Ambitions.page(c.getSource().getPlayerOrException());
                    lines.forEach(l -> c.getSource().sendSuccess(() -> Component.literal(l), false));
                    return lines.size();
                })));
    }
}

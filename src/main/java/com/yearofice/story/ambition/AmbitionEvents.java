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
            // "survive_minus_40": the day that ended had a low of -40 C or colder at the player, and the player is here to see the next
            try {
                var d = com.yearofice.api.env.EnvService.get().day(p.serverLevel(), e.absDay() - 1);
                if (d != null && d.meanC() - d.swingC() / 2.0 <= -40) Ambitions.fire(p, "body.survived_minus_40", e.absDay() * 1440L);
            } catch (RuntimeException ignored) { }
            // "see_the_thaw": the river's breakup (the season core's last ice event for the river class)
            try {
                var core = com.yearofice.world.season.SeasonState.get(p.serverLevel().getServer().overworld()).core;
                if (core.lastEvent[com.yearofice.sim.world.IceModel.BodyClass.RIVER.ordinal()] == com.yearofice.sim.world.IceModel.Event.BREAKUP)
                    Ambitions.fire(p, "season.first_breakup", e.absDay() * 1440L);
            } catch (RuntimeException ignored) { }
            if (e.absDay() == 365) {
                Ambitions.fire(p, "calendar.year2_may1", e.absDay() * 1440L);
                if (Journal.flights(p) == 0) Ambitions.fire(p, "calendar.year2_may1_no_sos", e.absDay() * 1440L);
            }
        }
    }

    @SubscribeEvent
    public static void hung(TrophyHungEvent e) { Ambitions.fire(e.player(), "trophy.hung", ClockService.installed() ? ClockService.get().nowMinute() : 0); }

    @SubscribeEvent
    public static void crafted(com.yearofice.api.build.StationCraftEvent e) {
        if (e.failed()) return;
        long m = ClockService.installed() ? ClockService.get().nowMinute() : 0;
        if (e.station().getPath().equals("forge")) Ambitions.fire(e.player(), "craft.forged_iron", m);
        if (e.recipe().getPath().contains("fur_mittens")) Ambitions.fire(e.player(), "craft.fur_mittens", m);
        String made = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.made().getItem()).getPath();
        if (made.equals("pemmican")) {
            double kg = com.yearofice.api.story.AmbitionTracker.tally(e.player(), "food.pemmican_kg", com.yearofice.item.inventory.LoadCalculator.stackKg(e.made(), true));
            if (kg >= 5) Ambitions.fire(e.player(), "food.pemmican_5kg", m);
        }
    }

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

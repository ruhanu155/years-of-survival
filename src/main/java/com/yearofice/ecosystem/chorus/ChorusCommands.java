package com.yearofice.ecosystem.chorus;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.ecosystem.small.LifeConfig;
import com.yearofice.sim.life.Chorus;
import com.yearofice.sim.time.Calendar;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi chorus [set <volume>|auto]}: the dawn chorus's volume and voices now, and the hours it runs today. */
public final class ChorusCommands {
    private ChorusCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("chorus")
                .executes(ChorusCommands::show)
                .then(Commands.literal("auto").executes(c -> { ChorusService.pinnedVolume = Double.NaN; return show(c); }))
                .then(Commands.literal("set").then(Commands.argument("volume", DoubleArgumentType.doubleArg(0, 1.5))
                        .executes(c -> { ChorusService.pinnedVolume = DoubleArgumentType.getDouble(c, "volume"); return show(c); })))));
    }

    static int show(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        if (!(c.getSource().getEntity() instanceof ServerPlayer p)) {
            c.getSource().sendFailure(Component.literal("Run as a player."));
            return 0;
        }
        int doy = com.yearofice.ecosystem.migration.MigrationService.doy(l);
        double v = ChorusService.volume(l, p);
        int voices = Chorus.voices(v, LifeConfig.tuning().chorusVoices());
        int rise = ChorusService.sunriseMin(l);
        String when = Chorus.whiteNights(doy) ? "02:30-05:30 (white nights)" : Calendar.hhmm(rise - 45) + "-" + Calendar.hhmm(rise + 75);
        c.getSource().sendSuccess(() -> Component.literal(String.format("Dawn chorus %s: volume %.2f, %d voices; today %s; month curve %.2f%s",
                Calendar.shortDate(doy), v, voices, when, Chorus.month(Calendar.month(doy)), Double.isNaN(ChorusService.pinnedVolume) ? "" : " (pinned)")), false);
        return voices;
    }
}

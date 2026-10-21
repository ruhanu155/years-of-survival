package com.yearofice.ecosystem.migration;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.sim.life.MigrationDef;
import com.yearofice.sim.time.Calendar;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi migration wave|shift <days>} and {@code /yoi migrate status} (M7 lane C debug commands). */
public final class MigrationCommands {
    private MigrationCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("migration")
                        .then(Commands.literal("wave").executes(MigrationCommands::wave))
                        .then(Commands.literal("shift").then(Commands.argument("days", DoubleArgumentType.doubleArg(-14, 14))
                                .executes(c -> { MigrationService.breakupEarlyDays = DoubleArgumentType.getDouble(c, "days"); return wave(c); }))))
                .then(Commands.literal("migrate").then(Commands.literal("status").executes(MigrationCommands::status))));
    }

    static int wave(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        int doy = MigrationService.doy(l);
        var on = MigrationService.active(l);
        StringBuilder sb = new StringBuilder(Calendar.shortDate(doy) + ": ");
        if (on.isEmpty()) sb.append("no wave on");
        for (MigrationDef d : on) sb.append(d.id().substring(d.id().indexOf(':') + 1)).append(String.format(" %.0f%%", 100 * d.strength(doy))).append("; ");
        sb.append(" sky flocks today ").append(MigrationData.calendar().flocksToday(doy, MigrationService.breakupEarlyDays));
        sb.append(", breakup shift ").append(MigrationService.breakupEarlyDays).append(" d");
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return on.size();
    }

    static int status(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        int doy = MigrationService.doy(l);
        StringBuilder sb = new StringBuilder(Calendar.shortDate(doy) + " (year " + MigrationService.year(l) + ")");
        for (MigrationDef d : MigrationData.calendar().all()) {
            if (!(d.kind().endsWith("run"))) continue;
            String state = MigrationService.dying(l).contains(d) ? "dying" : d.active(doy) ? String.format("running %.0f%%", 100 * d.strength(doy)) : doy < d.from() ? "ahead" : "past";
            sb.append("\n  ").append(d.id().substring(d.id().indexOf(':') + 1)).append(" ").append(Calendar.shortDate(d.from())).append("-").append(Calendar.shortDate(d.to()))
                    .append(" ").append(state);
        }
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}

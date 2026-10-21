package com.yearofice.ecosystem.ai;

import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.ecosystem.pool.ValleyPool;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi scent list}: every live scent source (kind, position, reach, wind yaw) and its distance from you (M6 lane G). */
public final class ScentCommands {
    private ScentCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("scent").then(Commands.literal("list").executes(c -> list(c.getSource())))));
    }

    static int list(CommandSourceStack src) {
        var level = src.getLevel();
        var all = ValleyPool.sources(level, SliceClock.minute(level));
        for (var s : all) {
            src.sendSuccess(() -> Component.literal(String.format("%s at %s reach %.0f m, wind toward %.0f deg, %.0f m away", s.kind(), s.pos().toShortString(), s.reachM(), s.towardDeg(),
                    Math.sqrt(s.pos().distSqr(net.minecraft.core.BlockPos.containing(src.getPosition()))))), false);
        }
        if (all.isEmpty()) src.sendSuccess(() -> Component.literal("No scent sources."), false);
        return all.size();
    }
}

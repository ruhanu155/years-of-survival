package com.yearofice.ecosystem.theft;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yearofice.ecosystem.clock.SliceClock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi theft}: the theft system's debug commands. {@code status} shows the toggle and the count; {@code toggle
 * <on|off>} flips the player's {@code rack_thieves} option for this session; {@code watch} joins the container under
 * the source to the hourly rolls; {@code roll <hours>} replays that many hours on it at once, with the context at the
 * source (so a source within 30 m of the store sees no theft).
 */
public final class TheftCommands {
    private TheftCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("theft")
                        .then(Commands.literal("status").executes(c -> {
                            c.getSource().sendSuccess(() -> Component.literal("rack_thieves " + (TheftService.enabled() ? "on" : "off")
                                    + ", thefts so far " + TheftService.thefts()), false);
                            return 1;
                        }))
                        .then(Commands.literal("toggle").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                            TheftService.setEnabled(BoolArgumentType.getBool(c, "on"));
                            c.getSource().sendSuccess(() -> Component.literal("rack_thieves " + (TheftService.enabled() ? "on" : "off")), true);
                            return 1;
                        })))
                        .then(Commands.literal("watch").executes(c -> {
                            BlockPos at = BlockPos.containing(c.getSource().getPosition()).below();
                            TheftService.watch(c.getSource().getLevel(), at);
                            c.getSource().sendSuccess(() -> Component.literal("Watching " + at.toShortString()), false);
                            return 1;
                        }))
                        .then(Commands.literal("roll").then(Commands.argument("hours", IntegerArgumentType.integer(1, 2000)).executes(c -> {
                            CommandSourceStack src = c.getSource();
                            BlockPos at = BlockPos.containing(src.getPosition()).below();
                            BlockEntity be = src.getLevel().getBlockEntity(at);
                            if (!(be instanceof Container store)) {
                                src.sendFailure(Component.literal("Stand on a container."));
                                return 0;
                            }
                            long minute = SliceClock.minute(src.getLevel());
                            int n = IntegerArgumentType.getInteger(c, "hours");
                            int got = 0;
                            for (int i = 0; i < n; i++) {
                                long h = minute / 60 + i;
                                var ctx = TheftService.context(src.getLevel(), at, (h) * 60);
                                var r = TheftService.hour(src.getLevel(), at, store, h, ctx);
                                if (r.isPresent()) {
                                    got++;
                                    String msg = r.get();
                                    src.sendSuccess(() -> Component.literal(msg), false);
                                }
                            }
                            int g = got;
                            src.sendSuccess(() -> Component.literal(g + " thefts in " + n + " hours."), false);
                            return got;
                        })))));
    }
}

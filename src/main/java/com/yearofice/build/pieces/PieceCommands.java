package com.yearofice.build.pieces;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yearofice.api.build.Room;
import com.yearofice.api.build.RoomService;
import com.yearofice.build.rooms.RoomsData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi struct save|stamp <name>}, {@code /yoi room info}, {@code /yoi show rooms}. */
public final class PieceCommands {
    private PieceCommands() {}

    public static void register(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> yoi = Commands.literal("yoi").requires(s -> s.hasPermission(2));
        yoi.then(Commands.literal("struct")
                .then(Commands.literal("save").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> save(c.getSource(), StringArgumentType.getString(c, "name"), 12))
                        .then(Commands.argument("radius", IntegerArgumentType.integer(1, 48))
                                .executes(c -> save(c.getSource(), StringArgumentType.getString(c, "name"), IntegerArgumentType.getInteger(c, "radius"))))))
                .then(Commands.literal("stamp").then(Commands.argument("name", StringArgumentType.word()).executes(c -> {
                    CommandSourceStack s = c.getSource();
                    try {
                        double min = Structs.stamp(s.getLevel(), StringArgumentType.getString(c, "name"), BlockPos.containing(s.getPosition()));
                        s.sendSuccess(() -> Component.literal(String.format("stamp queued: %d pieces, %.0f player minutes", PieceJobs.pending(s.getLevel()), min)), false);
                        return 1;
                    } catch (Exception ex) { s.sendFailure(Component.literal("stamp failed: " + ex.getMessage())); return 0; }
                }))));
        yoi.then(Commands.literal("room").then(Commands.literal("info").executes(c -> info(c.getSource()))));
        yoi.then(Commands.literal("show").then(Commands.literal("rooms").executes(c -> {
            ServerLevel l = c.getSource().getLevel();
            c.getSource().sendSuccess(() -> Component.literal("rooms known: " + RoomsData.get(l).count()), false);
            return 1;
        })));
        e.getDispatcher().register(yoi);
    }

    static int save(CommandSourceStack s, String name, int radius) {
        try {
            int n = Structs.save(s.getLevel(), name, BlockPos.containing(s.getPosition()), radius, 16);
            s.sendSuccess(() -> Component.literal("saved " + n + " pieces as " + name), false);
            return n;
        } catch (Exception ex) { s.sendFailure(Component.literal("save failed: " + ex.getMessage())); return 0; }
    }

    static int info(CommandSourceStack s) {
        Room r = RoomService.get().roomAt(s.getLevel(), BlockPos.containing(s.getPosition()));
        if (r == null) { s.sendSuccess(() -> Component.literal("not in a room"), false); return 0; }
        s.sendSuccess(() -> Component.literal(String.format("room: %d m3, U %.1f W/C, %.0f W in, outside %.1f C, inside %.1f C, %d leaks",
                r.volumeM3(), r.uWPerK(), r.sourceW(), r.outsideC(), r.feelsLikeC(), r.leaks().size())), false);
        return 1;
    }
}

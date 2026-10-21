package com.yearofice.build.dugout;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.sim.build.Dugout;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi dugout check <hillside|cut_bank|rock_shelter>} validates the room the player stands in and seals its shell
 * when valid; {@code template <kind> <wide> <high> <deep>} lays a finished dugout in front of the player.
 */
public final class DugoutCommands {
    private DugoutCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("dugout")
                .then(Commands.literal("check").then(Commands.argument("kind", StringArgumentType.word()).executes(DugoutCommands::check)))
                .then(Commands.literal("template").then(Commands.argument("kind", StringArgumentType.word())
                        .then(Commands.argument("wide", IntegerArgumentType.integer(1, 8))
                                .then(Commands.argument("high", IntegerArgumentType.integer(1, 4))
                                        .then(Commands.argument("deep", IntegerArgumentType.integer(1, 10)).executes(DugoutCommands::template))))))));
    }

    private static int check(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Dugout.Kind k = Dugout.Kind.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(Locale.ROOT));
        DugoutService.Result r = DugoutService.validate(p.serverLevel(), p.blockPosition(), k);
        int sealed = DugoutService.seal(p.serverLevel(), r);
        String line = String.format(Locale.ROOT, "%s %dx%dx%d, cover %d, earth faces %d, wood %d, hearth %s, flue %s -> %s%s", k, r.wide(), r.high(), r.deep(),
                r.cover(), r.earthFaces(), r.woodFaces(), r.hearth(), r.flue(), r.fault() == null ? "VALID" : r.fault(), sealed > 0 ? ", sealed " + sealed + " blocks" : "");
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return r.valid() ? 1 : 0;
    }

    private static int template(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Dugout.Kind k = Dugout.Kind.valueOf(StringArgumentType.getString(c, "kind").toUpperCase(Locale.ROOT));
        var front = p.getDirection();
        var origin = p.blockPosition().relative(front, 3);
        DugoutTemplate.build(p.serverLevel(), origin, front.getOpposite(), k, IntegerArgumentType.getInteger(c, "wide"),
                IntegerArgumentType.getInteger(c, "high"), IntegerArgumentType.getInteger(c, "deep"));
        c.getSource().sendSuccess(() -> Component.literal("Dugout laid at " + origin.toShortString() + "; stand inside and run /yoi dugout check"), false);
        return 1;
    }
}

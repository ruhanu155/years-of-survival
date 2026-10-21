package com.yearofice.fire.stove;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.sim.env.FireSetting;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi stove info|light|douse|setting <low|steady|hot>|sweep} act on the nearest stove within 6 blocks;
 * {@code /yoi struct check [sweep]} lists the stoves within 8 blocks with their chimney, draft and creosote and, with
 * {@code sweep}, does the cleaning job on each.
 */
public final class StoveCommands {
    private StoveCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("stove")
                        .then(Commands.literal("info").executes(c -> run(c, StoveCommands::info)))
                        .then(Commands.literal("light").executes(c -> run(c, be -> be.light() ? "lit" : "will not catch")))
                        .then(Commands.literal("douse").executes(c -> run(c, be -> { be.douse(); return "out"; })))
                        .then(Commands.literal("sweep").executes(c -> run(c, be -> { be.sweep(); return "swept"; })))
                        .then(Commands.literal("setting").then(Commands.argument("s", StringArgumentType.word()).executes(c ->
                                run(c, be -> { be.setting(FireSetting.valueOf(StringArgumentType.getString(c, "s").toUpperCase(Locale.ROOT))); return "set"; })))))
                .then(Commands.literal("struct").then(Commands.literal("check")
                        .executes(c -> check(c, false))
                        .then(Commands.literal("sweep").executes(c -> check(c, true))))));
    }

    private interface Act { String go(StoveBlockEntity be); }

    private static StoveBlockEntity nearest(ServerPlayer p, int r) {
        StoveBlockEntity best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-r, -r, -r), p.blockPosition().offset(r, r, r))) {
            if (p.level().getBlockEntity(q) instanceof StoveBlockEntity be && q.distSqr(p.blockPosition()) < bd) { best = be; bd = q.distSqr(p.blockPosition()); }
        }
        return best;
    }

    private static int run(CommandContext<CommandSourceStack> c, Act a) throws CommandSyntaxException {
        StoveBlockEntity be = nearest(c.getSource().getPlayerOrException(), 6);
        if (be == null) { c.getSource().sendFailure(Component.literal("No stove within 6 blocks.")); return 0; }
        String s = a.go(be);
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    private static String info(StoveBlockEntity be) {
        return String.format(Locale.ROOT, "%s %s %s | %.2f armloads | pipe %.0f m, %s, draft %.2f | %.0f W | creosote %.0f%s | smoke %.0f%% | chimney fires %d",
                be.kindId(), be.lit() ? "lit" : "out", be.setting(), be.armloads(), be.chimneyM(), be.flued() ? "flued" : "NO FLUE",
                be.draft(), be.outputW(), be.creosote(), be.creosote() >= 60 ? " (WARN)" : "", be.smokePct(), be.chimneyFires());
    }

    private static int check(CommandContext<CommandSourceStack> c, boolean sweep) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        int n = 0;
        for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-8, -8, -8), p.blockPosition().offset(8, 8, 8))) {
            if (p.level().getBlockEntity(q) instanceof StoveBlockEntity be) {
                n++;
                String line = q.toShortString() + " " + info(be) + (sweep ? " -> swept" : "");
                if (sweep) be.sweep();
                c.getSource().sendSuccess(() -> Component.literal(line), false);
            }
        }
        if (n == 0) c.getSource().sendFailure(Component.literal("No stove within 8 blocks."));
        return n;
    }
}

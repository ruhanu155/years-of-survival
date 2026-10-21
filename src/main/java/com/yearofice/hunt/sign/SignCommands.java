package com.yearofice.hunt.sign;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.yearofice.sim.hunt.Sign;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi sign list|age|clear [radius_m]}: debug for the sign system. */
public final class SignCommands {
    private SignCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("sign")
                .then(Commands.literal("list").executes(c -> list(c.getSource().getPlayerOrException(), 64))
                        .then(Commands.argument("radius_m", DoubleArgumentType.doubleArg(1, 512))
                                .executes(c -> list(c.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(c, "radius_m")))))
                .then(Commands.literal("age").executes(c -> age(c.getSource().getPlayerOrException())))
                .then(Commands.literal("clear").executes(c -> clear(c.getSource().getPlayerOrException(), 64))
                        .then(Commands.argument("radius_m", DoubleArgumentType.doubleArg(1, 512))
                                .executes(c -> clear(c.getSource().getPlayerOrException(), DoubleArgumentType.getDouble(c, "radius_m")))))));
    }

    private static int list(ServerPlayer p, double r) {
        var all = SignService.within(p.serverLevel(), p.blockPosition(), r);
        for (var pos : all.subList(0, Math.min(20, all.size()))) {
            Sign.Read rd = SignService.read(p.serverLevel(), pos);
            if (rd != null) p.sendSystemMessage(Component.literal(pos.toShortString() + " " + rd.kind() + " " + rd.species() + " " + rd.age() + " (" + Math.round(rd.ageMin()) + " min)"));
        }
        p.sendSystemMessage(Component.literal(all.size() + " sign within " + (int) r + " m."));
        return all.size();
    }

    private static int age(ServerPlayer p) {
        var pos = SignService.nearest(p.serverLevel(), p.blockPosition(), 16, null);
        Sign.Read rd = pos == null ? null : SignService.read(p.serverLevel(), pos, p);
        p.sendSystemMessage(Component.literal(rd == null ? "No sign within 16 m." : rd.text() + " [" + rd.age() + ", " + Math.round(rd.ageMin()) + " min, confidence " + rd.confidence() + "]"));
        return rd == null ? 0 : 1;
    }

    private static int clear(ServerPlayer p, double r) {
        int n = SignService.clear(p.serverLevel(), p.blockPosition(), r);
        p.sendSystemMessage(Component.literal("Cleared " + n + " sign."));
        return n;
    }
}

package com.yearofice.ecosystem.raid;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yearofice.ecosystem.encounter.WorldToggles;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi world set <option> <on|off>} and {@code /yoi raid status}: the world options at runtime and the raid counters. */
public final class RaidCommands {
    private RaidCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("world").then(Commands.literal("set")
                        .then(Commands.argument("option", StringArgumentType.word()).then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                            String o = StringArgumentType.getString(c, "option");
                            boolean on = BoolArgumentType.getBool(c, "on");
                            switch (o) {
                                case "animals_enter_open_doors" -> WorldToggles.setDoors(on);
                                case "rack_thieves" -> WorldToggles.setRacks(on);
                                default -> { c.getSource().sendFailure(Component.literal("Unknown option " + o)); return 0; }
                            }
                            c.getSource().sendSuccess(() -> Component.literal(o + " " + (on ? "on" : "off")), true);
                            return 1;
                        })))))
                .then(Commands.literal("raid").then(Commands.literal("status").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal("doors " + (WorldToggles.animalsEnterOpenDoors() ? "on" : "off")
                            + ", entries " + ShelterEntry.entries() + "; racks " + (WorldToggles.rackThieves() ? "on" : "off")), false);
                    return 1;
                }))));
    }
}

package com.yearofice.item.inventory;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.api.item.LoadService;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi pack}: the worn pack, open rows, load and band; {@code /yoi pack give} hands a crate backpack;
 * {@code /yoi pack wear} puts the held pack on the back; {@code /yoi pack autopickup <on>} sets the switch.
 */
public final class PackCommands {
    private PackCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("pack").executes(PackCommands::info)
                        .then(Commands.literal("give").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ItemStack s = new ItemStack(PackItems.CRATE_BACKPACK.get());
                            if (!p.getInventory().add(s)) p.drop(s, false);
                            return info(c);
                        }))
                        .then(Commands.literal("wear").executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            ItemStack held = p.getMainHandItem();
                            if (!PackRules.isPack(held) || !PackSlots.back(p).isEmpty()) {
                                c.getSource().sendFailure(Component.literal("Hold a pack, with nothing on the back"));
                                return 0;
                            }
                            PackSlots.setBack(p, held.copy());
                            held.setCount(0);
                            PackSync.tick(p);
                            return info(c);
                        }))
                        .then(Commands.literal("autopickup").then(Commands.argument("on", BoolArgumentType.bool()).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            PackAttachments.setAutoPickup(p, BoolArgumentType.getBool(c, "on"));
                            return info(c);
                        })))));
    }

    static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        LoadService l = LoadService.get();
        ItemStack back = PackSlots.back(p);
        String s = String.format(Locale.ROOT, "Pack: %s | rows %d | load %.1f / %.0f kg | band %s (strength x%.2f) | auto-pickup %s",
                back.isEmpty() ? "none" : back.getHoverName().getString(), l.openRows(p), l.kg(p), l.maxKg(p),
                l.band(p).name().toLowerCase(Locale.ROOT), PackConfig.bands().multiplier(LoadCalculator.strength(p)),
                PackAttachments.rules(p).autoPickup() ? "on" : "off");
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }
}

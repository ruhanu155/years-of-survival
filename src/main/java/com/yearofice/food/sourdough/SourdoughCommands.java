package com.yearofice.food.sourdough;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.api.clock.ClockService;
import com.yearofice.sim.food.Starter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi food starter state|age <days>|bake <dish>} on the starter in the main hand (op level 2). */
public final class SourdoughCommands {
    private SourdoughCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("food").then(Commands.literal("starter")
                .then(Commands.literal("state").executes(SourdoughCommands::state))
                .then(Commands.literal("age").then(Commands.argument("days", DoubleArgumentType.doubleArg(0, 60)).executes(SourdoughCommands::age)))
                .then(Commands.literal("bake").then(Commands.argument("dish", StringArgumentType.word()).executes(SourdoughCommands::bake))))));
    }

    private static ItemStack held(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack s = c.getSource().getPlayerOrException().getItemInHand(InteractionHand.MAIN_HAND);
        if (!s.is(SourdoughRegistry.SOURDOUGH_STARTER.get())) throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Hold a sourdough_starter.")).create();
        return s;
    }

    private static int say(CommandContext<CommandSourceStack> c, String s) { c.getSource().sendSuccess(() -> Component.literal(s), false); return 1; }

    private static int state(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack s = held(c);
        StarterComp k = StarterItem.of(s, ClockService.get().nowMinute());
        var d = k.data();
        return say(c, String.format(java.util.Locale.ROOT, "%s unfed_days=%.2f revive_feeds=%d frozen=%s hydration=%.0f%%", d.state(), d.unfedDays(), d.reviveFeeds(), d.frozen(), d.hydrationPct()));
    }

    /** Ages the starter {@code days} at kitchen warmth (20 C), a debug stand-in for waiting. */
    private static int age(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack s = held(c);
        long now = ClockService.get().nowMinute();
        StarterComp k = StarterItem.of(s, now);
        double days = DoubleArgumentType.getDouble(c, "days");
        s.set(SourdoughRegistry.STARTER.get(), k.with(Starter.advance(k.data(), days * 1440, 20, new java.util.Random(c.getSource().getLevel().random.nextLong())), now));
        return state(c);
    }

    private static int bake(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ItemStack s = held(c);
        var p = c.getSource().getPlayerOrException();
        ItemStack flour = new ItemStack(com.yearofice.food.FoodItems.BARLEY_FLOUR.get(), 4);
        ItemStack out = Sourdough.bake(c.getSource().getLevel(), StarterItem.of(s, ClockService.get().nowMinute()), flour, StringArgumentType.getString(c, "dish"));
        if (out == null) return say(c, "The starter will not raise it (hungry, asleep or dead, or no such dish).");
        if (!p.addItem(out)) p.drop(out, false);
        return say(c, "baked " + StringArgumentType.getString(c, "dish"));
    }
}

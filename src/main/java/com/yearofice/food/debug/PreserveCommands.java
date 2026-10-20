package com.yearofice.food.debug;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.food.preserve.RackBlockEntity;
import com.yearofice.food.preserve.StoreBlockEntity;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.FoodState;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * M5 food debug commands: {@code /yoi food rack|age}, {@code /yoi give food <id> <grams>},
 * {@code /yoi decay info|age|climate}. ({@code /yoi food info} and {@code /yoi decay advance} are in {@link FoodCommands}.)
 */
public final class PreserveCommands {
    private PreserveCommands() {}

    private static final SimpleCommandExceptionType NOT_FOOD = new SimpleCommandExceptionType(Component.literal("That is not a food item."));
    private static final SimpleCommandExceptionType NO_RACK = new SimpleCommandExceptionType(Component.literal("No rack or cache within 8 blocks."));

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("food")
                        .then(Commands.literal("rack").executes(PreserveCommands::rack))
                        .then(Commands.literal("age").then(Commands.argument("days", DoubleArgumentType.doubleArg(0, 400)).executes(PreserveCommands::age))))
                .then(Commands.literal("give").then(Commands.literal("food")
                        .then(Commands.argument("id", ResourceLocationArgument.id())
                                .then(Commands.argument("grams", IntegerArgumentType.integer(1, 100_000)).executes(PreserveCommands::give)))))
                .then(Commands.literal("decay")
                        .then(Commands.literal("info").executes(PreserveCommands::info))
                        .then(Commands.literal("climate").executes(PreserveCommands::info))
                        .then(Commands.literal("age").then(Commands.argument("days", DoubleArgumentType.doubleArg(0, 400)).executes(PreserveCommands::age)))));
    }

    private static String fmt(double v) { return String.format(Locale.ROOT, "%.2f", v); }

    private static int say(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int rack(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel level = p.serverLevel();
        for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-8, -3, -8), p.blockPosition().offset(8, 3, 8))) {
            if (!(level.getBlockEntity(q) instanceof StoreBlockEntity be)) continue;
            StringBuilder out = new StringBuilder(be.getBlockState().getBlock().getName().getString()).append(": ")
                    .append(fmt(be.heldKg())).append(" of ").append(fmt(be.capacityKg())).append(" kg");
            if (be instanceof RackBlockEntity r) {
                out.append(", ").append(fmt(r.progressH())).append(" h banked, ").append(fmt(r.hoursLeft())).append(" h left")
                        .append(r.done() ? " (done)" : "");
                if (be instanceof com.yearofice.food.preserve.DryingRackBlockEntity d) out.append(", rain run ").append(d.rainRun()).append(" h");
            }
            return say(c, out.toString());
        }
        throw NO_RACK.create();
    }

    private static int age(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        double days = DoubleArgumentType.getDouble(c, "days");
        ItemStack s = p.getMainHandItem();
        if (!s.has(YoiComponents.FRESH.get())) return say(c, "The held stack has no freshness.");
        DecayService.get().age(p.serverLevel(), s, days);
        return say(c, "Aged " + s.getHoverName().getString() + " by " + fmt(days) + " days: p "
                + fmt(DecayService.get().progress(p.serverLevel(), s)));
    }

    private static int give(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(c, "id");
        Item item = BuiltInRegistries.ITEM.get(id);
        ItemStack s = new ItemStack(item);
        if (FoodMap.row(s) == null) throw NOT_FOOD.create();
        s.set(YoiComponents.WEIGHT_G.get(), IntegerArgumentType.getInteger(c, "grams"));
        DecayService.get().stamp(p.serverLevel(), s, FoodState.RAW);
        if (!p.getInventory().add(s)) p.drop(s, false);
        return say(c, "Gave " + id + " of " + IntegerArgumentType.getInteger(c, "grams") + " g.");
    }

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getPlayerOrException().serverLevel();
        DecayService d = DecayService.get();
        return say(c, "Decay clock air:slice, air " + fmt(d.airC(level)) + " C, D now " + fmt(d.dNow(level))
                + " dh, D in 24 h " + fmt(d.dAt(level.getDayTime() / 24000.0 * 1440 + 1440) - d.dNow(level)) + " dh a day ahead.");
    }
}

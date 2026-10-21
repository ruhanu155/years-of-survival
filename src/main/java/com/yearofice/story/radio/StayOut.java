package com.yearofice.story.radio;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.yearofice.body.SosConfig;
import com.yearofice.core.YearOfIce;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi sos stayout <days|years>}: the SOS chain's stay-out horizon, the game's rescue clock. The SOS jump reads
 * {@link SosConfig#awayDays()}; this sets the override (default 14 days; 1, 2 or 5 years are the long stays).
 * ponytail: a years stay only advances the clock; the AgeWorld pass over the world (Decay and time) is not run yet.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class StayOut {
    private StayOut() {}

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        var days = Commands.literal("days").then(Commands.argument("n", IntegerArgumentType.integer(1, 3650)).executes(c -> set(c.getSource(), IntegerArgumentType.getInteger(c, "n"))));
        var years = Commands.literal("years").then(Commands.argument("n", IntegerArgumentType.integer(1, 30)).executes(c -> set(c.getSource(), IntegerArgumentType.getInteger(c, "n") * 365)));
        var reset = Commands.literal("default").executes(c -> set(c.getSource(), 0));
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("sos").then(Commands.literal("stayout").then(days).then(years).then(reset))));
    }

    private static int set(net.minecraft.commands.CommandSourceStack src, int days) {
        SosConfig.stayoutDays = days;
        src.sendSuccess(() -> Component.literal("SOS stay-out: " + SosConfig.awayDays() + " game days."), true);
        return days;
    }
}

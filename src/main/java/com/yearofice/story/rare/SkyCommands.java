package com.yearofice.story.rare;

import com.google.gson.JsonParser;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.story.SkyLore;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi sky}: this evening's sky signs (honest odds from sky_signs.json) and the barometer. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class SkyCommands {
    private SkyCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("sky").executes(SkyCommands::sky)));
    }

    private static int sky(CommandContext<CommandSourceStack> c) {
        var level = c.getSource().getLevel();
        var table = SkyLore.signs(JsonParser.parseReader(new InputStreamReader(SkyCommands.class
                .getResourceAsStream("/data/yearofice/story/sky_signs.json"))).getAsJsonObject());
        EnvService env = EnvService.get();
        long day = ClockService.get().nowMinute() / 1440;
        var today = env.day(level, day);
        var tom = env.day(level, day + 1);
        var shown = SkyLore.shown(table, new SkyLore.Tomorrow(tom.meanC(), today.meanC(), tom.precipMm(), tom.windMs()), new Random(day * 31 + 7));
        String names = shown.isEmpty() ? "none" : shown.stream().map(SkyLore.Sign::id).collect(Collectors.joining(", "));
        c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Barometer %.0f hPa. Signs tonight: %s.",
                env.valley(level).pressureHpa(), names)), false);
        return 1;
    }
}

package com.yearofice.story.clock;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.clock.ClockService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.time.Rate;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi time}: the clock now; {@code /yoi time day <n>} (day 1 is May 1 of year 1, same time of day);
 * {@code /yoi time rate <rate|auto>} pins the world rate or returns it to arbitration;
 * {@code /yoi time daylength <minutes>} sets this world's day length. {@code /yoi time forward} (the body's) steps
 * bodies across a span and moves the clock through this service.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class ClockCommands {
    private ClockCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("time").executes(ClockCommands::info)
                        .then(Commands.literal("day").then(Commands.argument("n", IntegerArgumentType.integer(1))
                                .executes(c -> day(c, IntegerArgumentType.getInteger(c, "n")))))
                        .then(Commands.literal("rate").then(Commands.argument("rate", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                        java.util.stream.Stream.concat(Arrays.stream(Rate.values()).map(Rate::id),
                                                java.util.stream.Stream.of("auto")), b))
                                .executes(c -> rate(c, StringArgumentType.getString(c, "rate")))))
                        .then(Commands.literal("daylength").then(Commands.argument("minutes", IntegerArgumentType.integer(24, 72))
                                .executes(c -> dayLength(c, IntegerArgumentType.getInteger(c, "minutes")))))));
    }

    static String describe() {
        ClockService c = ClockService.get();
        long day = c.absDay();
        return String.format(Locale.ROOT, "%s (day %d) | rate %s%s, %d ds/tick | %d-min day | sun %.1f° | daylight %.1f h, dawn %s, dusk %s | moon %.2f",
                Calendar.format(c.nowDs()), day + 1, c.rate().id(), c.forcedRate() != null ? " (pinned)" : "", c.dsPerTick(),
                c.dayLengthMin(), c.sunElevation(), c.daylightHours(day), Calendar.hhmm(c.sun().dawnMinute(day)),
                Calendar.hhmm(c.sun().duskMinute(day)), c.moonPhase());
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        c.getSource().sendSuccess(() -> Component.literal(describe()), false);
        return 1;
    }

    private static int day(CommandContext<CommandSourceStack> c, int n) {
        ClockService.get().setDay(n);
        c.getSource().sendSuccess(() -> Component.literal("Clock set: " + describe()), true);
        return n;
    }

    private static int rate(CommandContext<CommandSourceStack> c, String id) {
        Rate r = id.equals("auto") ? null : Rate.byId(id);
        if (r == null && !id.equals("auto")) {
            c.getSource().sendFailure(Component.literal("Unknown rate " + id + "; one of close, travel, world, work, until_done, sleep, auto"));
            return 0;
        }
        ClockService.get().forceRate(r);
        c.getSource().sendSuccess(() -> Component.literal(r == null ? "Rate back to arbitration" : "World rate pinned to " + r.id()
                + " (" + ClockService.get().dsPerTick(r) + " ds a tick)"), true);
        return 1;
    }

    private static int dayLength(CommandContext<CommandSourceStack> c, int minutes) {
        try {
            ClockService.get().setDayLength(minutes);
        } catch (IllegalArgumentException ex) {
            c.getSource().sendFailure(Component.literal(ex.getMessage() + " (24, 30, 36, 40, 45, 48, 60, 72)"));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("Day length " + minutes + " real minutes"), true);
        return minutes;
    }
}

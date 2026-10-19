package com.yearofice.env;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.env.EnvSample;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.WeatherModel;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi env}: the sample where the command runs and the valley's hour; {@code /yoi env day [n]}: the drawn
 * day (1-based, default today); {@code /yoi env set air <°C> | wind <m/s> | precip <type> [mm/h]} overrides the
 * valley on this level, {@code /yoi env clear} removes it. ({@code /yoi show current} is World generation's.)
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class EnvCommands {
    private EnvCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("env").executes(EnvCommands::here)
                        .then(Commands.literal("day").executes(c -> day(c, ClockService.get().absDay() + 1))
                                .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                        .executes(c -> day(c, IntegerArgumentType.getInteger(c, "n")))))
                        .then(Commands.literal("clear").executes(EnvCommands::clear))
                        .then(Commands.literal("set")
                                .then(Commands.literal("air").then(Commands.argument("c", DoubleArgumentType.doubleArg(-60, 45))
                                        .executes(c -> set(c, DoubleArgumentType.getDouble(c, "c"), Double.NaN, null, Double.NaN))))
                                .then(Commands.literal("wind").then(Commands.argument("ms", DoubleArgumentType.doubleArg(0, 60))
                                        .executes(c -> set(c, Double.NaN, DoubleArgumentType.getDouble(c, "ms"), null, Double.NaN))))
                                .then(Commands.literal("precip").then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Precip.values())
                                                .map(p -> p.name().toLowerCase(Locale.ROOT)), b))
                                        .executes(c -> set(c, Double.NaN, Double.NaN, precip(c), Double.NaN))
                                        .then(Commands.argument("mm_h", DoubleArgumentType.doubleArg(0, 100))
                                                .executes(c -> set(c, Double.NaN, Double.NaN, precip(c),
                                                        DoubleArgumentType.getDouble(c, "mm_h")))))))));
    }

    static String describe(ServerLevel level, BlockPos pos) {
        EnvService env = EnvService.get();
        EnvSample s = env.sample(level, pos);
        WeatherModel.Hour h = env.valley(level);
        return String.format(Locale.ROOT,
                "Here (y %d): air %.1f °C, feels %.1f °C, wind %.1f m/s gust %.1f from %.0f°, %s %.1f mm/h, vis %.0f m, snow %.0f cm, "
                        + "ground %.1f °C, creek %.1f °C, light %d | valley: %s, air %.1f °C%s",
                pos.getY(), s.airC(), s.feelsLikeC(), s.windMs(), s.gustMs(), s.windDirDeg(),
                s.precip().name().toLowerCase(Locale.ROOT), s.precipMmH(), s.visibilityM(), s.snowCm(), s.groundC(),
                s.waterC(), s.light(), h.state().id(), h.airC(), env.override(level) != null ? " (override on)" : "");
    }

    private static int here(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = BlockPos.containing(c.getSource().getPosition());
        c.getSource().sendSuccess(() -> Component.literal(describe(level, pos)), false);
        return 1;
    }

    private static int day(CommandContext<CommandSourceStack> c, long n) {
        WeatherModel.Day d = EnvService.get().day(c.getSource().getLevel(), n - 1);
        long ds = (n - 1) * Calendar.DS_PER_DAY;
        c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT,
                "Day %d (%s): %s, mean %.1f °C (normal %.1f, anomaly %+.1f), swing ±%.1f, wind %.1f m/s from %.0f°, "
                        + "precipitation %.1f mm from %02d:00 for %d h",
                n, Calendar.format(ds).replace(", 00:00", ""), d.state().id(), d.meanC(), d.normalC(), d.anomalyC(), d.swingC(),
                d.windMs(), d.windDirDeg(), d.precipMm(), d.precipStartH(), d.precipHours())), false);
        return 1;
    }

    private static Precip precip(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "type").toUpperCase(Locale.ROOT);
        try {
            return Precip.valueOf(id);
        } catch (IllegalArgumentException e) {
            return Precip.NONE;
        }
    }

    private static int set(CommandContext<CommandSourceStack> c, double air, double wind, Precip p, double mm) {
        ServerLevel level = c.getSource().getLevel();
        EnvService env = EnvService.get();
        EnvService.WeatherOverride o = env.override(level);
        EnvService.WeatherOverride next = new EnvService.WeatherOverride(
                !Double.isNaN(air) ? air : o == null ? Double.NaN : o.airC(),
                !Double.isNaN(wind) ? wind : o == null ? Double.NaN : o.windMs(),
                p != null ? p : o == null ? null : o.precip(),
                p != null ? mm : o == null ? Double.NaN : o.precipMmH());
        env.setOverride(level, next);
        EnvSystem.driveVanilla();
        c.getSource().sendSuccess(() -> Component.literal("Environment override set. " + describe(level,
                BlockPos.containing(c.getSource().getPosition()))), true);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> c) {
        EnvService.get().setOverride(c.getSource().getLevel(), null);
        EnvSystem.driveVanilla();
        c.getSource().sendSuccess(() -> Component.literal("Environment override cleared"), true);
        return 1;
    }
}

package com.yearofice.env.weather;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.weather.WeatherService;
import com.yearofice.api.weather.WeatherState;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.weather.DecadeStorm;
import com.yearofice.sim.weather.StormDef;
import com.yearofice.sim.weather.StormKind;
import com.yearofice.sim.weather.StormTable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
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
 * {@code /yoi weather show | forecast [signs...] | storm <id> [hours] | blizzard | temp <c> | glaze [layers] | strike | decade [variant] | table
 * [days] | sim <days>}. Op level 2; every command prints what it changed.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class WeatherCommands {
    private WeatherCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("weather")
                .then(Commands.literal("show").executes(WeatherCommands::show))
                .then(Commands.literal("forecast").executes(c -> forecast(c, ""))
                        .then(Commands.argument("signs", StringArgumentType.greedyString())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(WeatherSystem.loaded.forecast().signs().keySet(), b))
                                .executes(c -> forecast(c, StringArgumentType.getString(c, "signs")))))
                .then(Commands.literal("storm").then(Commands.argument("id", StringArgumentType.word())
                        .suggests((c, b) -> SharedSuggestionProvider.suggest(WeatherSystem.loaded.defs().keySet(), b))
                        .executes(c -> storm(c, StringArgumentType.getString(c, "id"), -1))
                        .then(Commands.argument("hours", IntegerArgumentType.integer(1, 240))
                                .executes(c -> storm(c, StringArgumentType.getString(c, "id"), IntegerArgumentType.getInteger(c, "hours"))))))
                .then(Commands.literal("blizzard").executes(c -> storm(c, "blizzard", 12)))
                .then(Commands.literal("temp").then(Commands.argument("c", DoubleArgumentType.doubleArg(-60, 45)).executes(WeatherCommands::temp)))
                .then(Commands.literal("glaze").executes(c -> glaze(c, 2))
                        .then(Commands.argument("layers", IntegerArgumentType.integer(0, 3)).executes(c -> glaze(c, IntegerArgumentType.getInteger(c, "layers")))))
                .then(Commands.literal("strike").executes(WeatherCommands::strike))
                .then(Commands.literal("decade").executes(c -> decade(c, ""))
                        .then(Commands.argument("variant", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(WeatherSystem.loaded.decade().variants().stream().map(DecadeStorm.Variant::id), b))
                                .executes(c -> decade(c, StringArgumentType.getString(c, "variant")))))
                .then(Commands.literal("table").executes(c -> table(c, 30))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 365)).executes(c -> table(c, IntegerArgumentType.getInteger(c, "days")))))
                .then(Commands.literal("sim").then(Commands.argument("days", IntegerArgumentType.integer(1, 36500))
                        .executes(c -> sim(c, IntegerArgumentType.getInteger(c, "days")))))));
    }

    private static void say(CommandContext<CommandSourceStack> c, String s) { c.getSource().sendSuccess(() -> Component.literal(s), true); }

    private static int show(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        long now = ClockService.get().nowMinute();
        WeatherState w = WeatherService.get().current(level, BlockPos.containing(c.getSource().getPosition()));
        StormTable.Event e = WeatherSystem.eventAt(level, now);
        say(c, String.format(Locale.ROOT, "Weather: %s level %.2f, wind %.1f m/s from %.0f°, %s %.1f mm/h, vis %.0f m%s", w.kind().id(), w.intensity(),
                w.windMs(), w.fromDeg(), w.precipKind().id(), w.precipMmPerH(), w.visibilityM(),
                e == null ? "" : String.format(Locale.ROOT, " | storm %s hours %d-%d (%s)", e.id(), e.startHour(), e.endHour(), e.forced() ? "forced" : "drawn")));
        return 1;
    }

    private static int forecast(CommandContext<CommandSourceStack> c, String signs) {
        ServerLevel level = c.getSource().getLevel();
        long now = ClockService.get().nowMinute(), hour = Math.floorDiv(now, 60L);
        List<String> seen = signs.isBlank() ? List.of() : Arrays.asList(signs.trim().split("\\s+"));
        Map<String, Double> p = new TreeMap<>(WeatherSystem.loaded.forecast().next24h(seen, WeatherConfig.tuning().forecastBonus()));
        StringBuilder truth = new StringBuilder();
        for (long h = hour; h < hour + 24; h++) {
            StormTable.Event e = WeatherSystem.table(level, Calendar.yearOfDay(h / 24)).at(h);
            if (e != null && e.startHour() >= hour && !truth.toString().contains(e.id())) truth.append(e.id()).append(' ');
        }
        StringBuilder sb = new StringBuilder("Next 24 h (signs: " + (seen.isEmpty() ? "none" : String.join(",", seen)) + "): ");
        p.forEach((k, v) -> sb.append(String.format(Locale.ROOT, "%s %.0f%% ", k, v * 100)));
        say(c, sb + "| true starts: " + (truth.length() == 0 ? "none" : truth));
        return 1;
    }

    private static int storm(CommandContext<CommandSourceStack> c, String id, int hours) {
        StormDef def = WeatherSystem.loaded.defs().get(id);
        if (def == null) {
            c.getSource().sendFailure(Component.literal("No storm " + id + "; try " + new TreeMap<>(WeatherSystem.loaded.defs()).keySet()));
            return 0;
        }
        ServerLevel level = c.getSource().getLevel();
        int h = hours > 0 ? hours : (int) Math.round((def.durationH()[0] + def.durationH()[1]) / 2);
        long start = Math.floorDiv(ClockService.get().nowMinute(), 60L);
        WeatherSystem.force(level, id, start, h, 1.0);
        say(c, "Forced " + id + " for " + h + " h from now (hour " + start + ").");
        return 1;
    }

    private static int temp(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        EnvService env = EnvService.get();
        EnvService.WeatherOverride o = env.override(level);
        env.setOverride(level, new EnvService.WeatherOverride(DoubleArgumentType.getDouble(c, "c"), o == null ? Double.NaN : o.windMs(),
                o == null ? null : o.precip(), o == null ? Double.NaN : o.precipMmH()));
        say(c, "Air held at " + DoubleArgumentType.getDouble(c, "c") + " °C (/yoi env clear removes it).");
        return 1;
    }

    private static int glaze(CommandContext<CommandSourceStack> c, int layers) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos p = BlockPos.containing(c.getSource().getPosition());
        int n = layers == 0 ? GlazePass.melt(level, p, 16, 2000, level.getGameTime(), WeatherRegistry.GLAZE_LAYER.get(), 1.0)
                : GlazePass.place(level, p, 16, 2000, level.getGameTime(), WeatherRegistry.GLAZE_LAYER.get(), layers, false);
        say(c, (layers == 0 ? "Melted " : "Laid ") + n + " glaze sheets within 16 blocks.");
        return n;
    }

    private static int strike(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        BlockPos p = BlockPos.containing(c.getSource().getPosition());
        BlockPos top = Lightning.highest(level, p.getX(), p.getZ());
        if (top == null) {
            c.getSource().sendFailure(Component.literal("Nothing loaded here."));
            return 0;
        }
        BlockPos limb = Lightning.strike(level, top, level.getGameTime());
        say(c, "Strike at " + top.toShortString() + (limb == null ? "" : ", limb at " + limb.toShortString()));
        return 1;
    }

    private static int decade(CommandContext<CommandSourceStack> c, String variant) {
        WeatherSystem.Loaded l = WeatherSystem.loaded;
        DecadeStorm.Variant v = l.decade().variants().stream().filter(x -> variant.isEmpty() || x.id().equals(variant)).findFirst().orElse(null);
        if (v == null) {
            c.getSource().sendFailure(Component.literal("No variant " + variant));
            return 0;
        }
        long start = Math.floorDiv(ClockService.get().nowMinute(), 60L);
        WeatherSystem.force(c.getSource().getLevel(), v.storm(), start, v.hours(), v.intensity());
        say(c, "The " + v.id() + " begins now: " + v.storm() + " for " + v.hours() + " h.");
        return 1;
    }

    private static int table(CommandContext<CommandSourceStack> c, int days) {
        ServerLevel level = c.getSource().getLevel();
        long hour = Math.floorDiv(ClockService.get().nowMinute(), 60L), end = hour + days * 24L;
        StormTable t = WeatherSystem.table(level, Calendar.yearOfDay(hour / 24));
        List<String> out = new ArrayList<>();
        for (StormTable.Event e : t.events())
            if (e.endHour() > hour && e.startHour() < end)
                out.add(String.format(Locale.ROOT, "%s d%d %02d:00 +%dh x%.2f", e.id(), e.startHour() / 24 % Calendar.DAYS_PER_YEAR, e.startHour() % 24, e.hours(), e.intensity()));
        say(c, "Year " + t.year() + " table hash " + Integer.toHexString(t.hash()) + ", " + t.events().size() + " storms; next " + days + " d: "
                + (out.isEmpty() ? "none" : String.join("; ", out)));
        return 1;
    }

    private static int sim(CommandContext<CommandSourceStack> c, int days) {
        ServerLevel level = c.getSource().getLevel();
        int y0 = Calendar.yearOfDay(Math.floorDiv(ClockService.get().nowMinute(), 1440L)), years = Math.max(1, (days + 364) / 365);
        Map<String, int[]> sum = new TreeMap<>();
        for (int y = y0; y < y0 + years; y++)
            for (StormTable.Event e : WeatherSystem.table(level, y).events()) {
                int[] a = sum.computeIfAbsent(e.id(), k -> new int[2]);
                a[0]++;
                a[1] += e.hours();
            }
        StringBuilder sb = new StringBuilder("Sim " + years + " y from year " + y0 + ": ");
        sum.forEach((k, a) -> sb.append(String.format(Locale.ROOT, "%s %.1f/y %.0f h/y; ", k, a[0] / (double) years, a[1] / (double) years)));
        say(c, sb.toString());
        return 1;
    }
}

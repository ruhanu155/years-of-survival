package com.yearofice.body.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.yearofice.body.BodyAttachments;
import com.yearofice.body.BodyConfig;
import com.yearofice.body.BodyTicker;
import com.yearofice.body.SosFlow;
import com.yearofice.body.clock.BodyClock;
import com.yearofice.body.env.VanillaEnvSampler;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.body.Activity;
import com.yearofice.sim.body.BodyModel;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.BodyTuning;
import com.yearofice.sim.body.DayPlan;
import com.yearofice.sim.body.Metabolism;
import com.yearofice.sim.body.part.Part;
import com.yearofice.sim.env.Precip;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * The body's {@code /yoi} commands, op level 2 (Ready for development › M2.5 and M4 debug commands). They join the
 * {@code /yoi} root that core registers; Brigadier merges the two trees.
 *
 * <pre>
 *   /yoi set calories|bmi|warmth|water|energy &lt;value&gt; [player]
 *   /yoi body show [player]
 *   /yoi body env air &lt;°C&gt; [wind m/s] [none|rain|snow]  |  /yoi body env clear
 *   /yoi time day &lt;n&gt;
 *   /yoi time forward &lt;minutes&gt; [chores|still]
 *   /yoi sos [player]
 * </pre>
 *
 * {@code set calories} sets the last 24 h balance (in minus out), the number the Calories icon and the energy cap
 * read. {@code time forward} moves the clock and steps every non-spectator player through those minutes in closed
 * form, holding still (rest) unless a day plan is named.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class BodyCommands {
    private BodyCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        d.register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(set())
                .then(Commands.literal("body")
                        .then(Commands.literal("show").executes(c -> show(c, List.of(c.getSource().getPlayerOrException())))
                                .then(Commands.argument("player", EntityArgument.players())
                                        .executes(c -> show(c, EntityArgument.getPlayers(c, "player")))))
                        .then(Commands.literal("part")
                                .then(Commands.argument("part", com.mojang.brigadier.arguments.StringArgumentType.word())
                                        .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(
                                                Arrays.stream(Part.values()).map(x -> x.name().toLowerCase(Locale.ROOT)), b))
                                        .executes(c -> part(c, com.mojang.brigadier.arguments.StringArgumentType.getString(c, "part")))))
                        .then(env()))
                .then(Commands.literal("time")
                        // /yoi time day <n> is the clock's (story.clock.ClockCommands).
                        .then(Commands.literal("forward").then(Commands.argument("minutes",
                                        IntegerArgumentType.integer(1, 366 * 1440))
                                .executes(c -> forward(c, IntegerArgumentType.getInteger(c, "minutes"), null))
                                .then(Commands.literal("chores").executes(c -> forward(c,
                                        IntegerArgumentType.getInteger(c, "minutes"), DayPlan.CHORES)))
                                .then(Commands.literal("still").executes(c -> forward(c,
                                        IntegerArgumentType.getInteger(c, "minutes"), DayPlan.STILL))))))
                .then(Commands.literal("sos").executes(c -> sos(List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(c -> sos(EntityArgument.getPlayers(c, "player"))))));
    }

    // --- /yoi set --------------------------------------------------------------------------------------------
    private static LiteralArgumentBuilder<CommandSourceStack> set() {
        LiteralArgumentBuilder<CommandSourceStack> set = Commands.literal("set");
        set.then(setter("calories", -20_000, 20_000, BodyCommands::setCalories));
        set.then(setter("bmi", 10, 40, (s, v) -> s.kg = Metabolism.kgForBmi(v, s.heightCm)));
        set.then(setter("warmth", 0, 100, (s, v) -> s.warmth = v));
        set.then(setter("water", 0, 100, (s, v) -> s.water = v));
        set.then(setter("energy", 0, 10, (s, v) -> s.energy = v));
        return set;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setter(String name, double min, double max,
                                                                       BiConsumer<BodyState, Double> apply) {
        return Commands.literal(name).then(Commands.argument("value", DoubleArgumentType.doubleArg(min, max))
                .executes(c -> setOn(c, name, List.of(c.getSource().getPlayerOrException()), apply))
                .then(Commands.argument("player", EntityArgument.players())
                        .executes(c -> setOn(c, name, EntityArgument.getPlayers(c, "player"), apply))));
    }

    private static int setOn(CommandContext<CommandSourceStack> c, String name, Collection<ServerPlayer> players,
                             BiConsumer<BodyState, Double> apply) {
        double v = DoubleArgumentType.getDouble(c, "value");
        for (ServerPlayer p : players) {
            BodyState s = BodyAttachments.of(p);
            apply.accept(s, v);
            long m = BodyTicker.clock.minute(p.server);
            BodyTicker.sync(p, m, true);
            c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + ": " + name + " = "
                    + fmt(v) + "  " + summary(s, m)), true);
        }
        return players.size();
    }

    /** Sets the last 24 h balance to {@code v}: clears the day bins and books it in the current hour. */
    static void setCalories(BodyState s, double v) {
        Arrays.fill(s.kcalIn, 0);
        Arrays.fill(s.kcalOut, 0);
        int h = s.binHour < 0 ? 0 : (int) Math.floorMod(s.binHour, 24);
        if (v >= 0) s.kcalIn[h] = v;
        else s.kcalOut[h] = -v;
    }

    // --- /yoi body part --------------------------------------------------------------------------------------
    private static int part(CommandContext<CommandSourceStack> c, String name) throws CommandSyntaxException {
        Part part;
        try {
            part = Part.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            c.getSource().sendFailure(Component.literal("No such part: " + name));
            return 0;
        }
        ServerPlayer p = c.getSource().getPlayerOrException();
        BodyState s = BodyAttachments.of(p);
        StringBuilder b = new StringBuilder();
        b.append(String.format(Locale.ROOT, "%s: hp %d (%s), frost %d, crooked %s, digits lost %d/%d", name, s.parts.hp(part),
                s.parts.band(part), s.parts.frost(part), s.parts.isCrooked(part), Integer.bitCount(s.parts.lostDigits()[0]),
                Integer.bitCount(s.parts.lostDigits()[1])));
        for (var pr : s.parts.problems()) if (pr.part() == part) b.append("\n  ").append(pr.id()).append(" stage ").append(pr.stage());
        String text = b.toString();
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    // --- /yoi body show --------------------------------------------------------------------------------------
    private static int show(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> players) {
        BodyTuning t = BodyConfig.tuning();
        for (ServerPlayer p : players) {
            BodyState s = BodyAttachments.of(p);
            long m = BodyTicker.clock.minute(p.server);
            StringBuilder b = new StringBuilder();
            b.append(p.getGameProfile().getName()).append(" | day ").append(BodyClock.day(m)).append(' ')
                    .append(BodyClock.hhmm(m)).append(" | preset ").append(t.preset()).append('\n');
            b.append(String.format(Locale.ROOT, "Weight %.1f kg, BMI %.1f (%s), BMR %.0f, height %d cm%n", s.kg,
                    s.bmi(), Metabolism.bmiWord(s.bmi()), s.bmr(), s.heightCm));
            b.append(String.format(Locale.ROOT, "Today in %.0f / out %.0f kcal, 24 h balance %+.0f, stomach %.0f%%%n",
                    s.kcalInToday(m), s.kcalOutToday(m), s.balance24h(), s.stomachPct(t.stomachKg()) * 100));
            b.append(String.format(Locale.ROOT, "Macros 72 h: carbs %.0f%%, fat %.0f%%, protein %.0f%%%n",
                    s.macroMeter(0) * 100, s.macroMeter(1) * 100, s.macroMeter(2) * 100));
            b.append(String.format(Locale.ROOT, "Energy %.1f / cap %.0f bolts, awake %d min%s%n", s.energy,
                    s.lastEnergyCap, s.awakeMin, s.asleep ? " (asleep)" : ""));
            b.append(String.format(Locale.ROOT, "Warmth %.1f, feels like %s, wetness %.0f%%%n", s.warmth,
                    s.lastFeel == null ? "?" : String.format(Locale.ROOT, "%.1f °C", s.lastFeel.totalC()),
                    s.wetness() * 100));
            b.append(String.format(Locale.ROOT, "Water %.1f (%.2f L today)%s%n", s.water, s.drunkMlToday(m) / 1000,
                    s.gutIll(m) ? ", gut illness" : (s.gutOnset >= 0 ? ", gut illness coming" : "")));
            b.append(String.format(Locale.ROOT, "Condition %.2f, d_sys %.2f, parts ", s.condition(),
                    s.parts.dSys()));
            for (Part part : Part.values()) b.append(part.name().toLowerCase(Locale.ROOT)).append('=')
                    .append(s.parts.hp(part)).append(' ');
            b.append("\nProblems ").append(s.parts.problems().size()).append(", SOS state ").append(s.sos)
                    .append(", flights out ").append(SosFlow.flightsOut(p)).append(", activity ")
                    .append(s.lastActivity.name().toLowerCase(Locale.ROOT));
            String text = b.toString();
            c.getSource().sendSuccess(() -> Component.literal(text), false);
        }
        return players.size();
    }

    // --- /yoi body env ---------------------------------------------------------------------------------------
    private static LiteralArgumentBuilder<CommandSourceStack> env() {
        return Commands.literal("env")
                .then(Commands.literal("clear").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BodyTicker.VANILLA_ENV.override(p, null);
                    c.getSource().sendSuccess(() -> Component.literal("Environment override cleared"), true);
                    return 1;
                }))
                .then(Commands.literal("air").then(Commands.argument("c", DoubleArgumentType.doubleArg(-60, 45))
                        .executes(c -> envSet(c, 0, Precip.NONE))
                        .then(Commands.argument("wind", DoubleArgumentType.doubleArg(0, 40))
                                .executes(c -> envSet(c, DoubleArgumentType.getDouble(c, "wind"), Precip.NONE))
                                .then(Commands.literal("none").executes(c -> envSet(c,
                                        DoubleArgumentType.getDouble(c, "wind"), Precip.NONE)))
                                .then(Commands.literal("rain").executes(c -> envSet(c,
                                        DoubleArgumentType.getDouble(c, "wind"), Precip.RAIN)))
                                .then(Commands.literal("snow").executes(c -> envSet(c,
                                        DoubleArgumentType.getDouble(c, "wind"), Precip.SNOW))))));
    }

    private static int envSet(CommandContext<CommandSourceStack> c, double wind, Precip precip)
            throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        double air = DoubleArgumentType.getDouble(c, "c");
        BodyTicker.VANILLA_ENV.override(p, new VanillaEnvSampler.EnvOverride(air, wind, precip,
                precip == Precip.NONE ? 0 : 2.5, Double.NaN));
        c.getSource().sendSuccess(() -> Component.literal("Environment override: air " + fmt(air) + " °C, wind "
                + fmt(wind) + " m/s, " + precip.name().toLowerCase(Locale.ROOT)), true);
        return 1;
    }

    // --- /yoi time -------------------------------------------------------------------------------------------
    private static int forward(CommandContext<CommandSourceStack> c, int minutes, DayPlan plan) {
        MinecraftServer server = c.getSource().getServer();
        BodyTuning t = BodyConfig.tuning();
        long start = BodyTicker.clock.minute(server);
        int n = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || BodyAttachments.of(p).sos != 0) continue;
            BodyModel.Result r = plan == null
                    ? BodyTicker.step(p, start, minutes, Activity.REST, false, false, t)
                    : BodyTicker.stepPlan(p, start, minutes, plan, t);
            n++;
            if (r.collapsed()) {
                c.getSource().sendSuccess(() -> Component.literal(p.getGameProfile().getName() + " collapsed ("
                        + r.collapse().name().toLowerCase(Locale.ROOT) + ") on day " + BodyClock.day(r.minute())
                        + " at " + BodyClock.hhmm(r.minute())), true);
            }
        }
        BodyTicker.clock.advance(server, minutes);
        BodyTicker.resync(server);
        long m = BodyTicker.clock.minute(server);
        int stepped = n;
        c.getSource().sendSuccess(() -> Component.literal("Forward " + minutes + " min" + (plan == null ? ""
                : " (" + plan.id() + ")") + ": now day " + BodyClock.day(m) + ", " + BodyClock.hhmm(m) + "; "
                + stepped + " bodies stepped"), true);
        return stepped;
    }

    // --- /yoi sos --------------------------------------------------------------------------------------------
    private static int sos(Collection<ServerPlayer> players) {
        for (ServerPlayer p : players) SosFlow.trigger(p, SosFlow.Cause.MANUAL);
        return players.size();
    }

    static String summary(BodyState s, long m) {
        return String.format(Locale.ROOT, "(kg %.1f, BMI %.1f, 24 h %+.0f kcal, warmth %.0f, water %.0f, energy %.1f)",
                s.kg, s.bmi(), s.balance24h(), s.warmth, s.water, s.energy);
    }

    static String fmt(double v) { return String.format(Locale.ROOT, "%.1f", v); }
}

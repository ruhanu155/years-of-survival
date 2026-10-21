package com.yearofice.build.craft;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.yearofice.api.build.GateService;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.sim.build.Practice;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.build.Tier;
import com.yearofice.sim.build.ToolWear;
import java.util.Arrays;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Crafting's debug commands (every system ships {@code /yoi} commands):
 * <ul>
 *   <li>{@code gate open <tier>} opens a gate (the Ready for development line); {@code gate state} lists them.</li>
 *   <li>{@code practice show|set|add}: the {@code yearofice:practice} reps ({@code add} obeys the daily cap).</li>
 *   <li>{@code craft list} the recipes and their problems; {@code craft start <recipe>} begins one;
 *       {@code craft now <recipe>} does it at once.</li>
 *   <li>{@code tool info} the main-hand tool's durability and Edge; {@code tool edge <n>} sets it; {@code tool hone <n>} hones to at least n.</li>
 * </ul>
 */
public final class CraftCommands {
    private CraftCommands() {}

    private static final SimpleCommandExceptionType BAD = new SimpleCommandExceptionType(Component.literal("Unknown name"));

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("gate")
                        .then(Commands.literal("open").then(Commands.argument("tier", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Tier.values()).map(Tier::id), b))
                                .executes(CraftCommands::gateOpen)))
                        .then(Commands.literal("state").executes(CraftCommands::gateState)))
                .then(Commands.literal("practice")
                        .then(Commands.literal("show").executes(CraftCommands::practiceShow))
                        .then(Commands.literal("set").then(skillArg().then(Commands.argument("reps", DoubleArgumentType.doubleArg(0, 10000))
                                .executes(c -> practiceSet(c, false)))))
                        .then(Commands.literal("add").then(skillArg().then(Commands.argument("reps", DoubleArgumentType.doubleArg(0, 10000))
                                .executes(c -> practiceSet(c, true))))))
                .then(Commands.literal("craft")
                        .then(Commands.literal("list").executes(CraftCommands::craftList))
                        .then(Commands.literal("start").then(Commands.argument("recipe", ResourceLocationArgument.id()).executes(c -> craft(c, false))))
                        .then(Commands.literal("now").then(Commands.argument("recipe", ResourceLocationArgument.id()).executes(c -> craft(c, true)))))
                .then(Commands.literal("tool")
                        .then(Commands.literal("info").executes(CraftCommands::toolInfo))
                        .then(Commands.literal("edge").then(Commands.argument("edge", DoubleArgumentType.doubleArg(0, ToolWear.EDGE_MAX))
                                .executes(c -> toolEdge(c, false))))
                        .then(Commands.literal("hone").then(Commands.argument("edge", DoubleArgumentType.doubleArg(0, ToolWear.EDGE_MAX))
                                .executes(c -> toolEdge(c, true))))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> skillArg() {
        return Commands.argument("skill", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Skill.values()).map(Skill::id), b));
    }

    private static int gateOpen(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Tier t = Tier.byId(StringArgumentType.getString(c, "tier")).orElseThrow(BAD::create);
        boolean opened = GateService.get().open(p, t, "command");
        c.getSource().sendSuccess(() -> Component.literal(opened ? "Gate " + t.id() + " opened." : "Gate " + t.id() + " was already open."), false);
        return opened ? 1 : 0;
    }

    private static int gateState(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        StringBuilder sb = new StringBuilder("Gates:");
        for (Tier t : Tier.values()) sb.append(' ').append(t.id()).append('=').append(GateService.get().isOpen(p, t) ? "open" : "shut");
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int practiceShow(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        PracticeService s = PracticeService.get();
        StringBuilder sb = new StringBuilder("Practice:");
        for (Skill k : Skill.values()) {
            double r = s.reps(p, k);
            if (r > 0) sb.append(String.format(Locale.ROOT, " %s %.1f/%d (%s)", k.id(), r, k.n, Practice.word(r, k.n).text));
        }
        if (sb.toString().equals("Practice:")) sb.append(" none yet");
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    private static int practiceSet(CommandContext<CommandSourceStack> c, boolean add) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Skill k = Skill.byId(StringArgumentType.getString(c, "skill")).orElseThrow(BAD::create);
        double reps = DoubleArgumentType.getDouble(c, "reps");
        PracticeService s = PracticeService.get();
        double gained = add ? s.add(p, k, reps) : reps;
        if (!add) s.set(p, k, reps);
        c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "%s: %.1f reps%s", k.id(), s.reps(p, k),
                add ? String.format(Locale.ROOT, " (gained %.1f of %.1f, daily cap %.1f)", gained, reps, Practice.dailyCap(k.n)) : "")), false);
        return 1;
    }

    private static int craftList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        var stations = StationCraft.stations(p, null);
        for (RecipeHolder<StationRecipe> h : StationCraft.all(p.level())) {
            StationCraft.Plan plan = StationCraft.plan(p, h, stations);
            c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "%s [%s] %s: %.1f min, %.0f kcal (%s)",
                    h.id(), h.value().tier().id(), plan.problem(), plan.minutes(), plan.kcal(), plan.word().text)), false);
        }
        return 1;
    }

    private static int craft(CommandContext<CommandSourceStack> c, boolean now) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ResourceLocation id = ResourceLocationArgument.getId(c, "recipe");
        StationCraft.Job j = StationCraft.start(p, id, null);
        if (j == null) {
            c.getSource().sendFailure(Component.literal("Cannot start " + id + " (see /yoi craft list)."));
            return 0;
        }
        if (now) {
            StationCraft.advance(p, j, Long.MAX_VALUE / 4);
            StationCraft.cancel(p);
        }
        c.getSource().sendSuccess(() -> Component.literal(now ? "Crafted " + id : "Started " + id), false);
        return 1;
    }

    private static int toolInfo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack t = p.getMainHandItem();
        WearData w = WearData.of(t);
        String line = t.isEmpty() ? "Nothing in the main hand." : String.format(Locale.ROOT, "%s: durability %d/%d, edge %s%s",
                t.getHoverName().getString(), t.getMaxDamage() - t.getDamageValue(), t.getMaxDamage(),
                w != null && w.hasEdge() ? String.format(Locale.ROOT, "%.1f (%d uses sharp to dull)", Wear.edge(t), Math.round(w.edgeUses())) : "none",
                Wear.dull(t) ? ", dull: x1.5 time, x1.3 kcal" : "");
        c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    private static int toolEdge(CommandContext<CommandSourceStack> c, boolean hone) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ItemStack t = p.getMainHandItem();
        if (t.isEmpty()) return 0;
        double e = DoubleArgumentType.getDouble(c, "edge");
        if (hone) Wear.hone(t, e);
        else Wear.setEdge(t, e);
        c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "Edge is now %.1f", Wear.edge(t))), false);
        return 1;
    }
}

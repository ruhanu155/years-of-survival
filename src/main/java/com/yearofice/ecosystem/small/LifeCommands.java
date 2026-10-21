package com.yearofice.ecosystem.small;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.world.FishSpotService;
import com.yearofice.ecosystem.clock.SliceClock;
import com.yearofice.sim.life.Bugs;
import com.yearofice.sim.life.LemmingCycle;
import com.yearofice.sim.life.Winter;
import com.yearofice.sim.time.Calendar;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi lemming [set <low|rise|peak|crash>|clear]}, {@code /yoi insects [on|off|auto]} and
 * {@code /yoi winterkill [hardness <h>|run|status]} (M7 lane C debug commands).
 */
public final class LifeCommands {
    private LifeCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("lemming").executes(LifeCommands::lemming)
                        .then(Commands.literal("clear").executes(c -> { LemmingIndex.clear(); return lemming(c); }))
                        .then(Commands.literal("set").then(Commands.argument("phase", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"low", "rise", "peak", "crash"}, b))
                                .executes(LifeCommands::setLemming))))
                .then(Commands.literal("insects").executes(LifeCommands::insects)
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"on", "off", "auto"}, b))
                                .executes(LifeCommands::setInsects)))
                .then(Commands.literal("winterkill").executes(LifeCommands::winterStatus)
                        .then(Commands.literal("hardness").then(Commands.argument("h", DoubleArgumentType.doubleArg(0.5, 2.0))
                                .executes(c -> { UnderIce.pinnedHardness = DoubleArgumentType.getDouble(c, "h"); return winterStatus(c); })))
                        .then(Commands.literal("auto").executes(c -> { UnderIce.pinnedHardness = Double.NaN; return winterStatus(c); }))
                        .then(Commands.literal("run").executes(LifeCommands::winterRun))));
    }

    static int lemming(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        LemmingCycle.Phase p = LemmingIndex.phase(l);
        c.getSource().sendSuccess(() -> Component.literal("Lemming index L " + p.index() + " (" + p.name().toLowerCase() + "); bold " + LemmingCycle.bold(p.index())
                + "; pike bite x" + LemmingCycle.pikeBiteMult(p.index(), 5) + " in May"), false);
        return 1;
    }

    static int setLemming(CommandContext<CommandSourceStack> c) {
        LemmingCycle.Phase p = LemmingCycle.Phase.of(StringArgumentType.getString(c, "phase"));
        if (p == null) {
            c.getSource().sendFailure(Component.literal("Phase is low, rise, peak or crash."));
            return 0;
        }
        LemmingIndex.pin(p);
        return lemming(c);
    }

    static int insects(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        StringBuilder sb = new StringBuilder("Insects " + InsectService.mode.name().toLowerCase() + "; bites " + InsectService.bitesTotal);
        if (c.getSource().getEntity() instanceof ServerPlayer p) {
            for (Bugs.Kind k : Bugs.Kind.values()) sb.append("; ").append(k.path()).append(" B ").append(String.format("%.2f", InsectService.pressureAt(l, p.blockPosition(), k)));
            sb.append("; swarms near ").append(InsectService.near(l, p.blockPosition(), 16).size());
        }
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    static int setInsects(CommandContext<CommandSourceStack> c) {
        String m = StringArgumentType.getString(c, "mode");
        try {
            InsectService.mode = InsectService.Mode.valueOf(m.toUpperCase());
        } catch (IllegalArgumentException ex) {
            c.getSource().sendFailure(Component.literal("Mode is on, off or auto."));
            return 0;
        }
        return insects(c);
    }

    static int winterStatus(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        int year = Calendar.yearOfDay(Calendar.absDayOfMinute(SliceClock.minute(l)));
        double h = UnderIce.hardness(l, year);
        StringBuilder sb = new StringBuilder(String.format("Winter %d hardness %.2f%s.", year, h, Double.isNaN(UnderIce.pinnedHardness) ? "" : " (pinned)"));
        for (String type : new String[] {"bog_pool", "beaver_pond", "oxbow", "tarn", "lake"}) {
            Winter.Result r = Winter.simulate(type, 1.2, false, h, 1, LifeConfig.tuning());
            sb.append(String.format(" %s: DO %.1f (min %.1f), %.0f%% lost;", type, r.finalDo(), r.minDo(), 100 * r.lostShare()));
        }
        c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }

    /** Runs the breakup pass now for the spots within 48 m of the source. */
    static int winterRun(CommandContext<CommandSourceStack> c) {
        ServerLevel l = c.getSource().getLevel();
        int year = Calendar.yearOfDay(Calendar.absDayOfMinute(SliceClock.minute(l)));
        int n = 0;
        for (FishSpotService.Spot s : FishSpotService.get().spotsNear(l, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()), 48)) n += UnderIce.breakup(l, s, year);
        int placed = n;
        c.getSource().sendSuccess(() -> Component.literal("Winterkill: " + placed + " fish carcasses at the surface."), true);
        return n;
    }
}

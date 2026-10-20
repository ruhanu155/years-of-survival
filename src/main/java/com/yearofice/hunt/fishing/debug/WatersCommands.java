package com.yearofice.hunt.fishing.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.hunt.fishing.FishingClock;
import com.yearofice.hunt.fishing.FishingSpecies;
import com.yearofice.hunt.fishing.RunLedger;
import com.yearofice.hunt.fishing.WaterDrawService;
import com.yearofice.hunt.fishing.WaterLife;
import com.yearofice.sim.fishing.FishSeason;
import com.yearofice.sim.fishing.SalmonRun;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /yoi waters}: the M6 water-life debug commands. {@code run [species]} prints each run's strength today, its
 * year factor, echo and size; {@code draw} posts a water-draw event for the spots near the source and says what was
 * sampled; {@code take} takes one fish from the nearest spot as a predator would.
 */
public final class WatersCommands {
    private WatersCommands() {}

    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("waters")
                .then(Commands.literal("run").executes(c -> run(c, null))
                        .then(Commands.argument("species", StringArgumentType.word()).executes(c -> run(c, StringArgumentType.getString(c, "species")))))
                .then(Commands.literal("draw").executes(WatersCommands::draw))
                .then(Commands.literal("take").executes(WatersCommands::take))));
    }

    static int run(CommandContext<CommandSourceStack> c, String only) {
        ServerLevel level = c.getSource().getLevel();
        long day = FishingClock.day(FishingClock.minute(level.getServer()));
        int doy = FishSeason.dayOfYear(day), year = RunLedger.yearOf(day);
        RunLedger l = RunLedger.get(level);
        for (var en : SalmonRun.RUNS.entrySet()) {
            if (only != null && !en.getKey().startsWith(only.toLowerCase(Locale.ROOT))) continue;
            String id = en.getKey();
            c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "%s: strength %.2f, y*e %.2f, R %.0f, taken this year %d (h %.2f)%s",
                    id, SalmonRun.strength(id, doy), l.strengthFactor(level.getSeed(), id, year), l.runSize(level.getSeed(), id, year),
                    l.taken(id, year), l.share(level.getSeed(), id, year), FishingSpecies.has(id) ? "" : " [no fish file]")), false);
        }
        return 1;
    }

    static int draw(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        var spots = WaterLife.spotsNear(level, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()), 64);
        for (var s : spots) {
            var ev = WaterDrawService.post(level, s);
            c.getSource().sendSuccess(() -> Component.literal(String.format(Locale.ROOT, "%s at %s: schedule %.2f, run %.2f, drawn %s",
                    s.type(), s.centre().toShortString(), ev.strength(), ev.runStrength(), ev.drawn())), false);
        }
        if (spots.isEmpty()) c.getSource().sendSuccess(() -> Component.literal("No fishing spot within 64 m."), false);
        return spots.size();
    }

    static int take(CommandContext<CommandSourceStack> c) {
        ServerLevel level = c.getSource().getLevel();
        var spots = WaterLife.spotsNear(level, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()), 64);
        String got = spots.isEmpty() ? null : WaterLife.takeFish(level, spots.get(0));
        c.getSource().sendSuccess(() -> Component.literal(got == null ? "Nothing to take." : "Took a " + got + "."), false);
        return got == null ? 0 : 1;
    }
}

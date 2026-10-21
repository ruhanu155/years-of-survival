package com.yearofice.ecosystem.pool;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.yearofice.sim.ecosystem.PoolModel;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi pool list|show <species>|pull <species>} (RECONCILE 306). */
public final class PoolCommands {
    private PoolCommands() {}

    public static void register(RegisterCommandsEvent event) {
        var species = Commands.argument("species", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(ValleyPool.SPECIES.stream().map(s -> s.substring(s.indexOf(':') + 1)), b));
        event.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("pool")
                        .then(Commands.literal("list").executes(c -> list(c.getSource())))
                        .then(Commands.literal("show").then(species.executes(c -> show(c.getSource(), StringArgumentType.getString(c, "species")))))
                        .then(Commands.literal("pull").then(Commands.argument("species", StringArgumentType.word())
                                .suggests((c, b) -> SharedSuggestionProvider.suggest(ValleyPool.SPECIES.stream().map(s -> s.substring(s.indexOf(':') + 1)), b))
                                .executes(c -> pull(c.getSource(), StringArgumentType.getString(c, "species")))))));
    }

    static int list(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        ValleyPool pool = ValleyPool.of(level);
        pool.ensureSeeded(level);
        Map<String, int[]> by = new TreeMap<>();
        for (var a : pool.individuals()) {
            int[] v = by.computeIfAbsent(a.species, k -> new int[5]);
            v[0]++;
            if (a.dead) v[4]++;
            else if (a.live > 0) v[3]++;
            else if (a.mode == PoolModel.Mode.APPROACHING) v[2]++;
            else if (a.mode == PoolModel.Mode.DENNED) v[1]++;
        }
        StringBuilder sb = new StringBuilder("Valley pool (in " + pool.returned() + ", out " + pool.instantiated() + "):");
        by.forEach((k, v) -> sb.append(String.format("%n%s: %d (denned %d, approaching %d, live %d, dead %d)", k, v[0], v[1], v[2], v[3], v[4])));
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return by.size();
    }

    static int show(CommandSourceStack src, String name) {
        String sp = "yearofice:" + name;
        ServerLevel level = src.getLevel();
        ValleyPool pool = ValleyPool.of(level);
        pool.ensureSeeded(level);
        StringBuilder sb = new StringBuilder(name + ":");
        int n = 0;
        for (var a : pool.individuals()) {
            if (!a.species.equals(sp)) continue;
            n++;
            if (n <= 12) sb.append(String.format("%n%s %s at %.0f,%.0f home %.0f,%.0f hunger %.2f x%d%s", a.id, a.mode, a.x, a.z, a.homeX, a.homeZ, a.hunger, a.members, a.dead ? " DEAD" : a.live > 0 ? " LIVE" : ""));
        }
        int total = n;
        src.sendSuccess(() -> Component.literal(sb + (total > 12 ? "\n... " + total + " in all" : "")), false);
        return n;
    }

    static int pull(CommandSourceStack src, String name) {
        var p = src.getPlayer();
        if (p == null) return 0;
        var a = ValleyPool.pull(src.getLevel(), p, "yearofice:" + name);
        src.sendSuccess(() -> Component.literal(a == null ? "No " + name + " in the pool." : a.id + " is walking in: " + a.mode + (a.live > 0 ? " (live)" : "") + "."), true);
        return a == null ? 0 : 1;
    }
}

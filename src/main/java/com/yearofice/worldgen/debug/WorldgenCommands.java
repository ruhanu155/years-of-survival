package com.yearofice.worldgen.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.SeedStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /yoi worldgen ...}: M0 debug commands for the seeded world. Hooked under {@code /yoi} by one line in
 * {@code core.debug.YoiCommands}, so the operator check there covers these too.
 *
 * <ul>
 *   <li>{@code /yoi worldgen seed [attempt]}: stage 1's frozen parameters for this world's level seed.</li>
 *   <li>{@code /yoi worldgen hash}: runs stages 1–3 off the main thread and prints the attempt that passed,
 *       the landform and height hashes, and the height and floor numbers. Compare with
 *       {@code ./gradlew debugmap --args="seed=<level seed>"}.</li>
 * </ul>
 */
public final class WorldgenCommands {
    private WorldgenCommands() {}

    private static final ExecutorService GEN = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "yearofice-gen-debug");
        t.setDaemon(true);
        return t;
    });

    private static volatile Pipeline pipeline;

    private static Pipeline pipeline() {
        Pipeline p = pipeline;
        if (p == null) pipeline = p = Pipeline.standard();
        return p;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("worldgen")
                .then(Commands.literal("seed")
                        .executes(ctx -> seed(ctx, 0))
                        .then(Commands.argument("attempt", IntegerArgumentType.integer(0, 19))
                                .executes(ctx -> seed(ctx, IntegerArgumentType.getInteger(ctx, "attempt")))))
                .then(Commands.literal("hash").executes(WorldgenCommands::hash));
    }

    private static int seed(CommandContext<CommandSourceStack> ctx, int attempt) {
        long levelSeed = ctx.getSource().getLevel().getSeed();
        SeedStage.SeedParams s = pipeline().seed(levelSeed, attempt);
        StringBuilder roles = new StringBuilder();
        for (int i = 0; i < s.regionRoles().size(); i++) {
            if (i > 0) roles.append(i % 4 == 0 ? " / " : " ");
            roles.append(SeedStage.regionCode(i)).append('=').append(s.regionRoles().get(i).name().toLowerCase());
        }
        String msg = String.format(
                "Worldgen seed: level %d | our seed %s | attempt %d seed %s%n%s%s | river %c@%.0f m -> %c@%.0f m | catchment %.0f km2 | hare phase %d%n%s",
                levelSeed, Integer.toUnsignedString(s.seed()), s.attempt(), Integer.toUnsignedString(s.attemptSeed()),
                s.archetype().id(), s.mirror() ? " (mirrored)" : "", s.entryEdge(), s.entryOffsetM(), s.exitEdge(),
                s.exitOffsetM(), s.virtualCatchmentKm2(), s.harePhase(), roles);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int hash(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        MinecraftServer server = src.getServer();
        long levelSeed = src.getLevel().getSeed();
        src.sendSuccess(() -> Component.literal("Worldgen: running stages 1-3 for seed " + levelSeed + "..."), false);
        GEN.execute(() -> {
            String msg;
            try {
                Pipeline.Result r = pipeline().run(levelSeed);
                Pipeline.Stats st = r.stats();
                msg = String.format(
                        "Worldgen v%d seed %d: attempt %d %s%s | landform %08x | height %08x | %.0f-%.0f m | floor median %.1f m%s",
                        pipeline().params().generatorVersion(), levelSeed, r.attempt(), r.seed().archetype().id(),
                        r.seed().mirror() ? " mirrored" : "", r.landformHash(), r.heightHash(), st.minM(), st.maxM(),
                        st.floorMedianM(), r.passed() ? "" : " | FAILED " + r.failures());
            } catch (RuntimeException e) {
                msg = "Worldgen failed: " + e;
            }
            String out = msg;
            server.execute(() -> src.sendSuccess(() -> Component.literal(out), false));
        });
        return 1;
    }
}

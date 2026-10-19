package com.yearofice.worldgen.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.yearofice.worldgen.CoarseRivers;
import com.yearofice.worldgen.Landmarks;
import com.yearofice.worldgen.Pipeline;
import com.yearofice.worldgen.Rivers;
import com.yearofice.worldgen.SeedStage;
import com.yearofice.worldgen.SetPieceDef;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.WaterStage;
import com.yearofice.worldgen.structure.PlacedMask;
import com.yearofice.worldgen.structure.SetPieceTemplates;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import javax.imageio.ImageIO;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * {@code /yoi worldgen ...}: debug commands for the seeded world. Hooked under {@code /yoi} by one line in
 * {@code core.debug.YoiCommands}, so the operator check there covers these too. Every command that needs the
 * layers runs stages 1–5 for this world's level seed once, off the main thread, and keeps the result.
 *
 * <ul>
 *   <li>M0: {@code seed [attempt]}, {@code hash}.</li>
 *   <li>M1: {@code layers} (every stage's hash), {@code rivers}, {@code lakes}, {@code falls},
 *       {@code spots [count]}, {@code faces}, {@code landmarks}, {@code setpiece list},
 *       {@code setpiece info <id>}, {@code setpiece place <id> [variant]} (stamps the template at your feet with
 *       the placed mask), {@code map [layer]} (writes {@code debugmap/seed-<seed>/<layer>.png} in the server
 *       folder: m1, water, current, spots, rock, places, world).</li>
 * </ul>
 * Compare any of it with {@code ./gradlew debugmap --args="seed=<level seed>"}.
 */
public final class WorldgenCommands {
    private WorldgenCommands() {}

    private static final ExecutorService GEN = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "yearofice-gen-debug");
        t.setDaemon(true);
        return t;
    });

    private static volatile Pipeline pipeline;
    private static volatile Pipeline.Result cached;

    private static Pipeline pipeline() {
        Pipeline p = pipeline;
        if (p == null) pipeline = p = Pipeline.standard();
        return p;
    }

    private static final SuggestionProvider<CommandSourceStack> SETPIECES = (ctx, b) -> {
        List<String> ids = new ArrayList<>();
        for (SetPieceDef d : pipeline().setPieces()) ids.add(d.path());
        return SharedSuggestionProvider.suggest(ids, b);
    };

    private static final SuggestionProvider<CommandSourceStack> VARIANTS = (ctx, b) -> {
        String id = StringArgumentType.getString(ctx, "id");
        List<String> names = new ArrayList<>();
        for (SetPieceDef d : pipeline().setPieces()) if (d.path().equals(id)) for (SetPieceDef.Variant v : d.variants()) names.add(v.name());
        return SharedSuggestionProvider.suggest(names, b);
    };

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("worldgen")
                .then(Commands.literal("seed")
                        .executes(ctx -> seed(ctx, 0))
                        .then(Commands.argument("attempt", IntegerArgumentType.integer(0, 19))
                                .executes(ctx -> seed(ctx, IntegerArgumentType.getInteger(ctx, "attempt")))))
                .then(Commands.literal("hash").executes(WorldgenCommands::hash))
                .then(Commands.literal("layers").executes(ctx -> report(ctx, WorldgenCommands::layers)))
                .then(Commands.literal("rivers").executes(ctx -> report(ctx, WorldgenCommands::rivers)))
                .then(Commands.literal("lakes").executes(ctx -> report(ctx, WorldgenCommands::lakes)))
                .then(Commands.literal("falls").executes(ctx -> report(ctx, WorldgenCommands::falls)))
                .then(Commands.literal("spots")
                        .executes(ctx -> report(ctx, r -> spots(r, 10)))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 200))
                                .executes(ctx -> {
                                    int c = IntegerArgumentType.getInteger(ctx, "count");
                                    return report(ctx, r -> spots(r, c));
                                })))
                .then(Commands.literal("faces").executes(ctx -> report(ctx, WorldgenCommands::faces)))
                .then(Commands.literal("landmarks").executes(ctx -> report(ctx, WorldgenCommands::landmarks)))
                .then(Commands.literal("setpiece")
                        .then(Commands.literal("list").executes(ctx -> report(ctx, WorldgenCommands::setpieces)))
                        .then(Commands.literal("info")
                                .then(Commands.argument("id", StringArgumentType.word()).suggests(SETPIECES)
                                        .executes(ctx -> info(ctx, StringArgumentType.getString(ctx, "id")))))
                        .then(Commands.literal("place")
                                .then(Commands.argument("id", StringArgumentType.word()).suggests(SETPIECES)
                                        .executes(ctx -> place(ctx, StringArgumentType.getString(ctx, "id"), null))
                                        .then(Commands.argument("variant", StringArgumentType.word()).suggests(VARIANTS)
                                                .executes(ctx -> place(ctx, StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "variant")))))))
                .then(Commands.literal("map")
                        .executes(ctx -> map(ctx, "m1"))
                        .then(Commands.argument("layer", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(MapRender.LAYERS, b))
                                .executes(ctx -> map(ctx, StringArgumentType.getString(ctx, "layer")))));
    }

    // ------------------------------------------------------------------------------------------------ M0

    private static int seed(CommandContext<CommandSourceStack> ctx, int attempt) {
        long levelSeed = ctx.getSource().getLevel().getSeed();
        SeedStage.SeedParams s = pipeline().seed(levelSeed, attempt);
        StringBuilder roles = new StringBuilder();
        for (int i = 0; i < s.regionRoles().size(); i++) {
            if (i > 0) roles.append(i % 4 == 0 ? " / " : " ");
            roles.append(SeedStage.regionCode(i)).append('=').append(s.regionRoles().get(i).name().toLowerCase(Locale.ROOT));
        }
        String msg = String.format(Locale.ROOT,
                "Worldgen seed: level %d | our seed %s | attempt %d seed %s%n%s%s | river %c@%.0f m -> %c@%.0f m | catchment %.0f km2 | hare phase %d%n%s",
                levelSeed, Integer.toUnsignedString(s.seed()), s.attempt(), Integer.toUnsignedString(s.attemptSeed()),
                s.archetype().id(), s.mirror() ? " (mirrored)" : "", s.entryEdge(), s.entryOffsetM(), s.exitEdge(),
                s.exitOffsetM(), s.virtualCatchmentKm2(), s.harePhase(), roles);
        ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int hash(CommandContext<CommandSourceStack> ctx) {
        return report(ctx, r -> {
            Pipeline.Stats st = r.stats();
            return String.format(Locale.ROOT,
                    "Worldgen v%d seed %d: attempt %d %s%s | landform %08x | height %08x | %.0f-%.0f m | floor median %.1f m%s",
                    pipeline().params().generatorVersion(), r.seed().levelSeed(), r.attempt(), r.seed().archetype().id(),
                    r.seed().mirror() ? " mirrored" : "", r.landformHash(), r.heightHash(), st.minM(), st.maxM(),
                    st.floorMedianM(), r.passed() ? "" : " | FAILED " + r.failures());
        });
    }

    // ------------------------------------------------------------------------------------------------ M1

    /** Runs (or reuses) stages 1–5 for this level's seed off the main thread and replies with {@code text}. */
    private static int report(CommandContext<CommandSourceStack> ctx, Function<Pipeline.Result, String> text) {
        CommandSourceStack src = ctx.getSource();
        MinecraftServer server = src.getServer();
        long levelSeed = src.getLevel().getSeed();
        Pipeline.Result have = cached;
        if (have == null || have.seed().levelSeed() != levelSeed) {
            src.sendSuccess(() -> Component.literal("Worldgen: running stages 1-5 for seed " + levelSeed + "..."), false);
        }
        GEN.execute(() -> {
            String msg;
            try {
                Pipeline.Result r = result(levelSeed);
                msg = r.water() == null ? "Worldgen: stages 1-3 failed: " + r.failures() : text.apply(r);
            } catch (RuntimeException e) {
                msg = "Worldgen failed: " + e;
            }
            String out = msg;
            server.execute(() -> src.sendSuccess(() -> Component.literal(out), false));
        });
        return 1;
    }

    static Pipeline.Result result(long levelSeed) {
        Pipeline.Result r = cached;
        if (r == null || r.seed().levelSeed() != levelSeed) cached = r = pipeline().run(levelSeed);
        return r;
    }

    private static String layers(Pipeline.Result r) {
        return String.format(Locale.ROOT,
                "Layers, seed %d attempt %d (%s): landform %08x | height %08x | sediment %08x | coarse rivers %08x | water %08x | places %08x | M1 %08x%s",
                r.seed().levelSeed(), r.attempt(), r.seed().archetype().path(), r.landformHash(), r.heightHash(),
                r.erosion().sedimentHash(), r.water().coarse().hash(), r.waterHash(), r.placesHash(), r.m1Hash(),
                r.passed() ? "" : " | FAILED " + r.failures());
    }

    private static double dist(double a, double b) { return Math.sqrt(a * a + b * b); }

    private static String xz(double x, double z) { return String.format(Locale.ROOT, "%.0f, %.0f", x, z); }

    private static String cell(Pipeline.Result r, int cell) {
        int n = r.water().cells();
        return xz(-2048 + (cell % n + 0.5) * 2, (cell / n + 0.5) * 2);
    }

    private static String rivers(Pipeline.Result r) {
        WaterStage.Layers w = r.water();
        Rivers.Main m = w.main();
        double wmin = Double.MAX_VALUE, wmax = 0, vmax = 0;
        for (Rivers.Line ln : m.lines()) for (int i = 0; i < ln.size(); i++) {
            wmin = Math.min(wmin, ln.w()[i]);
            wmax = Math.max(wmax, ln.w()[i]);
            vmax = Math.max(vmax, ln.v()[i]);
        }
        CoarseRivers.Net net = w.coarse();
        Rivers.Line ax = m.lines().get(0);
        double de = dist(ax.x()[0] - net.entry().lx(), ax.z()[0] - net.entry().lz());
        double dx = dist(ax.x()[ax.size() - 1] - net.exit().lx(), ax.z()[ax.size() - 1] - net.exit().lz());
        return String.format(Locale.ROOT,
                "Main river: %.0f m, %d channel(s)%s, swing %.2f rad over %.0f m (retries %d), width %.1f-%.1f m, current up to %.1f m/s%n"
                        + "Entry %.1f m and exit %.1f m from the coarse crossings; %d oxbows (%d lakes); %d creeks; %d inflows from the coarse net",
                m.lengthM(), m.lines().size(), m.braids() > 1 ? String.format(Locale.ROOT, " across a %.0f m gravel belt", m.beltM()) : "",
                m.omega(), m.wavelengthM(), m.retries(), wmin, wmax, vmax, de, dx, m.oxbows().size(), w.oxbowLakes(),
                w.creeks().size(), net.inflows().size());
    }

    private static String lakes(Pipeline.Result r) {
        StringBuilder b = new StringBuilder("Lakes: " + r.water().lakes().size());
        List<WaterStage.Lake> ls = new ArrayList<>(r.water().lakes());
        ls.sort((a, c) -> Double.compare(c.areaM2(), a.areaM2()));
        for (int i = 0; i < Math.min(10, ls.size()); i++) {
            WaterStage.Lake l = ls.get(i);
            b.append(String.format(Locale.ROOT, "%n  %s %d: %.0f m2, %.1f m deep (mean %.1f), level %.1f m, centre %s, outlet %s",
                    l.kind(), l.bodyId(), l.areaM2(), l.maxDepthM(), l.meanDepthM(), l.levelM(), xz(-2048 + l.cx(), l.cz()),
                    l.outlet() >= 0 ? cell(r, l.outlet()) : "edge"));
        }
        return b.toString();
    }

    private static String falls(Pipeline.Result r) {
        StringBuilder b = new StringBuilder("Falls: " + r.water().falls().size() + " (archetype minimum "
                + pipeline().archetype(r.seed().archetype()).minimumFalls() + ")");
        for (WaterStage.Fall f : r.water().falls()) {
            b.append(String.format(Locale.ROOT, "%n  %s %.1f m over %.0f m at %s, pool %.0f m%s%s", f.onMain() ? "main" : "creek",
                    f.heightM(), f.runM(), cell(r, f.foot()), f.poolRadiusM(), f.guaranteed() ? ", guaranteed" : "",
                    f.faceHeightM() > 0 ? String.format(Locale.ROOT, ", rock face %.0f m", f.faceHeightM()) : ""));
        }
        return b.toString();
    }

    private static String spots(Pipeline.Result r, int count) {
        List<WaterStage.Spot> all = new ArrayList<>(r.spots());
        all.sort((a, b) -> Double.compare(b.q(), a.q()));
        int major = 0;
        for (WaterStage.Spot s : all) if (s.major()) major++;
        StringBuilder b = new StringBuilder("Fishing spots: " + all.size() + " (" + major + " major)");
        for (int i = 0; i < Math.min(count, all.size()); i++) {
            WaterStage.Spot s = all.get(i);
            StringBuilder stock = new StringBuilder();
            for (int k = 0; k < s.species().size(); k++) stock.append(k > 0 ? ", " : "").append(s.species().get(k)).append(' ').append(s.stock()[k]);
            b.append(String.format(Locale.ROOT, "%n  %d. %s q %.2f at %s, %.1f m deep, r %.0f m: %s", i + 1, s.type(), s.q(),
                    xz(s.x(), s.z()), s.depthM(), s.radiusM(), stock));
        }
        return b.toString();
    }

    private static String faces(Pipeline.Result r) {
        WaterStage.Layers w = r.water();
        StringBuilder b = new StringBuilder("Rock faces: " + w.faces().size() + ", shelter candidates " + w.shelters().size()
                + ", iron clusters " + w.iron().size());
        for (WaterStage.Face f : w.faces()) {
            if (f.rank() == 0) continue;
            b.append(String.format(Locale.ROOT, "%n  #%d %.0f m tall facing %.0f deg, foot %s", f.rank(), f.heightM(), f.aspectDeg(), cell(r, f.foot())));
        }
        for (WaterStage.IronCluster c : w.iron()) {
            b.append(String.format(Locale.ROOT, "%n  iron: %d nodes at %s, %.0f m", c.nodes().length, cell(r, c.centre()), c.elevationM()));
        }
        return b.toString();
    }

    private static String landmarks(Pipeline.Result r) {
        StringBuilder b = new StringBuilder("Landmarks (2 km test: points seen of 8):");
        for (Landmarks.Landmark l : r.places().landmarks()) {
            b.append(String.format(Locale.ROOT, "%n  %s %s %s %.0f m at %s, base %.0f m: %d/8", SeedStage.regionCode(l.region()),
                    l.kind(), l.source(), l.heightM(), xz(l.x(), l.z()), l.baseM(), l.visiblePoints()));
        }
        return b.toString();
    }

    private static String setpieces(Pipeline.Result r) {
        StringBuilder b = new StringBuilder("Set pieces: " + r.places().placements().size());
        for (SetPieces.Placement q : r.places().placements()) {
            double d = r.places().fromStart(q);
            b.append(String.format(Locale.ROOT, "%n  %s #%d %s on %s at %s y %.0f, %s, %s%s%s", q.id(), q.instance(), q.variant(),
                    q.site(), xz(q.x(), q.z()), q.y(), q.rotationName(), SeedStage.regionCode(q.region()),
                    q.coarse() ? " (coarse)" : "", Double.isNaN(d) || q.id().equals("yearofice:outpost") ? ""
                            : String.format(Locale.ROOT, ", %.0f m from start", d)));
        }
        return b.toString();
    }

    private static int info(CommandContext<CommandSourceStack> ctx, String id) {
        for (SetPieceDef d : pipeline().setPieces()) {
            if (!d.path().equals(id)) continue;
            StringBuilder b = new StringBuilder(String.format(Locale.ROOT, "%s (%s): count %.0f-%.0f, priority %d, spacing %.0f m, areas %s%n  fit %s%n  variants:",
                    d.name(), d.id(), d.count().min(), d.count().max(), d.priority(), d.spacingM(), d.areas(), d.fit()));
            for (SetPieceDef.Variant v : d.variants()) {
                b.append(String.format(Locale.ROOT, " %s (%dx%dx%d, weight %.0f%s)", v.template(), v.footprintX(), v.footprintZ(),
                        v.heightM(), v.weight(), v.sites().isEmpty() ? "" : ", " + v.sites()));
            }
            b.append("\n  ").append(d.story());
            String msg = b.toString();
            ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("No set piece " + id));
        return 0;
    }

    private static int place(CommandContext<CommandSourceStack> ctx, String id, String variant) {
        SetPieceDef def = null;
        for (SetPieceDef d : pipeline().setPieces()) if (d.path().equals(id)) def = d;
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("No set piece " + id));
            return 0;
        }
        SetPieceDef.Variant v = def.variants().get(0);
        if (variant != null) {
            v = null;
            for (SetPieceDef.Variant x : def.variants()) if (x.name().equals(variant)) v = x;
            if (v == null) {
                ctx.getSource().sendFailure(Component.literal("No variant " + variant + " of " + id));
                return 0;
            }
        }
        ServerLevel level = ctx.getSource().getLevel();
        Optional<StructureTemplate> t = SetPieceTemplates.load(level, v.template());
        if (t.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("Template missing: " + SetPieceTemplates.id(v.template())));
            return 0;
        }
        BlockPos at = BlockPos.containing(ctx.getSource().getPosition()).offset(1, 0, 1);
        PlacedMask mask = PlacedMask.get(level);
        int before = mask.count();
        Vec3i size = SetPieceTemplates.place(level, t.get(), at, net.minecraft.world.level.block.Rotation.NONE);
        int marked = mask.count() - before;
        String msg = "Placed " + SetPieceTemplates.id(v.template()) + " at " + at.toShortString() + " (" + size.getX() + "x"
                + size.getY() + "x" + size.getZ() + "), " + marked + " blocks in the placed mask";
        ctx.getSource().sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int map(CommandContext<CommandSourceStack> ctx, String layer) {
        boolean known = false;
        for (String l : MapRender.LAYERS) if (l.equals(layer)) known = true;
        if (!known) {
            ctx.getSource().sendFailure(Component.literal("Layers: " + String.join(", ", MapRender.LAYERS)));
            return 0;
        }
        File dir = ctx.getSource().getServer().getServerDirectory().resolve("debugmap/seed-" + ctx.getSource().getLevel().getSeed()).toFile();
        return report(ctx, r -> {
            try {
                dir.mkdirs();
                File f = new File(dir, layer + ".png");
                ImageIO.write(MapRender.layer(layer, r), "png", f);
                return "Wrote " + f.getPath() + " | " + MapRender.summary(r);
            } catch (java.io.IOException e) {
                return "Map failed: " + e;
            }
        });
    }
}

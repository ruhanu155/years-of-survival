package com.yearofice.worldgen.debug;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.world.WorldHooks;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * M2's walking commands (Ready for development › M2 debug commands), hooked under {@code /yoi} next to
 * {@code /yoi worldgen}:
 * <ul>
 *   <li>{@code /yoi tp region <cell>}: to a region's centre (A1–D4), or a named place: {@code start}, {@code slice},
 *       {@code spot}, a set-piece id ({@code outpost}, {@code fire_lookout}, ...), standing on its ground;</li>
 *   <li>{@code /yoi show current}: the region, cover class and biome, column and layer values at your feet;</li>
 *   <li>{@code /yoi show slice}: the slice valley's cut, counts, spot and seams.</li>
 * </ul>
 */
public final class WorldCommands {
    private WorldCommands() {}

    public static LiteralArgumentBuilder<CommandSourceStack> tp() {
        return Commands.literal("tp").then(Commands.literal("region").then(Commands.argument("cell", StringArgumentType.word())
                .suggests((c, b) -> {
                    List<String> s = new ArrayList<>(List.of("start", "slice", "spot"));
                    for (char col = 'A'; col <= 'D'; col++) for (int row = 1; row <= 4; row++) s.add("" + col + row);
                    ValleyWorld w = WorldHooks.world(c.getSource().getLevel());
                    if (w != null) for (SetPieces.Placement q : w.placements) s.add(q.id().substring(q.id().indexOf(':') + 1));
                    return SharedSuggestionProvider.suggest(s, b);
                })
                .executes(c -> tpRegion(c, StringArgumentType.getString(c, "cell")))));
    }

    public static LiteralArgumentBuilder<CommandSourceStack> show() {
        return Commands.literal("show")
                .then(Commands.literal("current").executes(WorldCommands::showCurrent))
                .then(Commands.literal("slice").executes(WorldCommands::showSlice));
    }

    private static ValleyWorld world(CommandContext<CommandSourceStack> c) {
        ValleyWorld w = WorldHooks.world(c.getSource().getLevel());
        if (w == null) c.getSource().sendFailure(Component.literal("This level's generator is not yearofice:valley"));
        return w;
    }

    static int tpRegion(CommandContext<CommandSourceStack> c, String cell) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ValleyWorld w = world(c);
        if (w == null) return 0;
        ServerPlayer p = c.getSource().getPlayerOrException();
        int x, z;
        String k = cell.toLowerCase(Locale.ROOT);
        if (k.equals("start")) {
            int[] s = w.spawn();
            x = s[0];
            z = s[2];
        } else if (k.equals("slice")) {
            x = w.slice.centreX();
            z = w.slice.centreZ();
        } else if (k.equals("spot") && w.sliceSpot != null) {
            x = (int) Math.floor(w.sliceSpot.x()) - 4;
            z = (int) Math.floor(w.sliceSpot.z()) - 4;
        } else if (k.matches("[a-d][1-4]")) {
            double rs = w.p.grid().worldSizeM() / 4.0, half = w.p.grid().worldSizeM() / 2.0;
            x = (int) (-half + (k.charAt(0) - 'a' + 0.5) * rs);
            z = (int) (-half + (k.charAt(1) - '1' + 0.5) * rs);
        } else {
            SetPieces.Placement hit = null;
            for (SetPieces.Placement q : w.placements) if (q.id().equals("yearofice:" + k)) { hit = q; break; }
            if (hit == null) {
                c.getSource().sendFailure(Component.literal("Unknown place " + cell + ": a region A1–D4, start, slice, spot or a set piece"));
                return 0;
            }
            int[] r = ValleyWorld.rect(hit);
            x = r[0] - 3;
            z = r[1] + r[3] / 2;
        }
        ValleyWorld.Columns col = w.columns(x, z, 1, 1);
        ServerLevel level = c.getSource().getLevel();
        level.getChunk(x >> 4, z >> 4);
        int y = Math.max(col.top(0) + 1, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z));
        p.teleportTo(level, x + 0.5, y, z + 0.5, p.getYRot(), p.getXRot());
        c.getSource().sendSuccess(() -> Component.literal("To " + cell + " at " + x + " " + y + " " + z + " (" + w.regionCode(x, z) + ")"), false);
        return 1;
    }

    static int showCurrent(CommandContext<CommandSourceStack> c) {
        ValleyWorld w = world(c);
        if (w == null) return 0;
        BlockPos at = BlockPos.containing(c.getSource().getPosition());
        for (String line : YoiChunkGenerator.debugLines(w, at.getX(), at.getZ())) c.getSource().sendSuccess(() -> Component.literal(line), false);
        return 1;
    }

    static int showSlice(CommandContext<CommandSourceStack> c) {
        ValleyWorld w = world(c);
        if (w == null) return 0;
        String s = String.format(Locale.ROOT, "Slice %d %d to %d %d%s | creek %d bar %d willow flat %d spruce %d birch %d cells, %d spots",
                w.slice.minX(), w.slice.minZ(), w.slice.minX() + w.slice.size(), w.slice.minZ() + w.slice.size(),
                w.slice.complete() ? "" : " (incomplete)", w.slice.creekCells(), w.slice.barCells(), w.slice.riparianCells(),
                w.slice.spruceCells(), w.slice.birchCells(), w.slice.spots());
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        if (w.sliceSpot != null) {
            String sp = String.format(Locale.ROOT, "Spot %s q %.2f at %.0f %.0f", w.sliceSpot.type(), w.sliceSpot.q(), w.sliceSpot.x(), w.sliceSpot.z());
            c.getSource().sendSuccess(() -> Component.literal(sp), false);
        }
        StringBuilder b = new StringBuilder("Coal seams:");
        for (var m : w.seams) b.append(String.format(Locale.ROOT, " %.0f,%.0f (%.0f m%s)", m.x(), m.z(), m.fromStartM(), m.coarse() ? ", bluff" : ""));
        c.getSource().sendSuccess(() -> Component.literal(b.toString()), false);
        return 1;
    }
}

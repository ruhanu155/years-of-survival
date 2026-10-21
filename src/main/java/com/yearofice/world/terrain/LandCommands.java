package com.yearofice.world.terrain;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.world.ChangeOp;
import com.yearofice.api.world.WorldEditService;
import com.yearofice.core.YearOfIce;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.world.IceModel;
import com.yearofice.sim.world.IceModel.BodyClass;
import com.yearofice.sim.world.SeasonModel;
import com.yearofice.sim.world.SnowPack.SnowClass;
import com.yearofice.world.ice.IceBlocks;
import com.yearofice.world.ice.IcePass;
import com.yearofice.world.season.PassContext;
import com.yearofice.world.WorldHooks;
import com.yearofice.world.season.SeasonConfig;
import com.yearofice.world.season.SeasonState;
import com.yearofice.world.season.SeasonSystem;
import com.yearofice.world.snow.SnowPass;
import com.yearofice.worldgen.block.WorldBlocks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** {@code /yoi ice set <cm>} and {@code /yoi land read|sky|micro|phenomenon|stand|slide|heave|hazard}. */
@EventBusSubscriber(modid = YearOfIce.MOD_ID)
public final class LandCommands {
    private LandCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        var land = Commands.literal("land")
                .then(Commands.literal("read").executes(c -> say(c, read(c.getSource()))))
                .then(Commands.literal("stand").executes(c -> say(c, stand(c.getSource()))))
                .then(Commands.literal("heave").executes(c -> say(c, heave(c.getSource()))))
                .then(Commands.literal("hazard").executes(c -> say(c, hazard(c.getSource()))));
        for (String s : new String[] {"sky", "micro", "phenomenon", "slide"}) {
            land.then(Commands.literal(s).executes(c -> say(c, "/yoi land " + s + ": not built yet (needs the env phenomena and the sky-lore lanes).")));
        }
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2))
                .then(Commands.literal("ice").executes(c -> say(c, iceLine(c.getSource().getLevel())))
                        .then(Commands.literal("set").then(Commands.argument("cm", DoubleArgumentType.doubleArg(0, 400))
                                .executes(c -> say(c, setIce(c.getSource(), DoubleArgumentType.getDouble(c, "cm")))))))
                .then(land));
    }

    private static int say(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    static String iceLine(ServerLevel level) {
        var core = SeasonState.get(level).core;
        StringBuilder sb = new StringBuilder("ice cm:");
        for (BodyClass b : BodyClass.values()) {
            var s = core.ice(b);
            sb.append(' ').append(b.name().toLowerCase()).append('=').append(String.format("%.1f", s.cm)).append(s.rotten ? "(rotten)" : "");
        }
        return sb.append("; flood ").append(String.format("%.2f m", core.floodM)).toString();
    }

    static String setIce(CommandSourceStack src, double cm) {
        ServerLevel level = src.getLevel();
        var core = SeasonState.get(level).core;
        for (BodyClass b : BodyClass.values()) {
            var s = core.ice(b);
            s.cm = cm; s.peakCm = cm; s.rotten = false; s.fdd = cm > 0 ? s.fdd : 0;
        }
        SeasonState.get(level).setDirty();
        int n = resettle(src);
        return "ice set to " + cm + " cm on every body class; " + n + " chunks settled.";
    }

    /** Settles the chunks around the source at once (live writes). */
    static int resettle(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        long today = ClockService.get().absDay();
        int cx = BlockPos.containing(src.getPosition()).getX() >> 4, cz = BlockPos.containing(src.getPosition()).getZ() >> 4;
        int n = 0;
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
            if (level.getChunkSource().getChunkNow(cx + dx, cz + dz) instanceof LevelChunk ch) { SeasonSystem.settle(level, ch, true, today); n++; }
        }
        return n;
    }

    static BlockPos here(CommandSourceStack src) { return BlockPos.containing(src.getPosition()); }

    static String read(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        int doy = Calendar.dayOfYearOfDay(ClockService.get().absDay());
        var core = SeasonState.get(level).core;
        var t = SeasonConfig.season();
        StringBuilder sb = new StringBuilder("day-of-year ").append(doy).append(", ground ")
                .append(SeasonModel.groundFrozen(doy, t) ? "frozen" : "open").append("; leafed ")
                .append(String.format("%.2f", SeasonModel.leafed(doy, t))).append("; snow cm:");
        for (SnowClass c : SnowClass.values()) {
            var s = core.snow(c);
            sb.append(' ').append(c.name().toLowerCase()).append('=').append(String.format("%.0f", s.depthCm()))
                    .append(s.crust ? "c" : "").append(s.wet ? "w" : "");
        }
        return sb.append("; ").append(iceLine(level)).toString();
    }

    static String stand(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos p = here(src);
        if (!(level.getChunk(p) instanceof LevelChunk ch)) return "chunk not loaded";
        var c = new PassContext(level, ch, true, SeasonState.get(level).core, SeasonConfig.snow(), SeasonConfig.ice(),
                TerrainChangeQueue.get(level).floodFlags(ch.getPos()));
        SnowClass cls = SnowPass.classify(c, p.getX() & 15, p.getZ() & 15);
        return "snow class here: " + cls.name().toLowerCase() + " (depth " + String.format("%.0f", c.core.snow(cls).depthCm()) + " cm)";
    }

    static String heave(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos p = here(src);
        BlockPos g = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p);
        long day = ClockService.get().absDay();
        WorldEditService.get().submit(level, new ChangeOp(day, ChangeOp.Kind.HEAVE, 0, g, "yearofice:till", true, 0));
        return "frost heave queued at " + g.toShortString() + " (a till mound; applied within a tick).";
    }

    static String hazard(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        BlockPos under = here(src).below();
        BlockState s = level.getBlockState(under);
        if (!IceBlocks.isIce(s) && !s.is(IceBlocks.ICE_HOLE.get())) {
            BlockState up = level.getBlockState(under.above());
            String snow = up.is(WorldBlocks.SNOW_COVER.get()) ? ", snow " + up.getValue(com.yearofice.worldgen.block.SnowCoverBlock.LAYERS) + " layers" : "";
            return "no ice underfoot (" + s.getBlock().getDescriptionId() + snow + ").";
        }
        int[] k = IcePass.classOf(WorldHooks.world(level), under.getX(), under.getZ());
        var ice = SeasonState.get(level).core.ice(BodyClass.values()[k[0]]);
        var t = SeasonConfig.ice();
        double eff = IceModel.effectiveCm(ice, t);
        return "ice " + BodyClass.values()[k[0]].name().toLowerCase() + ": " + String.format("%.1f", ice.cm) + " cm" + (ice.rotten ? " rotten" : "")
                + (k[1] == 1 ? ", thin spot" : "") + "; holds a walker: " + IceModel.holds(eff, 90, t) + ", a loaded sled: " + IceModel.holds(eff, 250, t) + ".";
    }
}

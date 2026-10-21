package com.yearofice.world.longterm;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.event.ClockEvent;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.world.BeaverDam;
import com.yearofice.sim.world.Regrowth;
import com.yearofice.sim.world.Wildfire;
import com.yearofice.world.terrain.TerrainChangeQueue;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.minecraft.world.level.chunk.LevelChunk;

/** Events and {@code /yoi longterm burn|camp|dam|status} for the long world. */
public final class LongTermHooks {
    private LongTermHooks() {}

    /** A chunk left alone this many days or more is aged on load. */
    public static final int ABSENT_DAYS = 365;

    public static void init() {
        // Highest: read the chunk's settle stamp before the season catch-up renews it.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, LongTermHooks::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(LongTermHooks::onDay);
        NeoForge.EVENT_BUS.addListener(LongTermHooks::commands);
    }

    static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk) || !ClockService.installed() || !EnvService.installed()) return;
        long stamp = TerrainChangeQueue.get(level).stamp(chunk.getPos());
        if (stamp == Long.MIN_VALUE) return;
        long today = ClockService.get().absDay();
        if (today - stamp < ABSENT_DAYS) return;
        LongTerm.ageChunk(level, chunk, (today - stamp) / 365.0, today); // the event's own chunk: asking the level for it inside its load would ask for it again
    }

    static void onDay(ClockEvent.Day e) {
        int doy = Calendar.dayOfYearOfDay(e.absDay());
        if (doy != BeaverDam.GROW_DOY) return;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            LongTermData d = LongTermData.get(level);
            for (var c : d.colonies) {
                if (!level.hasChunkAt(c.pos)) continue;
                c.abandoned = level.getEntitiesOfClass(com.yearofice.ecosystem.species.SmallGameEntity.class,
                        new net.minecraft.world.phys.AABB(c.pos).inflate(40), b -> b.speciesPath().equals("beaver")).isEmpty();
                LongTerm.growColony(level, c, treesNear(level, c.pos), e.absDay());
            }
        }
    }

    /** Logs of birch, aspen and willow within 30 blocks, sampled every second column. */
    static int treesNear(ServerLevel level, BlockPos at) {
        int n = 0;
        for (int dx = -30; dx <= 30; dx += 2)
            for (int dz = -30; dz <= 30; dz += 2) {
                BlockPos p = at.offset(dx, 0, dz);
                if (!level.hasChunkAt(p)) continue;
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, p.getX(), p.getZ());
                if (level.getBlockState(new BlockPos(p.getX(), y, p.getZ())).is(BlockTags.LOGS)) n++;
            }
        return n * 4;
    }

    static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("longterm")
                .then(Commands.literal("burn").then(Commands.argument("radius", IntegerArgumentType.integer(1, 40)).executes(c -> {
                    var p = BlockPos.containing(c.getSource().getPosition());
                    return say(c, "burn queued " + LongTerm.burn(c.getSource().getLevel(), p, IntegerArgumentType.getInteger(c, "radius"), ClockService.get().absDay()) + " ops");
                })))
                .then(Commands.literal("camp").then(Commands.argument("years", IntegerArgumentType.integer(1, 50)).executes(c -> {
                    var chunk = c.getSource().getLevel().getChunkAt(BlockPos.containing(c.getSource().getPosition()));
                    return say(c, "camp aged " + LongTerm.ageChunk(c.getSource().getLevel(), chunk, IntegerArgumentType.getInteger(c, "years"), ClockService.get().absDay()) + " blocks");
                })))
                .then(Commands.literal("dam").then(Commands.argument("stage", IntegerArgumentType.integer(0, BeaverDam.MAX_STAGE)).executes(c -> {
                    var p = BlockPos.containing(c.getSource().getPosition());
                    var lvl = c.getSource().getLevel();
                    int stage = IntegerArgumentType.getInteger(c, "stage");
                    var d = LongTermData.get(lvl);
                    d.colonies.add(new LongTermData.Colony(p, stage, ClockService.get().absDay(), false));
                    d.touch();
                    return say(c, "dam queued " + LongTerm.dam(lvl, p, stage, ClockService.get().absDay()) + " ops");
                })))
                .then(Commands.literal("status").executes(c -> {
                    var lvl = c.getSource().getLevel();
                    var q = TerrainChangeQueue.get(lvl);
                    int year = Calendar.yearOfDay(ClockService.get().absDay());
                    return say(c, "terrain ops pending=" + q.totalPending() + " colonies=" + LongTermData.get(lvl).colonies.size()
                            + " wildfire_year=" + Wildfire.wildfireYear(lvl.getSeed(), year, LongTerm.tuning())
                            + " regrowth_stage_of_burn_today=" + Regrowth.stageAt(ClockService.get().absDay(), ClockService.get().absDay(), 1.0));
                }))));
    }

    private static int say(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }
}

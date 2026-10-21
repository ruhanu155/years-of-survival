package com.yearofice.build.outpost;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yearofice.world.WorldHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Outposts (Open world in depth › Outposts): the decay data map, the worldgen hook that leaves a decayed line cabin,
 * fish camp, blind camp or rock shelter at the blueprint spacing, and {@code /yoi struct place|check|age|repair}.
 * Sites are deterministic: one chunk per spacing cell per outpost, picked by hashing the cell, and stamped when that
 * chunk is first generated. ponytail: sites follow region/<id>.json's outposts list (M9) and ignore rings beyond 1.
 */
public final class OutpostSystem {
    private OutpostSystem() {}

    public static void init(IEventBus modBus) {
        OutpostSounds.SOUNDS.register(modBus);
        OutpostRegistry.register(modBus);
        com.yearofice.build.raft.RaftRegistry.register(modBus);
        modBus.addListener((RegisterDataMapTypesEvent e) -> e.register(DecayRow.MAP));
        NeoForge.EVENT_BUS.register(OutpostSystem.class);
        NeoForge.EVENT_BUS.register(com.yearofice.world.snow.FenceDrift.class);
        NeoForge.EVENT_BUS.addListener(OutpostSystem::commands);
    }

    /** The chunk a cell's outpost of this kind stands in, as a hash of (seed, cell). */
    public static ChunkPos siteChunk(long seed, String id, OutpostDef def, int cellX, int cellZ) {
        int cells = Math.max(1, def.spacingM() / 16);
        long h = seed * 31 + id.hashCode() * 0x9E3779B97F4A7C15L + cellX * 0xC2B2AE3D27D4EB4FL + cellZ * 0x165667B19E3779F9L;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32;
        int ox = (int) Math.floorMod(h, (long) cells), oz = (int) Math.floorMod(h >>> 20, (long) cells);
        return new ChunkPos(cellX * cells + ox, cellZ * cells + oz);
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!e.isNewChunk() || !(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
        if (WorldHooks.world(level) == null) return;
        ChunkPos c = chunk.getPos();
        for (String id : OutpostDef.IDS) {
            OutpostDef def = OutpostDef.get(id);
            int cells = Math.max(1, def.spacingM() / 16);
            if (!siteChunk(level.getSeed(), id, def, Math.floorDiv(c.x, cells), Math.floorDiv(c.z, cells)).equals(c)) continue;
            if (!com.yearofice.worldgen.regions.Regions.allowsOutpost(c.getMinBlockX() + 3, c.getMinBlockZ() + 3, id)) continue; // M9: by region
            BlockPos o = new BlockPos(c.getMinBlockX() + 3, 0, c.getMinBlockZ() + 3);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX(), o.getZ());
            BlockPos origin = o.atY(y);
            if (!level.getFluidState(origin.below()).isEmpty() && !def.fit().equals("shore")) continue;
            long seed = level.getSeed() ^ c.toLong();
            Outposts.stamp(level, origin, def);
            double years = (def.minAge() + (def.maxAge() - def.minAge()) * com.yearofice.sim.build.OutpostDecay.roll(seed, 1, 2, 3)) * OutpostConfig.get("outpost_age_scale");
            Outposts.age(level, origin, def, years, seed);
            com.yearofice.story.keepsake.OutpostKeepsakes.place(level, origin, id);
        }
    }

    /** The first time a player stands within 30 m of a worldgen outpost it posts one Messenger line (M9 J). Sites are deterministic, so no index is kept. */
    @SubscribeEvent
    public static void visit(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) || p.tickCount % 40 != 0 || WorldHooks.world(p.serverLevel()) == null) return;
        scan(p);
    }

    /** One visit check for {@code p} (the tick handler's body; public for tests, which have no valley generator). */
    public static void scan(net.minecraft.server.level.ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        var seen = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        var tag = seen.getCompound("yoi_outposts_seen");
        for (String id : OutpostDef.IDS) {
            OutpostDef def = OutpostDef.get(id);
            int cells = Math.max(1, def.spacingM() / 16);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                ChunkPos c = siteChunk(level.getSeed(), id, def, Math.floorDiv(p.chunkPosition().x, cells) + dx, Math.floorDiv(p.chunkPosition().z, cells) + dz);
                double ddx = p.getX() - (c.getMinBlockX() + 3), ddz = p.getZ() - (c.getMinBlockZ() + 3);
                String key = id + "@" + c.toLong();
                if (ddx * ddx + ddz * ddz > 900 || tag.getBoolean(key) || !level.hasChunk(c.x, c.z)) continue;
                if (!com.yearofice.worldgen.regions.Regions.allowsOutpost(c.getMinBlockX() + 3, c.getMinBlockZ() + 3, id)) continue;
                tag.putBoolean(key, true);
                seen.put("yoi_outposts_seen", tag);
                p.getPersistentData().put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, seen);
                com.yearofice.api.hud.HudBus.get().post(p, com.yearofice.api.hud.HudLine.of(com.yearofice.api.hud.HudLineType.EVENT, "outpost_visit", "build.outpost",
                        com.yearofice.api.hud.HudArg.text(def.name())));
            }
        }
    }

    static void commands(RegisterCommandsEvent e) {
        var id = Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(OutpostDef.IDS, b));
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("raft").then(Commands.literal("spawn").executes(c -> {
            var r = new com.yearofice.build.raft.RaftEntity(com.yearofice.build.raft.RaftRegistry.RAFT.get(), c.getSource().getLevel());
            r.setPos(c.getSource().getPosition());
            c.getSource().getLevel().addFreshEntity(r);
            c.getSource().sendSuccess(() -> Component.literal("A raft of six lashed logs."), true);
            return 1;
        }))));
        e.getDispatcher().register(Commands.literal("yoi").requires(s -> s.hasPermission(2)).then(Commands.literal("struct")
                .then(Commands.literal("place").then(id.executes(c -> place(c))))
                .then(Commands.literal("check").then(Commands.argument("id", StringArgumentType.word()).executes(OutpostSystem::check)))
                .then(Commands.literal("age").then(Commands.argument("years", DoubleArgumentType.doubleArg(0, 500))
                        .then(Commands.argument("id", StringArgumentType.word()).executes(OutpostSystem::age))))));
    }

    private static int place(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        OutpostDef def = OutpostDef.get(StringArgumentType.getString(c, "id"));
        Outposts.stamp(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), def);
        c.getSource().sendSuccess(() -> Component.literal("Placed " + def.name() + " with its south-west floor corner at your feet."), true);
        return 1;
    }

    private static int check(CommandContext<CommandSourceStack> c) {
        OutpostDef def = OutpostDef.get(StringArgumentType.getString(c, "id"));
        var miss = Outposts.missing(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()), def);
        java.util.Map<String, Long> by = miss.stream().collect(java.util.stream.Collectors.groupingBy(Outposts.Piece::role, java.util.TreeMap::new, java.util.stream.Collectors.counting()));
        c.getSource().sendSuccess(() -> Component.literal(def.name() + ": " + miss.size() + " pieces to repair " + by), false);
        return miss.size();
    }

    private static int age(CommandContext<CommandSourceStack> c) {
        OutpostDef def = OutpostDef.get(StringArgumentType.getString(c, "id"));
        double years = DoubleArgumentType.getDouble(c, "years");
        BlockPos o = BlockPos.containing(c.getSource().getPosition());
        int lost = Outposts.age(c.getSource().getLevel(), o, def, years, o.asLong());
        c.getSource().sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT, "Aged %s %.1f years: %d pieces lost.", def.name(), years, lost)), true);
        return lost;
    }
}

package com.yearofice.world;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.SliceCut;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.WaterStage;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import com.yearofice.worldgen.structure.PlacedMask;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/**
 * The world side of play in a {@code yearofice:valley} level (Minecraft engine › Vanilla switched off, No
 * monsters; Minecraft development › World creation options; Layer 9: World edits):
 * <ul>
 *   <li>the spawn is the outpost's door (both cuts), with {@code spawnRadius} 0;</li>
 *   <li>the slice valley's world border is its 1,024-block cut; the 8 km valley gets the 8,192-block development
 *       border in dev builds only;</li>
 *   <li>the world-owned game rules are re-applied on every start ({@code randomTickSpeed} 0, no mob spawning,
 *       raids, patrols, traders, fire spread or griefing). Time and weather rules wait for the clock (lane B);</li>
 *   <li>a catch-all removes any vanilla mob that joins one of our levels, and no portal lights there;</li>
 *   <li>blocks a player places set their {@code placed_mask} bit, and breaking clears it.</li>
 * </ul>
 * Levels with any other generator (the GameTest server's flat overworld, other mods' dimensions) are untouched.
 */
public final class WorldHooks {
    private WorldHooks() {}

    public static void init() {
        NeoForge.EVENT_BUS.addListener(WorldHooks::spawn);
        NeoForge.EVENT_BUS.addListener(WorldHooks::started);
        NeoForge.EVENT_BUS.addListener(WorldHooks::join);
        NeoForge.EVENT_BUS.addListener(WorldHooks::portal);
        NeoForge.EVENT_BUS.addListener(WorldHooks::placed);
        NeoForge.EVENT_BUS.addListener(WorldHooks::broken);
        NeoForge.EVENT_BUS.register(GroundYield.class);
        FishSpotService.install(new Spots());
    }

    /** The bound layers of a level whose generator is ours, or null. */
    public static ValleyWorld world(LevelAccessor level) {
        if (level instanceof ServerLevel sl && sl.getChunkSource().getGenerator() instanceof YoiChunkGenerator g) return g.world();
        return null;
    }

    public static YoiChunkGenerator generator(Level level) {
        return level instanceof ServerLevel sl && sl.getChunkSource().getGenerator() instanceof YoiChunkGenerator g ? g : null;
    }

    static void spawn(LevelEvent.CreateSpawnPosition e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        YoiChunkGenerator g = generator(level);
        if (g == null) return;
        ValleyWorld w = g.bind(level.getSeed());
        int[] s = w.spawn();
        level.setDefaultSpawnPos(new BlockPos(s[0], s[1], s[2]), s[3]);
        e.setCanceled(true);
    }

    static void started(ServerStartedEvent e) {
        MinecraftServer server = e.getServer();
        ServerLevel over = server.overworld();
        YoiChunkGenerator g = generator(over);
        if (g == null) return;
        ValleyWorld w = g.bind(over.getSeed());
        GameRules r = server.getGameRules();
        r.getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        r.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(false, server);
        r.getRule(GameRules.RULE_DOINSOMNIA).set(false, server);
        r.getRule(GameRules.RULE_DISABLE_RAIDS).set(true, server);
        r.getRule(GameRules.RULE_DOFIRETICK).set(false, server);
        r.getRule(GameRules.RULE_MOBGRIEFING).set(false, server);
        r.getRule(GameRules.RULE_SPAWN_RADIUS).set(0, server);
        r.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, server);
        r.getRule(GameRules.RULE_MAX_ENTITY_CRAMMING).set(0, server);
        r.getRule(GameRules.RULE_DOMOBLOOT).set(false, server);
        if (g.settings().slice()) {
            SliceCut.Window c = w.slice;
            over.getWorldBorder().setCenter(c.centreX(), c.centreZ());
            over.getWorldBorder().setSize(c.size());
        } else if (!FMLEnvironment.production || Boolean.getBoolean("yearofice.dev")) {
            over.getWorldBorder().setCenter(0, 0);
            over.getWorldBorder().setSize(com.yearofice.worldgen.regions.Regions.borderSize(com.yearofice.worldgen.regions.Regions.defaultRing()));
        }
        YearOfIce.LOG.info("Year of Ice world: {} cut, spawn {}, border {} at {}, {}", g.settings().cut(), java.util.Arrays.toString(w.spawn()),
                over.getWorldBorder().getSize(), over.getWorldBorder().getCenterX(), over.getWorldBorder().getCenterZ());
    }

    static void join(EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof Mob)) return;
        if (generator(e.getLevel()) == null) return;
        if (BuiltInRegistries.ENTITY_TYPE.getKey(e.getEntity().getType()).getNamespace().equals("minecraft")) e.setCanceled(true);
    }

    static void portal(BlockEvent.PortalSpawnEvent e) {
        if (e.getLevel() instanceof Level l && generator(l) != null) e.setCanceled(true);
    }

    static void placed(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof ServerLevel l && generator(l) != null) PlacedMask.get(l).mark(e.getPos());
    }

    static void broken(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof ServerLevel l && generator(l) != null) PlacedMask.get(l).unmark(e.getPos());
    }

    /** {@link FishSpotService} over the bound layers. */
    static final class Spots implements FishSpotService {
        @Override
        public List<Spot> spotsNear(ServerLevel level, BlockPos pos, double radius) {
            ValleyWorld w = world(level);
            if (w == null) return List.of();
            List<Spot> out = new ArrayList<>();
            for (WaterStage.Spot s : w.spotsNear(pos.getX() + 0.5, pos.getZ() + 0.5, radius)) out.add(of(w, s));
            return out;
        }

        @Override
        public Optional<Spot> sliceSpot(ServerLevel level) {
            ValleyWorld w = world(level);
            return w == null || w.sliceSpot == null ? Optional.empty() : Optional.of(of(w, w.sliceSpot));
        }

        static Spot of(ValleyWorld w, WaterStage.Spot s) {
            int x = (int) Math.floor(s.x()), z = (int) Math.floor(s.z());
            ValleyWorld.Columns c = w.columns(x, z, 1, 1);
            List<Integer> stock = new ArrayList<>();
            for (int v : s.stock()) stock.add(v);
            return new Spot(s.type(), new BlockPos(x, c.top(0), z), s.radiusM(), s.q(), s.depthM(), s.bodyId(), s.species(),
                    List.copyOf(stock), s.major());
        }
    }
}

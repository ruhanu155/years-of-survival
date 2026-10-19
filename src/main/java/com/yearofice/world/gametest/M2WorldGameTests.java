package com.yearofice.world.gametest;

import com.yearofice.api.world.FishSpotService;
import com.yearofice.core.YearOfIce;
import com.yearofice.worldgen.CoalSeams;
import com.yearofice.worldgen.SetPieces;
import com.yearofice.worldgen.TreePlan;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.block.WorldBlocks;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import com.yearofice.worldgen.structure.PlacedMask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * M2's world GameTests (batch {@code m2}; Minecraft development › GameTests and checks for the world). The slice
 * valley runs in {@code yearofice:slice_test}, a dimension the built-in test pack adds to the GameTest server's flat
 * world: the {@code yearofice:valley} generator on the slice cut and layer seed 3141592, so every test sees the
 * same layers JUnit does. Lives outside {@code worldgen} because it reads the clock for the chunk budget.
 */
@GameTestHolder(YearOfIce.MOD_ID)
@PrefixGameTestTemplate(false)
public final class M2WorldGameTests {
    private M2WorldGameTests() {}

    public static final ResourceKey<net.minecraft.world.level.Level> SLICE =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "slice_test"));

    static ServerLevel slice(GameTestHelper h) {
        ServerLevel l = h.getLevel().getServer().getLevel(SLICE);
        if (l == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no yearofice:slice_test dimension (test pack not loaded)");
        return l;
    }

    static ValleyWorld world(ServerLevel l) {
        YoiChunkGenerator g = (YoiChunkGenerator) l.getChunkSource().getGenerator();
        return g.bind(l.getSeed());
    }

    // ------------------------------------------------------------------ the slice valley

    /**
     * The slice preset loads: both world presets exist with our generator; the slice's spawn stands on the outpost
     * pad (inside its footprint, on solid ground, with room to stand), the outpost's template blocks are there
     * with their placed-mask bits (the structure path, not a direct stamp), water lies within 64 blocks of the
     * spawn, and the slice's fishing spot is a pool at least 2 × 2 blocks and 2 blocks deep.
     */
    @GameTest(template = "gametest/empty3", batch = "m2", timeoutTicks = 2400)
    public static void worldgen_slice_valley(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        var presets = server.registryAccess().registryOrThrow(Registries.WORLD_PRESET);
        for (String p : List.of("year_of_ice", "slice_valley")) {
            WorldPreset wp = presets.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, p));
            h.assertTrue(wp != null, "world preset yearofice:" + p + " missing");
            var over = wp.createWorldDimensions().dimensions().get(net.minecraft.world.level.dimension.LevelStem.OVERWORLD);
            h.assertTrue(over != null && over.generator() instanceof YoiChunkGenerator g
                    && g.settings().slice() == p.equals("slice_valley"), "preset " + p + " is not our generator on its cut");
        }
        ServerLevel l = slice(h);
        ValleyWorld w = world(l);
        int[] s = w.spawn();
        SetPieces.Placement o = w.outpost();
        h.assertTrue(o != null, "no outpost placed");
        int[] r = ValleyWorld.rect(o);
        h.assertTrue(s[0] >= r[0] && s[0] < r[0] + r[2] && s[2] >= r[1] && s[2] < r[1] + r[3],
                "spawn " + Arrays.toString(s) + " is not on the outpost footprint " + Arrays.toString(r));
        h.assertTrue(w.slice.contains(s[0], s[2]), "spawn outside the slice cut " + w.slice);
        for (int cz = (r[1] >> 4) - 1; cz <= ((r[1] + r[3]) >> 4) + 1; cz++)
            for (int cx = (r[0] >> 4) - 1; cx <= ((r[0] + r[2]) >> 4) + 1; cx++) l.getChunk(cx, cz);
        BlockPos sp = new BlockPos(s[0], s[1], s[2]);
        h.assertTrue(l.getBlockState(sp.below()).isSolid(), "spawn stands on " + l.getBlockState(sp.below()));
        h.assertTrue(!l.getBlockState(sp).isSolid() && !l.getBlockState(sp.above()).isSolid(), "no room to stand at the spawn " + sp);
        PlacedMask mask = PlacedMask.get(l);
        if (mask.count(new BlockPos(r[0], s[1], r[1]), new BlockPos(r[0] + r[2] - 1, s[1] + 6, r[1] + r[3] - 1)) == 0) {
            // The GameTest world turns structure generation off: build the outpost's start through the same
            // registrations (structure, placement data, piece, processor list) and place it chunk by chunk.
            placeSetPiece(l, w, o);
        }
        int marked = mask.count(new BlockPos(r[0], s[1], r[1]), new BlockPos(r[0] + r[2] - 1, s[1] + 6, r[1] + r[3] - 1));
        h.assertTrue(marked >= 40, "outpost template: only " + marked + " placed-mask bits on its footprint");

        // Water within 64 blocks of the spawn.
        int best = Integer.MAX_VALUE;
        for (int cz = (s[2] - 64) >> 4; cz <= (s[2] + 64) >> 4; cz++) {
            for (int cx = (s[0] - 64) >> 4; cx <= (s[0] + 64) >> 4; cx++) {
                LevelChunk c = l.getChunk(cx, cz);
                for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    int wx = cx * 16 + x, wz = cz * 16 + z;
                    int top = c.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                    if (c.getFluidState(wx, top, wz).is(Fluids.WATER)) {
                        int d = Math.max(Math.abs(wx - s[0]), Math.abs(wz - s[2]));
                        best = Math.min(best, d);
                    }
                }
            }
        }
        h.assertTrue(best <= 64, "nearest water is " + best + " blocks from the spawn");

        // The fishing spot: 2 × 2 columns at least 2 blocks deep, through the service Food reads.
        var spot = FishSpotService.get().sliceSpot(l);
        h.assertTrue(spot.isPresent(), "the slice has no fishing spot");
        BlockPos c0 = spot.get().centre();
        int deep = 0;
        for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
            BlockPos top = new BlockPos(c0.getX() + dx, c0.getY(), c0.getZ() + dz);
            l.getChunk(top.getX() >> 4, top.getZ() >> 4);
            int y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, top.getX(), top.getZ()) - 1;
            if (l.getFluidState(new BlockPos(top.getX(), y, top.getZ())).isSource() && l.getFluidState(new BlockPos(top.getX(), y - 1, top.getZ())).isSource()) deep++;
        }
        h.assertTrue(deep >= 4, "fishing spot at " + c0.toShortString() + ": only " + deep + " columns 2 blocks deep");
        h.assertTrue(!FishSpotService.get().spotsNear(l, c0, 8).isEmpty(), "FishSpotService finds no spot at the slice spot");
        h.succeed();
    }

    // ------------------------------------------------------------------ water

    /**
     * {@code worldgen/no_spill}: around the slice's creek and fishing spot, every water block is made to tick and the
     * area runs 200 ticks. No water block may appear or vanish outside a falls zone, and no water block may stand above
     * its column's stage 4 surface (the column builder's water top). 7 × 7 chunks live here; JUnit checks the
     * same rule statically over a 512 × 512 river area of every fixture seed.
     */
    @GameTest(template = "gametest/empty3", batch = "m2", timeoutTicks = 1200)
    public static void worldgen_no_spill(GameTestHelper h) {
        ServerLevel l = slice(h);
        ValleyWorld w = world(l);
        int[] s = w.spawn();
        int[] c = w.nearestWater(s[0], s[2], 300, com.yearofice.worldgen.WaterStage.CREEK);
        int cx0 = (c == null ? s[0] : c[0]) >> 4, cz0 = (c == null ? s[2] : c[1]) >> 4;
        Map<BlockPos, BlockState> before = new HashMap<>();
        List<BlockPos> water = new ArrayList<>();
        ValleyWorld.Columns col = w.columns((cx0 - 3) * 16, (cz0 - 3) * 16, 112, 112);
        for (int cz = cz0 - 3; cz <= cz0 + 3; cz++) for (int cx = cx0 - 3; cx <= cx0 + 3; cx++) l.getChunk(cx, cz);
        int flowCols = 0;
        for (int i = 0; i < 112 * 112; i++) {
            int x = col.x0 + i % 112, z = col.z0 + i / 112;
            if (col.has(i, ValleyWorld.F_FLOW)) { flowCols++; continue; }
            for (int y = col.ground[i] - 1; y <= col.top(i) + 2; y++) {
                BlockPos p = new BlockPos(x, y, z);
                BlockState st = l.getBlockState(p);
                before.put(p, st);
                if (st.getFluidState().is(Fluids.WATER)) {
                    water.add(p);
                    h.assertTrue(col.kind[i] == ValleyWorld.WATER && y <= col.water[i],
                            "water at " + p.toShortString() + " above the column's surface (" + col.water[i] + ")");
                }
            }
        }
        h.assertTrue(water.size() > 200, "only " + water.size() + " water blocks in the test area");
        for (BlockPos p : water) l.scheduleTick(p, Fluids.WATER, 1);
        final int flows = flowCols;
        h.runAfterDelay(200, () -> {
            int changed = 0;
            BlockPos first = null;
            for (Map.Entry<BlockPos, BlockState> e : before.entrySet()) {
                BlockState now = l.getBlockState(e.getKey());
                // Only water moving counts: other tests in the batch may stamp set pieces nearby meanwhile.
                if (now.getFluidState() != e.getValue().getFluidState()) {
                    if (nearFlow(col, e.getKey())) continue;
                    changed++;
                    if (first == null) first = e.getKey();
                }
            }
            h.assertTrue(changed == 0, changed + " blocks changed after 200 ticks of forced fluid ticks, first at "
                    + (first == null ? "-" : first.toShortString() + " now " + l.getBlockState(first)) + " (" + flows + " falls columns exempt)");
            h.succeed();
        });
    }

    static boolean nearFlow(ValleyWorld.Columns c, BlockPos p) {
        for (int dz = -8; dz <= 8; dz++) for (int dx = -8; dx <= 8; dx++) {
            int x = p.getX() + dx, z = p.getZ() + dz;
            if (x < c.x0 || z < c.z0 || x >= c.x0 + c.w || z >= c.z0 + c.d) continue;
            if (c.has(c.at(x, z), ValleyWorld.F_FLOW)) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ explore, don't dig

    /**
     * {@code worldgen/no_nodes_below_surface}: on generated chunks around every iron cluster and coal seam of the
     * slice seed (and the start), no resource block ({@code iron_outcrop}, {@code coal_seam}) has deep ground or deep
     * rock above it, every resource block and every {@code face_rock} touches air or water, and no node sits behind
     * the shell. Chunks are built as bare proto-chunks with their neighbours, so the check reads real blocks.
     */
    @GameTest(template = "gametest/empty3", batch = "m2", timeoutTicks = 1200)
    public static void worldgen_no_nodes_below_surface(GameTestHelper h) {
        ServerLevel l = slice(h);
        YoiChunkGenerator gen = (YoiChunkGenerator) l.getChunkSource().getGenerator();
        ValleyWorld w = world(l);
        List<int[]> at = new ArrayList<>();
        for (CoalSeams.Seam s : w.seams) at.add(new int[] {(int) s.x(), (int) s.z()});
        for (int[] p : w.ironNodes()) at.add(p);
        int[] s0 = w.spawn();
        at.add(new int[] {s0[0], s0[2]});
        Proto proto = new Proto(l, gen);
        int nodes = 0, faces = 0;
        Block iron = WorldBlocks.IRON_OUTCROP.get(), coal = WorldBlocks.COAL_SEAM.get(), face = WorldBlocks.FACE_ROCK.get();
        Block deepG = WorldBlocks.DEEP_GROUND.get(), deepR = WorldBlocks.DEEP_ROCK.get();
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (int[] p : at) {
            int cx = p[0] >> 4, cz = p[1] >> 4;
            if (!done.add(ChunkPos.asLong(cx, cz))) continue;
            proto.get(cx, cz);
            ValleyWorld.Columns cc = w.columns(cx * 16, cz * 16, 16, 16);
            int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
            for (int i = 0; i < 256; i++) { lo = Math.min(lo, cc.ground[i]); hi = Math.max(hi, cc.top(i)); }
            for (int y = lo - 40; y <= hi + 2; y++) {
                for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
                    int wx = cx * 16 + x, wz = cz * 16 + z;
                    Block b = proto.state(wx, y, wz).getBlock();
                    boolean node = b == iron || b == coal;
                    if (!node && b != face) continue;
                    if (node) nodes++; else faces++;
                    boolean open = false;
                    for (Direction d : Direction.values()) {
                        if (d == Direction.DOWN) continue;
                        BlockState n = proto.state(wx + d.getStepX(), y + d.getStepY(), wz + d.getStepZ());
                        if (n.isAir() || n.getFluidState().is(Fluids.WATER) || !n.isSolid()) { open = true; break; }
                    }
                    h.assertTrue(open, b + " at " + wx + " " + y + " " + wz + " touches neither air nor water (behind the shell)");
                    if (node) {
                        for (int yy = y + 1; yy < y + 40; yy++) {
                            Block above = proto.state(wx, yy, wz).getBlock();
                            h.assertTrue(above != deepG && above != deepR, b + " at " + wx + " " + y + " " + wz + " has " + above + " above it");
                        }
                    }
                }
            }
        }
        h.assertTrue(nodes > 0, "no iron or coal blocks found near " + at.size() + " node sites");
        h.assertTrue(faces > 0, "no face rock found");
        h.succeed();
    }

    /**
     * {@code worldgen/coal_seams} on the slice seed: 3–6 seams, at least one 1.5–3 km from the start, none within
     * 600 m, and each seam's blocks exist in the shell of its chunk. JUnit checks the counts on every fixture seed.
     */
    @GameTest(template = "gametest/empty3", batch = "m2", timeoutTicks = 1200)
    public static void worldgen_coal_seams(GameTestHelper h) {
        ServerLevel l = slice(h);
        YoiChunkGenerator gen = (YoiChunkGenerator) l.getChunkSource().getGenerator();
        ValleyWorld w = world(l);
        var cp = w.p.terrain().coal();
        h.assertTrue(w.seams.size() >= 3 && w.seams.size() <= 6, w.seams.size() + " seams");
        boolean ring = false;
        Proto proto = new Proto(l, gen);
        for (CoalSeams.Seam s : w.seams) {
            h.assertTrue(s.fromStartM() >= cp.minStartM(), "seam " + s.fromStartM() + " m from the start");
            if (cp.ringM().contains(s.fromStartM())) ring = true;
            int found = 0;
            for (int dz = -12; dz <= 12; dz++) for (int dx = -12; dx <= 12; dx++) {
                int x = (int) s.x() + dx, z = (int) s.z() + dz;
                proto.get(x >> 4, z >> 4);
                for (int y = s.bandY() - 6; y <= s.bandY() + 8; y++) if (proto.state(x, y, z).is(WorldBlocks.COAL_SEAM.get())) found++;
            }
            h.assertTrue(found > 0, "no coal_seam blocks at the seam " + s.x() + " " + s.z());
        }
        h.assertTrue(ring, "no seam 1.5–3 km from the start");
        h.succeed();
    }

    // ------------------------------------------------------------------ budget

    /**
     * {@code worldgen/chunk_budget}: 2,000 chunks of the core (columns, blocks and the tree plan) generate at an
     * average under 5 ms and a 99th percentile under 15 ms. Timed on bare proto-chunks, one thread, after a
     * warm-up; the whole chunk pipeline in the live dimension (light, structures, trees written) is logged too.
     */
    @GameTest(template = "gametest/empty3", batch = "m2", timeoutTicks = 2400)
    public static void worldgen_chunk_budget(GameTestHelper h) {
        ServerLevel l = slice(h);
        YoiChunkGenerator gen = (YoiChunkGenerator) l.getChunkSource().getGenerator();
        ValleyWorld w = world(l);
        var biomes = l.registryAccess().registryOrThrow(Registries.BIOME);
        int x0 = (int) w.g.originX >> 4, z0 = (int) w.g.originZ >> 4;
        int per = (int) (w.g.size / 16);
        long[] ns = new long[2000];
        for (int k = -200; k < ns.length; k++) {
            int idx = Math.floorMod(k * 7919, per * per);
            int cx = x0 + idx % per, cz = z0 + idx / per;
            ProtoChunk pc = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, l, biomes, null);
            long t0 = System.nanoTime();
            gen.fill(pc);
            TreePlan.treesIn(w, cx * 16, cz * 16, cx * 16 + 16, cz * 16 + 16);
            long t = System.nanoTime() - t0;
            if (k >= 0) ns[k] = t;
        }
        long[] sorted = ns.clone();
        Arrays.sort(sorted);
        double avg = Arrays.stream(ns).average().orElse(0) / 1e6, p99 = sorted[(int) (sorted.length * 0.99)] / 1e6;
        // The live pipeline: 64 fresh chunks to FULL in the dimension.
        int[] s = w.spawn();
        long t1 = System.nanoTime();
        int live = 0;
        for (int dz = 0; dz < 8; dz++) for (int dx = 0; dx < 8; dx++) { l.getChunk((s[0] >> 4) + 20 + dx, (s[2] >> 4) + 20 + dz); live++; }
        double liveMs = (System.nanoTime() - t1) / 1e6 / live;
        String msg = String.format(Locale.ROOT, "chunk budget: columns+blocks+trees avg %.2f ms, p99 %.2f ms over %d core chunks; live pipeline %.1f ms a chunk (%d chunks, main thread)",
                avg, p99, ns.length, liveMs, live);
        YearOfIce.LOG.info(msg);
        h.assertTrue(avg < 5.0 && p99 < 15.0, msg);
        h.succeed();
    }

    // ------------------------------------------------------------------ vanilla off

    /**
     * {@code world/no_random_ticks}: no block state of ours ticks at random, and with {@code randomTickSpeed} forced to
     * 3 for 1,000 ticks a row of every world block (default state) changes none of them.
     */
    @GameTest(template = "gametest/setpiece_pad", batch = "m2", timeoutTicks = 1300)
    public static void world_no_random_ticks(GameTestHelper h) {
        for (Block b : BuiltInRegistries.BLOCK) {
            if (!BuiltInRegistries.BLOCK.getKey(b).getNamespace().equals(YearOfIce.MOD_ID)) continue;
            for (BlockState s : b.getStateDefinition().getPossibleStates()) {
                h.assertTrue(!s.isRandomlyTicking(), s + " ticks at random");
            }
        }
        MinecraftServer server = h.getLevel().getServer();
        GameRules.IntegerValue rule = server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING);
        int old = rule.get();
        rule.set(3, server);
        List<BlockPos> at = new ArrayList<>();
        List<BlockState> st = new ArrayList<>();
        int i = 0;
        for (var e : WorldBlocks.byName().entrySet()) {
            BlockPos p = new BlockPos(2 + (i % 16) * 2, 2, 2 + (i / 16) * 2);
            h.setBlock(p.below(), Blocks.STONE);
            h.setBlock(p, e.getValue().defaultBlockState());
            at.add(p);
            st.add(h.getBlockState(p));
            i++;
        }
        h.runAfterDelay(1000, () -> {
            rule.set(old, server);
            for (int k = 0; k < at.size(); k++) {
                BlockState now = h.getBlockState(at.get(k));
                h.assertTrue(now == st.get(k), at.get(k).toShortString() + " changed from " + st.get(k) + " to " + now);
            }
            h.succeed();
        });
    }

    /**
     * {@code ecosystem/no_monsters_full_night}: every biome of ours has no spawners in any category, and across the
     * slice (a 64 m grid of surface points through the cut, every mob category, after structure overrides and
     * NeoForge's potential-spawns event, exactly what natural spawning would draw from) the spawn list is empty, so no
     * mob can spawn on any night, however long. A zombie forced into the dimension is turned away by the catch-all.
     * The control: the flat overworld's plains do list monsters, so the check can fail. (The doc's 17-hour night
     * with a player present is the same check run in time; spawning draws only from these lists.)
     */
    @GameTest(template = "gametest/empty3", batch = "m2", timeoutTicks = 400)
    public static void no_monsters_full_night(GameTestHelper h) {
        MinecraftServer server = h.getLevel().getServer();
        var reg = server.registryAccess().registryOrThrow(Registries.BIOME);
        HolderSet.Named<Biome> ours = reg.getTag(TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "valley_biomes")))
                .orElseThrow();
        h.assertTrue(ours.size() == 20, ours.size() + " biomes in yearofice:valley_biomes");
        for (Holder<Biome> b : ours) {
            for (MobCategory c : MobCategory.values()) {
                h.assertTrue(b.value().getMobSettings().getMobs(c).isEmpty(), b.getRegisteredName() + " spawns " + c);
            }
        }
        ServerLevel l = slice(h);
        ValleyWorld w = world(l);
        int points = 0;
        for (int z = w.slice.minZ() + 32; z < w.slice.minZ() + w.slice.size(); z += 64) {
            for (int x = w.slice.minX() + 32; x < w.slice.minX() + w.slice.size(); x += 64) {
                ValleyWorld.Columns c = w.columns(x, z, 1, 1);
                BlockPos p = new BlockPos(x, c.top(0) + 1, z);
                Holder<Biome> biome = l.getChunkSource().getGenerator().getBiomeSource().getNoiseBiome(x >> 2, p.getY() >> 2, z >> 2,
                        l.getChunkSource().randomState().sampler());
                for (MobCategory cat : MobCategory.values()) {
                    var list = l.getChunkSource().getGenerator().getMobsAt(biome, l.structureManager(), cat, p);
                    list = net.neoforged.neoforge.event.EventHooks.getPotentialSpawns(l, cat, p, list);
                    h.assertTrue(list.isEmpty(), cat + " spawns listed at " + p.toShortString() + " (" + biome.getRegisteredName() + ")");
                }
                points++;
            }
        }
        h.assertTrue(points >= 200, "only " + points + " points checked");
        BlockPos at = new BlockPos(w.spawn()[0], w.spawn()[1], w.spawn()[2]);
        l.getChunk(at);
        var zombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(l);
        zombie.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        boolean added = l.addFreshEntity(zombie);
        h.assertTrue(!added && !zombie.isAddedToLevel(), "a vanilla zombie joined the slice valley");
        // Control: the GameTest server's flat overworld (plains) lists monsters.
        ServerLevel over = server.overworld();
        BlockPos cp = h.absolutePos(BlockPos.ZERO);
        Holder<Biome> plains = over.getBiome(cp);
        var control = over.getChunkSource().getGenerator().getMobsAt(plains, over.structureManager(), MobCategory.MONSTER, cp);
        h.assertTrue(!control.isEmpty(), "the control biome lists no monsters: the check cannot fail");
        h.succeed();
    }

    // ------------------------------------------------------------------ helpers

    /** Generates one set piece's structure start from the registry and places it in every chunk it touches. */
    static void placeSetPiece(ServerLevel l, ValleyWorld w, SetPieces.Placement q) {
        var structures = l.registryAccess().registryOrThrow(Registries.STRUCTURE);
        var st = structures.get(ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "setpiece/" + q.id().substring(q.id().indexOf(':') + 1)));
        if (st == null) throw new net.minecraft.gametest.framework.GameTestAssertException("no structure for " + q.id());
        int[] r = ValleyWorld.rect(q);
        ChunkPos start = new ChunkPos(r[0] >> 4, r[1] >> 4);
        var gen = l.getChunkSource().getGenerator();
        var ss = st.generate(l.registryAccess(), gen, gen.getBiomeSource(), l.getChunkSource().randomState(), l.getStructureManager(),
                l.getSeed(), start, 0, l, b -> true);
        if (!ss.isValid()) throw new net.minecraft.gametest.framework.GameTestAssertException("structure " + q.id() + " found no start at " + start);
        var box = ss.getBoundingBox();
        for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            l.getChunk(cx, cz);
            ss.placeInChunk(l, l.structureManager(), gen, l.getRandom(),
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(cx * 16, l.getMinBuildHeight(), cz * 16, cx * 16 + 15, l.getMaxBuildHeight() - 1, cz * 16 + 15),
                    new ChunkPos(cx, cz));
        }
    }

    /** Bare proto-chunks built by our generator, with block lookups across their borders. */
    static final class Proto {
        final ServerLevel l;
        final YoiChunkGenerator gen;
        final Map<Long, ProtoChunk> chunks = new HashMap<>();

        Proto(ServerLevel l, YoiChunkGenerator gen) {
            this.l = l;
            this.gen = gen;
        }

        ProtoChunk get(int cx, int cz) {
            return chunks.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> {
                ProtoChunk pc = new ProtoChunk(new ChunkPos(cx, cz), UpgradeData.EMPTY, l, l.registryAccess().registryOrThrow(Registries.BIOME), null);
                gen.fill(pc);
                return pc;
            });
        }

        BlockState state(int x, int y, int z) { return get(x >> 4, z >> 4).getBlockState(new BlockPos(x, y, z)); }
    }
}

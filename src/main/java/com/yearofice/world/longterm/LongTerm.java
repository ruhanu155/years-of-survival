package com.yearofice.world.longterm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.world.ChangeOp;
import com.yearofice.api.world.WorldEditService;
import com.yearofice.sim.world.BeaverDam;
import com.yearofice.sim.world.LongWorldTuning;
import com.yearofice.sim.world.OldCamp;
import com.yearofice.sim.world.Regrowth;
import com.yearofice.sim.world.Wildfire;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The long world's Minecraft side (M9 lane C): burns, regrowth, beaver ponds and old camps, all as permanent edits
 * through the terrain queue ({@link WorldEditService}), so a chunk nobody visits catches up when it loads. The numbers
 * are in {@code sim.world}. Regrowth is future-dated ops written once when the ground is cleared; nothing ticks.
 */
public final class LongTerm {
    private LongTerm() {}

    /** Most burn ops one fire writes; a crown fire's disc is cut to this, nearest cells first. */
    public static final int MAX_BURN_OPS = 6000;
    public static final int MAX_POND_CELLS = 400;

    public static LongWorldTuning tuning() { return LongWorldConfig.tuning(); }

    private static Map<String, String> stageBlocks;

    static synchronized Map<String, String> stageBlocks() {
        if (stageBlocks == null) {
            Map<String, String> m = new LinkedHashMap<>();
            try (var in = LongTerm.class.getResourceAsStream("/data/yearofice/regrowth/stages.json")) {
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("stages");
                o.entrySet().forEach(e -> m.put(e.getKey(), e.getValue().getAsString()));
            } catch (java.io.IOException | RuntimeException ex) {
                throw new IllegalStateException("regrowth/stages.json", ex);
            }
            stageBlocks = m;
        }
        return stageBlocks;
    }

    private static int seq = 0;

    private static void submit(ServerLevel level, long day, ChangeOp.Kind kind, int prio, BlockPos p, String block, boolean airOnly) {
        WorldEditService.get().submit(level, new ChangeOp(day, kind, prio, p.immutable(), block, airOnly, seq++ & 0xFFFFFF));
    }

    // ---- burns and regrowth -------------------------------------------------------------------------------------

    /**
     * Burns a disc of {@code radius} blocks around {@code centre} on day {@code today}: leaves go, logs go bar a
     * charred stump, and regrowth is scheduled for every burned column. Returns the ops queued.
     */
    public static int burn(ServerLevel level, BlockPos centre, int radius, long today) {
        int ops = 0;
        long seed = level.getSeed();
        for (int r = 0; r <= radius && ops < MAX_BURN_OPS; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r || dx * dx + dz * dz > radius * radius) continue;
                    if (ops >= MAX_BURN_OPS) return ops;
                    int x = centre.getX() + dx, z = centre.getZ() + dz;
                    if (!level.hasChunkAt(new BlockPos(x, 0, z))) continue;
                    int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                    boolean burned = false;
                    for (int y = top; y > top - 32 && y > level.getMinBuildHeight(); y--) {
                        BlockPos p = new BlockPos(x, y, z);
                        BlockState s = level.getBlockState(p);
                        if (s.is(BlockTags.LEAVES)) { submit(level, today, ChangeOp.Kind.BURN, 0, p, "minecraft:air", false); ops++; burned = true; }
                        else if (s.is(BlockTags.LOGS)) {
                            boolean base = !level.getBlockState(p.below()).is(BlockTags.LOGS);
                            submit(level, today, ChangeOp.Kind.BURN, 1, p, base ? "yearofice:stump" : "minecraft:air", false);
                            ops++; burned = true;
                        }
                    }
                    if (burned || Math.floorMod(x * 31 + z, 3) != 0) ops += scheduleRegrowth(level, seed, x, z, groundY(level, x, z), today);
                }
            }
        }
        return ops;
    }

    /** The first free block above the ground of a column, looking through leaves, logs and air (a tree stands on it). */
    public static int groundY(ServerLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(x, y, z);
        while (m.getY() > level.getMinBuildHeight()) {
            BlockState s = level.getBlockState(m);
            if (!(s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced())) break;
            m.move(0, -1, 0);
        }
        return m.getY() + 1;
    }

    /** Future-dated REGROW ops for one cleared column, one per stage it takes (a hash roll at the stage's density). */
    public static int scheduleRegrowth(ServerLevel level, long seed, int x, int z, int groundY, long clearedDay) {
        int n = 0;
        double scale = tuning().regrowthTimeScale();
        for (Regrowth.Stage s : Regrowth.Stage.values()) {
            if (s == Regrowth.Stage.CLEARED || !Regrowth.cellGrows(seed, x, z, s)) continue;
            String block = stageBlocks().get(s.name().toLowerCase(java.util.Locale.ROOT));
            if (block == null) continue;
            long day = Regrowth.stageDay(clearedDay, s, scale);
            submit(level, day, ChangeOp.Kind.REGROW, s.ordinal(), new BlockPos(x, groundY, z), block, true);
            n++;
        }
        return n;
    }

    // ---- wildfire ------------------------------------------------------------------------------------------------

    /** The class of wildfire a strike on {@code seed} escalates to today, or null when it fizzles. */
    public static Wildfire.FireClass escalation(ServerLevel level, int seed, long today) {
        LongWorldTuning t = tuning();
        int year = com.yearofice.sim.time.Calendar.yearOfDay(today);
        boolean wfYear = Wildfire.wildfireYear(level.getSeed(), year, t);
        Wildfire.Danger danger = switch (com.yearofice.fire.FireDangerIndex.level(level)) {
            case LOW -> Wildfire.Danger.LOW; case MODERATE -> Wildfire.Danger.MODERATE;
            case HIGH -> Wildfire.Danger.HIGH; case EXTREME -> Wildfire.Danger.EXTREME;
        };
        SplittableRandom rng = new SplittableRandom(level.getSeed() ^ ((long) seed << 20) ^ today);
        return Wildfire.escalates(wfYear, danger, rng, t) ? Wildfire.classFor(danger, 0) : null;
    }

    /** A struck tree: may escalate to a wildfire by the year and the danger. Returns the ops queued (0 when it fizzles). */
    public static int treeStruck(ServerLevel level, BlockPos top, int seed) {
        long today = ClockService.get().absDay();
        Wildfire.FireClass c = escalation(level, seed, today);
        return c == null ? 0 : wildfire(level, top, c, today);
    }

    /** Burns a class of fire: cells of 8 m, cut to the op cap. */
    public static int wildfire(ServerLevel level, BlockPos at, Wildfire.FireClass c, long today) {
        int radius = c.cells == 0 ? 2 : (int) Math.min(40, Math.sqrt(c.cells * 64.0 / Math.PI));
        return burn(level, at, radius, today);
    }

    // ---- beaver ponds --------------------------------------------------------------------------------------------

    /** Writes the dam at {@code stage} and the pond it holds: logs across x, pond on the +z side. Returns ops queued. */
    public static int dam(ServerLevel level, BlockPos pos, int stage, long today) {
        int ops = 0;
        int half = 3 + stage;
        int rise = BeaverDam.riseBlocks(stage);
        for (int dx = -half; dx <= half; dx++)
            for (int h = 0; h <= Math.max(0, rise); h++) {
                submit(level, today, ChangeOp.Kind.DAM, 0, pos.offset(dx, h, 0), "yearofice:fallen_log", false);
                ops++;
            }
        if (rise <= 0) return ops;
        // The pond: air cells at the water line, flood-filled upstream and bounded by anything not open air.
        int y = pos.getY() + rise - 1;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        BlockPos start = new BlockPos(pos.getX(), y, pos.getZ() + 1);
        q.add(start);
        seen.add(start);
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (seen.size() > MAX_POND_CELLS) return ops;
            for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos n = p.relative(d);
                if (seen.contains(n) || n.getZ() <= pos.getZ()) continue;
                if (!level.getBlockState(n).isAir() && !level.getBlockState(n).canBeReplaced()) continue;
                seen.add(n);
                q.add(n);
            }
        }
        for (BlockPos p : seen) { submit(level, today, ChangeOp.Kind.POND, 0, p, "minecraft:water", true); ops++; }
        return ops;
    }

    /** The yearly growth of every colony: trees in reach (the count the caller measured) set the next stage. */
    public static int growColony(ServerLevel level, LongTermData.Colony c, int trees, long today) {
        int next = BeaverDam.nextStage(c.stage, trees, c.abandoned);
        c.lastGrowDay = today;
        if (next == c.stage) { LongTermData.get(level).touch(); return 0; }
        c.stage = next;
        LongTermData.get(level).touch();
        return dam(level, c.pos, next, today);
    }

    // ---- old camps -----------------------------------------------------------------------------------------------

    private static java.util.List<Object[]> rows;

    static synchronized java.util.List<Object[]> rows() {
        if (rows == null) {
            java.util.List<Object[]> l = new java.util.ArrayList<>();
            try (var in = LongTerm.class.getResourceAsStream("/data/yearofice/longterm/decay.json")) {
                var arr = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("rows");
                arr.forEach(e -> l.add(new Object[] {e.getAsJsonObject().get("match").getAsString(), e.getAsJsonObject().get("rot_pct_per_year").getAsDouble()}));
            } catch (java.io.IOException | RuntimeException ex) {
                throw new IllegalStateException("longterm/decay.json", ex);
            }
            rows = l;
        }
        return rows;
    }

    /**
     * Rot rate (percent a year) of a block: the outposts' block data map {@code yearofice:decay} first (a roofed block
     * rots at the sheltered rate), then this lane's own rows for the homestead pieces the map does not name; 0 when it does not rot.
     */
    public static double rotRate(BlockState s, boolean sheltered) {
        var row = com.yearofice.build.outpost.DecayRow.of(s);
        if (row != null) return sheltered ? row.rotPctSheltered() : row.rotPctExposed();
        return rotRate(BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath());
    }

    /** Rot rate (percent a year) of a block id path from this lane's own rows, 0 when it does not rot. */
    public static double rotRate(String path) {
        for (Object[] r : rows()) if (path.contains((String) r[0])) return (Double) r[1];
        return 0;
    }

    /**
     * Ages the built blocks of one chunk by {@code years} unvisited: each rotting block is lost by the hash of its
     * place, as a DECAY op for today (the chunk's catch-up applies it). Returns ops queued.
     */
    public static int ageChunk(ServerLevel level, net.minecraft.world.level.chunk.LevelChunk chunk, double years, long today) {
        if (years <= 0) return 0;
        ChunkPos cp = chunk.getPos();
        int n = 0;
        double scale = tuning().decayRateScale();
        long seed = level.getSeed();
        int minY = level.getMinBuildHeight();
        for (int x = cp.getMinBlockX(); x <= cp.getMaxBlockX(); x++)
            for (int z = cp.getMinBlockZ(); z <= cp.getMaxBlockZ(); z++) {
                int top = chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                for (int y = Math.max(minY, top - 24); y <= top; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = chunk.getBlockState(p);
                    if (st.isAir()) continue;
                    var row = com.yearofice.build.outpost.DecayRow.of(st);
                    var key = BuiltInRegistries.BLOCK.getKey(st.getBlock());
                    if (row == null && !key.getNamespace().equals("yearofice")) continue;
                    double rate = rotRate(st, y < top - 1);
                    if (rate > 0 && OldCamp.lost(seed, x, y, z, rate * scale, years)) {
                        submit(level, today, ChangeOp.Kind.DECAY, 0, p, row != null ? row.becomes().toString() : "minecraft:air", false);
                        n++;
                    }
                }
            }
        return n;
    }
}

package com.yearofice.client.smoke;

import com.yearofice.api.build.Room;
import com.yearofice.build.craft.StationCraft;
import net.minecraft.resources.ResourceLocation;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.build.outpost.OutpostDef;
import com.yearofice.build.outpost.OutpostSystem;
import com.yearofice.build.outpost.Outposts;
import com.yearofice.build.outpost.RepairJob;
import com.yearofice.build.pieces.PieceJobs;
import com.yearofice.build.pieces.Structs;
import com.yearofice.build.wood.ChoppingBlockEntity;
import com.yearofice.build.wood.FellingService;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.build.wood.WoodJobs;
import com.yearofice.build.wood.WoodRegistry;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkService;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireParts;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.stove.StoveBlockEntity;
import com.yearofice.fire.stove.StoveRegistry;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.time.Rate;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.world.logs.FelledLogEntity;
import com.yearofice.worldgen.block.YoiLogBlock;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The M8 proxy check's player ({@code YEAROFICE_SMOKE=m8cabin}; never active otherwise), standing in for Ruhan's gate
 * "repairs the outpost and lays one wall course without help". A scripted survivor in survival, no debug commands:
 * finds the nearest region-placed outpost, repairs it (RepairJob through WorkService), fells/limbs/bucks/splits real
 * trees with the crate hatchet (WoodJobs), builds a 4x4 log cabin piece by piece through the block-item use path, loads
 * and lights a tin stove with a chimney, jumps the clock (the driver's own hook) to the coldest December night and
 * logs the room heat balance, then saves the cabin and stamps it at a second site. Every step where a real player has
 * no path (recipes, a repair entry point, materials) is logged as BLOCKED/GRANT, never papered over. Log:
 * {@code smoke/m8cabin.log}; the client (BodySmoke) grabs the screenshots named in {@link #shot}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class M8CabinSmoke {
    private M8CabinSmoke() {}

    public static final boolean ON = "m8cabin".equals(System.getenv("YEAROFICE_SMOKE"));
    public static volatile boolean done = false;
    public static volatile String verdict = "";
    /** A screenshot name the client should grab now; the client clears it. */
    public static volatile String shot = null;

    static final int SPLITS_WANTED = Integer.parseInt(System.getenv().getOrDefault("YEAROFICE_M8_SPLITS", "56"));
    static final double WALK_MS = 1.2;

    private enum Ph { INIT, OUTPOST, OUTPOST_WAIT, REPAIR, ROOM_WAIT, TREES, FELL_WAIT, PROCESS, SITE, CABIN, CABIN_WAIT, STAMP, STAMP_WAIT, JUMP, NIGHT, END }

    private static Ph ph = Ph.INIT;
    private static int tick = 0, wait = 0;
    private static PrintWriter out;
    private static final List<String> fails = new ArrayList<>(), blocked = new ArrayList<>();
    private static final Map<String, Double> chores = new LinkedHashMap<>();
    private static final Map<String, Integer> grants = new LinkedHashMap<>();

    private static ServerLevel level;
    private static BlockPos spawn;
    private static int startMatches, grantedSplits;
    private static long grantDay;
    private static ItemStack extraGrant;
    private static int lastMoist;
    private static long extShotAt, prevNow;
    private static boolean extDone, inDone;
    private static int wood_in_pack;
    private static int hatchetSlot = 8, workSlot = 7;
    // outpost
    private static OutpostDef opDef;
    private static BlockPos opOrigin;
    private static int opLayout, opMissing, opRepaired;
    private static BlockPos opInside;
    // wood
    private static List<BlockPos> trees = new ArrayList<>();
    private static int treeIx = 0, splitsMade = 0, roundsMade = 0, treesFelled = 0, subStep = 0;
    private static BlockPos curBase;
    private static TrunkSearch.Trunk curTrunk;
    private static WoodJobs.Fell curFell;
    private static final Set<Integer> usedLogs = new HashSet<>();
    private static FelledLogEntity curLog;
    private static BlockPos chopBlock;
    private static int splitAlt = 0;
    // cabin
    private static BlockPos site, site2;
    private static int cabinStep = 0;
    private static double firstBuildMin = 0, stampMin = 0;
    private static int piecesPlaced = 0, placeFallbacks = 0;
    private static BlockPos stovePos;
    // night
    private static long nightStart, nightEnd;
    private static boolean diag = false;
    private static double dMin = 99, dMax = -99, dSrcMax = 0, dOutMin = 99;
    private static int dN = 0;
    private static double feelsMin = 99, feelsMax = -99, outMin = 99, warmMin = 100, sourceMax = 0, uAvg = 0, wSum = 0;
    private static int nightTicks = 0, refuels = 0, loadedSplits = 0, shotsDone = -1;
    private static double burnedStart = 0;
    private static String roomInfo = "none";
    private static BodyState bodyAtStart;

    static String f(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static void log(String s) { out.println(s); out.flush(); YearOfIce.LOG.info("M8CABIN {}", s); }

    static void chore(String k, double m) { chores.merge(k, m, Double::sum); }

    static void blocked(String s) { blocked.add(s); log("BLOCKED " + s); }

    static void fail(String s) { fails.add(s); log("FAIL " + s); }

    static void grant(String what, int n) { grants.merge(what, n, Integer::sum); }

    static String clock(long m) { long d = Math.floorMod(m, 1440L); return String.format("%02d:%02d", d / 60, d % 60); }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("yearofice:" + id)); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ON || done || !ClockService.installed() || !HudBus.installed()) return;
        var server = e.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        tick++;
        if (tick < 200) return;
        try {
            if (ph == Ph.NIGHT) { night(p); return; } // the night samples every tick, shots or not
            if (wait > 0) { wait--; return; }
            step(p, server);
        } catch (RuntimeException ex) {
            YearOfIce.LOG.error("M8 cabin driver failed", ex);
            if (out == null) { done = true; verdict = "FAIL driver exception " + ex; return; }
            fail("driver exception " + ex);
            finish(p);
        }
    }

    private static void snap(String name, int settleTicks) { shot = name; wait = settleTicks; }

    /** Puts the camera/player at (x,y,z) looking at {@code look}. */
    private static void cam(ServerPlayer p, Vec3 at, Vec3 look) {
        double dx = look.x - at.x, dz = look.z - at.z, dy = look.y - at.y;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        p.teleportTo(level, at.x, at.y, at.z, yaw, pitch);
    }

    /** A standing spot 7-12 m from {@code c} whose ground is nearest c's height, so the shot looks at the thing from solid ground. */
    private static Vec3 spot(Vec3 c) {
        Vec3 best = null;
        double bd = 1e9;
        for (int r = 7; r <= 12; r += 5) for (int a = 0; a < 360; a += 30) {
            int x = (int) Math.floor(c.x + r * Math.cos(Math.toRadians(a))), z = (int) Math.floor(c.z + r * Math.sin(Math.toRadians(a)));
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            double d = Math.abs(h - c.y) + (r - 7) * 0.2;
            if (d < bd) { bd = d; best = new Vec3(x + 0.5, h, z + 0.5); }
        }
        return best == null ? new Vec3(c.x - 8, c.y + 4, c.z - 8) : best;
    }

    private static void hold(ServerPlayer p, int slot, ItemStack s) {
        p.getInventory().setItem(slot, s);
        p.getInventory().selected = slot;
    }

    private static int count(ServerPlayer p, java.util.function.Predicate<ItemStack> pr) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (!s.isEmpty() && pr.test(s)) n += s.getCount(); }
        return n;
    }

    private static ItemStack take(ServerPlayer p, java.util.function.Predicate<ItemStack> pr) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (!s.isEmpty() && pr.test(s)) return s; }
        return ItemStack.EMPTY;
    }

    static WorkStartPayload at(BlockPos pos, Direction face) {
        return new WorkStartPayload(true, pos, -1, (byte) face.get3DDataValue(), pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }

    static WorkStartPayload at(net.minecraft.world.entity.Entity e) {
        return new WorkStartPayload(true, BlockPos.ZERO, e.getId(), (byte) 0, (float) e.getX(), (float) e.getY(), (float) e.getZ());
    }

    /** Runs a job as a held use would and ledgers the game minutes it took. */
    private static double run(ServerPlayer p, WorkJob j, String kind) {
        double m = WorkService.runFor(p, j, 60_000);
        if (m < 0) { log("  job " + kind + " did not finish"); return -1; }
        chore(kind, m);
        return m;
    }

    // -------------------------------------------------------------------------------------------------------------

    private static void step(ServerPlayer p, net.minecraft.server.MinecraftServer server) {
        switch (ph) {
            case INIT -> init(p, server);
            case OUTPOST -> findOutpost(p);
            case OUTPOST_WAIT -> outpostLook(p);
            case REPAIR -> repair(p);
            case ROOM_WAIT -> roomWait(p);
            case TREES -> findTrees(p);
            case FELL_WAIT -> fellWait(p);
            case PROCESS -> process(p);
            case SITE -> chooseSite(p);
            case CABIN -> cabin(p);
            case CABIN_WAIT -> { ph = Ph.STAMP; }
            case STAMP -> stamp(p);
            case STAMP_WAIT -> stampWait(p);
            case JUMP -> jump(p);
            case NIGHT -> night(p);
            case END -> finish(p);
        }
    }

    private static void init(ServerPlayer p, net.minecraft.server.MinecraftServer server) {
        try {
            var f = server.getServerDirectory().resolve("smoke/m8cabin.log");
            Files.createDirectories(f.getParent());
            out = new PrintWriter(Files.newBufferedWriter(f));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        level = p.serverLevel();
        spawn = p.blockPosition();
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true); // camera teleports must not kill the scripted survivor (falls, rock); lifted for the night
        int moved = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-24, -6, -24), c.offset(24, 8, 24))) {
            if (level.getBlockEntity(q) instanceof BarrelBlockEntity b) {
                b.unpackLootTable(p);
                for (int i = 0; i < b.getContainerSize(); i++) {
                    ItemStack st = b.getItem(i);
                    if (!st.isEmpty()) { p.getInventory().add(st.copy()); b.setItem(i, ItemStack.EMPTY); moved++; }
                }
                if (moved > 0) break;
            }
        }
        // the hatchet rides in hotbar slot 8
        int hi = -1;
        for (int i = 0; i < p.getInventory().getContainerSize() && hi < 0; i++) if (p.getInventory().getItem(i).is(item("hatchet"))) hi = i;
        if (hi < 0) { fail("no hatchet in the crate kit"); finish(p); return; }
        ItemStack h = p.getInventory().getItem(hi).copy();
        ItemStack displaced = p.getInventory().getItem(hatchetSlot).copy();
        p.getInventory().setItem(hi, ItemStack.EMPTY);
        p.getInventory().setItem(hatchetSlot, h);
        if (!displaced.isEmpty() && hi != hatchetSlot) { if (hi < 36) p.getInventory().setItem(hi, displaced); else p.getInventory().add(displaced); }
        log("hatchet found in slot " + hi + ", now slot " + hatchetSlot + ": " + p.getInventory().getItem(hatchetSlot));
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty()) log("  kit: " + BuiltInRegistries.ITEM.getKey(st.getItem()) + " x" + st.getCount());
        }
        ClockService.get().forceRate(null); // World rate while the bot works: jobs are ledgered, the clock is not run through them
        bodyAtStart = BodyAttachments.of(p).copy();
        startMatches = count(p, s -> s.is(FireRegistry.MATCH.get()));
        log("START minute " + ClockService.get().nowMinute() + " clock " + clock(ClockService.get().nowMinute()) + " kit stacks " + moved
                + " matches " + count(p, s -> s.is(FireRegistry.MATCH.get())) + " bmi " + f(bodyAtStart.bmi()) + " kg " + f(bodyAtStart.kg));
        log("INSTANT_PLACE: no [pieces] instant_place dial exists; pieces are plain BlockItems that place instantly and free (no hold-to-work). Labour is ledgered from piece.json.");
        ph = Ph.OUTPOST;
    }

    // ---- 1. the outpost -------------------------------------------------------------------------------------------

    private static void findOutpost(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        long seed = level.getSeed();
        record Cand(String id, ChunkPos chunk, double dist) {}
        List<Cand> cands = new ArrayList<>();
        for (String id : OutpostDef.IDS) {
            OutpostDef d = OutpostDef.get(id);
            boolean enclosable = d.fills().stream().anyMatch(fl -> fl.role().equals("wall")) && d.fills().stream().anyMatch(fl -> fl.role().equals("roof"));
            log("  kind " + id + " spacing " + d.spacingM() + " m, enclosable " + enclosable);
            if (!enclosable) continue;
            int cells = Math.max(1, d.spacingM() / 16);
            int cx0 = Math.floorDiv(c.getX() >> 4, cells), cz0 = Math.floorDiv(c.getZ() >> 4, cells);
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                ChunkPos ch = OutpostSystem.siteChunk(seed, id, d, cx0 + dx, cz0 + dz);
                if (!com.yearofice.worldgen.regions.Regions.allowsOutpost(ch.getMinBlockX() + 3, ch.getMinBlockZ() + 3, id)) continue;
                double dist = Math.hypot(ch.getMinBlockX() + 3 - c.getX(), ch.getMinBlockZ() + 3 - c.getZ());
                cands.add(new Cand(id, ch, dist));
            }
        }
        cands.sort(java.util.Comparator.comparingDouble(Cand::dist));
        log("OUTPOST candidates (kinds with walls and a roof, by region data): " + cands.size() + (cands.isEmpty() ? "" : " nearest " + cands.get(0).id() + " chunk " + cands.get(0).chunk() + " " + f(cands.get(0).dist()) + " m"));
        if (cands.isEmpty()) { fail("no outpost site within 4 cells of spawn"); finish(p); return; }
        Cand best = cands.get(0);
        opDef = OutpostDef.get(best.id());
        double walkMin = best.dist() / WALK_MS / 60.0;
        chore("walk", walkMin);
        log("  walk " + f(best.dist()) + " m at " + WALK_MS + " m/s = " + f(walkMin) + " game min (teleported: the bot does not path-find)");
        // arrive: generating the chunk fires the outpost placement the player's approach would
        level.getChunk(best.chunk().x, best.chunk().z);
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk(best.chunk().x + dx, best.chunk().z + dz);
        int ox = best.chunk().getMinBlockX() + 3, oz = best.chunk().getMinBlockZ() + 3;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, oz);
        p.teleportTo(level, ox + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, oz - 10), oz - 10.5, 0, 10);
        opOrigin = new BlockPos(ox, sy, oz);
        ph = Ph.OUTPOST_WAIT;
        wait = 60;
    }

    private static void outpostLook(ServerPlayer p) {
        // the origin y was the pre-stamp ground; find the y whose layout matches best
        int best = Integer.MAX_VALUE;
        BlockPos o = opOrigin;
        for (int y = o.getY() - 14; y <= o.getY() + 3; y++) {
            int m = Outposts.missing(level, o.atY(y), opDef).size();
            if (m < best) { best = m; opOrigin = o.atY(y); }
        }
        opLayout = Outposts.layout(opDef, opOrigin).size();
        opMissing = best;
        log("OUTPOST " + opDef.id() + " (" + opDef.name() + ") origin " + opOrigin.toShortString() + ": layout " + opLayout + " pieces, " + opMissing + " missing/rotted ("
                + (opLayout == 0 ? 0 : Math.round(100.0 * opMissing / opLayout)) + "%)");
        if (opMissing == opLayout) { blocked("outpost layout not found at origin (stamp not there?) - region placement may have stamped elsewhere"); }
        Map<String, Long> by = Outposts.missing(level, opOrigin, opDef).stream().collect(java.util.stream.Collectors.groupingBy(Outposts.Piece::role, java.util.TreeMap::new, java.util.stream.Collectors.counting()));
        log("  repair list by role: " + by);
        double cx = opOrigin.getX() + opDef.size()[0] / 2.0, cz = opOrigin.getZ() + opDef.size()[2] / 2.0;
        cam(p, spot(new Vec3(cx, opOrigin.getY(), cz)), new Vec3(cx, opOrigin.getY() + 1.5, cz));
        ph = Ph.REPAIR;
        snap("m8_outpost_decayed", 70);
    }

    private static void repair(ServerPlayer p) {
        var todo = Outposts.missing(level, opOrigin, opDef);
        blocked("no player-facing entry point starts RepairJob (only GameTests construct it) and it spends no materials (ponytail: flat 10 s per piece); the bot runs the same job through WorkService");
        if (!todo.isEmpty()) {
            double m = run(p, new RepairJob(level, todo), "repair");
            log("REPAIR " + todo.size() + " jobs in " + f(m) + " game min (" + RepairJob.DS_PER_PIECE / 10 + " s each)");
        }
        opRepaired = opLayout - Outposts.missing(level, opOrigin, opDef).size();
        if (opRepaired != opLayout) fail("repair left " + (opLayout - opRepaired) + " pieces missing");
        else log("REPAIR_OK all " + opRepaired + "/" + opLayout + " pieces standing");
        ph = Ph.ROOM_WAIT;
        wait = 20;
        subStep = 0;
    }

    private static void roomWait(ServerPlayer p) {
        Room r = null;
        int[] s = opDef.size();
        outer:
        for (int y = 1; y < s[1]; y++) for (int x = 0; x < s[0]; x++) for (int z = 0; z < s[2]; z++) {
            BlockPos q = opOrigin.offset(x, y, z);
            if (!level.getBlockState(q).isAir()) continue;
            r = RoomService.get().roomAt(level, q);
            if (r != null) { opInside = q; break outer; }
        }
        if (r == null && subStep++ < 15) { wait = 20; return; }
        if (r == null) {
            fail("RoomService finds no room inside the repaired " + opDef.id() + " (leaks: roof/door/seams?)");
        } else {
            log(String.format(Locale.ROOT, "OUTPOST_ROOM %s at %s: %d m3, U %.1f W/C, source %.0f W, outside %.1f C, feels %.1f C, %d leaks", opDef.id(), opInside.toShortString(),
                    r.volumeM3(), r.uWPerK(), r.sourceW(), r.outsideC(), r.feelsLikeC(), r.leaks().size()));
        }
        double cx = opOrigin.getX() + s[0] / 2.0, cz = opOrigin.getZ() + s[2] / 2.0;
        cam(p, spot(new Vec3(cx, opOrigin.getY(), cz)), new Vec3(cx, opOrigin.getY() + 1.5, cz));
        ph = Ph.TREES;
        subStep = -1;
        snap("m8_outpost_repaired", 70);
    }

    // ---- 2a. honest wood ---------------------------------------------------------------------------------------------

    private static int hopIx = 0;

    /** Scan points (blocks from the start) for forest: the start valley first, then rings. */
    static int[][] hops() {
        List<int[]> l = new ArrayList<>();
        l.add(new int[] {0, 0});
        for (int r : new int[] {120, 240, 360}) for (int a = 0; a < 360; a += 60) l.add(new int[] {(int) (r * Math.cos(Math.toRadians(a))), (int) (r * Math.sin(Math.toRadians(a)))});
        return l.toArray(new int[0][]);
    }

    private static void hopTo(ServerPlayer p, int hx, int hz, String why) {
        int x = spawn.getX() + hx, z = spawn.getZ() + hz;
        level.getChunk(x >> 4, z >> 4);
        double d = Math.hypot(x - p.getX(), z - p.getZ());
        chore("walk", d / WALK_MS / 60.0);
        log("WALK " + why + ": " + f(d) + " m = " + f(d / WALK_MS / 60.0) + " game min (teleported)");
        p.teleportTo(level, x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, 0, 0);
        subStep = 0;
        wait = 90;
    }

    private static void findTrees(ServerPlayer p) {
        if (subStep == -1) { // walk back to the valley forest
            hopTo(p, 0, 0, "back to the start valley for wood");
            return;
        }
        BlockPos c = p.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Map<String, Integer> band = new LinkedHashMap<>();
        for (int dx = -80; dx <= 80; dx++) for (int dz = -80; dz <= 80; dz++) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            BlockPos q = new BlockPos(x, y, z);
            if (!(level.getBlockState(q).getBlock() instanceof YoiLogBlock)) continue;
            while (level.getBlockState(q.below()).getBlock() instanceof YoiLogBlock) q = q.below();
            TrunkSearch.Trunk t = FellingService.trunk(level, q);
            if (t == null) continue;
            BlockPos base = FellingService.base(t);
            double dia = FellingService.diameterCm(t);
            if (!seen.add(base)) continue;
            band.merge(dia < 8 ? "<8" : dia < 18 ? "8-18" : dia <= 40 ? "18-40" : ">40", 1, Integer::sum);
            if (dia > 40 || dia < 8) continue;
            found.add(base);
        }
        found.sort(java.util.Comparator.comparingDouble(b -> b.distSqr(c)));
        if (found.size() > 12) found = new ArrayList<>(found.subList(0, 12));
        trees = found;
        treeIx = 0;
        log("TREES at hop " + hopIx + " around " + c.toShortString() + ": " + found.size() + " fellable trunks (8-40 cm) within 80 m, all trunks by diameter band " + band + "; splits so far " + splitsMade + " of " + SPLITS_WANTED);
        if (found.isEmpty()) { nextHop(p); return; }
        ph = Ph.FELL_WAIT;
        subStep = 0;
        startTree(p);
    }

    private static void nextHop(ServerPlayer p) {
        int[][] h = hops();
        hopIx++;
        if (hopIx >= h.length) { log("WOOD no more forest within 360 m of the start"); afterWood(p); return; }
        ph = Ph.TREES;
        hopTo(p, h[hopIx][0], h[hopIx][1], "to look for forest (hop " + hopIx + ")");
    }

    private static void startTree(ServerPlayer p) {
        curTrunk = null;
        curFell = null;
        while (treeIx < trees.size() && curFell == null) {
            curBase = trees.get(treeIx++);
            curTrunk = FellingService.trunk(level, curBase.above());
            if (curTrunk == null) continue;
            hold(p, hatchetSlot, p.getInventory().getItem(hatchetSlot));
            stand(p, curBase, 0);
            WorkJob j = WoodJobs.resolve(p, at(curBase, Direction.NORTH));
            if (j instanceof WoodJobs.Fell fell) {
                curFell = fell;
                subStep = 0;
                log("TREE " + FellingService.kind(curTrunk) + " at " + curBase.toShortString() + " dia " + f(FellingService.diameterCm(curTrunk)) + " cm, " + fell.game().swings() + " swings");
            } else log("  tree " + curBase.toShortString() + " refused: " + j + " (main hand " + p.getMainHandItem() + ", slot " + p.getInventory().selected + ")");
        }
        if (curFell == null) {
            if (splitsMade >= SPLITS_WANTED) afterWood(p); else nextHop(p);
        }
    }

    /** Stand 2 m from the trunk base on the given compass bearing (0 north). */
    private static void stand(ServerPlayer p, BlockPos base, double bearing) {
        double[] u = Felling.unit(bearing);
        int x = (int) Math.floor(base.getX() + 0.5 + u[0] * 2), z = (int) Math.floor(base.getZ() + 0.5 + u[1] * 2);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        p.teleportTo(level, x + 0.5, y, z + 0.5, 0, 0);
    }

    private static void fellWait(ServerPlayer p) {
        if (curFell == null) { startTree(p); return; }
        if (subStep == 0) { // notch from the north, then the back cut from the south
            if (!curFell.valid(p)) { log("  tree " + curBase.toShortString() + " out of reach of the stand spot; skipped"); startTree(p); return; }
            int t1 = 0;
            while (!curFell.game().notchDone() && t1 < 3000 && curFell.valid(p)) { WorkService.runFor(p, curFell, 1); t1++; }
            stand(p, curBase, 180);
            int t2 = 0;
            WorkJob back = WoodJobs.resolve(p, at(curBase, Direction.SOUTH));
            while (back != null && !back.finished() && t2 < 3000 && back.valid(p)) { WorkService.runFor(p, back, 1); t2++; }
            chore("fell", (t1 + t2) * WorkService.dsPerTick() / 600.0);
            if (back == null || !back.finished()) { log("  felling did not finish (notch " + t1 + " ticks, back cut " + t2 + ")"); startTree(p); return; }
            treesFelled++;
            subStep = 1;
            wait = 5;
            return;
        }
        // wait for the landed log
        var logs = level.getEntitiesOfClass(FelledLogEntity.class, new net.minecraft.world.phys.AABB(curBase).inflate(30), l -> !usedLogs.contains(l.getId()));
        if (logs.isEmpty()) { if (subStep++ > 400) { log("  no log landed"); startTree(p); } wait = 5; return; }
        curLog = logs.get(0);
        log(String.format(Locale.ROOT, "  log landed: %s %.1f m, %d rounds left, limbed %s", curLog.kind(), curLog.lengthM(), curLog.roundsLeft(), curLog.limbed()));
        usedLogs.add(curLog.getId());
        curFell = null;
        ph = Ph.PROCESS;
        subStep = 0;
    }

    private static final List<ItemStack> roundPile = new ArrayList<>(), splitPile = new ArrayList<>();

    /** Gathers every round and split lying near {@code c} (the pack overflows onto the ground) and in the pack into the piles. */
    private static void sweepDrops(ServerPlayer p, Vec3 c) {
        for (var ie : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(16))) {
            ItemStack st = ie.getItem();
            if (!WoodItems.isRound(st) && !WoodItems.isSplit(st)) continue;
            for (int i = 0; i < st.getCount(); i++) (WoodItems.isRound(st) ? roundPile : splitPile).add(st.copyWithCount(1));
            ie.discard();
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty() || !(WoodItems.isRound(st) || WoodItems.isSplit(st))) continue;
            for (int k = 0; k < st.getCount(); k++) (WoodItems.isRound(st) ? roundPile : splitPile).add(st.copyWithCount(1));
            p.getInventory().setItem(i, ItemStack.EMPTY);
        }
        splitsMade = splitPile.size();
    }

    private static void process(ServerPlayer p) {
        hold(p, hatchetSlot, p.getInventory().getItem(hatchetSlot));
        Vec3 lp = curLog.position();
        p.teleportTo(level, lp.x + 1.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) lp.x, (int) lp.z), lp.z, 0, 0);
        WorkJob limb = WoodJobs.resolve(p, at(curLog));
        if (limb instanceof WoodJobs.Limb) run(p, limb, "limb");
        WorkJob buck = WoodJobs.resolve(p, at(curLog));
        if (buck instanceof WoodJobs.Buck) run(p, buck, "buck");
        else log("  buck refused: " + buck);
        sweepDrops(p, lp);
        log("  after bucking: " + roundPile.size() + " rounds gathered (pack cap 30 kg: the rest lay on the ground until swept), " + splitPile.size() + " splits");
        // the chopping block goes down beside the log; a round makes it
        if (chopBlock == null || !level.getBlockState(chopBlock).is(WoodRegistry.CHOPPING_BLOCK.get())) setChoppingBlock(p);
        if (chopBlock != null) splitRounds(p);
        sweepDrops(p, p.position());
        log("  wood so far: " + treesFelled + " trees, " + roundsMade + " rounds split, " + splitsMade + " splits piled");
        if (splitsMade >= SPLITS_WANTED) { afterWood(p); return; }
        ph = Ph.FELL_WAIT;
        startTree(p);
    }

    private static void setChoppingBlock(ServerPlayer p) {
        if (roundPile.isEmpty()) return;
        BlockPos g = p.blockPosition().east(2);
        g = new BlockPos(g.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, g.getX(), g.getZ()) - 1, g.getZ());
        p.setShiftKeyDown(true);
        ItemStack one = roundPile.get(0).copy();
        hold(p, workSlot, one);
        WorkJob set = WoodJobs.resolve(p, at(g, Direction.UP));
        if (set != null && run(p, set, "set_block") >= 0) {
            roundPile.remove(0);
            chopBlock = g.above();
        } else log("  chopping block not set: " + set);
        p.setShiftKeyDown(false);
        p.getInventory().setItem(workSlot, ItemStack.EMPTY);
    }

    private static void splitRounds(ServerPlayer p) {
        Vec3 cb = Vec3.atCenterOf(chopBlock);
        p.teleportTo(level, cb.x, chopBlock.getY(), cb.z - 1.5, 0, 0);
        while (!roundPile.isEmpty() && splitsMade < SPLITS_WANTED + 8) {
            ItemStack one = roundPile.remove(0);
            roundsMade++;
            // a real player lays the round with the use key on the block
            hold(p, workSlot, one.copy());
            var hit = new BlockHitResult(cb.add(0, 0.4, 0), Direction.UP, chopBlock, false);
            p.gameMode.useItemOn(p, level, p.getInventory().getItem(workSlot), InteractionHand.MAIN_HAND, hit);
            if (!(level.getBlockEntity(chopBlock) instanceof ChoppingBlockEntity be) || !be.hasRound()) {
                if (level.getBlockEntity(chopBlock) instanceof ChoppingBlockEntity be2) be2.setRound(one.copyWithCount(1), 0);
            }
            p.getInventory().setItem(workSlot, ItemStack.EMPTY);
            hold(p, hatchetSlot, p.getInventory().getItem(hatchetSlot));
            WorkJob j = WoodJobs.resolve(p, at(chopBlock, Direction.UP));
            if (!(j instanceof WoodJobs.Split sp)) { log("  split refused: " + j); break; }
            // alternate an off-crack and an on-crack strike: a mid-skill player (the crack is a crosshair read)
            if ((splitAlt++ & 1) == 0) sp.aim(0.8, 0.5); else sp.aim(0.5, 0.3);
            run(p, sp, "split");
            sweepDrops(p, cb);
        }
    }

    private static void afterWood(ServerPlayer p) {
        sweepDrops(p, p.position());
        int splits = splitPile.size();
        wood_in_pack = splits;
        double kg = splits * 3.75;
        log(String.format(Locale.ROOT, "WOOD done: %d trees felled, %d rounds, %d splits in the pack (%.0f kg), labour fell %.0f / limb %.0f / buck %.0f / split %.0f game min",
                treesFelled, roundsMade, splits, kg, chores.getOrDefault("fell", 0.0), chores.getOrDefault("limb", 0.0), chores.getOrDefault("buck", 0.0), chores.getOrDefault("split", 0.0)));
        if (splits < SPLITS_WANTED) {
            int g = SPLITS_WANTED - splits;
            blocked("only " + splits + " of " + SPLITS_WANTED + " splits could be cut; GRANTED " + g + " green birch splits stamped on day 1 so the burn can be measured");
            grantedSplits += g;
            grantDay = WoodItems.today();
            grant("birch_split", g);
        }
        ph = Ph.SITE;
    }

    // ---- 2b. the cabin ---------------------------------------------------------------------------------------------

    static final int N = 6; // outer ring 6x6, interior 4x4 (PieceGameTests.cabin)

    private static boolean flat(BlockPos o) {
        int y0 = Integer.MIN_VALUE;
        for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, o.getX() + x, o.getZ() + z);
            if (y0 == Integer.MIN_VALUE) y0 = h;
            if (h != y0 || !level.getFluidState(new BlockPos(o.getX() + x, h - 1, o.getZ() + z)).isEmpty()) return false;
            if (!level.getBlockState(new BlockPos(o.getX() + x, h, o.getZ() + z)).isAir()) return false;
        }
        return true;
    }

    private static BlockPos flatPad(BlockPos around, BlockPos not) {
        BlockPos best = null;
        double bd = 1e18;
        for (int dx = -40; dx <= 40; dx++) for (int dz = -40; dz <= 40; dz++) {
            BlockPos o = new BlockPos(around.getX() + dx, 0, around.getZ() + dz);
            if (!level.hasChunkAt(o) || (not != null && Math.abs(o.getX() - not.getX()) < 14 && Math.abs(o.getZ() - not.getZ()) < 14)) continue;
            double d = dx * dx + dz * dz;
            if (d < bd && d > 100 && flat(o)) { bd = d; best = o; }
        }
        if (best == null) return null;
        return best.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, best.getX(), best.getZ()));
    }

    private static void chooseSite(ServerPlayer p) {
        BlockPos around = p.blockPosition();
        site = flatPad(around, null);
        if (site == null) {
            // level a pad by hand: a real player cannot dig below the soil layer; filling is placing blocks
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, around.getX() + 12, around.getZ() + 12);
            site = new BlockPos(around.getX() + 12, y, around.getZ() + 12);
            levelPad(site);
            log("SITE no flat 6x6 within 40 m: levelled a pad at " + site.toShortString() + " (bot setBlock; a real player has no digging tool for this)");
        }
        site2 = flatPad(site, site);
        if (site2 == null) {
            site2 = new BlockPos(site.getX() + 20, 0, site.getZ());
            site2 = site2.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site2.getX(), site2.getZ()));
            levelPad(site2);
            log("SITE2 levelled at " + site2.toShortString());
        }
        log("SITE cabin pad " + site.toShortString() + " (ground y " + (site.getY() - 1) + "), second site " + site2.toShortString());
        double walk = Math.hypot(site.getX() - p.getX(), site.getZ() - p.getZ()) / WALK_MS / 60.0;
        chore("walk", walk);
        p.teleportTo(level, site.getX() + 3, site.getY(), site.getZ() - 4, 0, 20);
        ph = Ph.CABIN;
        cabinStep = 0;
    }

    private static void levelPad(BlockPos o) {
        int y = o.getY();
        for (int x = -1; x <= N; x++) for (int z = -1; z <= N; z++) {
            BlockPos g = new BlockPos(o.getX() + x, y - 1, o.getZ() + z);
            level.setBlock(g, Blocks.DIRT.defaultBlockState(), 3);
            for (int dy = 0; dy < 8; dy++) level.setBlock(g.above(1 + dy), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** Places {@code it} at {@code target} through the real block-item use path, clicking a supporting neighbour. */
    private static boolean placeAt(ServerPlayer p, Item it, BlockPos target) {
        ItemStack st = new ItemStack(it);
        obtain(p, it);
        hold(p, workSlot, st);
        for (Direction d : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos nb = target.relative(d);
            BlockState ns = level.getBlockState(nb);
            if (ns.isAir() || ns.canBeReplaced()) continue;
            Vec3 hitPos = Vec3.atCenterOf(nb).add(d.getOpposite().getStepX() * 0.5, d.getOpposite().getStepY() * 0.5, d.getOpposite().getStepZ() * 0.5);
            p.gameMode.useItemOn(p, level, st, InteractionHand.MAIN_HAND, new BlockHitResult(hitPos, d.getOpposite(), nb, false));
            if (!level.getBlockState(target).isAir() && !level.getBlockState(target).canBeReplaced()) { piecesPlaced++; return true; }
        }
        // nothing to click on or the placement refused: set it directly and say so
        if (it instanceof net.minecraft.world.item.BlockItem bi) level.setBlock(target, bi.getBlock().defaultBlockState(), 3);
        placeFallbacks++;
        piecesPlaced++;
        return false;
    }

    private static final Map<Item, ResourceLocation> recipeFor = new HashMap<>();
    static double craftMin;
    static int crafted;

    /** Crafts one {@code it} through its station recipe (the craft job the Hands run); raw ingredients are granted and logged; no recipe: GRANT the item. */
    private static void obtain(ServerPlayer p, Item it) {
        String nm = BuiltInRegistries.ITEM.getKey(it).getPath();
        if (recipeFor.isEmpty()) for (var h : StationCraft.all(level)) recipeFor.put(h.value().resultItem(), h.id());
        ResourceLocation rid = recipeFor.get(it);
        if (rid == null) { grant(nm, 1); return; }
        var holder = StationCraft.byId(level, rid);
        for (var need : holder.value().ingredients()) {
            ItemStack[] opts = need.ingredient().getItems();
            if (opts.length == 0) continue;
            int have = StationCraft.count(p.getInventory(), need);
            if (have < need.count()) { grant("ingredient:" + BuiltInRegistries.ITEM.getKey(opts[0].getItem()).getPath(), need.count() - have); p.getInventory().add(opts[0].copyWithCount(need.count() - have)); }
        }
        hold(p, hatchetSlot, p.getInventory().getItem(hatchetSlot));
        var job = StationCraft.start(p, rid, null);
        if (job == null) { grant(nm + "(craft refused)", 1); return; }
        int t = 0;
        while (!StationCraft.advance(p, job, WorkService.dsPerTick()) && t++ < 200000) { }
        double m = t * WorkService.dsPerTick() / 600.0;
        craftMin += m;
        firstBuildMin += m;
        chore("craft", m);
        crafted++;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(it)) { p.getInventory().setItem(i, ItemStack.EMPTY); break; }
    }

    private static double labour(Item it) {
        var d = it instanceof net.minecraft.world.item.BlockItem bi ? bi.getBlock().builtInRegistryHolder().getData(com.yearofice.api.build.PieceData.MAP) : null;
        return d == null ? 0 : d.labourMin();
    }

    private static void cabin(ServerPlayer p) {
        Item floor = item("slab_floor"), wall = item("wall_log"), roof = item("roof_pole_sod"), door = item("door_frame"), win = item("window_frame");
        int y0 = site.getY();
        if (cabinStep == 0) { // floor on the ground layer, then the first wall course
            for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) { placeAt(p, floor, site.offset(x, 0, z)); firstBuildMin += labour(floor); }
            blocked("pieces now craft through station recipes (log_piece/plank/pole/sod ingredients are GRANTED in bulk, the supply is a finding); old note: no recipe or loot path (creative tab only); piece.json prices them in log_piece/pole/plank/sod/shingle/stone which also have no recipe. GRANTED for the build.");
            blocked("dressed_stone: no recipe path either (nothing crafts it)");
            for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
                boolean ring = x == 0 || x == N - 1 || z == 0 || z == N - 1;
                if (!ring || (x == 2 && z == 0)) continue;
                placeAt(p, wall, site.offset(x, 1, z));
                firstBuildMin += labour(wall);
            }
            placeAt(p, door, site.offset(2, 1, 0));
            firstBuildMin += labour(door);
            log("FIRST_COURSE floor 36 slabs + 19 wall_log + door frame; labour so far " + f(firstBuildMin) + " game min");
            cam(p, new Vec3(site.getX() + 3, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.getX() + 3, site.getZ() - 6), site.getZ() - 6), new Vec3(site.getX() + 3, y0 + 1, site.getZ() + 3));
            cabinStep = 1;
            snap("m8_first_course", 60);
            return;
        }
        if (cabinStep == 1) { // second course, the window, roof with the chimney column left open
            for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
                boolean ring = x == 0 || x == N - 1 || z == 0 || z == N - 1;
                if (!ring) continue;
                if (x == 3 && z == 0) { placeAt(p, win, site.offset(x, 2, z)); firstBuildMin += labour(win); continue; }
                placeAt(p, wall, site.offset(x, 2, z));
                firstBuildMin += labour(wall);
            }
            // the door frame is two blocks high in GameTest cabins' tall form? one frame cell here; the upper cell is a wall log
            placeAt(p, wall, site.offset(2, 2, 0));
            firstBuildMin += labour(wall);
            for (int ring = 0; ring <= 2; ring++) for (int x = ring; x < N - ring; x++) for (int z = ring; z < N - ring; z++) {
                if (Math.min(Math.min(x, z), Math.min(N - 1 - x, N - 1 - z)) != ring) continue;
                if (x == 1 && z == 1) continue; // the chimney column
                placeAt(p, roof, site.offset(x, 3, z));
                firstBuildMin += labour(roof);
            }
            cabinStep = 2;
            return;
        }
        if (cabinStep == 2) { // stove, chimney, fuel
            stovePos = site.offset(1, 1, 1);
            Item stove = StoveRegistry.TIN_STOVE_ITEM.get(), pipe = StoveRegistry.CHIMNEY_PIPE_ITEM.get();
            blocked("tin_stove / chimney_pipe need iron_bar + furnace (iron tier): not reachable in a day-one kit. GRANTED.");
            placeAt(p, stove, stovePos);
            for (int i = 1; i <= 5; i++) placeAt(p, pipe, stovePos.above(i));
            if (!(level.getBlockEntity(stovePos) instanceof StoveBlockEntity be)) { fail("no stove block entity at " + stovePos.toShortString()); finish(p); return; }
            log(String.format(Locale.ROOT, "STOVE tin_stove at %s, chimney %.1f m, flued %s (min 1.5 m, ideal 3 m)", stovePos.toShortString(), be.chimneyM(), be.flued()));
            log(String.format(Locale.ROOT, "CABIN built: %d piece placements (%d fallbacks to setBlock), first-build labour %.0f game min = %.1f h", piecesPlaced, placeFallbacks, firstBuildMin, firstBuildMin / 60));
            cabinStep = 3;
            subStep = 0;
            return;
        }
        // cabinStep 3: wait for the room, then look
        BlockPos in = site.offset(3, 1, 3);
        Room r = RoomService.get().roomAt(level, in);
        if (r == null && subStep++ < 20) { wait = 20; return; }
        if (r == null) {
            fail("RoomService finds no room in the finished cabin (chimney pipe through roof? door frame?)");
        } else {
            log(String.format(Locale.ROOT, "CABIN_ROOM %d m3, U %.1f W/C, source %.0f W, outside %.1f C, feels %.1f C, leaks %s (stove cold)", r.volumeM3(), r.uWPerK(), r.sourceW(), r.outsideC(), r.feelsLikeC(), r.leaks()));
        }
        ph = Ph.CABIN_WAIT;
    }

    // ---- 3. save and stamp -----------------------------------------------------------------------------------------

    private static void stamp(ServerPlayer p) {
        try {
            BlockPos anchor = site.offset(3, 0, 3);
            int n = Structs.save(level, "m8cabin", anchor, 4, 6);
            log("SAVE struct m8cabin: " + n + " pieces (stove/pipes saved too if lane D saves them)");
            site2 = site2.atY(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site2.getX(), site2.getZ()));
            p.teleportTo(level, site2.getX() + 3, site2.getY(), site2.getZ() - 4, 0, 20);
            stampMin = Structs.stamp(level, "m8cabin", site2.offset(3, 0, 3));
            double pct = 100.0 * stampMin / firstBuildMin;
            log(String.format(Locale.ROOT, "STAMP queued %d pieces at %s: %.0f player min vs first build %.0f min = %.1f %%", PieceJobs.pending(level), site2.toShortString(), stampMin, firstBuildMin, pct));
            if (pct >= 10) fail(String.format(Locale.ROOT, "stamp %.1f%% of first build, want < 10%%", pct));
        } catch (IOException ex) {
            fail("struct save/stamp: " + ex);
        }
        ph = Ph.STAMP_WAIT;
        subStep = 0;
        wait = 20;
    }

    private static void stampWait(ServerPlayer p) {
        if (PieceJobs.pending(level) > 0 && subStep++ < 100) { wait = 10; return; }
        BlockPos in = site2.offset(3, 1, 3);
        Room r = RoomService.get().roomAt(level, in);
        if (r == null && subStep++ < 130) { wait = 10; return; }
        log("STAMP built: second site room " + (r == null ? "NOT detected (the stove column is a roof hole: pipes are not pieces)" : String.format(Locale.ROOT, "%d m3, %d leaks %s", r.volumeM3(), r.leaks().size(), r.leaks())));
        // seal the stove column with a pipe, as the player would
        if (r == null) placeAt(p, StoveRegistry.CHIMNEY_PIPE_ITEM.get(), site2.offset(1, 3, 1));
        Room r2 = RoomService.get().roomAt(level, in);
        log("STAMP second site after one chimney pipe in the roof hole: room " + (r2 == null ? "still NOT detected" : r2.volumeM3() + " m3, " + r2.leaks().size() + " leaks"));
        wait = 40;
        ph = Ph.JUMP;
        subStep = 0;
    }

    // ---- 4. the December night -------------------------------------------------------------------------------------

    private static void jump(ServerPlayer p) {
        ClockService ck = ClockService.get();
        BlockPos in = site.offset(3, 1, 3);
        double coldest = 99;
        long bestDay = 214;
        for (long d = 214; d <= 290; d++) { // Dec 1 .. mid-Feb
            double lo = 99;
            for (int h = 16; h <= 32; h += 2) lo = Math.min(lo, EnvService.get().sampleAbstract(level, in.getX(), in.getY(), in.getZ(), d * 1440 + h * 60).airC());
            if (lo < coldest) { coldest = lo; bestDay = d; }
        }
        log(String.format(Locale.ROOT, "NIGHT chosen: absolute day %d (dayOfYear %d), coldest drawn air %.1f C between 16:00 and 08:00 (the blueprint number is -30)", bestDay, bestDay % 365, coldest));
        long targetMin = bestDay * 1440 + 15 * 60;
        ck.setNow(targetMin * 600L);
        log("CLOCK jumped by the driver's own hook to " + clock(ck.nowMinute()) + " day " + ck.absDay() + " (the smoke cannot wait 230 days at the 24 s/day Sleep rate inside its 30-minute cap)");
        nightStart = ck.nowMinute();
        nightEnd = (bestDay + 1) * 1440 + 8 * 60;
        p.teleportTo(level, site.getX() + 3.5, site.getY() + 1, site.getZ() + 3.5, 0, 0);
        // the stove: load splits and light it with a match, through the block's use path
        StoveBlockEntity be = (StoveBlockEntity) level.getBlockEntity(stovePos);
        WoodItems.settle(p, WoodItems.today());
        loadStove(p, be);
        ItemStack match = take(p, s -> s.is(FireRegistry.MATCH.get()));
        if (match.isEmpty()) { blocked("no match in the pack after the day (kit had " + startMatches + "); GRANTED 1"); grant("match", 1); match = new ItemStack(FireRegistry.MATCH.get(), 1); p.getInventory().add(match); match = take(p, s -> s.is(FireRegistry.MATCH.get())); }
        {
            ItemStack one = match.copyWithCount(1);
            hold(p, workSlot, one);
            p.gameMode.useItemOn(p, level, one, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stovePos), Direction.UP, stovePos, false));
            if (!be.lit()) be.light();
            if (be.lit()) match.shrink(1);
        }
        StringBuilder byS = new StringBuilder();
        for (var v : com.yearofice.sim.env.FireSetting.values()) { be.setting(v); byS.append(v).append(' ').append(Math.round(be.outputW())).append(" W (").append(f(be.minutesPerArmload())).append(" min/armload); "); }
        log("STOVE W by setting at the lit draft (flued " + be.flued() + ", chimney " + be.chimneyM() + " m): " + byS);
        for (int k = 0; k < 3 && be.setting() != com.yearofice.sim.env.FireSetting.STEADY; k++) p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stovePos), Direction.UP, stovePos, false));
        log(String.format(Locale.ROOT, "STOVE lit %s, setting %s, %.0f W, armloads %.2f", be.lit(), be.setting(), be.outputW(), be.armloads()));
        burnedStart = be.burnedArmloads();
        p.setInvulnerable(false);
        ck.forceRate(Rate.SLEEP); // the night: sleeping/waiting by the fire runs at 60 game min a real second
        ph = Ph.NIGHT;
        subStep = 0;
        // exterior at dusk with smoke after the stove catches (the player stands out only a few ticks)
        cam(p, new Vec3(site.getX() + 3, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, site.getX() + 3, site.getZ() - 9), site.getZ() - 9), new Vec3(site.getX() + 1.5, site.getY() + 5, site.getZ() + 1.5));
        extShotAt = nightStart + 8;
    }

    private static void loadStove(ServerPlayer p, StoveBlockEntity be) {
        int guard = 0;
        while (be.armloads() <= 1.75 && guard++ < 8) {
            boolean fromPile = !splitPile.isEmpty();
            ItemStack s = fromPile ? splitPile.get(splitPile.size() - 1) : grantedSplits > 0 ? WoodItems.split(com.yearofice.sim.wood.WoodKind.BIRCH, 1, 50, grantDay) : ItemStack.EMPTY;
            if (s.isEmpty()) return;
            WoodItems.settle(s, WoodItems.today());
            ItemStack one = s.copyWithCount(1);
            hold(p, workSlot, one);
            double before = be.armloads();
            p.gameMode.useItemOn(p, level, one, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stovePos), Direction.UP, stovePos, false));
            if (be.armloads() > before) { if (fromPile) splitPile.remove(splitPile.size() - 1); else grantedSplits--; loadedSplits++; lastMoist = FireParts.moisture(one); }
            else { log("  stove refused a split (moisture " + FireParts.moisture(one) + "%)"); return; }
        }
        p.getInventory().setItem(workSlot, ItemStack.EMPTY);
    }

    private static void night(ServerPlayer p) {
        ClockService ck = ClockService.get();
        long now = ck.nowMinute();
        StoveBlockEntity be = (StoveBlockEntity) level.getBlockEntity(stovePos);
        nightTicks++;
        if (!extDone && now >= extShotAt) { extDone = true; shot = "m8_cabin_smoke"; }
        if (extDone && !inDone && now >= extShotAt + 40) { inDone = true; p.teleportTo(level, site.getX() + 3.5, site.getY() + 1, site.getZ() + 3.5, 0, 0); }
        // finding: the room's balance is cached 200 ticks, which at the Sleep rate is 10 game hours; a block update drops the cache, so the bot pokes one each 10 ticks
        if (nightTicks % 10 == 0) com.yearofice.build.rooms.Rooms.dirty(level, stovePos);
        if (prevNow > 0 && now - prevNow > 20) { fail(String.format(Locale.ROOT, "the clock jumped %d min inside one tick at %s (the body collapsed and the SOS flight ran?): night aborted", now - prevNow, clock(prevNow))); prevNow = now; ph = Ph.END; return; }
        prevNow = now;
        if (be != null && nightTicks % 2 == 0 && be.armloads() < 1.2 && (splitPile.size() + grantedSplits) > 0) {
            loadStove(p, be); refuels++; chore("stoke", 2);
            if (!be.lit()) { ItemStack m = take(p, s -> s.is(FireRegistry.MATCH.get())); if (m.isEmpty()) { grant("match", 1); m = new ItemStack(FireRegistry.MATCH.get(), 1); } hold(p, workSlot, m.copyWithCount(1)); p.gameMode.useItemOn(p, level, p.getInventory().getItem(workSlot), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stovePos), Direction.UP, stovePos, false)); if (!be.lit()) be.light(); else m.shrink(1); log("  relit the stove at " + clock(now) + ": " + be.lit()); }
        }
        if (be != null && !be.lit() && be.armloads() > 0 && nightTicks % 40 == 0) { log("  stove went out at " + clock(now) + " with " + f(be.armloads()) + " armloads"); }
        if (nightTicks <= 20 && nightTicks % 5 == 0) {
            var heats = com.yearofice.api.fire.StoveHeatLookup.INSTANCE.near(level, stovePos, 6);
            log(String.format(Locale.ROOT, "  probe %s tick %d: player alive %s hp %.0f at %s inRoom %s | stove lit %s %s %.0f W armloads %.2f flued %s smoke %.0f%% creosote %.1f | StoveHeatLookup %s | HeatSources in room %.0f W",
                    clock(now), nightTicks, p.isAlive(), p.getHealth(), p.blockPosition().toShortString(), RoomService.get().roomAt(level, p.blockPosition()) != null,
                    be != null && be.lit(), be == null ? "-" : be.setting(), be == null ? 0 : be.outputW(), be == null ? 0 : be.armloads(), be != null && be.flued(), be == null ? 0 : be.smokePct(), be == null ? 0 : be.creosote(),
                    heats, com.yearofice.api.fire.HeatSources.wattsIn(level, c -> c.distSqr(stovePos) < 16)));
        }
        Room r = RoomService.get().roomAt(level, site.offset(3, 1, 3));
        if (be != null && r != null && diag && nightTicks % 10 == 0) { // the player's damper: hotter below 12 C, cooler above 25 C (an empty-hand use cycles LOW > STEADY > HOT)
            var all = com.yearofice.sim.env.FireSetting.values();
            com.yearofice.sim.env.FireSetting want = r.feelsLikeC() < 8 ? all[Math.min(2, be.setting().ordinal() + 1)] : r.feelsLikeC() > 22 ? all[Math.max(0, be.setting().ordinal() - 1)] : be.setting();
            for (int k = 0; k < 3 && be.setting() != want; k++) p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stovePos), Direction.UP, stovePos, false));
        }
        BodyState bs = BodyAttachments.of(p);
        warmMin = Math.min(warmMin, bs.warmth);
        if (be != null && now > nightStart + 120 && sourceMax <= 0) { // DIAGNOSTIC (only when the real wiring still reports 0 W): the stove's own watts posted to an air cell, as lane A's GameTests do (see the finding: a real stove block is not an air cell)
            if (!diag) { diag = true; log("DIAG from " + clock(now) + ": the real stove's W is now also posted via HeatSources at the air cell beside it, to show what the cabin does once the stove counts"); }
            com.yearofice.api.fire.HeatSources.post(level, site.offset(2, 1, 1), be.lit() ? be.outputW() : 0);
        }
        if (r != null && diag) { dMin = Math.min(dMin, r.feelsLikeC()); dMax = Math.max(dMax, r.feelsLikeC()); dSrcMax = Math.max(dSrcMax, r.sourceW()); dOutMin = Math.min(dOutMin, r.outsideC()); dN++; }
        if (r != null && now > nightStart + 30 && !diag) { // let the first half hour warm the room; this is the REAL wiring
            feelsMin = Math.min(feelsMin, r.feelsLikeC());
            feelsMax = Math.max(feelsMax, r.feelsLikeC());
            outMin = Math.min(outMin, r.outsideC());
            sourceMax = Math.max(sourceMax, r.sourceW());
            uAvg += r.uWPerK();
            wSum += 1;
            if (nightTicks % 10 == 0) log(String.format(Locale.ROOT, "  %s outside %.1f C, inside feels %.1f C, source %.0f W, U %.1f W/C, stove %s %s %.2f armloads, warmth %.0f", clock(now), r.outsideC(), r.feelsLikeC(), r.sourceW(), r.uWPerK(), be != null && be.lit() ? "lit" : "out", be == null ? "-" : be.setting(), be == null ? -1 : be.armloads(), bs.warmth));
            if (roomInfo.equals("none") && now > nightStart + 120) {
                roomInfo = String.format(Locale.ROOT, "room: %d m3, U %.1f W/C, %.0f W in, outside %.1f C, inside %.1f C, %d leaks", r.volumeM3(), r.uWPerK(), r.sourceW(), r.outsideC(), r.feelsLikeC(), r.leaks().size());
                log("ROOM_INFO " + roomInfo);
            }
        } else if (r != null && diag && nightTicks % 10 == 0) {
            log(String.format(Locale.ROOT, "  DIAG %s outside %.1f C, inside feels %.1f C, source %.0f W, U %.1f W/C, stove %s %s %.2f armloads, warmth %.0f, splits left %d", clock(now), r.outsideC(), r.feelsLikeC(), r.sourceW(), r.uWPerK(),
                    be != null && be.lit() ? "lit" : "out", be == null ? "-" : be.setting(), be == null ? -1 : be.armloads(), bs.warmth, splitPile.size() + grantedSplits));
        } else if (r == null && now > nightStart + 30 && nightTicks % 60 == 0) log("  " + clock(now) + " no room at the bed cell");
        if (shotsDone == -1 && now > nightStart + 90) { // interior, lit stove
            cam(p, new Vec3(site.getX() + 4, site.getY() + 1.6, site.getZ() + 4), new Vec3(site.getX() + 1.5, site.getY() + 0.8, site.getZ() + 1.5));
            shot = "m8_cabin_stove_lit";
            shotsDone = 0;
        }
        if (shotsDone == 0 && now > nightStart + 360) { // inside, in bed light, strip drawn in full
            cam(p, new Vec3(site.getX() + 4, site.getY() + 1.6, site.getZ() + 4), new Vec3(site.getX() + 1.5, site.getY() + 1.5, site.getZ() + 1.5));
            shot = "m8_cabin_night_vitals";
            shotsDone = 1;
        }
        if (now >= nightEnd) ph = Ph.END;
    }

    private static void finish(ServerPlayer p) {
        log("finish at minute " + ClockService.get().nowMinute() + " nightEnd " + nightEnd + " phase " + ph + " alive " + p.isAlive());
        ClockService.get().forceRate(null);
        BodyState s = BodyAttachments.of(p);
        long m = ClockService.get().nowMinute();
        StoveBlockEntity be = stovePos == null ? null : (StoveBlockEntity) level.getBlockEntity(stovePos);
        double burned = be == null ? 0 : be.burnedArmloads() - burnedStart;
        double woodKg = loadedSplits * 3.75;
        double chor = chores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (wSum > 0 && feelsMin <= 5) fail(String.format(Locale.ROOT, "inside feels-like min %.1f C <= 5 C with the stove burning (outside min %.1f C)", feelsMin, outMin));
        if (wSum == 0) fail("no night samples (room never detected or the night never ran)");
        double dT = wSum > 0 && uAvg > 0 ? sourceMax / (uAvg / wSum) : 0;
        log("");
        log(String.format(Locale.ROOT, "DAY 1  (build day, ledger only; the driver acts instantly) kcal_in %.0f kcal_out %.0f kg %.2f bmi %.2f | chores %.0f game min (%s) vs free %d min waking/day => %.1f waking days of labour",
                s.kcalInToday(m), s.kcalOutToday(m), s.kg, s.bmi(), chor, chores, 16 * 60, chor / (16 * 60)));
        log(String.format(Locale.ROOT, "NIGHT (%s to %s) kcal_in %.0f kcal_out %.0f bmi %.2f warmth_min %.1f | wood: %d splits loaded = %.0f kg, %.1f armloads burned, %d stoke visits",
                clock(nightStart), clock(nightEnd), s.kcalInToday(m), s.kcalOutToday(m), s.bmi(), warmMin, loadedSplits, woodKg, burned, refuels));
        log(String.format(Locale.ROOT, "HEAT BALANCE inside feels-like min %.1f max %.1f C, outside min %.1f C, stove peak %.0f W, U %.1f W/C avg => dT %.1f C (blueprint: > 5 C at -30 C)", feelsMin, feelsMax, outMin, sourceMax, wSum > 0 ? uAvg / wSum : 0, dT));
        log(String.format(Locale.ROOT, "DIAG (stove W posted to an air cell) inside feels-like min %.1f max %.1f C over %d samples, outside min %.1f C, room source peak %.0f W => %s", dMin, dMax, dN, dOutMin, dSrcMax, dN == 0 ? "no samples" : dMin > 5 ? "above 5 C" : "NOT above 5 C"));
        log(String.format(Locale.ROOT, "OUTPOST %s repair jobs %d/%d done, room detected %s", opDef == null ? "-" : opDef.id(), opRepaired, opLayout, opInside != null));
        log(String.format(Locale.ROOT, "STAMP labour %.0f min vs first build %.0f min = %.1f %%", stampMin, firstBuildMin, firstBuildMin > 0 ? 100.0 * stampMin / firstBuildMin : -1));
        log("GRANTED (no honest path): " + grants);
        log("BLOCKED findings: " + blocked.size());
        for (String b : blocked) log("  - " + b);
        String res = fails.isEmpty() ? "PASS" : "FAIL";
        log("RESULT " + res + (fails.isEmpty() ? "" : " " + fails));
        verdict = "RESULT " + res;
        try { Files.writeString(p.getServer().getServerDirectory().resolve("smoke/m8cabin_room_info.txt"), "/yoi room info\n" + roomInfo + "\n"); } catch (IOException ignored) { }
        done = true;
    }
}

package com.yearofice.client.smoke;

import com.yearofice.api.build.Room;
import com.yearofice.api.build.RoomService;
import com.yearofice.api.build.GateService;
import com.yearofice.build.craft.StationCraft;
import com.yearofice.build.craft.CraftRegistry;
import com.yearofice.build.work.WorkService;
import com.yearofice.sim.build.Tier;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.body.BodyAttachments;
import com.yearofice.build.outpost.OutpostDef;
import com.yearofice.build.outpost.OutpostSystem;
import com.yearofice.build.outpost.Outposts;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.core.YearOfIce;
import com.yearofice.crop.CropBlockEntity;
import com.yearofice.crop.CropRegistry;
import com.yearofice.ecosystem.EcosystemRegistry;
import com.yearofice.ecosystem.companion.WolfEntity;
import com.yearofice.ecosystem.livestock.FeedBoxBlockEntity;
import com.yearofice.ecosystem.livestock.Livestock;
import com.yearofice.ecosystem.livestock.LivestockRegistry;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.stove.StoveBlockEntity;
import com.yearofice.fire.stove.StoveRegistry;
import com.yearofice.sim.body.BodyState;
import com.yearofice.sim.body.part.Part;
import com.yearofice.story.Journal;
import com.yearofice.story.radio.RadioDeskBlockEntity;
import com.yearofice.story.radio.RadioRegistry;
import com.yearofice.worldgen.block.WorldBlocks;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The M9 proxy check's player ({@code YEAROFICE_SMOKE=m9hour}; never active otherwise), standing in for Ruhan's gate
 * "an hour of year 2". v2: the homestead is earned through the honest sources where they exist (every remaining grant
 * is logged): the bench, planks, hoe, troughs, rack, nest box, radio desk and the pen's wall pieces are station recipes;
 * the seeds and the five goats and hens come down in the supply crate (SupplyParachute.land); the wolf is the den pup
 * (DenPups.leave). Still granted: raw materials (splits, sticks, log pieces, iron bars), the gates the clock jump
 * skipped, the cabin shell, stoves, the pup's trust and age, lamps. The hour itself
 * (23:30 to 00:30 on 20 December of year 2, World rate, so the midnight day tick falls inside it) uses no debug
 * command: the bot crouches, uses, feeds and walks (teleports, ledgered) through the real block and entity paths.
 * Asserts: no collapse, every vital change has a Messenger line, mod save data under 5 MB, MSPT inside the budget
 * (avg 30 ms, never over 50 ms), the wolf follows, livestock eat, greens grow, the radio line fires. Log
 * {@code smoke/m9hour.log}; the client (BodySmoke) grabs the screenshots named in {@link #shot}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class M9HourSmoke {
    private M9HourSmoke() {}

    public static final boolean ON = "m9hour".equals(System.getenv("YEAROFICE_SMOKE"));
    public static volatile boolean done = false;
    public static volatile String verdict = "";
    public static volatile String shot = null;
    /** Set when a shot is asked for and cleared by the client after the grab: the bot does not move on until the frame is taken. */
    public static volatile boolean shotBusy = false;
    private static int shotWaited = 0;

    static final long DEC20_Y2 = 365 + 233; // absolute day: year 2, 20 December (Dec 1 is day 214 of the year)
    static final int N = 6;
    static final double WALK_MS = 1.2;
    static final int HOUR_CAP_TICKS = 9000;

    private static int tick = 0, wait = 0, st = 0, sub = 0;
    private static PrintWriter out;
    private static ServerLevel level;
    private static MinecraftServer srv;
    private static ServerPlayer pl;
    private static final List<String> fails = new ArrayList<>(), findings = new ArrayList<>();
    private static final Map<String, Integer> grants = new LinkedHashMap<>();
    private static final int hatchetSlot = 8, workSlot = 7;

    // layout
    private static BlockPos siteA, siteB, penO, stoveA, stoveB, deskPos, rackPos, benchPos;
    private static double craftMin = 0;
    private static final Map<String, Integer> crafted = new LinkedHashMap<>();
    private static int padY;
    private static final List<ItemStack> splitPile = new ArrayList<>();
    private static int splitsLoaded = 0, refuels = 0;
    private static long grantDay;

    // actors
    private static java.util.UUID wolfId;
    private static final List<java.util.UUID> flockIds = new ArrayList<>();
    private static int flockMissing = 0;
    private static final List<BlockPos> beds = new ArrayList<>();
    private static BlockPos opOrigin;
    private static OutpostDef opDef;
    private static int opLayout, opMissing;

    // hour bookkeeping
    private static long S = -1, startTick = -1;
    private static int ev = 0;
    private static double stored0, storedEnd, units0Sum, unitsEndSum;
    private static double wolfD0 = -1, wolfDmin = 999, wolfDend = -1;
    private static int wolfTestStart = -1;
    private static boolean radioFired = false, radioBefore = false;
    private static double radioBattery = -1;
    private static boolean sawSos = false;
    private static String bodyStart = "", bodyEnd = "";
    private static double minWarmth = 100, minWater = 100, minEnergy = 99, minCond = 100;
    private static final Map<String, double[]> animalStart = new LinkedHashMap<>();

    // mspt
    private static long t0Nanos;
    private static final List<Double> msptAll = new ArrayList<>(), msptClean = new ArrayList<>();
    private static int cleanFrom = Integer.MAX_VALUE, artifactUntil = 0, over50 = 0, over50Clean = 0;
    private static boolean sampling = false;
    private static int outpostSince = -1;
    private static final List<String> worst = new ArrayList<>();

    // vitals crossings
    private record Pend(long minute, String label, String key, int before, int due) {}
    private static final List<Pend> pend = new ArrayList<>();
    private static final List<String> unmessaged = new ArrayList<>();
    private static boolean[] was = null;
    private static int[] wasBand = new int[Part.COUNT], wasFrost = new int[Part.COUNT];
    private static int wasDigits = 0, crossings = 0, covered = 0;
    private static Map<String, Integer> prevCounts = new HashMap<>();

    static String f(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static void log(String s) { out.println(s); out.flush(); YearOfIce.LOG.info("M9HOUR {}", s); }

    static void fail(String s) { fails.add(s); log("FAIL " + s); }

    static void finding(String s) { findings.add(s); log("FINDING " + s); }

    static void grant(String what, int n) { grants.merge(what, n, Integer::sum); }

    static String clock(long m) { long d = Math.floorMod(m, 1440L); return String.format("%02d:%02d", d / 60, d % 60); }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("yearofice:" + id)); }

    // ---- the tick -----------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void pre(ServerTickEvent.Pre e) { if (ON) t0Nanos = System.nanoTime(); }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ON || done || !ClockService.installed() || !HudBus.installed()) return;
        double ms = (System.nanoTime() - t0Nanos) / 1e6;
        var server = e.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        tick++;
        if (sampling) {
            msptAll.add(ms);
            if (ms > 50) over50++;
            if (tick >= artifactUntil) { msptClean.add(ms); if (ms > 50) { over50Clean++; if (worst.size() < 40) worst.add(String.format(Locale.ROOT, "+%d min tick %d: %.1f ms (event %d)", S >= 0 ? ClockService.get().nowMinute() - S : -1, tick - (int) startTick, ms, ev)); } }
        }
        if (tick < 200) return;
        try {
            if (out == null) init(p, server);
            p.resetFallDistance();
            track(p);
            if (st >= 20) { hourTick(p); return; }
            if (wait > 0) { wait--; return; }
            setup(p);
        } catch (RuntimeException ex) {
            YearOfIce.LOG.error("M9 hour driver failed", ex);
            if (out == null) { done = true; verdict = "FAIL driver exception " + ex; return; }
            fail("driver exception " + ex + " at " + Arrays.toString(Arrays.copyOf(ex.getStackTrace(), 4)));
            finish(p);
        }
    }

    private static void snap(String name, int settle) { shotBusy = true; shotWaited = 0; shot = name; wait = settle; }

    /** Marks the next few ticks as driver-caused (teleport, chunk load, block burst) for the cleaned MSPT series. */
    private static void artifact(int ticks) { artifactUntil = Math.max(artifactUntil, tick + ticks); }

    private static void cam(ServerPlayer p, Vec3 at, Vec3 look) {
        double dx = look.x - at.x, dz = look.z - at.z, dy = look.y - at.y;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        p.teleportTo(level, at.x, at.y, at.z, yaw, pitch);
        artifact(4);
    }

    private static void hold(ServerPlayer p, int slot, ItemStack s) {
        p.getInventory().setItem(slot, s);
        p.getInventory().selected = slot;
    }

    private static ItemStack take(ServerPlayer p, java.util.function.Predicate<ItemStack> pr) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (!s.isEmpty() && pr.test(s)) return s; }
        return ItemStack.EMPTY;
    }

    private static BlockHitResult top(BlockPos g) { return new BlockHitResult(Vec3.atCenterOf(g).add(0, 0.5, 0), Direction.UP, g, false); }

    /** Places {@code it} through the real block-item use path, clicking a supporting neighbour; setBlock only if nothing can be clicked. */
    private static boolean placeAt(ServerPlayer p, Item it, BlockPos target) {
        ItemStack st = new ItemStack(it);
        hold(p, workSlot, st);
        grant(BuiltInRegistries.ITEM.getKey(it).getPath(), 1);
        for (Direction d : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            BlockPos nb = target.relative(d);
            BlockState ns = level.getBlockState(nb);
            if (ns.isAir() || ns.canBeReplaced()) continue;
            Vec3 hitPos = Vec3.atCenterOf(nb).add(d.getOpposite().getStepX() * 0.5, d.getOpposite().getStepY() * 0.5, d.getOpposite().getStepZ() * 0.5);
            p.gameMode.useItemOn(p, level, st, InteractionHand.MAIN_HAND, new BlockHitResult(hitPos, d.getOpposite(), nb, false));
            if (!level.getBlockState(target).isAir() && !level.getBlockState(target).canBeReplaced()) return true;
        }
        if (it instanceof net.minecraft.world.item.BlockItem bi) level.setBlock(target, bi.getBlock().defaultBlockState(), 3);
        return false;
    }

    // ---- setup --------------------------------------------------------------------------------------------------

    private static void init(ServerPlayer p, MinecraftServer server) {
        try {
            var fl = server.getServerDirectory().resolve("smoke/m9hour.log");
            Files.createDirectories(fl.getParent());
            out = new PrintWriter(Files.newBufferedWriter(fl));
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        srv = server;
        pl = p;
        level = p.serverLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.setNoGravity(true); // the bot teleports into chunks the client has not got yet: without this the client falls through the world and the DamageRouter collapses it ("fell")
        p.setInvulnerable(true); // camera teleports must not kill the scripted survivor; collapse is judged on the body state and the journal
        int moved = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-24, -6, -24), c.offset(24, 8, 24))) {
            if (level.getBlockEntity(q) instanceof BarrelBlockEntity b) {
                b.unpackLootTable(p);
                for (int i = 0; i < b.getContainerSize(); i++) {
                    ItemStack s = b.getItem(i);
                    if (!s.isEmpty()) { p.getInventory().add(s.copy()); b.setItem(i, ItemStack.EMPTY); moved++; }
                }
                if (moved > 0) break;
            }
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty()) log("  kit: " + BuiltInRegistries.ITEM.getKey(s.getItem()) + " x" + s.getCount());
        }
        log("GRANT " + "the crate kit is used as dealt (" + moved + " stacks), except that the camp gear (bread, pot, panel, guide, medkit) is stashed in a barrel by the door: the 30 kg pack has no room for the homestead's materials otherwise");
        stashKit(p);
        ClockService ck = ClockService.get();
        ck.forceRate(null);
        long m0 = DEC20_Y2 * 1440 + 22 * 60 + 50;
        ck.setNow(m0 * 600L);
        log("CLOCK set through ClockService.setNow to " + clock(ck.nowMinute()) + " absolute day " + ck.absDay() + ", year " + ck.year() + ", day of year " + ck.dayOfYear()
                + " (setup jump; no /yoi)");
        if (ck.year() != 2) fail("clock is not in year 2: " + ck.year());
        wait = 100;
    }

    /** Crafts one {@code id} through its station recipe at the bench. A missing ingredient with no source in this driver is a logged GRANT; a refusal grants the result. */
    private static boolean craftAt(ServerPlayer p, String id) {
        Item it = item(id);
        ResourceLocation rid = null;
        for (var h : StationCraft.all(level)) if (h.value().resultItem() == it) { rid = h.id(); break; }
        if (rid == null) { grant(id, 1); log("GRANT " + id + " x1: no station recipe produces it"); p.getInventory().add(new ItemStack(it)); return false; }
        var holder = StationCraft.byId(level, rid);
        for (var need : holder.value().ingredients()) {
            ItemStack[] opts = need.ingredient().getItems();
            if (opts.length == 0) continue;
            int hv = StationCraft.count(p.getInventory(), need);
            if (hv < need.count()) {
                String nm = BuiltInRegistries.ITEM.getKey(opts[0].getItem()).getPath();
                grant(nm, need.count() - hv);
                if (!p.getInventory().add(opts[0].copyWithCount(need.count() - hv))) log("  PACK FULL (weight cap): " + nm + " x" + (need.count() - hv) + " dropped at the feet");
                if (grants.get(nm) == need.count() - hv) log("GRANT " + nm + " (ingredient of " + id + "; first shortfall, more are tallied in the final ledger)");
            }
        }
        p.setShiftKeyDown(false);
        var job = StationCraft.start(p, rid, benchPos);
        if (job == null) {
            var pl = StationCraft.plan(p, holder, StationCraft.stations(p, benchPos));
            finding("craft refused " + id + ": " + pl.problem() + " (stations " + StationCraft.stations(p, benchPos) + ", pack " + packCount(p, item("birch_split")) + " splits)");
            grant(id + "(refused)", 1);
            p.getInventory().add(new ItemStack(it, holder.value().result().getCount()));
            return false;
        }
        int t = 0;
        while (!StationCraft.advance(p, job, WorkService.dsPerTick()) && t++ < 400000) { }
        craftMin += t * WorkService.dsPerTick() / 600.0;
        crafted.merge(id, holder.value().result().getCount(), Integer::sum);
        return true;
    }

    /** As placeAt for an item made at the bench: not counted as a grant (falls back to setBlock only if nothing can be clicked). */
    private static boolean placeMade(ServerPlayer p, Item it, BlockPos target) {
        Integer before = grants.get(BuiltInRegistries.ITEM.getKey(it).getPath());
        boolean ok = placeAt(p, it, target);
        String k = BuiltInRegistries.ITEM.getKey(it).getPath();
        if (before == null) grants.remove(k); else grants.put(k, before);
        return ok;
    }

    private static void remove(ServerPlayer p, Item it, int n) {
        for (int i = 0; i < p.getInventory().getContainerSize() && n > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(it)) { int k = Math.min(n, s.getCount()); s.shrink(k); n -= k; }
        }
    }

    private static int packCount(ServerPlayer p, Item it) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (s.is(it)) n += s.getCount(); }
        return n;
    }

    /** The homestead set earned at a bench: gates, bench, planks, troughs, rack, nest box, hoe and radio desk, all through recipes. */
    private static void homesteadCrafts(ServerPlayer p) {
        for (Tier t : new Tier[] {Tier.BUSHCRAFT, Tier.HOMESTEAD}) {
            boolean open = GateService.get().isOpen(p, t);
            log("GATE " + t + " open: " + open);
            if (!open) {
                GateService.get().open(p, t, "m9 proxy setup");
                grant("gate_" + t.name().toLowerCase(Locale.ROOT), 1);
                log("GRANT gate " + t + " opened through GateService.open: " + (t == Tier.HOMESTEAD ? "the clock was set to December of year 2, skipping the first breakup in a warm room that opens it" : "no fire was lit before the clock jump"));
            }
        }
        // the pack carries about 30 kg: raw wood is granted one craft at a time (craftAt tops up the shortfall) and each product is placed or put away at once
        grant("stick", 8);
        p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.STICK, 8));
        log("GRANT 8 sticks for stakes (limbing gives kindling, fallen_limb comes from lightning, a plant pull gives one)");
        benchPos = siteA.offset(3, 1, 4);
        // the crate knife is not in #yearofice:knives: recipes want a flint knife (M6 hunt proxy: gravel gives the stone and flint)
        p.getInventory().add(new ItemStack(item("rough_stone"), 3));
        grant("rough_stone", 3);
        log("GRANT 3 rough stones (knapping tool, knife, hoe head); the gravel path that makes them is proven by the M6 hunt proxy");
        craftAt(p, "flint_knife");
        craftAt(p, "stake"); craftAt(p, "stake");
        craftAt(p, "workbench");
        placeMade(p, item("workbench"), benchPos);
        remove(p, item("workbench"), 1);
        p.teleportTo(level, siteA.getX() + 3.5, padY + 1, siteA.getZ() + 3.5, 0, 10);
        boolean bench = level.getBlockState(benchPos).is(CraftRegistry.WORKBENCH.get());
        log("BENCH crafted and placed at " + benchPos.toShortString() + ": " + bench + ", stations " + StationCraft.stations(p, benchPos));
        if (!bench) fail("the crafted workbench is not at " + benchPos.toShortString());
        craftAt(p, "tool_handle");
        craftAt(p, "hoe");
        craftAt(p, "plank"); // four planks for the desk
        craftAt(p, "radio_desk");
        remove(p, item("plank"), 8);
        log("CRAFTED at the cabin bench: " + crafted + "; labour " + String.format(Locale.ROOT, "%.0f", craftMin) + " game min so far");
    }

    /** A second bench by the pen, so the walls and furniture are made where they stand (the pack cannot carry them). */
    private static void penBench(ServerPlayer p) {
        BlockPos spot = new BlockPos(penO.getX() - 3, penO.getY(), penO.getZ() + 2);
        p.teleportTo(level, spot.getX() + 1.5, spot.getY(), spot.getZ() + 0.5, 0, 0);
        craftAt(p, "stake"); craftAt(p, "stake");
        craftAt(p, "workbench");
        placeMade(p, item("workbench"), spot);
        remove(p, item("workbench"), 1);
        benchPos = spot;
        boolean ok = level.getBlockState(spot).is(CraftRegistry.WORKBENCH.get());
        log("BENCH 2 crafted and placed at " + spot.toShortString() + " beside the pen: " + ok + ", stations " + StationCraft.stations(p, benchPos));
    }

    /** Crafts {@code id} at the pen bench and places it at {@code at}; the leftovers of the recipe stay in the pack (light). */
    private static void makeAndPlace(ServerPlayer p, String id, BlockPos at) {
        if (packCount(p, item(id)) == 0) craftAt(p, id);
        placeMade(p, item(id), at);
        remove(p, item(id), 1);
    }

    private static void stashKit(ServerPlayer p) {
        java.util.Set<String> keep = java.util.Set.of("hatchet", "fixed_blade_knife", "cord", "match", "bear_spray", "crate_backpack");
        BlockPos at = p.blockPosition().east(2);
        at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
        level.setBlock(at, Blocks.BARREL.defaultBlockState(), 3);
        int n = 0;
        if (level.getBlockEntity(at) instanceof BarrelBlockEntity b) {
            for (int i = 0; i < p.getInventory().getContainerSize() && n < b.getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (st.isEmpty()) continue;
                String path = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
                if (keep.contains(path) || path.endsWith("boots") || path.endsWith("jacket") || path.endsWith("shell") || path.endsWith("pants") || path.endsWith("socks") || path.endsWith("beanie") || path.endsWith("gloves")) continue;
                b.setItem(n++, st.copy());
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        log("STASH " + n + " kit stacks left in a barrel at " + at.toShortString());
    }

    private static void levelPad(BlockPos o, int w, int d, int y) {
        for (int x = 0; x < w; x++) for (int z = 0; z < d; z++) {
            BlockPos g = new BlockPos(o.getX() + x, y - 1, o.getZ() + z);
            for (int dy = 1; dy <= 3; dy++) level.setBlock(g.below(dy), Blocks.DIRT.defaultBlockState(), 2);
            level.setBlock(g, WorldBlocks.LOESS.get().defaultBlockState(), 2);
            for (int dy = 0; dy < 10; dy++) level.setBlock(g.above(1 + dy), Blocks.AIR.defaultBlockState(), 2);
        }
    }

    private static void setup(ServerPlayer p) {
        switch (st) {
            case 0 -> { // the homestead pad
                BlockPos s = p.blockPosition();
                int bx = s.getX() + 10, bz = s.getZ() - 4;
                level.getChunk(bx >> 4, bz >> 4);
                padY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx + 3, bz + 3);
                levelPad(new BlockPos(bx - 3, 0, bz - 10), 34, 22, padY);
                siteA = new BlockPos(bx, padY, bz);
                siteB = new BlockPos(bx + 9, padY, bz);
                penO = new BlockPos(bx + 19, padY, bz);
                log("GRANT homestead pad levelled by setBlock at " + siteA.toShortString() + " ground y " + (padY - 1) + " (a real player has no digging tool for this); cabin " + siteA.toShortString()
                        + ", stovehouse " + siteB.toShortString() + ", pen " + penO.toShortString());
                artifact(40);
                p.teleportTo(level, bx + 3, padY, bz - 6, 0, 10);
                st = 1;
                wait = 40;
            }
            case 1 -> { // cabin A: floor, walls, door, window, roof, stove, chimney
                buildShell(p, siteA, false);
                stoveA = siteA.offset(1, 1, 1);
                placeStove(p, stoveA);
                deskPos = siteA.offset(4, 1, 1);
                grantDay = WoodItems.today();
                for (int i = 0; i < 24; i++) splitPile.add(WoodItems.split(com.yearofice.sim.wood.WoodKind.BIRCH, 1, 18, grantDay));
                grant("birch_split", 24);
                log("GRANT 24 seasoned birch splits for the two stoves (the wood chain was proven in M8)");
                lightStove(p, stoveA);
                p.teleportTo(level, siteA.getX() + 3.5, padY + 1, siteA.getZ() + 3.5, 0, 10);
                homesteadCrafts(p);
                placeMade(p, RadioRegistry.RADIO_DESK_ITEM.get(), deskPos);
                log("radio_desk crafted at the bench and placed at " + deskPos.toShortString());
                p.teleportTo(level, siteA.getX() + 3.5, padY + 1, siteA.getZ() + 3.5, 0, 10); // the rest of the setup is done from indoors: the bot is a warm homesteader, not a frostbitten one
                lamps();
                artifact(40);
                st = 2;
                wait = 40;
            }
            case 2 -> { // stovehouse B: loess floor, beds, hoe, seeds
                buildShell(p, siteB, true);
                stoveB = siteB.offset(1, 1, 1);
                placeStove(p, stoveB);
                lightStove(p, stoveB);
                supplyDrop(p);
                sowBeds(p);
                artifact(40);
                st = 3;
                wait = 60;
            }
            case 3 -> { // rooms; pen and stock
                Room ra = RoomService.get().roomAt(level, siteA.offset(3, 1, 3)), rb = RoomService.get().roomAt(level, siteB.offset(3, 1, 3));
                if ((ra == null || rb == null) && sub++ < 20) { wait = 20; return; }
                log("ROOM cabin: " + (ra == null ? "NONE" : roomLine(ra)));
                log("ROOM stovehouse: " + (rb == null ? "NONE" : roomLine(rb)));
                if (ra == null) fail("RoomService finds no room in the cabin");
                if (rb == null) fail("RoomService finds no room in the stovehouse");
                buildPen(p);
                st = 4;
                wait = 20;
            }
            case 4 -> { // livestock, fed through the real paths
                stockPen(p);
                st = 5;
                wait = 40;
            }
            case 5 -> { // the wolf
                spawnWolf(p);
                st = 6;
                wait = 40;
            }
            case 6 -> { // find the outpost, generating its chunk (the stamp fires on first generation)
                findOutpost(p);
                p.teleportTo(level, siteA.getX() + 3.5, padY + 1, siteA.getZ() + 3.5, 0, 10);
                st = 7;
                wait = 20;
            }
            case 7 -> { // wait for the start minute, then go
                ClockService ck = ClockService.get();
                long goMin = DEC20_Y2 * 1440 + 23 * 60 + 30;
                if (ck.nowMinute() < goMin - 3) { wait = 20; return; } // the world rate carries it; the bot waits indoors by the stove
                if (ck.nowMinute() > goMin + 12) finding("setup ran " + (ck.nowMinute() - goMin) + " game min past 23:30; the hour starts late");
                beginHour(p);
                st = 20;
            }
            default -> { }
        }
    }

    private static String roomLine(Room r) {
        return String.format(Locale.ROOT, "%d m3, U %.1f W/C, source %.0f W, outside %.1f C, feels %.1f C, %d leaks", r.volumeM3(), r.uWPerK(), r.sourceW(), r.outsideC(), r.feelsLikeC(), r.leaks().size());
    }

    private static void buildShell(ServerPlayer p, BlockPos site, boolean soilFloor) {
        Item floor = item("slab_floor"), wall = item("wall_log"), roof = item("roof_pole_sod"), door = item("door_frame"), win = item("window_frame");
        for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
            boolean ring = x == 0 || x == N - 1 || z == 0 || z == N - 1;
            if (soilFloor && !ring) level.setBlock(site.offset(x, 0, z), WorldBlocks.LOESS.get().defaultBlockState(), 3);
            else placeAt(p, floor, site.offset(x, 0, z));
        }
        for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
            boolean ring = x == 0 || x == N - 1 || z == 0 || z == N - 1;
            if (!ring || (x == 2 && z == 0)) continue;
            placeAt(p, wall, site.offset(x, 1, z));
        }
        placeAt(p, door, site.offset(2, 1, 0));
        for (int x = 0; x < N; x++) for (int z = 0; z < N; z++) {
            boolean ring = x == 0 || x == N - 1 || z == 0 || z == N - 1;
            if (!ring) continue;
            if (x == 3 && z == 0) { placeAt(p, win, site.offset(x, 2, z)); continue; }
            placeAt(p, wall, site.offset(x, 2, z));
        }
        placeAt(p, wall, site.offset(2, 2, 0));
        for (int ring = 0; ring <= 2; ring++) for (int x = ring; x < N - ring; x++) for (int z = ring; z < N - ring; z++) {
            if (Math.min(Math.min(x, z), Math.min(N - 1 - x, N - 1 - z)) != ring) continue;
            if (x == 1 && z == 1) continue;
            placeAt(p, roof, site.offset(x, 3, z));
        }
        log("GRANT shell " + site.toShortString() + ": slab_floor, wall_log, door_frame, window_frame, roof_pole_sod (station recipes exist at the iron tier; not run in an hour)");
    }

    private static void placeStove(ServerPlayer p, BlockPos pos) {
        placeAt(p, StoveRegistry.TIN_STOVE_ITEM.get(), pos);
        for (int i = 1; i <= 5; i++) placeAt(p, StoveRegistry.CHIMNEY_PIPE_ITEM.get(), pos.above(i));
        if (level.getBlockEntity(pos) instanceof StoveBlockEntity be)
            log(String.format(Locale.ROOT, "GRANT tin_stove + 5 chimney_pipe at %s: chimney %.1f m, flued %s", pos.toShortString(), be.chimneyM(), be.flued()));
        else fail("no stove block entity at " + pos.toShortString());
    }

    private static StoveBlockEntity stove(BlockPos pos) { return level.getBlockEntity(pos) instanceof StoveBlockEntity be ? be : null; }

    private static void loadStove(ServerPlayer p, StoveBlockEntity be, BlockPos pos) {
        int guard = 0;
        while (be.armloads() <= 1.75 && guard++ < 8 && !splitPile.isEmpty()) {
            ItemStack s = splitPile.get(splitPile.size() - 1);
            WoodItems.settle(s, WoodItems.today());
            ItemStack one = s.copyWithCount(1);
            hold(p, workSlot, one);
            double before = be.armloads();
            p.gameMode.useItemOn(p, level, one, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            if (be.armloads() > before) { splitPile.remove(splitPile.size() - 1); splitsLoaded++; } else { log("  stove refused a split"); break; }
        }
        p.getInventory().setItem(workSlot, ItemStack.EMPTY);
    }

    private static void lightStove(ServerPlayer p, BlockPos pos) {
        StoveBlockEntity be = stove(pos);
        if (be == null) return;
        loadStove(p, be, pos);
        ItemStack m = take(p, s -> s.is(FireRegistry.MATCH.get()));
        if (m.isEmpty()) { grant("match", 1); m = new ItemStack(FireRegistry.MATCH.get(), 1); p.getInventory().add(m); m = take(p, s -> s.is(FireRegistry.MATCH.get())); log("GRANT 1 match (kit had none left)"); }
        ItemStack one = m.copyWithCount(1);
        hold(p, workSlot, one);
        p.gameMode.useItemOn(p, level, one, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        if (!be.lit()) { be.light(); finding("a match used on the stove block did not light it through the use path; the bot called light()"); }
        else m.shrink(1);
        for (int k = 0; k < 3 && be.setting() != com.yearofice.sim.env.FireSetting.STEADY; k++)
            p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
        log(String.format(Locale.ROOT, "STOVE %s lit %s, setting %s, %.0f W, armloads %.2f", pos.toShortString(), be.lit(), be.setting(), be.outputW(), be.armloads()));
    }

    private static void sowBeds(ServerPlayer p) {
        ItemStack hoe = new ItemStack(CropRegistry.HOE.get());
        log("SOW the hoe is the one crafted at the bench (" + packCount(p, CropRegistry.HOE.get()) + " in the pack); the seeds came in the supply drop crate (cabbage " + packCount(p, CropRegistry.CABBAGE_SEED.get()) + ", turnip " + packCount(p, CropRegistry.TURNIP_SEED.get()) + ")");
        BlockPos[] spots = {siteB.offset(3, 0, 3), siteB.offset(4, 0, 3), siteB.offset(3, 0, 4), siteB.offset(4, 0, 4)};
        for (int i = 0; i < spots.length; i++) {
            BlockPos g = spots[i];
            hold(p, workSlot, hoe);
            p.gameMode.useItemOn(p, level, hoe, InteractionHand.MAIN_HAND, top(g));
            if (!(level.getBlockState(g).getBlock() instanceof com.yearofice.crop.GardenBedBlock)) { fail("hoe did not make a garden bed at " + g.toShortString()); continue; }
            ItemStack seed = new ItemStack(i % 2 == 0 ? CropRegistry.CABBAGE_SEED.get() : CropRegistry.TURNIP_SEED.get());
            hold(p, workSlot, seed);
            p.gameMode.useItemOn(p, level, seed, InteractionHand.MAIN_HAND, top(g));
            BlockPos c = g.above();
            if (level.getBlockEntity(c) instanceof CropBlockEntity be) {
                log(String.format(Locale.ROOT, "SOW %s at %s: %d of %d up", be.blockId(), c.toShortString(), be.up(), be.seeds()));
                if (be.up() == 0) {
                    finding("sowing inside the heated stovehouse in December germinates 0 of " + be.seeds() + " (Garden.soilC reads the 7-day outdoor air mean, not the room); the bot set up=1 on the crop block entity (GRANT)");
                    be.sow((int) ClockService.get().absDay(), be.seeds(), 1, 8.0);
                }
                beds.add(c);
            } else fail("no crop block entity after sowing at " + c.toShortString());
        }
        p.getInventory().setItem(workSlot, ItemStack.EMPTY);
    }

    /** Set dressing so the night frames read: yard lamps (stone-brick posts with lanterns) and a lantern inside each building. Not part of the play. */
    private static void lamps() {
        int[][] posts = {{-2, -3}, {8, -3}, {16, -6}, {26, -6}, {16, 8}, {26, 8}};
        for (int[] d : posts) {
            BlockPos b = new BlockPos(siteA.getX() + d[0], padY, siteA.getZ() + d[1]);
            for (int i = 0; i < 4; i++) level.setBlock(b.above(i), Blocks.STONE_BRICKS.defaultBlockState(), 3); // not a fence: FenceDrift hangs the server on a chunk reload (see the findings)
            level.setBlock(b.above(4), Blocks.LANTERN.defaultBlockState(), 3);
        }
        level.setBlock(siteA.offset(4, 1, 4), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(siteB.offset(2, 1, 4), Blocks.LANTERN.defaultBlockState(), 3);
        level.setBlock(siteB.offset(4, 1, 1), Blocks.LANTERN.defaultBlockState(), 3);
        grant("lantern", 9);
        log("GRANT 9 vanilla lanterns (6 on stone-brick lamp posts in the yard, 3 indoors) so the midnight frames are readable; screenshot dressing only");
    }

    private static void buildPen(ServerPlayer p) {
        // 6x6 interior log-stockade pen, two courses (wall_log is the mod's stockade kind: Pens.kindOf). A pole fence is the natural first choice, but FenceDrift hangs the server on a reload.
        int x0 = penO.getX(), z0 = penO.getZ(), y = penO.getY();
        penBench(p);
        p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.STICK, 8));
        grant("stick", 8);
        log("GRANT 8 more sticks for the furniture's stakes");
        rackPos = new BlockPos(x0 + 4, y, z0 + 4);
        craftAt(p, "stake"); craftAt(p, "stake"); craftAt(p, "stake"); craftAt(p, "stake"); craftAt(p, "stake");
        craftAt(p, "plank"); craftAt(p, "plank"); craftAt(p, "plank"); craftAt(p, "plank");
        makeAndPlace(p, "hay_rack", rackPos);
        makeAndPlace(p, "feed_trough", new BlockPos(x0 + 4, y, z0 + 1));
        makeAndPlace(p, "nest_box", new BlockPos(x0 + 1, y, z0 + 4));
        makeAndPlace(p, "water_trough", new BlockPos(x0 + 1, y, z0 + 1));
        log("PEN hay_rack, feed_trough, nest_box, water_trough crafted at the pen bench and placed through the block-item use path");
        remove(p, item("plank"), 16);
        remove(p, item("stake"), 10);
        int nWall = 0;
        for (int x = -1; x <= 6; x++) for (int z = -1; z <= 6; z++) {
            boolean edge = x == -1 || x == 6 || z == -1 || z == 6;
            if (!edge) continue;
            for (int c = 1; c <= 2; c++) {
                makeAndPlace(p, "wall_log", new BlockPos(x0 + x, y + c - 1, z0 + z));
                nWall++;
            }
        }
        log("PEN " + nWall + " wall_log pieces crafted one at a time (piece_wall_log from log_piece) and placed: a closed two-course log stockade; crafted in all " + crafted + "; labour " + String.format(Locale.ROOT, "%.0f", craftMin) + " game min (" + String.format(Locale.ROOT, "%.1f", craftMin / 60) + " h) that the hour test does not spend");
        finding("the homestead set (two benches, planks, hoe, troughs, rack, nest box, radio desk, 56 wall pieces) is " + String.format(Locale.ROOT, "%.1f", craftMin / 60) + " game hours of crafting labour once its raw materials are on hand");
    }

    private static void stockPen(ServerPlayer p) {
        int x0 = penO.getX(), z0 = penO.getZ(), y = penO.getY();
        int herded = 0;
        for (var id : flockIds) {
            var e = level.getEntity(id);
            if (e == null || !e.isAlive()) continue;
            e.moveTo(x0 + 2.5 + (herded % 3), y, z0 + 2.5 + (herded / 3), 0, 0);
            herded++;
        }
        log("LIVESTOCK " + herded + " animals from the supply drop herded into the pen (teleported: " + String.format(Locale.ROOT, "%.0f", 18 / WALK_MS / 60) + " game min of leading on foot); no goat or hen is spawned by the driver");
        if (herded < 5) finding("the supply drop delivered " + herded + " of the 5 manifest animals (hens 3, goats 2)");
        // keeper's round, through the real paths: straw into the rack and by hand, a water bucket per animal
        grant("straw", 40);
        grant("water_bucket", 5);
        log("GRANT 40 straw and 5 water buckets for the keeper's round (the hay and well chain is the farm's, not the hour's)");
        p.teleportTo(level, x0 + 3, y, z0 - 2.5, 0, 0);
        ItemStack straw = new ItemStack(CropRegistry.STRAW.get(), 40);
        hold(p, workSlot, straw);
        int into = 0;
        for (int i = 0; i < 16; i++) {
            if (p.gameMode.useItemOn(p, level, straw, InteractionHand.MAIN_HAND, top(rackPos)).consumesAction()) into++;
        }
        int by = 0;
        for (var a : flock()) {
            for (int k = 0; k < 3; k++) { if (a.interact(p, InteractionHand.MAIN_HAND).consumesAction()) by++; }
        }
        for (var a : flock()) {
            ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
            hold(p, workSlot, bucket);
            if (a.interact(p, InteractionHand.MAIN_HAND).consumesAction()) by++;
        }
        p.getInventory().setItem(workSlot, ItemStack.EMPTY);
        var rack = (FeedBoxBlockEntity) level.getBlockEntity(rackPos);
        log(String.format(Locale.ROOT, "KEEPER round: %d straw into the rack (rack now %.1f kg), %d hand feeds/waterings on animals", into, rack == null ? -1 : rack.feedKg(), by));
        if (rack != null && rack.feedKg() <= 0) fail("hay rack took no straw by use");
        p.getInventory().setItem(workSlot, ItemStack.EMPTY);
    }

    /** The kept animals that are loaded now (an entity that was unloaded and reloaded is a new object: look it up by id). */
    private static List<net.minecraft.world.entity.Entity> flock() {
        List<net.minecraft.world.entity.Entity> l = new ArrayList<>();
        flockMissing = 0;
        for (var id : flockIds) { var e = level.getEntity(id); if (e != null && e.isAlive()) l.add(e); else flockMissing++; }
        return l;
    }

    private static WolfEntity wolf() { return wolfId != null && level.getEntity(wolfId) instanceof WolfEntity w && w.isAlive() ? w : null; }

    private static void spawnWolf(ServerPlayer p) {
        int year = ClockService.get().year();
        // the honest source: the den pup the world leaves 120-200 m away each summer (DenPups.leave)
        BlockPos at = p.blockPosition();
        var before = level.getEntitiesOfClass(WolfEntity.class, new net.minecraft.world.phys.AABB(at).inflate(260));
        boolean left = false;
        int tries = 0;
        for (; tries < 40 && !left; tries++) {
            net.minecraft.nbt.CompoundTag root = p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            left = com.yearofice.ecosystem.companion.DenPups.leave(p, year) && level.getEntitiesOfClass(WolfEntity.class, new net.minecraft.world.phys.AABB(at).inflate(260)).size() > before.size();
        }
        WolfEntity wolf = null;
        for (WolfEntity w : level.getEntitiesOfClass(WolfEntity.class, new net.minecraft.world.phys.AABB(at).inflate(260))) if (!before.contains(w)) wolf = w;
        if (wolf == null) {
            finding("DenPups.leave placed no pup in " + tries + " tries: it needs the chunk 120-200 m out loaded, and this box runs a render distance of 6 (96 m). At the default view distance it reaches.");
            wolf = EcosystemRegistry.WOLF.get().create(level);
            wolf.moveTo(siteA.getX() + 3, padY, siteA.getZ() - 5, 0, 0);
            wolf.makeTameable(0, 60);
            level.addFreshEntity(wolf);
            grant("den_pup_fallback", 1);
            log("GRANT a pup created as DenPups does (tameable, trust 0, 60 days) beside the cabin because the den pup could not be placed");
        } else {
            log("PUP the den pup arrived at " + wolf.blockPosition().toShortString() + " (" + String.format(Locale.ROOT, "%.0f", Math.sqrt(wolf.distanceToSqr(p))) + " m out), unowned and tameable, trust " + wolf.trust().value());
            wolf.moveTo(siteA.getX() + 3, padY, siteA.getZ() - 5, 0, 0);
            log("WALK the pup is led to the cabin (teleported)");
        }
        // taming is 30-60 game days of feeding; the hour cannot spend that, so trust and age are set by hand
        wolf.makeTameable(85, 400);
        wolf.setOwner(p);
        wolf.trust().set(85);
        wolf.setName("Ash");
        wolfId = wolf.getUUID();
        grant("wolf_trust_and_age", 1);
        log("GRANT the pup's trust set to 85, age 400 days and owner set: honest taming is 30-60 game days of feeding and growing up");
    }

    /** The supply crate lands where the parachute would put it (SupplyParachute.land: barrel, manifest and, from day 365, the livestock). */
    private static void supplyDrop(ServerPlayer p) {
        BlockPos crate = new BlockPos(siteA.getX() + 15, padY, siteA.getZ() - 4);
        int before = flockIds.size();
        com.yearofice.story.radio.SupplyParachute.land(level, crate);
        grant("supply_drop_trigger", 1);
        log("GRANT the drop is landed by calling SupplyParachute.land at " + crate.toShortString() + " (the radio sked that would fly it is exercised separately by the radio moment)");
        int taken = 0;
        if (level.getBlockEntity(crate) instanceof BarrelBlockEntity b) {
            for (int i = 0; i < b.getContainerSize(); i++) {
                ItemStack st = b.getItem(i);
                if (!st.isEmpty()) {
                    if (p.getInventory().add(st.copy())) { b.setItem(i, ItemStack.EMPTY); taken += st.getCount(); }
                    else log("  crate: " + st + " left in the crate (the pack's weight cap)");
                }
            }
        }
        for (var e : level.getEntitiesOfClass(com.yearofice.ecosystem.YoiAnimal.class, new net.minecraft.world.phys.AABB(crate).inflate(8), a -> a instanceof Livestock)) flockIds.add(e.getUUID());
        log("SUPPLY crate opened: " + taken + " items taken (" + packCount(p, CropRegistry.TURNIP_SEED.get()) + " turnip seed, " + packCount(p, CropRegistry.CABBAGE_SEED.get()) + " cabbage seed); " + (flockIds.size() - before) + " livestock came down with it");
    }

    private static void findOutpost(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        long seed = level.getSeed();
        record Cand(String id, ChunkPos chunk, double dist) {}
        List<Cand> cands = new ArrayList<>();
        for (String id : OutpostDef.IDS) {
            OutpostDef d = OutpostDef.get(id);
            int cells = Math.max(1, d.spacingM() / 16);
            int cx0 = Math.floorDiv(c.getX() >> 4, cells), cz0 = Math.floorDiv(c.getZ() >> 4, cells);
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                ChunkPos ch = OutpostSystem.siteChunk(seed, id, d, cx0 + dx, cz0 + dz);
                if (!com.yearofice.worldgen.regions.Regions.allowsOutpost(ch.getMinBlockX() + 3, ch.getMinBlockZ() + 3, id)) continue;
                cands.add(new Cand(id, ch, Math.hypot(ch.getMinBlockX() + 3 - c.getX(), ch.getMinBlockZ() + 3 - c.getZ())));
            }
        }
        cands.sort(java.util.Comparator.comparingDouble(Cand::dist));
        if (cands.isEmpty()) { fail("no outpost site within 4 cells of spawn"); return; }
        Cand best = cands.get(0);
        opDef = OutpostDef.get(best.id());
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.getChunk(best.chunk().x + dx, best.chunk().z + dz);
        int ox = best.chunk().getMinBlockX() + 3, oz = best.chunk().getMinBlockZ() + 3;
        int sy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ox, oz);
        BlockPos o = new BlockPos(ox, sy, oz);
        int best2 = Integer.MAX_VALUE;
        opOrigin = o;
        for (int y = o.getY() - 14; y <= o.getY() + 3; y++) {
            int m = Outposts.missing(level, o.atY(y), opDef).size();
            if (m < best2) { best2 = m; opOrigin = o.atY(y); }
        }
        opLayout = Outposts.layout(opDef, opOrigin).size();
        opMissing = best2;
        log(String.format(Locale.ROOT, "OUTPOST nearest %s (%s) %.0f m away, origin %s: layout %d pieces, %d missing/rotted. Chunks generated in setup (the first-load stamp)", opDef.id(), opDef.name(), best.dist(), opOrigin.toShortString(), opLayout, opMissing));
        artifact(60);
    }

    // ---- the hour ------------------------------------------------------------------------------------------------

    private static double stored() {
        double t = 0;
        if (level.getBlockEntity(rackPos) instanceof FeedBoxBlockEntity r) t += r.feedKg();
        for (var a : flock()) if (a instanceof Livestock l) t += l.core().feedKg;
        return t;
    }

    private static double unitsSum() {
        double t = 0;
        for (BlockPos c : beds) if (level.getBlockEntity(c) instanceof CropBlockEntity be) t += be.units();
        return t;
    }

    private static void beginHour(ServerPlayer p) {
        S = ClockService.get().nowMinute();
        startTick = tick;
        stored0 = stored();
        units0Sum = unitsSum();
        for (var a : flock()) if (a instanceof Livestock l) animalStart.put(a.getUUID().toString().substring(0, 4) + "/" + a.getType().builtInRegistryHolder().key().location().getPath(),
                new double[] {l.core().care.fed(), l.core().care.watered(), l.core().feedKg, l.core().waterL});
        BodyState b = BodyAttachments.of(p);
        bodyStart = String.format(Locale.ROOT, "warmth %.0f water %.0f energy %.1f kg %.1f bmi %.2f", b.warmth, b.water, b.energy, b.kg, b.bmi());
        radioBefore = com.yearofice.story.radio.Radio.working(p);
        sampling = true;
        log("");
        log("HOUR BEGIN " + clock(S) + " day " + ClockService.get().absDay() + " year " + ClockService.get().year() + " | stored feed " + f(stored0) + " kg | crop units " + f(units0Sum) + " | body " + bodyStart
                + " | sos " + b.sos);
        log("  journal at start: " + Journal.entries(p).size() + " entries; HudBus journal " + HudBus.get().journal(p).size() + " lines");
        artifact(2);
    }

    private static long m() { return ClockService.get().nowMinute() - S; }

    private static void hourTick(ServerPlayer p) {
        BodyState b = BodyAttachments.of(p);
        minWarmth = Math.min(minWarmth, b.warmth);
        minWater = Math.min(minWater, b.water);
        minEnergy = Math.min(minEnergy, b.energy);
        minCond = Math.min(minCond, b.condition());
        if (b.sos != 0) { sawSos = true; fail("COLLAPSE: SOS state " + b.sos + " at " + clock(ClockService.get().nowMinute())); finish(p); return; }
        if (!p.isAlive()) { fail("the player died"); finish(p); return; }
        for (var e : Journal.entries(p)) if (Journal.FLIGHT.equals(e.kind()) || Journal.RUN_ENDED.equals(e.kind())) { fail("collapse journal entry " + e.kind() + ": " + e.line()); finish(p); return; }
        
        // wolf distance probe
        if (wolfTestStart >= 0 && tick % 10 == 0) {
            WolfEntity w = wolf();
            double d = w == null ? 999 : w.distanceTo(p);
            wolfDmin = Math.min(wolfDmin, d);
            if (tick - wolfTestStart == 80 || tick - wolfTestStart == 160) log(String.format(Locale.ROOT, "  wolf follow +%d s: %.1f m from the player (started %.1f m)", (tick - wolfTestStart) / 20, d, wolfD0));
            if (tick - wolfTestStart >= 160) { wolfDend = d; wolfTestStart = -1; }
        }
        // the stoves are kept by hand every ten game seconds of attention
        if (tick % 40 == 0) upkeep(p);
        if (m() >= 60 || tick - startTick > HOUR_CAP_TICKS) { endHour(p); return; }
        if (wait > 0) { wait--; return; }
        if (shotBusy && shotWaited++ < 700) return; // the client is still meshing the scene or grabbing the frame
        shotBusy = false;
        runEvents(p);
    }

    private static void upkeep(ServerPlayer p) {
        for (BlockPos pos : new BlockPos[] {stoveA, stoveB}) {
            StoveBlockEntity be = stove(pos);
            if (be == null) continue;
            if (be.armloads() < 1.2 && !splitPile.isEmpty()) {
                loadStove(p, be, pos);
                refuels++;
                if (!be.lit()) {
                    ItemStack mt = take(p, s -> s.is(FireRegistry.MATCH.get()));
                    if (mt.isEmpty()) { grant("match", 1); mt = new ItemStack(FireRegistry.MATCH.get(), 1); }
                    hold(p, workSlot, mt.copyWithCount(1));
                    p.gameMode.useItemOn(p, level, p.getInventory().getItem(workSlot), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
                    if (!be.lit()) be.light();
                    log("  relit stove " + pos.toShortString() + " at " + clock(ClockService.get().nowMinute()) + ": " + be.lit());
                }
            }
        }
    }

    private static Vec3 ground(double x, double z) {
        return new Vec3(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z)), z);
    }

    /** Eye height 1.62 above the feet: the feet go on the floor (padY + 1 inside a building, padY on the yard). */
    private static void runEvents(ServerPlayer p) {
        long mm = m();
        int ax = siteA.getX(), az = siteA.getZ(), bx = siteB.getX(), bz = siteB.getZ();
        switch (ev) {
            case 0 -> { // the vitals strip, indoors by the stove (a good player is warm at the top of the hour)
                cam(p, new Vec3(ax + 4.5, padY + 1, az + 4.5), new Vec3(ax + 1.5, padY + 1.6, az + 1.5));
                ev = 1;
                snap("m9_vitals_strip", 40);
            }
            case 1 -> {
                if (mm < 3) return;
                cam(p, new Vec3(bx + 2.6, padY + 1, bz + 1.5), new Vec3(bx + 4.2, padY + 1.2, bz + 4.2)); // the stovehouse beds
                ev = 2;
                snap("m9_stovehouse_interior", 40);
            }
            case 2 -> {
                if (mm < 6) return;
                // wolf follows: the wolf waits at the cabin door and the player walks 18 m off along the pad
                WolfEntity w = wolf();
                if (w == null) { fail("the wolf is not loaded at the follow test"); ev = 3; return; }
                w.teleportTo(ax + 3, padY, az - 7);
                p.teleportTo(level, ax + 21, padY, az - 7, 90, 5);
                artifact(40);
                wolfD0 = w.distanceTo(p);
                wolfTestStart = tick;
                log(String.format(Locale.ROOT, "WOLF follow test at +%d min: the player is %.1f m from the wolf (command %s, trust %.0f)", mm, wolfD0, w.command(), w.trust().value()));
                ev = 3;
                wait = 165;
            }
            case 3 -> {
                if (mm < 14) return;
                WolfEntity w = wolf();
                Vec3 pc = new Vec3(penO.getX() + 3, padY, penO.getZ() + 3);
                if (w != null) w.teleportTo(penO.getX() + 3.5, padY, penO.getZ() - 4.5);
                cam(p, new Vec3(penO.getX() + 3, padY + 3, penO.getZ() - 9), pc.add(0, 0.3, 0)); // from above the stockade (the bot hovers: no gravity)
                ev = 4;
                snap("m9_wolf_and_livestock", 40);
            }
            case 4 -> {
                if (mm < 18) return;
                cam(p, new Vec3(ax + 2.5, padY, az - 13), new Vec3(ax + 2.5, padY + 3.6, az + 1));
                ev = 5;
                snap("m9_homestead_exterior", 40);
            }
            case 5 -> {
                if (mm < 22) return;
                // the outpost visit (the homestead's chunks unload while the player is away)
                if (opOrigin == null) { ev = 7; return; }
                double dist = Math.hypot(opOrigin.getX() - p.getX(), opOrigin.getZ() - p.getZ());
                log(String.format(Locale.ROOT, "WALK to the outpost: %.0f m = %.1f game min at %.1f m/s (teleported; the clock keeps its own pace)", dist, dist / WALK_MS / 60, WALK_MS));
                int[] sz = opDef.size();
                double cx = opOrigin.getX() + sz[0] / 2.0, cz = opOrigin.getZ() + sz[2] / 2.0;
                Vec3 at = spot(new Vec3(cx, opOrigin.getY(), cz));
                at = new Vec3(at.x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) at.x, (int) at.z), at.z);
                cam(p, at, new Vec3(cx, opOrigin.getY() + 1.2, cz));
                artifact(500);
                outpostSince = tick;
                int nowMissing = Outposts.missing(level, opOrigin, opDef).size();
                log("OUTPOST visited: " + opDef.id() + " layout " + opLayout + " pieces, " + nowMissing + " missing; HudBus lines mentioning outpost: " + countKey(p, "outpost") + ", journal entries of kind outpost: "
                        + Journal.entries(p).stream().filter(e -> e.kind().contains("outpost")).count());
                if (countKey(p, "outpost") == 0) finding("entering an outpost raises no Messenger or journal line (no visit event in the build/outpost package)");
                ev = 6;
                snap("m9_outpost_visit", 70);
            }
            case 6 -> {
                WolfEntity w6 = wolf();
                log("WOLF at the outpost: " + (w6 == null ? "not loaded there" : String.format(Locale.ROOT, "%.1f m from the player after the arrival (a Companion past 48 m teleports to its owner)", w6.distanceTo(p))));
                if (w6 != null) w6.teleportTo(ax + 3, padY, az - 6); // the pair walk home together; the bot moves both
                p.teleportTo(level, ax + 3.5, padY + 1, az + 3.5, 0, 10); // home, indoors
                artifact(300);
                log("WALK home (teleported, indoors)");
                ev = 7;
                wait = 20;
            }
            case 7 -> {
                if (mm < 38) return; // midnight (+33 min) passes with the player home, the stovehouse inside the crop pass's reach; the radio waits until the client has meshed the home chunks again
                radio(p);
                ev = 8;
                snap("m9_radio_moment", 14);
            }
            case 8 -> {
                if (mm < 52) return;
                log("CHECK at +" + mm + " min: stored feed " + f(stored()) + " kg, crop units " + f(unitsSum()) + ", stoves lit " + (stove(stoveA) != null && stove(stoveA).lit()) + "/" + (stove(stoveB) != null && stove(stoveB).lit()));
                ev = 9;
            }
            default -> { }
        }
    }

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

    private static void radio(ServerPlayer p) {
        // inside, at the desk
        cam(p, new Vec3(siteA.getX() + 2.5, padY + 1, siteA.getZ() + 4.2), new Vec3(deskPos.getX() + 0.5, deskPos.getY() + 0.6, deskPos.getZ() + 0.5));
        var be = (RadioDeskBlockEntity) level.getBlockEntity(deskPos);
        if (be == null) { fail("no radio desk block entity"); return; }
        radioBattery = be.battery();
        int cranks = 0;
        while (be.battery() < 12 && cranks < 40) { // sked_cost_pct is read from the config; crank by use until a sked can be afforded
            p.setShiftKeyDown(true);
            p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(deskPos), Direction.UP, deskPos, false));
            p.setShiftKeyDown(false);
            cranks++;
        }
        log(String.format(Locale.ROOT, "RADIO battery %.0f%% at the desk, %d crank uses with sneak -> %.0f%% (a real crank is a held job: not paced here)", radioBattery, cranks, be.battery()));
        int jBefore = Journal.entries(p).size(), cBefore = countKey(p, "radio");
        p.gameMode.useItemOn(p, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(deskPos), Direction.UP, deskPos, false));
        radioFired = com.yearofice.story.radio.Radio.working(p);
        log("RADIO tuned in: working=" + radioFired + " (was " + radioBefore + "), battery now " + f(be.battery()) + "%, journal +" + (Journal.entries(p).size() - jBefore)
                + ", HudBus radio lines +" + (countKey(p, "radio") - cBefore));
        for (String line : com.yearofice.story.radio.Radio.skedLines(level, deskPos, ClockService.get().nowMinute())) log("  sked: " + line);
        if (countKey(p, "radio") - cBefore == 0) finding("the radio sked is spoken with sendSystemMessage straight to chat, not through HudBus/Messenger (Layer 8 says every system word goes through the bus)");
    }

    // ---- crossings: every vital change must have a Messenger line ----------------------------------------------------

    private static int countKey(ServerPlayer p, String key) {
        int n = 0;
        for (var l : HudBus.get().journal(p)) if (l.key().contains(key)) n++;
        for (var l : HudBus.get().sent(p)) if (l.key().contains(key)) n++;
        for (var l : HudBus.get().held(p)) if (l.key().contains(key)) n++;
        return n;
    }

    private static void track(ServerPlayer p) {
        if (S < 0 && st < 1) return;
        BodyState s = BodyAttachments.of(p);
        long mn = ClockService.get().nowMinute();
        boolean[] now = {s.warmth < 70, s.water < 60, s.energy < 5, s.condition() < 60, s.balance24h() < -1000};
        String[] labels = {"warmth_low", "water_low", "energy_low", "condition_low", "calories_low"};
        Map<String, Integer> prev = prevCounts;
        prevCounts = new HashMap<>();
        for (String k : new String[] {"warmth_low", "water_low", "energy_low", "condition_low", "calories_low", "part_worse", "frost", "digit_lost"}) prevCounts.put(k, countKey(p, k));
        if (was != null) {
            for (int i = 0; i < now.length; i++) if (now[i] && !was[i]) add(mn, labels[i], labels[i], prev.getOrDefault(labels[i], 0));
            for (Part part : Part.values()) {
                int bnd = s.parts.band(part).ordinal();
                if (bnd > wasBand[part.ordinal()]) add(mn, "part_worse:" + part, "part_worse", prev.getOrDefault("part_worse", 0));
                int fs = com.yearofice.sim.body.FrostModel.stage(s.parts.frost(part));
                if (fs > wasFrost[part.ordinal()]) add(mn, "frost_stage" + fs + ":" + part, "frost", prev.getOrDefault("frost", 0));
            }
            int dg = Integer.bitCount(s.parts.lostDigits()[0]) + Integer.bitCount(s.parts.lostDigits()[1]);
            if (dg > wasDigits) add(mn, "digit_lost", "digit_lost", prev.getOrDefault("digit_lost", 0));
        }
        was = now;
        for (Part part : Part.values()) { wasBand[part.ordinal()] = s.parts.band(part).ordinal(); wasFrost[part.ordinal()] = com.yearofice.sim.body.FrostModel.stage(s.parts.frost(part)); }
        wasDigits = Integer.bitCount(s.parts.lostDigits()[0]) + Integer.bitCount(s.parts.lostDigits()[1]);
        for (var it = pend.iterator(); it.hasNext();) {
            Pend q = it.next();
            if (tick < q.due) continue;
            it.remove();
            if (countKey(p, q.key) > q.before) covered++;
            else {
                unmessaged.add(clock(q.minute) + " " + q.label);
                StringBuilder ks = new StringBuilder();
                for (var l : HudBus.get().journal(p)) ks.append(l.key()).append(' ');
                log("UNMESSAGED vital change at " + clock(q.minute) + ": " + q.label + " ; journal keys: " + ks);
            }
        }
    }

    private static void add(long mn, String label, String key, int before) {
        crossings++;
        pend.add(new Pend(mn, label, key, before, tick + 60));
        log("  crossing at " + clock(mn) + ": " + label);
    }

    // ---- the end ------------------------------------------------------------------------------------------------

    private static void endHour(ServerPlayer p) {
        sampling = false;
        log("HOUR END " + clock(ClockService.get().nowMinute()) + " (ran " + m() + " game min in " + (tick - startTick) + " ticks)");
        if (m() < 59) fail("the hour did not complete: " + m() + " game min in " + (tick - startTick) + " ticks");
        storedEnd = stored();
        unitsEndSum = unitsSum();
        BodyState b = BodyAttachments.of(p);
        bodyEnd = String.format(Locale.ROOT, "warmth %.0f water %.0f energy %.1f kg %.1f bmi %.2f", b.warmth, b.water, b.energy, b.kg, b.bmi());
        finish(p);
    }

    private static double pct(List<Double> l, double q) {
        if (l.isEmpty()) return -1;
        List<Double> s = new ArrayList<>(l);
        java.util.Collections.sort(s);
        return s.get(Math.min(s.size() - 1, (int) Math.floor(q * s.size())));
    }

    private static long dirSize(Path root, java.util.function.Predicate<Path> take) {
        long[] t = {0};
        try (var w = Files.walk(root)) { w.filter(Files::isRegularFile).filter(take).forEach(f -> { try { t[0] += Files.size(f); } catch (IOException ignored) { } }); } catch (IOException ignored) { }
        return t[0];
    }

    private static long homesteadNbtBytes() {
        long[] t = {0};
        var ra = level.registryAccess();
        for (BlockPos q : BlockPos.betweenClosed(siteA.offset(-3, -3, -10), siteA.offset(31, 12, 12))) {
            var be = level.getBlockEntity(q);
            if (be == null) continue;
            t[0] += nbtBytes(be.saveWithFullMetadata(ra));
        }
        var all = new ArrayList<net.minecraft.world.entity.Entity>(flock());
        if (wolf() != null) all.add(wolf());
        for (var e : all) t[0] += nbtBytes(e.saveWithoutId(new net.minecraft.nbt.CompoundTag()));
        return t[0];
    }

    private static long nbtBytes(net.minecraft.nbt.CompoundTag tag) {
        try {
            var bo = new java.io.ByteArrayOutputStream();
            net.minecraft.nbt.NbtIo.writeCompressed(tag, bo);
            return bo.size();
        } catch (IOException ex) {
            return 0;
        }
    }

    private static void finish(ServerPlayer p) {
        sampling = false;
        ClockService.get().forceRate(null);
        log("");
        // --- assertions
        if (S >= 0 && m() >= 59) {
            // wolf
            WolfEntity wolf = wolf();
            log(String.format(Locale.ROOT, "WOLF follow: start %.1f m, closest %.1f m, after 10 s %.1f m; wolf loaded at the end %s", wolfD0, wolfDmin, wolfDend, wolf != null));
            if (wolfDend < 0 || wolfDend > 13) fail(String.format(Locale.ROOT, "the wolf did not follow: %.1f m after 10 s from %.1f m", wolfDend, wolfD0));
            if (wolf == null) {
                var near = level.getEntitiesOfClass(WolfEntity.class, new net.minecraft.world.phys.AABB(p.position(), p.position()).inflate(96));
                fail("the wolf is gone or unloaded at the end of the hour; wolves within 96 m: " + near.size() + ", away record " + com.yearofice.ecosystem.companion.Companions.away(p));
            }
            // livestock
            double eaten = stored0 - storedEnd;
            StringBuilder as = new StringBuilder();
            int alive = 0;
            var herd = flock();
            alive = herd.size();
            for (var a : herd) if (a instanceof Livestock l) as.append(String.format(Locale.ROOT, "%s fed %.0f watered %.0f feed_kg %.2f debt %.2f | ", a.getType().builtInRegistryHolder().key().location().getPath(), l.core().care.fed(), l.core().care.watered(), l.core().feedKg, l.core().feedDebtKg));
            log(String.format(Locale.ROOT, "LIVESTOCK stored feed %.2f -> %.2f kg (%.2f kg eaten), %d/%d found alive and loaded; %s", stored0, storedEnd, eaten, alive, flockIds.size(), as));
            if (eaten <= 0.001) fail("livestock ate nothing in the hour (" + f(stored0) + " -> " + f(storedEnd) + " kg)");
            if (alive != flockIds.size()) fail("a kept animal is dead or missing at the end: " + alive + "/" + flockIds.size());
            // greens
            StringBuilder cs = new StringBuilder();
            for (BlockPos c : beds) if (level.getBlockEntity(c) instanceof CropBlockEntity be) cs.append(be.blockId().replace("yearofice:", "")).append(" units ").append(f(be.units())).append(" stage ").append(be.stage()).append(" up ").append(be.up()).append(" lastDone ").append(be.lastDone()).append(" | ");
            log(String.format(Locale.ROOT, "GREENS crop units %.3f -> %.3f; %s", units0Sum, unitsEndSum, cs));
            Room rb = RoomService.get().roomAt(level, siteB.offset(3, 1, 3));
            log("  stovehouse room at the end: " + (rb == null ? "none" : roomLine(rb)));
            if (unitsEndSum <= units0Sum + 1e-9) fail(String.format(Locale.ROOT, "greens did not grow: %.3f -> %.3f units across the midnight day tick", units0Sum, unitsEndSum));
            // radio
            if (!radioFired) fail("the radio sked did not fire (Radio.working false)");
            boolean jr = Journal.entries(p).stream().anyMatch(e -> e.kind().equals("radio"));
            log("RADIO journal entry present: " + jr);
            if (!jr) fail("no radio journal entry");
            if (opOrigin == null) fail("no outpost visited");
        } else {
            fail("the hour never completed");
        }
        // vitals
        log(String.format(Locale.ROOT, "VITALS start [%s] end [%s]; minima warmth %.0f water %.0f energy %.1f condition %.0f; crossings %d, with a Messenger line %d, without %d", bodyStart, bodyEnd, minWarmth, minWater, minEnergy, minCond,
                crossings, covered, unmessaged.size()));
        long frostMiss = unmessaged.stream().filter(u -> u.contains("frost_stage")).count();
        log("  of the " + unmessaged.size() + " unmessaged, " + frostMiss + " are frost-stage steps and " + (unmessaged.size() - frostMiss) + " are strip icons, part bands or lost digits (the three kinds VitalExplainer posts)");
        if (frostMiss > 0) finding("frost progression has no Messenger line past the first 'Frostnip on your hands, feet or face' (once per 10 game minutes): " + frostMiss + " frost-stage steps on torso, arms, legs, head, feet and hands went unannounced in the hour (VitalExplainer only posts strip icons, part bands and lost digits)");
        if (!unmessaged.isEmpty()) fail("vital changes without a Messenger line: " + unmessaged.size() + " (" + frostMiss + " frost-stage, " + (unmessaged.size() - frostMiss) + " other) " + unmessaged);
        if (sawSos) fail("collapse");
        log("MESSENGER device: battery " + com.yearofice.api.hud.MessengerAccess.battery(p) + " (lines reach the screen only above 0; the journal keeps the rest)");
        // --- MSPT
        double avgAll = msptAll.stream().mapToDouble(Double::doubleValue).average().orElse(-1), avgClean = msptClean.stream().mapToDouble(Double::doubleValue).average().orElse(-1);
        double maxAll = msptAll.stream().mapToDouble(Double::doubleValue).max().orElse(-1), maxClean = msptClean.stream().mapToDouble(Double::doubleValue).max().orElse(-1);
        log(String.format(Locale.ROOT, "MSPT all %d ticks: avg %.2f p50 %.2f p95 %.2f max %.2f, over 50 ms: %d", msptAll.size(), avgAll, pct(msptAll, 0.5), pct(msptAll, 0.95), maxAll, over50));
        log(String.format(Locale.ROOT, "MSPT clean (driver teleports/chunk loads excluded) %d ticks: avg %.2f p50 %.2f p95 %.2f max %.2f, over 50 ms: %d (budget: avg < 30, never > 50)", msptClean.size(), avgClean, pct(msptClean, 0.5), pct(msptClean, 0.95), maxClean, over50Clean));
        for (String w : worst) log("  clean tick over 50 ms: " + w);
        if (avgClean > 30) fail(String.format(Locale.ROOT, "MSPT average %.2f ms over the 30 ms budget", avgClean));
        if (over50Clean > 0) fail("MSPT over 50 ms on " + over50Clean + " clean ticks (max " + f(maxClean) + ")");
        // --- save
        try {
            srv.saveEverything(true, true, true);
            Path root = srv.getWorldPath(LevelResource.ROOT);
            long total = dirSize(root, f -> true);
            long chunks = dirSize(root, f -> { String s = f.toString().replace('\\', '/'); return s.contains("/region/") || s.contains("/entities/") || s.contains("/poi/"); });
            long data = dirSize(root.resolve("data"), f -> true), players = dirSize(root.resolve("playerdata"), f -> true);
            long mod = total - chunks;
            long homestead = homesteadNbtBytes(), chunkFiles = 0;
            try (var w = Files.walk(root)) { chunkFiles = w.filter(Files::isRegularFile).filter(f -> f.toString().endsWith(".mca")).count(); } catch (IOException ignored) { }
            log(String.format(Locale.ROOT, "SAVE homestead NBT (block entities in the yard box + the wolf and the flock, compressed) %.1f KB; %d region-file chunks sets (.mca files)", homestead / 1e3, chunkFiles));
            mod += homestead;
            log(String.format(Locale.ROOT, "SAVE world folder %.2f MB total; chunk files (region/entities/poi) %.2f MB; everything else %.2f MB (data/ %.0f KB, playerdata/ %.0f KB)", total / 1e6, chunks / 1e6, mod / 1e6, data / 1e3, players / 1e3));
            try (var w = Files.walk(root)) {
                w.filter(Files::isRegularFile).sorted((a, c) -> Long.compare(c.toFile().length(), a.toFile().length())).limit(8).forEach(f -> log("  largest: " + root.relativize(f) + " " + f.toFile().length() / 1000 + " KB"));
            }
            if (mod >= 5_000_000) fail(String.format(Locale.ROOT, "mod save data %.2f MB >= 5 MB", mod / 1e6));
            log(String.format(Locale.ROOT, "SAVE mod save data = data/ + playerdata/ + level.dat + the homestead's NBT = %.3f MB (chunk files carry terrain; the five-year test line is mod save data)", mod / 1e6));
            if (total >= 5_000_000) log("NOTE the whole world folder is " + String.format(Locale.ROOT, "%.2f", total / 1e6) + " MB, over 5 MB, because of generated terrain in the region files; it is not mod save data");
        } catch (RuntimeException | IOException ex) {
            fail("save measurement failed: " + ex);
        }
        finding("SERVER HANG (seen in an earlier run of this driver, pole-fence pen): FenceDrift.onChunkLoad (ChunkEvent.Load, non-new chunk, winter) calls build(), which reads up to fence_drift_reach (5-10) blocks downwind with Level.getBlockState; across a chunk edge that is a blocking getChunk on the server thread inside the chunk's own FULL-status task, so the server thread parks forever in ServerChunkCache.managedBlock (stack: FenceDrift.build:64 <- settle:101 <- onChunkLoad:109). Any fence or wall within ~10 blocks of a chunk edge hangs the world when its chunk reloads. This run uses log walls, which the yearofice:fences tag does not list.");
        log("GRANTED (no honest path or shortcut taken): " + grants);
        log("DESIGN FINDINGS: " + findings.size());
        for (String s : findings) log("  - " + s);
        String res = fails.isEmpty() ? "PASS" : "FAIL";
        log("RESULT " + res + (fails.isEmpty() ? "" : " " + fails));
        verdict = "RESULT " + res;
        done = true;
    }
}

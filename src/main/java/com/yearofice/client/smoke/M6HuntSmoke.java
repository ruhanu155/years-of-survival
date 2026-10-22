package com.yearofice.client.smoke;

import com.yearofice.api.build.GateService;
import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.build.craft.CraftRegistry;
import com.yearofice.build.craft.StationCraft;
import com.yearofice.build.wood.ChoppingBlockEntity;
import com.yearofice.build.wood.FellingService;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.build.wood.WoodJobs;
import com.yearofice.build.wood.WoodRegistry;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkService;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.client.horizon.PresetDefaults;
import com.yearofice.core.YearOfIce;
import com.yearofice.ecosystem.YoiAnimal;
import com.yearofice.ecosystem.carcass.Butchering;
import com.yearofice.ecosystem.carcass.CarcassEntity;
import com.yearofice.ecosystem.carcass.WholeGameItem;
import com.yearofice.ecosystem.species.MooseEntity;
import com.yearofice.fire.FireRegistry;
import com.yearofice.hunt.BloodTrail;
import com.yearofice.hunt.BowItem;
import com.yearofice.hunt.HuntConfig;
import com.yearofice.hunt.HuntDebug;
import com.yearofice.hunt.HuntRegistry;
import com.yearofice.hunt.sign.SignService;
import com.yearofice.hunt.wound.WoundTracker;
import com.yearofice.sim.build.Tier;
import com.yearofice.sim.hunt.Ballistics;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.world.logs.FelledLogEntity;
import com.yearofice.worldgen.block.WorldBlocks;
import com.yearofice.worldgen.block.YoiLogBlock;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The M6 human gate's stand-in ({@code YEAROFICE_SMOKE=m6hunt}; never active otherwise): "a first moose using only
 * sign, wind and the blood trail". A survival player, no {@code /yoi} command after setup (a CommandEvent counter
 * asserts it). The bow chain is earned in play: gravel for rough stone and flint, a knapped arrowhead, a felled and
 * split birch for the stave, sinew and feathers from small game killed by hand, then bow, arrows. Then the hunt: read
 * moose sign (SignService), take the downwind side (EnvService.wind), close to bow range, draw and loose, follow the
 * blood trail, dress the carcass. Every step is a ledger line naming the api it used; a step with no honest path is a
 * GRANT line with the reason and the run goes on (that is the finding). Teleports stand in for walking (ledgered).
 * Log {@code smoke/m6hunt.log}; the client half grabs the shots named in {@link #shot}.
 */
@EventBusSubscriber(modid = YearOfIce.MOD_ID, value = Dist.CLIENT)
public final class M6HuntSmoke {
    private M6HuntSmoke() {}

    public static final boolean ON = "m6hunt".equals(System.getenv("YEAROFICE_SMOKE"));
    public static volatile boolean done = false;
    public static volatile String shot = null;
    public static volatile boolean shotBusy = false;
    public static volatile String verdict = "";

    static final double WALK_MS = 1.2;
    static final int HATCHET = 8, WORK = 7, BOWSLOT = 0;

    private enum Ph { INIT, GRAVEL_SCAN, GRAVEL_DIG, FIRE, KNAP, TREES, WOOD, BENCH, GAME, BOWCHAIN, SCOUT, SIGN, FIND, STALK, DRAW, AFTER, TRAIL, BUTCHER, END }

    private static Ph ph = Ph.INIT;
    private static int tick = 0, wait = 0, sub = 0;
    private static PrintWriter out;
    private static ServerLevel level;
    private static BlockPos spawn;
    private static final List<String> fails = new ArrayList<>(), ledger = new ArrayList<>(), findings = new ArrayList<>();
    private static final Map<String, Integer> grants = new LinkedHashMap<>();
    private static int steps = 0, commands = 0;
    private static boolean setupOver = false;
    private static double walkMin = 0, craftMin = 0;

    // gravel
    private static final List<BlockPos> gravel = new ArrayList<>();
    private static int gravelIx = 0, gravelBroken = 0;
    // fire and bench
    private static BlockPos shop, benchPos;
    // wood
    private static List<BlockPos> trees = new ArrayList<>(), bestTrees = new ArrayList<>();
    private static int treeHop = 0, treeIx = 0, treesTried = 0, treeStage = 0;
    private static BlockPos curBase, chopBlock;
    private static WoodJobs.Fell curFell;
    private static FelledLogEntity curLog;
    private static final Set<Integer> usedLogs = new HashSet<>();
    private static final List<ItemStack> roundPile = new ArrayList<>(), splitPile = new ArrayList<>();
    private static int splitAlt = 0;
    // small game
    private static int gameTries = 0, gameHop = 0;
    // hunt
    private static java.util.UUID mooseId;
    private static BlockPos signPos, firstSignPos;
    private static int scoutHop = 0, followHop = 0, drawStart = -1, shotsFired = 0, approachTicks = 0, mooseAttempts = 0;
    private static Vec3 signHeading = Vec3.ZERO, impact;
    private static final List<BlockPos> visited = new ArrayList<>();
    private static BlockPos curPatch;
    private static CarcassEntity carcass;
    private static int trailWaits = 0, trailStepTicks = 0, patchesFollowed = 0, shotWait = 0;
    private static boolean redraw = false, shotShown = false, signShown = false, windLogged = false, tookMoose = false;
    private static int arrowsBefore = 0;
    private static String lastOutcome = "none";
    private static double startMin;

    static String f(double v) { return String.format(Locale.ROOT, "%.1f", v); }

    static void log(String s) { if (out != null) { out.println(s); out.flush(); } YearOfIce.LOG.info("M6HUNT {}", s); }

    static void step(String what, String api) { steps++; String l = "STEP " + steps + " [" + api + "] " + what; ledger.add(l); log(l); }

    static void grant(String what, int n, String why) { grants.merge(what, n, Integer::sum); log("GRANT " + what + " x" + n + ": " + why); }

    static void finding(String s) { findings.add(s); log("FINDING " + s); }

    static void fail(String s) { fails.add(s); log("FAIL " + s); }

    static Item item(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.parse("yearofice:" + id)); }

    @SubscribeEvent
    public static void onCommand(CommandEvent e) {
        if (!ON || !setupOver) return;
        commands++;
        log("COMMAND after setup: " + e.getParseResults().getReader().getString());
    }

    // ---- server tick ----------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (!ON || done || !ClockService.installed() || !HudBus.installed()) return;
        var server = e.getServer();
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        ServerPlayer p = server.getPlayerList().getPlayers().get(0);
        tick++;
        if (tick < 200) return;
        try {
            if (shotBusy) { if (++sub > 1500) shotBusy = false; return; }
            if (wait > 0) { wait--; return; }
            run(p, server);
        } catch (RuntimeException ex) {
            YearOfIce.LOG.error("M6 hunt driver failed", ex);
            if (out == null) { done = true; verdict = "FAIL driver exception " + ex; return; }
            finding("phase " + ph + " threw " + ex + " at " + java.util.Arrays.toString(java.util.Arrays.copyOf(ex.getStackTrace(), 4)) + "; skipping to the next phase");
            sub = 0;
            ph = Ph.values()[Math.min(Ph.values().length - 1, ph.ordinal() + 1)];
        }
    }

    private static void snap(String name, int settle) { shotBusy = true; sub = 0; shot = name; wait = settle; }

    private static void run(ServerPlayer p, net.minecraft.server.MinecraftServer server) {
        switch (ph) {
            case INIT -> init(p, server);
            case GRAVEL_SCAN -> gravelScan(p);
            case GRAVEL_DIG -> gravelDig(p);
            case FIRE -> fire(p);
            case KNAP -> knap(p);
            case TREES -> findTrees(p);
            case WOOD -> wood(p);
            case BENCH -> bench(p);
            case GAME -> game(p);
            case BOWCHAIN -> bowChain(p);
            case SCOUT -> scout(p);
            case SIGN -> signStep(p);
            case FIND -> findMoose(p);
            case STALK -> stalk(p);
            case DRAW -> draw(p);
            case AFTER -> after(p);
            case TRAIL -> trail(p);
            case BUTCHER -> butcher(p);
            case END -> finish(p);
        }
    }

    // ---- helpers ----------------------------------------------------------------------------------------------------

    private static void hold(ServerPlayer p, int slot, ItemStack s) { p.getInventory().setItem(slot, s); p.getInventory().selected = slot; }

    private static int count(ServerPlayer p, java.util.function.Predicate<ItemStack> pr) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) { ItemStack s = p.getInventory().getItem(i); if (!s.isEmpty() && pr.test(s)) n += s.getCount(); }
        return n;
    }

    private static int have(ServerPlayer p, String id) { Item it = item(id); return count(p, s -> s.is(it)); }

    private static void give(ServerPlayer p, ItemStack s) { ItemStack c = s.copy(); if (!p.getInventory().add(s)) { log("  PACK FULL: " + c + " dropped at the feet"); p.drop(s, false); } }

    private static void giveGrant(ServerPlayer p, Item it, int n) { give(p, new ItemStack(it, n)); }

    private static void remove(ServerPlayer p, Item it, int n) {
        for (int i = 0; i < p.getInventory().getContainerSize() && n > 0; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(it)) { int k = Math.min(n, s.getCount()); s.shrink(k); n -= k; }
        }
    }

    private static int ground(int x, int z) { level.getChunk(x >> 4, z >> 4); return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z); }

    /** A walk ledgered as game minutes (the box teleports; a real walk is the distance over 1.2 m/s). */
    private static void walkTo(ServerPlayer p, double x, double z, String why) {
        double d = Math.hypot(x - p.getX(), z - p.getZ());
        double m = d / WALK_MS / 60.0;
        walkMin += m;
        int xi = (int) Math.floor(x), zi = (int) Math.floor(z);
        p.teleportTo(level, x, ground(xi, zi), z, p.getYRot(), p.getXRot());
        ClockService.get().advance((long) Math.ceil(m));
        log("WALK " + why + ": " + f(d) + " m = " + f(m) + " game min (teleported)");
    }

    private static void cam(ServerPlayer p, Vec3 at, Vec3 look) {
        double dx = look.x - at.x, dz = look.z - at.z, dy = look.y - at.y;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        p.teleportTo(level, at.x, at.y, at.z, yaw, pitch);
    }

    private static void sweepItems(ServerPlayer p, Vec3 c, double r, java.util.function.Predicate<ItemStack> pr) {
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(r))) {
            ItemStack st = ie.getItem();
            if (!pr.test(st)) continue;
            give(p, st.copy());
            ie.discard();
        }
    }

    /** Crafts one {@code id} through its station recipe; a missing ingredient is a GRANT (the shortfall), a refusal grants the result. */
    private static boolean craft(ServerPlayer p, String id) {
        Item it = item(id);
        ResourceLocation rid = null;
        for (var h : StationCraft.all(level)) if (h.value().resultItem() == it) { rid = h.id(); break; }
        if (rid == null) { grant(id, 1, "no station recipe produces it"); giveGrant(p, it, 1); return false; }
        var holder = StationCraft.byId(level, rid);
        for (var need : holder.value().ingredients()) {
            ItemStack[] opts = need.ingredient().getItems();
            if (opts.length == 0) continue;
            int hv = StationCraft.count(p.getInventory(), need);
            if (hv < need.count()) {
                String nm = BuiltInRegistries.ITEM.getKey(opts[0].getItem()).toString();
                grant(nm, need.count() - hv, "ingredient of " + id + " with no obtainable source found by this driver");
                give(p, opts[0].copyWithCount(need.count() - hv));
            }
        }
        p.setShiftKeyDown(false);
        var job = StationCraft.start(p, rid, benchPos);
        if (job == null) {
            var pl = StationCraft.plan(p, holder, StationCraft.stations(p, benchPos));
            finding("craft refused " + id + ": " + pl.problem() + " (stations " + StationCraft.stations(p, benchPos) + ")");
            grant(id, 1, "StationCraft refused: " + pl.problem());
            giveGrant(p, it, holder.value().result().getCount());
            return false;
        }
        int t = 0;
        while (!StationCraft.advance(p, job, com.yearofice.build.work.WorkService.dsPerTick()) && t++ < 400000) { }
        double m = t * WorkService.dsPerTick() / 600.0;
        craftMin += m;
        step("craft " + id + " x" + holder.value().result().getCount() + " via recipe " + rid + " (" + f(m) + " game min of work)", "StationCraft.start/advance");
        return true;
    }

    // ---- init -------------------------------------------------------------------------------------------------------

    private static void init(ServerPlayer p, net.minecraft.server.MinecraftServer server) {
        try {
            var f = server.getServerDirectory().resolve("smoke/m6hunt.log");
            Files.createDirectories(f.getParent());
            out = new PrintWriter(Files.newBufferedWriter(f));
        } catch (IOException ex) { throw new IllegalStateException(ex); }
        level = p.serverLevel();
        spawn = p.blockPosition();
        p.setGameMode(GameType.SURVIVAL);
        p.setInvulnerable(true); // camera and walk teleports must not kill the scripted survivor; no hunt is decided by it
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
        int hi = -1;
        for (int i = 0; i < p.getInventory().getContainerSize() && hi < 0; i++) if (p.getInventory().getItem(i).is(item("hatchet"))) hi = i;
        if (hi >= 0) {
            ItemStack h = p.getInventory().getItem(hi).copy();
            ItemStack displaced = p.getInventory().getItem(HATCHET).copy();
            p.getInventory().setItem(hi, ItemStack.EMPTY);
            p.getInventory().setItem(HATCHET, h);
            if (!displaced.isEmpty() && hi != HATCHET) { if (hi < 36) p.getInventory().setItem(hi, displaced); else p.getInventory().add(displaced); }
        } else fail("no hatchet in the crate kit");
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty()) log("  kit: " + BuiltInRegistries.ITEM.getKey(st.getItem()) + " x" + st.getCount());
        }
        ClockService.get().forceRate(null);
        startMin = ClockService.get().nowMinute();
        stashKit(p);
        step("survival start, crate kit opened (" + moved + " stacks), hatchet in slot 9, start minute " + startMin + ", commands off", "BarrelBlockEntity loot (crate kit)");
        setupOver = true;
        ph = Ph.GRAVEL_SCAN;
    }

    /** The crate backpack goes on the back first: wearing it opens the pack rows (the kit lists it; a player puts it on). */
    private static void wearPack(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty() || !BuiltInRegistries.ITEM.getKey(st.getItem()).getPath().equals("crate_backpack")) continue;
            if (com.yearofice.item.inventory.PackSlots.setBack(p, st.copy())) {
                p.getInventory().setItem(i, ItemStack.EMPTY);
                com.yearofice.item.inventory.PackSync.refresh(p);
                log("  put on the crate backpack: pack rows open");
            } else log("  could not put on the crate backpack (no back slot)");
            return;
        }
        log("  no crate backpack in the kit");
    }

    /** The kit's cooking and camp gear goes into a barrel at the start so the pack has slots for what the hunt makes (logged). */
    private static void stashKit(ServerPlayer p) {
        wearPack(p);
        Set<String> keep = Set.of("hatchet", "fixed_blade_knife", "cord", "match", "bear_spray", "crate_backpack");
        BlockPos at = spawn.east(2);
        at = new BlockPos(at.getX(), ground(at.getX(), at.getZ()), at.getZ());
        level.setBlock(at, Blocks.BARREL.defaultBlockState(), 3);
        int n = 0;
        if (level.getBlockEntity(at) instanceof BarrelBlockEntity b) {
            for (int i = 0; i < p.getInventory().getContainerSize() && n < b.getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (st.isEmpty() || st.getItem() instanceof net.minecraft.world.item.ArmorItem) continue;
                String path = BuiltInRegistries.ITEM.getKey(st.getItem()).getPath();
                if (keep.contains(path) || path.endsWith("boots") || path.endsWith("jacket") || path.endsWith("shell") || path.endsWith("pants") || path.endsWith("socks") || path.endsWith("beanie") || path.endsWith("gloves")) continue;
                b.setItem(n++, st.copy());
                p.getInventory().setItem(i, ItemStack.EMPTY);
            }
        }
        int free = 0;
        for (int i = 0; i < p.getInventory().items.size(); i++) if (p.getInventory().items.get(i).isEmpty()) free++;
        log("STASH " + n + " kit stacks (cooking pot, seeds, panel, lamp, guide ...) left in a barrel at " + at.toShortString() + "; free pack slots " + free);
    }

    // ---- 1. gravel -> rough stone and flint ---------------------------------------------------------------------------

    private static void gravelScan(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        for (int dx = -110; dx <= 110; dx++) for (int dz = -110; dz <= 110; dz++) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
            for (int k = 0; k < 3; k++) {
                BlockPos q = new BlockPos(x, y - k, z);
                if (level.getBlockState(q).is(WorldBlocks.GRAVEL.get())) { gravel.add(q); break; }
            }
        }
        gravel.sort(java.util.Comparator.comparingDouble(b -> b.distSqr(c)));
        step("looked for gravel bars within 110 m of the start: " + gravel.size() + " gravel blocks on the surface", "level scan (what the eye sees)");
        if (gravel.isEmpty()) {
            grant("rough_stone", 1, "no gravel within 110 m of the start in the loaded valley");
            grant("flint_nodule", 2, "no gravel within 110 m of the start in the loaded valley");
            giveGrant(p, item("rough_stone"), 1);
            giveGrant(p, item("flint_nodule"), 2);
            ph = Ph.TREES;
        } else ph = Ph.GRAVEL_DIG;
    }

    private static final int FLINT_WANT = 3, DIG_CAP = 2500;

    private static void gravelDig(ServerPlayer p) {
        hold(p, 6, ItemStack.EMPTY);
        p.getInventory().selected = 6;
        for (int n = 0; n < 12; n++) {
            if (have(p, "flint_nodule") >= FLINT_WANT && have(p, "rough_stone") >= 1 || gravelIx >= gravel.size() || gravelBroken >= DIG_CAP) {
                step("dug " + gravelBroken + " gravel blocks by hand: " + have(p, "rough_stone") + " rough stone, " + have(p, "flint_nodule") + " flint nodules", "block break, loot_table/blocks/gravel.json");
                if (have(p, "flint_nodule") < FLINT_WANT) {
                    int g = FLINT_WANT - have(p, "flint_nodule");
                    grant("flint_nodule", g, "gravel's 1% flint chance gave too few nodules from " + gravelBroken + " blocks (" + gravel.size() + " within reach)");
                    giveGrant(p, item("flint_nodule"), g);
                }
                if (have(p, "rough_stone") < 1) {
                    grant("rough_stone", 1, "gravel's 10% rough-stone chance gave none from " + gravelBroken + " blocks");
                    giveGrant(p, item("rough_stone"), 1);
                }
                int extra = have(p, "rough_stone") - 2;
                if (extra > 0) { remove(p, item("rough_stone"), extra); log("  left " + extra + " surplus rough stones on the bar (the pack carries about 30 kg)"); }
                ph = Ph.TREES;
                return;
            }
            BlockPos g = gravel.get(gravelIx++);
            if (!level.getBlockState(g).is(WorldBlocks.GRAVEL.get())) continue;
            if (p.position().distanceToSqr(Vec3.atCenterOf(g)) > 16) {
                level.getChunk(g.getX() >> 4, g.getZ() >> 4);
                p.teleportTo(level, g.getX() + 0.5, g.getY() + 1, g.getZ() + 0.5, 0, 60);
            }
            boolean broke = p.gameMode.destroyBlock(g);
            gravelBroken++;
            int dropped = level.getEntitiesOfClass(ItemEntity.class, new AABB(g).inflate(3)).size();
            if (gravelBroken <= 4 || gravelBroken % 500 == 0) log("  dig " + gravelBroken + " at " + g.toShortString() + ": destroyBlock " + broke + ", block now " + level.getBlockState(g).getBlock() + ", item entities within 3 m " + dropped + ", held " + p.getMainHandItem());
            sweepItems(p, Vec3.atCenterOf(g), 3, s -> s.is(item("rough_stone")) || s.is(item("flint_nodule")));
        }
    }

    // ---- 2. fire, knapping -------------------------------------------------------------------------------------------

    private static void fire(ServerPlayer p) {
        BlockPos c = spawn;
        BlockPos g = null;
        for (int r = 2; r < 30 && g == null; r++) for (int a = 0; a < 360; a += 45) {
            int x = c.getX() + (int) Math.round(r * Math.cos(Math.toRadians(a))), z = c.getZ() + (int) Math.round(r * Math.sin(Math.toRadians(a)));
            int y = ground(x, z);
            BlockPos q = new BlockPos(x, y, z);
            boolean flat = true;
            for (int i = -1; i <= 4 && flat; i++) for (int j = -1; j <= 1 && flat; j++) {
                BlockPos t = q.offset(i, 0, j);
                flat = level.getBlockState(t).isAir() && level.getBlockState(t.below()).isFaceSturdy(level, t.below(), Direction.UP) && level.getFluidState(t.below()).isEmpty();
            }
            if (flat) { g = q; break; }
        }
        shop = g == null ? c : g;
        level.setBlock(shop, FireRegistry.FIRE_RING.get().defaultBlockState(), 3);
        p.teleportTo(level, shop.getX() + 1.5, shop.getY(), shop.getZ() + 0.5, 0, 0);
        p.setShiftKeyDown(false);
        if (!(level.getBlockEntity(shop) instanceof com.yearofice.fire.FireRingBlockEntity be)) { finding("no fire ring block entity at " + shop.toShortString()); ph = Ph.KNAP; return; }
        var t = com.yearofice.fire.FireConfig.tuning();
        if (have(p, "tinder_bundle") == 0) { grant("tinder_bundle", 1, "no recipe or gathering path for tinder was found by this driver (field note only)"); giveGrant(p, item("tinder_bundle"), 1); }
        be.addPart(new ItemStack(FireRegistry.TINDER_BUNDLE.get()), t, p);
        remove(p, FireRegistry.TINDER_BUNDLE.get(), 1);
        int kin = Math.min(2, have(p, "kindling"));
        if (kin < 2) { grant("kindling", 2 - kin, "limbing the felled tree gave " + kin); }
        for (int i = 0; i < 2; i++) be.addPart(new ItemStack(FireRegistry.KINDLING.get()), t, p);
        remove(p, FireRegistry.KINDLING.get(), kin);
        int logs = 0;
        for (int i = 0; i < p.getInventory().getContainerSize() && logs < 4; i++) {
            ItemStack st = p.getInventory().getItem(i);
            while (!st.isEmpty() && WoodItems.isSplit(st) && logs < 4) {
                if (be.addPart(st.copyWithCount(1), t, p).ok()) { st.shrink(1); logs++; } else break;
            }
        }
        if (logs < 4) { grant("log", 4 - logs, "the ring refused " + (4 - logs) + " splits as fuel"); for (; logs < 4; logs++) be.addPart(new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG), t, p); }
        if (have(p, "match") == 0) { grant("match", 1, "none in the kit"); giveGrant(p, item("match"), 1); }
        var lit = be.strikeMatch(p, new ItemStack(FireRegistry.MATCH.get()), 0.0);
        remove(p, FireRegistry.MATCH.get(), 1);
        be.puff();
        be.puff();
        step("laid tinder, kindling and " + logs + " splits in a fire ring at " + shop.toShortString() + " and struck a match: " + lit.line() + " (" + be.fire().state() + ")", "FireRingBlockEntity.addPart/strikeMatch (the ring's own api)");
        ph = Ph.KNAP;
        wait = 120; // the gate scan runs every 5 s
    }

    private static void knap(ServerPlayer p) {
        p.teleportTo(level, shop.getX() + 1.5, shop.getY(), shop.getZ() + 0.5, 0, 0);
        boolean open = GateService.get().isOpen(p, Tier.BUSHCRAFT);
        step("bushcraft gate open after the fire: " + open, "GateService.isOpen");
        if (!open) finding("the bushcraft gate did not open 6 s after a lit ring; crafts below are refused");
        craft(p, "flint_knife"); // the crate's fixed-blade knife is not in #yearofice:knives: recipes want a flint knife
        craft(p, "arrowhead"); // 4 heads per nodule
        if (have(p, "arrowhead") < 6 && have(p, "flint_nodule") > 0) craft(p, "arrowhead");
        ph = Ph.BENCH;
    }

    // ---- 3. wood: a birch for the stave, splits for the bench ------------------------------------------------------

    private static WorkStartPayload at(BlockPos pos, Direction face) {
        return new WorkStartPayload(true, pos, -1, (byte) face.get3DDataValue(), pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }

    private static WorkStartPayload at(net.minecraft.world.entity.Entity e) {
        return new WorkStartPayload(true, BlockPos.ZERO, e.getId(), (byte) 0, (float) e.getX(), (float) e.getY(), (float) e.getZ());
    }

    private static void findTrees(ServerPlayer p) {
        BlockPos c = p.blockPosition();
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Map<BlockPos, TreeKind> kinds = new java.util.HashMap<>();
        for (int dx = -90; dx <= 90; dx++) for (int dz = -90; dz <= 90; dz++) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            // our leaves are not LeavesBlock, so the no-leaves heightmap stops on the crown: walk down through it to the log
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos q = new BlockPos(x, y, z);
            int down = 0;
            while (!(level.getBlockState(q).getBlock() instanceof YoiLogBlock) && down++ < 24) {
                var st = level.getBlockState(q);
                if (!(st.isAir() || st.getBlock() instanceof com.yearofice.worldgen.block.YoiLeavesBlock || st.is(net.minecraft.world.level.block.Blocks.SNOW))) break;
                q = q.below();
            }
            if (!(level.getBlockState(q).getBlock() instanceof YoiLogBlock)) continue;
            while (level.getBlockState(q.below()).getBlock() instanceof YoiLogBlock) q = q.below();
            TrunkSearch.Trunk t = FellingService.trunk(level, q);
            if (t == null) continue;
            BlockPos base = FellingService.base(t);
            double dia = FellingService.diameterCm(t);
            if (!seen.add(base) || dia > 40 || dia < 8) continue;
            found.add(base);
            kinds.put(base, FellingService.kind(t));
        }
        found.sort(java.util.Comparator.<BlockPos>comparingInt(b -> kinds.get(b) == TreeKind.BIRCH ? 0 : 1).thenComparingDouble(b -> b.distSqr(c)));
        if (found.size() > bestTrees.size()) bestTrees = found.size() > 14 ? new ArrayList<>(found.subList(0, 14)) : found;
        trees = bestTrees;
        long birch = found.stream().filter(b -> kinds.get(b) == TreeKind.BIRCH).count();
        step("looked for trees of 8-40 cm within 90 m: " + found.size() + " (" + birch + " birch)", "FellingService.trunk scan");
        if (birch == 0 && treeHop < 4) {
            double ang = Math.toRadians(treeHop * 120 + 40);
            treeHop++;
            walkTo(p, spawn.getX() + 150 * Math.cos(ang), spawn.getZ() + 150 * Math.sin(ang), "no birch in sight; looking for a birch stand, hop " + treeHop);
            wait = 100;
            return;
        }
        if (birch == 0) finding("no birch of fellable size within 90 m of four spots around the start: the bow stave's birch_split has no source here");
        ph = Ph.WOOD;
        treeStage = 0;
    }

    private static void startTree(ServerPlayer p) {
        curFell = null;
        while (treeIx < trees.size() && curFell == null) {
            curBase = trees.get(treeIx++);
            var trunk = FellingService.trunk(level, curBase.above());
            if (trunk == null) continue;
            hold(p, HATCHET, p.getInventory().getItem(HATCHET));
            stand(p, curBase, 0);
            WorkJob j = WoodJobs.resolve(p, at(curBase, Direction.NORTH));
            if (j instanceof WoodJobs.Fell fell) {
                curFell = fell;
                treesTried++;
                treeStage = 1;
                log("TREE " + FellingService.kind(trunk) + " at " + curBase.toShortString() + " dia " + f(FellingService.diameterCm(trunk)) + " cm, " + fell.game().swings() + " swings");
            } else log("  tree " + curBase.toShortString() + " refused: " + j);
        }
        if (curFell == null) treeStage = 9;
    }

    private static void stand(ServerPlayer p, BlockPos base, double bearing) {
        double[] u = Felling.unit(bearing);
        int x = (int) Math.floor(base.getX() + 0.5 + u[0] * 2), z = (int) Math.floor(base.getZ() + 0.5 + u[1] * 2);
        p.teleportTo(level, x + 0.5, ground(x, z), z + 0.5, 0, 0);
    }

    private static int splitsHave() { return splitPile.size(); }

    private static void sweepWood(ServerPlayer p, Vec3 c) {
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(c, c).inflate(16))) {
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
    }

    private static void run(ServerPlayer p, WorkJob j, String kind) {
        double m = WorkService.runFor(p, j, 60_000);
        if (m < 0) log("  job " + kind + " did not finish");
    }

    private static boolean woodDone() {
        long birch = splitPile.stream().filter(s -> s.is(item("birch_split"))).count();
        return splitPile.size() >= 8 && birch >= 2 || treesTried >= 9 && splitPile.size() >= 6;
    }

    private static void wood(ServerPlayer p) {
        if (treeStage == 9 || woodDone()) {
            long birch = splitPile.stream().filter(s -> s.is(item("birch_split"))).count();
            int carry = 0;
            splitPile.sort(java.util.Comparator.comparingInt(x -> x.is(item("birch_split")) ? 0 : 1));
            for (ItemStack s : splitPile) if (carry < 8) { give(p, s); carry++; }
            if (splitPile.size() > carry) log("  " + (splitPile.size() - carry) + " splits left in a pile at the stump (the pack carries about 30 kg)");
            splitPile.clear();
            step("wood: " + treesTried + " trees felled, limbed, bucked and split with the crate hatchet; " + have(p, "birch_split") + " birch splits in the pack (" + birch + " cut), " + count(p, WoodItems::isSplit) + " splits in all", "WoodJobs Fell/Limb/Buck/Split via WorkService");
            if (have(p, "birch_split") < 2) {
                int g = 2 - have(p, "birch_split");
                grant("birch_split", g, "no birch split cut (stave and spear handle need birch); wood on hand is other species");
                ItemStack s = WoodItems.split(com.yearofice.sim.wood.WoodKind.BIRCH, g, 0.6, WoodItems.today());
                give(p, s);
            }
            ph = Ph.FIRE;
            return;
        }
        if (treeStage == 0) { startTree(p); return; }
        if (treeStage == 1) {
            if (!curFell.valid(p)) { log("  tree out of reach; skipped"); treeStage = 0; return; }
            int t1 = 0;
            while (!curFell.game().notchDone() && t1 < 3000 && curFell.valid(p)) { WorkService.runFor(p, curFell, 1); t1++; }
            stand(p, curBase, 180);
            WorkJob back = WoodJobs.resolve(p, at(curBase, Direction.SOUTH));
            int t2 = 0;
            while (back != null && !back.finished() && t2 < 3000 && back.valid(p)) { WorkService.runFor(p, back, 1); t2++; }
            if (back == null || !back.finished()) { log("  felling did not finish"); treeStage = 0; return; }
            treeStage = 2;
            sub = 0;
            wait = 5;
            return;
        }
        if (treeStage == 2) {
            var logs = level.getEntitiesOfClass(FelledLogEntity.class, new AABB(curBase).inflate(30), l -> !usedLogs.contains(l.getId()));
            if (logs.isEmpty()) { if (sub++ > 100) { log("  no log landed"); treeStage = 0; } wait = 5; return; }
            curLog = logs.get(0);
            usedLogs.add(curLog.getId());
            log(String.format(Locale.ROOT, "  log landed: %s %.1f m, %d rounds left", curLog.kind(), curLog.lengthM(), curLog.roundsLeft()));
            treeStage = 3;
            return;
        }
        if (treeStage == 3) {
            hold(p, HATCHET, p.getInventory().getItem(HATCHET));
            Vec3 lp = curLog.position();
            p.teleportTo(level, lp.x + 1.5, ground((int) lp.x, (int) lp.z), lp.z, 0, 0);
            WorkJob limb = WoodJobs.resolve(p, at(curLog));
            if (limb instanceof WoodJobs.Limb) run(p, limb, "limb");
            WorkJob buck = WoodJobs.resolve(p, at(curLog));
            if (buck instanceof WoodJobs.Buck) run(p, buck, "buck");
            else log("  buck refused: " + buck);
            sweepWood(p, lp);
            if (chopBlock == null || !level.getBlockState(chopBlock).is(WoodRegistry.CHOPPING_BLOCK.get())) setChoppingBlock(p);
            if (chopBlock != null) splitRounds(p);
            sweepWood(p, p.position());
            log("  wood so far: " + treesTried + " trees, " + splitsHave() + " splits piled, " + roundPile.size() + " rounds left");
            treeStage = 0;
        }
    }

    private static void setChoppingBlock(ServerPlayer p) {
        if (roundPile.isEmpty()) return;
        BlockPos g = p.blockPosition().east(2);
        g = new BlockPos(g.getX(), ground(g.getX(), g.getZ()) - 1, g.getZ());
        p.setShiftKeyDown(true);
        hold(p, WORK, roundPile.get(0).copy());
        WorkJob set = WoodJobs.resolve(p, at(g, Direction.UP));
        if (set != null && WorkService.runFor(p, set, 60_000) >= 0) { roundPile.remove(0); chopBlock = g.above(); }
        else log("  chopping block not set: " + set);
        p.setShiftKeyDown(false);
        p.getInventory().setItem(WORK, ItemStack.EMPTY);
    }

    private static void splitRounds(ServerPlayer p) {
        Vec3 cb = Vec3.atCenterOf(chopBlock);
        p.teleportTo(level, cb.x, chopBlock.getY(), cb.z - 1.5, 0, 0);
        while (!roundPile.isEmpty() && splitPile.size() < 14) {
            ItemStack one = roundPile.remove(0);
            hold(p, WORK, one.copy());
            var hit = new BlockHitResult(cb.add(0, 0.4, 0), Direction.UP, chopBlock, false);
            p.gameMode.useItemOn(p, level, p.getInventory().getItem(WORK), InteractionHand.MAIN_HAND, hit);
            if (!(level.getBlockEntity(chopBlock) instanceof ChoppingBlockEntity be) || !be.hasRound()) {
                if (level.getBlockEntity(chopBlock) instanceof ChoppingBlockEntity be2) be2.setRound(one.copyWithCount(1), 0);
            }
            p.getInventory().setItem(WORK, ItemStack.EMPTY);
            hold(p, HATCHET, p.getInventory().getItem(HATCHET));
            WorkJob j = WoodJobs.resolve(p, at(chopBlock, Direction.UP));
            if (!(j instanceof WoodJobs.Split sp)) { log("  split refused: " + j); break; }
            if ((splitAlt++ & 1) == 0) sp.aim(0.8, 0.5); else sp.aim(0.5, 0.3);
            run(p, sp, "split");
            sweepWood(p, cb);
        }
    }

    // ---- 4. bench ----------------------------------------------------------------------------------------------------

    private static void bench(ServerPlayer p) {
        p.teleportTo(level, shop.getX() + 1.5, shop.getY(), shop.getZ() + 0.5, 0, 0);
        if (have(p, "stick") == 0 && count(p, s -> s.is(Items.STICK)) == 0) {
            grant("minecraft:stick", 10, "sticks for stakes (2) and arrow shafts (6): limbing gives kindling, fallen_limb comes only from lightning, a plant pull gives one stick");
            give(p, new ItemStack(Items.STICK, 10));
        }
        craft(p, "stake"); // 2 stakes a stick
        craft(p, "stake");
        craft(p, "workbench");
        BlockPos g = shop.east(3);
        int y = ground(g.getX(), g.getZ());
        BlockPos spot = new BlockPos(g.getX(), y, g.getZ());
        level.setBlock(spot.below(), Blocks.DIRT.defaultBlockState(), 3);
        hold(p, WORK, new ItemStack(item("workbench"), 1));
        p.gameMode.useItemOn(p, level, p.getInventory().getItem(WORK), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(spot.below()).add(0, 0.5, 0), Direction.UP, spot.below(), false));
        p.getInventory().setItem(WORK, ItemStack.EMPTY);
        if (level.getBlockState(spot).is(CraftRegistry.WORKBENCH.get())) { benchPos = spot; remove(p, item("workbench"), 1); }
        else {
            level.setBlock(spot, CraftRegistry.WORKBENCH.get().defaultBlockState(), 3);
            benchPos = spot;
            finding("workbench item use did not place the block at " + spot.toShortString() + "; set by hand");
        }
        step("placed the crafted workbench at " + benchPos.toShortString(), "workbench BlockItem useItemOn");
        ph = Ph.GAME;
    }

    // ---- 5. small game by hand: sinew and feathers ---------------------------------------------------------------------

    private static List<LivingEntity> animals(Vec3 c, double r, String suffix) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r), e -> e.isAlive() && e instanceof YoiAnimal a && a.speciesId().endsWith(suffix));
    }

    private static void game(ServerPlayer p) {
        boolean needSinew = have(p, "sinew") < 2, needFeather = have(p, "feather") < 6;
        if (!needSinew && !needFeather || gameTries >= 10) {
            step("small game by hand (hatchet blows): " + have(p, "sinew") + " sinew, " + have(p, "feather") + " feathers after " + gameTries + " tries", "player melee on YoiAnimal, CarcassEntity.interact, Butchering.finish");
            if (have(p, "sinew") < 2) { grant("sinew", 2 - have(p, "sinew"), "no hare killed and butchered within " + gameTries + " tries"); giveGrant(p, item("sinew"), 2 - have(p, "sinew")); }
            if (have(p, "feather") < 6) { grant("feather", 6 - have(p, "feather"), "no bird killed and butchered within " + gameTries + " tries"); giveGrant(p, item("feather"), 6 - have(p, "feather")); }
            ph = Ph.BOWCHAIN;
            return;
        }
        String sp = needSinew ? "snowshoe_hare" : "willow_ptarmigan";
        // an open carcass or whole game already in the pack
        if (butcherWholeInPack(p)) return;
        var cs = level.getEntitiesOfClass(CarcassEntity.class, new AABB(p.blockPosition()).inflate(40), c -> c.isAlive() && !c.speciesId().endsWith("moose"));
        if (!cs.isEmpty()) {
            CarcassEntity c = cs.get(0);
            p.teleportTo(level, c.getX() + 0.8, c.getY(), c.getZ(), 0, 0);
            hold(p, 6, ItemStack.EMPTY);
            c.interact(p, InteractionHand.MAIN_HAND);
            log("  picked up a " + c.speciesId() + " carcass");
            wait = 5;
            return;
        }
        var as = animals(p.position(), 90, sp);
        if (as.isEmpty()) {
            double a = Math.toRadians(gameHop * 70);
            gameHop++;
            gameTries++;
            walkTo(p, spawn.getX() + 55 * Math.cos(a) * (1 + gameHop / 6), spawn.getZ() + 55 * Math.sin(a) * (1 + gameHop / 6), "looking for a " + sp);
            wait = 80;
            return;
        }
        LivingEntity t = as.get(0);
        gameTries++;
        hold(p, HATCHET, p.getInventory().getItem(HATCHET));
        for (int i = 0; i < 60 && t.isAlive(); i++) {
            p.teleportTo(level, t.getX() + 0.9, t.getY(), t.getZ(), 0, 0);
            p.resetAttackStrengthTicker();
            p.attack(t);
        }
        log("  hatchet blows on a " + sp + " at " + t.blockPosition().toShortString() + ": alive " + t.isAlive());
        wait = 20;
    }

    private static boolean butcherWholeInPack(ServerPlayer p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.getItem() instanceof WholeGameItem && WholeGameItem.record(s) != null) {
                String sp = WholeGameItem.record(s).species();
                Butchering.finish(p, s);
                step("dressed a " + sp + " from the pack: now " + have(p, "sinew") + " sinew, " + have(p, "feather") + " feathers", "Butchering.finish");
                wait = 5;
                return true;
            }
        }
        return false;
    }

    // ---- 6. the bow chain ----------------------------------------------------------------------------------------------

    private static void bowChain(ServerPlayer p) {
        p.teleportTo(level, benchPos.getX() + 1.5, benchPos.getY(), benchPos.getZ() + 0.5, 0, 0);
        craft(p, "sinew_cord");
        craft(p, "bow_stave");
        craft(p, "fletching");
        craft(p, "fletching");
        craft(p, "arrow");
        craft(p, "self_bow");
        boolean bow = have(p, "self_bow") > 0, arrows = have(p, "arrow") > 0;
        step("bow chain done: self_bow " + bow + ", arrows " + have(p, "arrow") + " (crafting labour " + f(craftMin) + " game min)", "StationCraft chain");
        int slot = -1;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item("self_bow"))) { slot = i; break; }
        if (slot >= 0 && slot != BOWSLOT) {
            ItemStack b = p.getInventory().getItem(slot).copy();
            ItemStack d = p.getInventory().getItem(BOWSLOT).copy();
            p.getInventory().setItem(slot, d);
            p.getInventory().setItem(BOWSLOT, b);
        }
        p.getInventory().selected = BOWSLOT;
        ph = Ph.SCOUT;
        scoutHop = 0;
    }

    // ---- 7. sign -------------------------------------------------------------------------------------------------------

    private static MooseEntity nearestMoose(Vec3 c, double r) {
        MooseEntity best = null;
        double bd = Double.MAX_VALUE;
        for (MooseEntity m : level.getEntitiesOfClass(MooseEntity.class, new AABB(c, c).inflate(r, 64, r), LivingEntity::isAlive)) {
            double d = m.position().distanceToSqr(c);
            if (d < bd) { bd = d; best = m; }
        }
        return best;
    }

    private static final List<BlockPos> scouted = new ArrayList<>();

    /** A bank spot near water (the moose's willow flats are riparian) not within 70 m of a place already looked at. */
    private static BlockPos goal;

    /** The start site's nearest moose home-range centre (Blueprint › Start site: three ranges within 1,500 m): where a player who reads the land heads. */
    private static void pickGoal(ServerPlayer p) {
        if (goal != null) return;
        if (!(level.getChunkSource().getGenerator() instanceof com.yearofice.worldgen.gen.YoiChunkGenerator g) || g.world() == null) return;
        double bd = Double.MAX_VALUE;
        for (var r : g.world().life().moose()) {
            double dx = r.x() - p.getX(), dz = r.z() - p.getZ(), d = dx * dx + dz * dz;
            if (d < bd) { bd = d; goal = new BlockPos((int) r.x(), 64, (int) r.z()); }
        }
        if (goal != null) log("NOTE heading for the nearest moose range centre " + goal.toShortString() + ", " + f(Math.sqrt(bd)) + " m away (start-site life layer; a player reads the same from the land)");
    }

    private static BlockPos nextBank(ServerPlayer p) {
        pickGoal(p);
        BlockPos c = p.blockPosition();
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (int dx = -110; dx <= 110; dx += 4) for (int dz = -110; dz <= 110; dz += 4) {
            int x = c.getX() + dx, z = c.getZ() + dz;
            if (!level.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
            BlockPos w = new BlockPos(x, y, z);
            if (!level.getFluidState(w).is(net.minecraft.tags.FluidTags.WATER)) continue;
            for (int[] o : new int[][] {{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
                int bx = x + o[0], bz = z + o[1];
                if (!level.hasChunkAt(new BlockPos(bx, 64, bz))) continue;
                int by = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
                BlockPos land = new BlockPos(bx, by, bz);
                if (!level.getFluidState(land.below()).isEmpty() || !level.getFluidState(land).isEmpty()) continue;
                boolean fresh = true;
                for (BlockPos q : scouted) if (q.distSqr(land) < 70 * 70) { fresh = false; break; }
                // the nearest bank that also brings the moose country closer (the start site's nearest moose range)
                double d = land.distSqr(c);
                if (goal != null) d = land.distSqr(goal) * 0.25 + d;
                if (fresh && land.distSqr(c) > 20 * 20 && d < bd) { bd = d; best = land; }
            }
        }
        return best;
    }

    private static boolean scoutLaid = false;

    private static void scout(ServerPlayer p) {
        BlockPos near = SignService.nearest(level, p.blockPosition(), 140, "yearofice:moose");
        MooseEntity seen = nearestMoose(p.position(), 150);
        if (near != null || seen != null) {
            signPos = near;
            if (near == null && !scoutLaid) {
                // a moose in sight and no sign yet: stay well back and let ninety game minutes pass for it to lay some
                scoutLaid = true;
                ClockService.get().forceRate(com.yearofice.sim.time.Rate.SLEEP);
                wait = 400;
                log("  moose in sight at " + f(Math.sqrt(seen.distanceToSqr(p))) + " m but no moose sign yet; letting the clock run to lay some");
                return;
            }
            ClockService.get().forceRate(null);
            step("scouted from bank " + scoutHop + ": moose sign " + (near == null ? "none" : near.toShortString()) + ", a moose in sight " + (seen != null), "SignService.nearest(\"yearofice:moose\"), riparian scan");
            scoutLaid = false;
            ph = Ph.SIGN;
            return;
        }
        if (scoutLaid) { scoutLaid = false; ClockService.get().forceRate(null); }
        if (scoutHop >= 40) {
            finding("no moose sign and no moose within 150 m of 40 riparian scouting spots");
            ph = Ph.END;
            return;
        }
        BlockPos bank = nextBank(p);
        if (bank != null) {
            scouted.add(bank);
            walkTo(p, bank.getX() + 0.5, bank.getZ() + 0.5, "to a creek bank, spot " + scoutHop);
        } else {
            double ang = Math.toRadians(scoutHop * 47), rad = 90 + 30 * (scoutHop / 6);
            walkTo(p, spawn.getX() + rad * Math.cos(ang), spawn.getZ() + rad * Math.sin(ang), "no water in the loaded ground; ring spot " + scoutHop);
        }
        scoutHop++;
        wait = 80;
    }

    private static void signStep(ServerPlayer p) {
        BlockPos any = SignService.nearest(level, p.blockPosition(), 120, null);
        BlockPos use = signPos != null ? signPos : any;
        if (use == null) {
            finding("no sign of any kind within 120 m: sign cannot be read (reading step skipped)");
            ph = Ph.FIND;
            return;
        }
        var rd = SignService.read(level, use, p);
        if (rd != null) {
            step("read sign at " + use.toShortString() + ": " + rd.text() + " [" + rd.kind() + ", " + rd.species() + ", age " + rd.age() + " " + Math.round(rd.ageMin()) + " min, confidence " + f(rd.confidence()) + ", heading dx " + f(rd.dx()) + " dz " + f(rd.dz()) + "]", "SignService.read(level, pos, player)");
            signHeading = new Vec3(rd.dx(), 0, rd.dz());
            if (signPos == null) finding("only non-moose sign found (" + rd.species() + "); the moose is found by sight instead");
        }
        firstSignPos = use;
        Vec3 sp = Vec3.atCenterOf(use);
        Vec3 from = sp.add(2.2, 1.8, 2.2);
        level.getChunk(use.getX() >> 4, use.getZ() >> 4);
        cam(p, new Vec3(from.x, ground((int) from.x, (int) from.z), from.z).add(0, 1.0, 0), sp);
        if (!signShown) { signShown = true; snap("m6hunt_sign", 40); }
        ph = Ph.FIND;
        followHop = 0;
    }

    private static void findMoose(ServerPlayer p) {
        Vec3 c = firstSignPos != null ? Vec3.atCenterOf(firstSignPos) : p.position();
        MooseEntity m = nearestMoose(c, 160);
        if (m == null && followHop < 6) {
            // follow the track's heading, hop by hop
            Vec3 h = signHeading.lengthSqr() > 1e-6 ? signHeading.normalize() : new Vec3(1, 0, 0);
            double d = 45;
            followHop++;
            c = c.add(h.scale(d));
            firstSignPos = BlockPos.containing(c);
            walkTo(p, c.x, c.z, "following the track heading " + BloodTrail.compass(h.x, h.z) + ", leg " + followHop);
            wait = 50;
            return;
        }
        if (m == null) {
            finding("sign led to no moose within 160 m after six legs");
            ph = Ph.END;
            return;
        }
        mooseId = m.getUUID();
        mooseAttempts++;
        step("a moose at " + m.blockPosition().toShortString() + ", " + f(Math.sqrt(m.position().distanceToSqr(c))) + " m from the sign (state " + m.state() + ", " + (m.male() ? "bull" : "cow") + ")", "level entity scan near the sign");
        ph = Ph.STALK;
        approachTicks = 0;
        windLogged = false;
    }

    // ---- 8. wind and the approach ---------------------------------------------------------------------------------------

    private static MooseEntity moose() { return mooseId != null && level.getEntity(mooseId) instanceof MooseEntity m && m.isAlive() ? m : null; }

    private static final double SHOOT_M = 14;

    private static void stalk(ServerPlayer p) {
        MooseEntity m = moose();
        if (m == null) { ph = Ph.AFTER; return; }
        approachTicks++;
        var w = EnvService.get().wind(level, m.blockPosition());
        double from = Math.toRadians(w.fromDeg());
        double fx = Math.sin(from), fz = -Math.cos(from); // unit vector toward where the wind comes FROM
        Vec3 mp = m.position();
        double dist = Math.hypot(mp.x - p.getX(), mp.z - p.getZ());
        if (!windLogged) {
            windLogged = true;
            // downwind of the moose: my scent blows away from it, its scent blows to me
            double hx = mp.x - fx * 75, hz = mp.z - fz * 75;
            step("wind from " + f(w.fromDeg()) + " deg at " + f(w.ms()) + " m/s (gust " + f(w.gustMs()) + "); taking the downwind side of the moose, " + BloodTrail.compass(-fx, -fz) + " of it, 75 m out", "EnvService.wind(level, pos)");
            walkTo(p, hx, hz, "to the downwind approach line");
            approachTicks = 0;
            wait = 40;
            return;
        }
        // 1.2 m/s along the line from the downwind side
        double want = Math.max(SHOOT_M, dist - 0.06);
        double tx = mp.x - fx * want, tz = mp.z - fz * want;
        double ddx = tx - p.getX(), ddz = tz - p.getZ(), dd = Math.hypot(ddx, ddz);
        double stepM = Math.min(dd, 0.06);
        double nx = dd < 1e-6 ? p.getX() : p.getX() + ddx / dd * stepM, nz = dd < 1e-6 ? p.getZ() : p.getZ() + ddz / dd * stepM;
        walkMin += stepM / WALK_MS / 60.0;
        p.setShiftKeyDown(true);
        double ny = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(nx), (int) Math.floor(nz));
        Vec3 eye = new Vec3(nx, ny + p.getEyeHeight(), nz);
        Vec3 look = mp.add(0, m.getBbHeight() * 0.6, 0).subtract(eye);
        p.teleportTo(level, nx, ny, nz, (float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))));
        if (approachTicks % 200 == 0) log("  stalk: " + f(dist) + " m, moose " + m.state() + ", awareness " + m.awareness());
        if (m.state() == com.yearofice.sim.ecosystem.AnimalState.FLEE) {
            log("  the moose fled at " + f(dist) + " m after " + approachTicks + " ticks of approach");
            finding("moose attempt " + mooseAttempts + ": spooked (state FLEE) at " + f(dist) + " m on the downwind approach");
            mooseId = null;
            if (mooseAttempts < 3) { ph = Ph.FIND; followHop = 0; firstSignPos = BlockPos.containing(mp); wait = 100; } else ph = Ph.END;
            p.setShiftKeyDown(false);
            return;
        }
        if (dist <= SHOOT_M + 0.5 && Math.abs(dist - SHOOT_M) < 1.5) {
            // a hunter looses only down a clear lane: willow leaves and shrub stems stop a shaft. Sidestep along the
            // moose's flank (up to 4 m either way) until the eye-to-lungs line clips nothing.
            Vec3 lungs = mp.add(0, m.getBbHeight() * 0.55, 0);
            double sx = -fz, sz = fx;
            boolean clear = false;
            for (int k = 0; k < 9 && !clear; k++) {
                double off = (k % 2 == 0 ? 1 : -1) * ((k + 1) / 2) * 1.0;
                double cx = p.getX() + sx * off, cz = p.getZ() + sz * off;
                double cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(cx), (int) Math.floor(cz));
                Vec3 ce = new Vec3(cx, cy + p.getEyeHeight(), cz);
                var hit = level.clip(new net.minecraft.world.level.ClipContext(ce, lungs, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
                if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS || hit.getLocation().distanceTo(lungs) < 1.2) {
                    clear = true;
                    if (k > 0) { p.teleportTo(level, cx, cy, cz, p.getYRot(), p.getXRot()); log("  sidestepped " + f(off) + " m for a clear lane"); }
                }
            }
            if (!clear) {
                if (laneWaits++ < 6) { log("  no clear lane to the lungs (cover in the line); waiting for the moose to step"); approachTicks += 100; return; }
                finding("no clear shooting lane within 4 m of sidestep at " + f(dist) + " m: the moose stands in cover; a shaft would stop in the willow");
            }
            laneWaits = 0;
            p.setShiftKeyDown(false);
            step("in bow range: " + f(dist) + " m from the moose (state " + m.state() + "), lane " + (clear ? "clear" : "obstructed"), "approach 1.2 m/s on the downwind line, level.clip eye→lungs");
            ph = Ph.DRAW;
            drawStart = -1;
        }
        if (approachTicks > 5000) { finding("approach timed out"); ph = Ph.END; }
    }

    // ---- 9. draw and loose -----------------------------------------------------------------------------------------------

    private static void aim(ServerPlayer p, MooseEntity m, double draw, BowItem bow) {
        Vec3 eye = p.getEyePosition();
        Vec3 target = m.position().add(0, m.getBbHeight() * 0.55, 0);
        double d = Math.hypot(target.x - eye.x, target.z - eye.z);
        double[] dd = Ballistics.dropAndDrift(bow.weapon().speedAt(draw), d, 0, HuntConfig.tuning());
        Vec3 look = target.add(0, dd[0], 0).subtract(eye);
        p.teleportTo(level, p.getX(), p.getY(), p.getZ(), (float) Math.toDegrees(Math.atan2(-look.x, look.z)), (float) -Math.toDegrees(Math.atan2(look.y, Math.hypot(look.x, look.z))));
    }

    private static int laneWaits = 0;

    private static void draw(ServerPlayer p) {
        MooseEntity m = moose();
        if (m == null) { ph = Ph.AFTER; return; }
        ItemStack bowStack = p.getInventory().getItem(BOWSLOT);
        if (!(bowStack.getItem() instanceof BowItem bow)) { finding("no bow in slot 1; the shot cannot be taken"); ph = Ph.END; return; }
        p.getInventory().selected = BOWSLOT;
        if (redraw) { redraw = false; p.stopUsingItem(); drawStart = -1; } // the frame was taken at full draw; draw again for the loose
        if (drawStart < 0) {
            arrowsBefore = have(p, "arrow");
            if (arrowsBefore == 0) { finding("no arrows left"); ph = Ph.AFTER; return; }
            aim(p, m, 1.0, bow);
            bow.use(level, p, InteractionHand.MAIN_HAND);
            drawStart = tick;
            return;
        }
        int held = tick - drawStart;
        aim(p, m, 1.0, bow);
        if (m.state() == com.yearofice.sim.ecosystem.AnimalState.FLEE) { p.stopUsingItem(); drawStart = -1; log("  moose bolted during the draw"); ph = Ph.AFTER; return; }
        double dr = bow.drawAfter(p, held);
        if (dr >= bow.weapon().maxDraw(100, 0.6) - 1e-6 || held > 140) {
            if (!shotShown) { shotShown = true; redraw = true; snap("m6hunt_shot", 30); return; }
            double dist = Math.hypot(m.getX() - p.getX(), m.getZ() - p.getZ());
            impact = m.position();
            p.releaseUsingItem();
            if (have(p, "arrow") == arrowsBefore) { // the use-timer did not run on this thread: loose through the item itself
                bow.releaseUsing(bowStack, level, p, BowItem.USE_TICKS - Math.max(held, 12));
                p.stopUsingItem();
            }
            shotsFired++;
            step("drew " + f(dr * 100) + "% for " + held + " ticks and loosed at " + f(dist) + " m, aim at the lungs with the arrow's drop added, arrows left " + have(p, "arrow"), "BowItem.use / releaseUsing, Ballistics.dropAndDrift");
            shotWait = 0;
            drawStart = -1;
            ph = Ph.AFTER;
            wait = 40;
        }
    }

    // ---- 10. what the arrow did, and the blood trail ----------------------------------------------------------------------

    private static void after(ServerPlayer p) {
        MooseEntity m = moose();
        lastOutcome = HuntDebug.last(p);
        log("  shot report: " + lastOutcome);
        boolean wounded = mooseId != null && level.getEntity(mooseId) instanceof LivingEntity le && WoundTracker.isWounded(le);
        var cs = level.getEntitiesOfClass(CarcassEntity.class, new AABB(impact == null ? p.position() : impact, impact == null ? p.position() : impact).inflate(150), c -> c.speciesId().endsWith("moose"));
        if (!cs.isEmpty()) {
            carcass = cs.get(0);
            step("the moose is down at " + carcass.blockPosition().toShortString() + " (" + f(Math.sqrt(carcass.position().distanceToSqr(p.position()))) + " m from me)", "CarcassEntity (moose) after HuntDebug.last: " + lastOutcome);
            ph = Ph.BUTCHER;
            sub = 0;
            return;
        }
        if (m != null && !wounded && shotsFired < 6 && have(p, "arrow") > 0 && m.state() != com.yearofice.sim.ecosystem.AnimalState.FLEE) {
            step("the arrow found nothing (" + lastOutcome + "); nocking another", "HuntDebug.last(player)");
            ph = Ph.DRAW;
            drawStart = -1;
            return;
        }
        if (!wounded) {
            finding(m == null ? "the moose left or the arrow missed: " + lastOutcome : "six arrows and no hit on the moose: " + lastOutcome);
            if (mooseAttempts < 3 && have(p, "arrow") > 0) { ph = Ph.FIND; followHop = 0; firstSignPos = impact == null ? null : BlockPos.containing(impact); mooseId = null; wait = 100; }
            else ph = Ph.END;
            return;
        }
        step("hit: the moose is wounded and has run (" + lastOutcome + "); waiting out the hunter's half hour at a distance, then reading the blood", "WoundTracker.isWounded, HuntDebug.last");
        curPatch = null;
        visited.clear();
        trailWaits = 0;
        patchesFollowed = 0;
        ph = Ph.TRAIL;
        // wait: a wounded animal pushed within 30 m runs again; the clock runs fast for the wait
        ClockService.get().forceRate(com.yearofice.sim.time.Rate.SLEEP);
        sub = 0;
        wait = 20;
    }

    private static BlockPos nearestPatch(BlockPos from, int r) {
        BlockPos best = null;
        double bd = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(from.offset(-r, -3, -r), from.offset(r, 3, r))) {
            if (!level.getBlockState(q).is(HuntRegistry.BLOOD_TRAIL.get()) || visited.contains(q)) continue;
            double d = q.distSqr(from);
            if (d < bd) { bd = d; best = q.immutable(); }
        }
        return best;
    }

    private static void trail(ServerPlayer p) {
        sub++;
        if (sub == 40) ClockService.get().forceRate(null);
        var cs = level.getEntitiesOfClass(CarcassEntity.class, new AABB(p.blockPosition()).inflate(120), c -> c.speciesId().endsWith("moose"));
        if (!cs.isEmpty() && (curPatch == null || cs.get(0).blockPosition().distSqr(curPatch) < 60 * 60)) {
            carcass = cs.get(0);
            step("followed the blood trail across " + patchesFollowed + " patches to the carcass at " + carcass.blockPosition().toShortString(), "BloodTrail patches (HuntRegistry.BLOOD_TRAIL), CarcassEntity");
            ph = Ph.BUTCHER;
            sub = 0;
            return;
        }
        if (sub < 60) return; // let the clock run
        BlockPos seed = curPatch != null ? curPatch : BlockPos.containing(impact == null ? p.position() : impact);
        if (curPatch == null) {
            int[] cnt = BloodTrail.count(level, seed, 16);
            log("  blood within 16 m of the hit site: " + cnt[0] + " patches, " + cnt[1] + " drops");
            curPatch = nearestPatch(seed, 16);
            if (curPatch == null) { finding("no blood patch within 16 m of where the moose was hit"); if (++trailWaits > 6) { ph = Ph.END; } wait = 200; return; }
        }
        BlockPos nxt = nearestPatch(curPatch, 9);
        visited.add(curPatch);
        if (patchesFollowed == 0 && !shotShown) { /* no-op */ }
        p.teleportTo(level, curPatch.getX() + 0.5, curPatch.getY(), curPatch.getZ() + 0.5, p.getYRot(), 35f);
        patchesFollowed++;
        if (nxt == null) {
            log("  trail ends at " + curPatch.toShortString() + " after " + patchesFollowed + " patches; looking about");
            if (++trailWaits > 8) { finding("the blood trail ran out without a carcass"); ph = Ph.END; return; }
            // wait at a distance for the animal to bed or die
            wait = 300;
            curPatch = curPatch;
            visited.clear();
            return;
        }
        curPatch = nxt;
        wait = Math.max(4, (int) (Math.sqrt(curPatch.distSqr(nxt)) / WALK_MS * 20));
        wait = 6;
        walkMin += 1.5 / WALK_MS / 60.0;
    }

    // ---- 11. the carcass ---------------------------------------------------------------------------------------------------

    private static void butcher(ServerPlayer p) {
        if (carcass == null || carcass.isRemoved()) { fail("carcass gone before dressing"); ph = Ph.END; return; }
        if (sub == 0) {
            Vec3 c = carcass.position();
            Vec3 from = c.add(3.5, 0, 1.5);
            cam(p, new Vec3(from.x, ground((int) from.x, (int) from.z), from.z).add(0, 0.4, 0), c.add(0, 0.7, 0));
            sub = 1;
            snap("m6hunt_carcass", 40);
            return;
        }
        p.teleportTo(level, carcass.getX() + 1.4, carcass.getY(), carcass.getZ(), p.getYRot(), p.getXRot());
        hold(p, 6, ItemStack.EMPTY);
        String sp = carcass.speciesId();
        double kg = carcass.record().weightG() / 1000.0;
        carcass.interact(p, InteractionHand.MAIN_HAND);
        Map<String, Integer> parts = new LinkedHashMap<>();
        for (ItemEntity ie : level.getEntitiesOfClass(ItemEntity.class, new AABB(p.blockPosition()).inflate(8))) {
            parts.merge(BuiltInRegistries.ITEM.getKey(ie.getItem().getItem()).getPath(), ie.getItem().getCount(), Integer::sum);
        }
        boolean meat = parts.keySet().stream().anyMatch(k -> k.contains("meat"));
        step("dressed the " + sp + " (" + f(kg) + " kg) where it lay: " + parts, "CarcassEntity.interact -> Butchering.dressInPlace");
        tookMoose = meat && sp.endsWith("moose");
        if (!meat) fail("butchering gave no meat: " + parts);
        ph = Ph.END;
    }

    // ---- end ------------------------------------------------------------------------------------------------------------------

    private static void finish(ServerPlayer p) {
        boolean noYoi = commands == 0;
        boolean pass = noYoi && tookMoose && fails.isEmpty();
        log("---- LEDGER (" + ledger.size() + " steps) ----");
        for (String s : ledger) log(s);
        log("---- GRANTS ----");
        if (grants.isEmpty()) log("none");
        for (var g : grants.entrySet()) log("GRANT " + g.getKey() + " x" + g.getValue());
        log("---- FINDINGS ----");
        for (String s : findings) log(s);
        log("ASSERT no /yoi command after setup: " + (noYoi ? "ok" : "FAILED (" + commands + ")"));
        log("ASSERT moose taken and dressed: " + (tookMoose ? "ok" : "FAILED"));
        log("ASSERT every step logged with its api: " + steps + " steps");
        log("labour: walking " + f(walkMin) + " game min, crafting " + f(craftMin) + " game min; shots fired " + shotsFired + "; last shot: " + lastOutcome);
        verdict = (pass ? "PASS" : "FAIL") + " grants " + grants.size();
        log("RESULT " + (pass ? "PASS" : "FAIL") + " : " + (tookMoose ? "a moose was taken with sign, wind and the blood trail" : "no moose was taken") + "; grants " + grants + "; " + fails);
        done = true;
    }

    // ---- client half: world creation, screenshots -------------------------------------------------------------------------------

    private enum CPhase { TITLE, CREATE, LOADING, PLAY, SHOT, DONE }

    private static CPhase cph = CPhase.TITLE;
    private static int cticks = 0, shotAt = -1;
    private static String pending = null;

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post e) {
        if (!ON || cph == CPhase.DONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getOverlay() != null) return;
        cticks++;
        switch (cph) {
            case TITLE -> {
                if (mc.screen != null && mc.screen.getClass().getSimpleName().contains("Onboarding")) {
                    mc.options.onboardAccessibility = false;
                    mc.setScreen(new TitleScreen());
                    return;
                }
                if (mc.screen instanceof TitleScreen && cticks > 40) { CreateWorldScreen.openFresh(mc, mc.screen); cph = CPhase.CREATE; cticks = 0; }
            }
            case CREATE -> {
                if (mc.screen instanceof CreateWorldScreen s && cticks > 20) {
                    WorldCreationUiState ui = s.getUiState();
                    ui.setName("yoi-smoke-m6hunt");
                    ui.setSeed("3141592");
                    ui.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
                    ui.setAllowCommands(false);
                    PresetDefaults.select(ui, "slice_valley");
                    press(s, "selectWorld.create");
                    cph = CPhase.LOADING;
                    cticks = 0;
                }
            }
            case LOADING -> {
                if (mc.screen != null && !(mc.screen instanceof CreateWorldScreen) && cticks % 20 == 0) pressAny(mc.screen);
                if (mc.player != null && mc.level != null && mc.screen == null) { cph = CPhase.PLAY; cticks = 0; }
            }
            case PLAY -> {
                mc.options.pauseOnLostFocus = false;
                mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                mc.getTutorial().stop();
                mc.getToasts().clear();
                if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
                if (cticks == 20) {
                    org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 720);
                    mc.options.guiScale().set(2);
                    mc.options.renderDistance().set(5);
                    mc.options.simulationDistance().set(6);
                    mc.options.hideGui = false;
                    com.yearofice.client.hud.HudDebug.set(com.yearofice.client.hud.HudDebug.Mode.FULL);
                }
                String want = shot;
                if (want != null && pending == null) {
                    shot = null;
                    mc.gui.getChat().clearMessages(true);
                    if (mc.screen != null) mc.setScreen(null);
                    pending = want;
                    shotAt = cticks + 8;
                }
                if (pending != null && cticks >= shotAt) {
                    // wait for the chunk meshing so the frame is not a sky (up to 25 s)
                    if (!mc.levelRenderer.hasRenderedAllSections() && cticks < shotAt + 500) return;
                    String n = pending;
                    pending = null;
                    Screenshot.grab(mc.gameDirectory, n + ".png", mc.getMainRenderTarget(), msg -> { YearOfIce.LOG.info("Smoke screenshot: {}", msg.getString()); shotBusy = false; });
                }
                if (done) { cph = CPhase.SHOT; cticks = 0; }
            }
            case SHOT -> {
                if (cticks == 60) {
                    YearOfIce.LOG.info("SMOKE M6HUNT DONE: {}", verdict);
                    cph = CPhase.DONE;
                    mc.stop();
                }
            }
            default -> {}
        }
    }

    private static void press(Screen s, String key) {
        for (var child : s.children()) {
            if (child instanceof Button b && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && t.getKey().equals(key)) { b.onPress(); return; }
        }
    }

    private static void pressAny(Screen s) {
        String n = s.getClass().getSimpleName();
        if (!(n.contains("Confirm") || n.contains("Warning") || n.contains("Backup"))) return;
        for (var child : s.children()) if (child instanceof Button b && b.active) { b.onPress(); return; }
    }
}

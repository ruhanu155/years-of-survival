package com.yearofice.build.wood;

import com.mojang.authlib.GameProfile;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkService;
import com.yearofice.build.work.WorkStartPayload;
import com.yearofice.core.YearOfIce;
import com.yearofice.fire.FireParts;
import com.yearofice.fire.FireRegistry;
import com.yearofice.fire.FuelData;
import com.yearofice.sim.fire.Seasoning;
import com.yearofice.sim.time.Calendar;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.sim.wood.TreeMarks;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.sim.wood.WoodKind;
import com.yearofice.world.attach.TreeMarksData;
import com.yearofice.world.attach.WorldAttachments;
import com.yearofice.world.gametest.M2WorldGameTests;
import com.yearofice.world.logs.FallingTreeEntity;
import com.yearofice.world.logs.FelledLogEntity;
import com.yearofice.worldgen.TreePlan;
import com.yearofice.worldgen.TreeShapes;
import com.yearofice.worldgen.ValleyWorld;
import com.yearofice.worldgen.block.StumpBlock;
import com.yearofice.worldgen.block.WorldBlocks;
import com.yearofice.worldgen.block.YoiLeavesBlock;
import com.yearofice.worldgen.block.YoiLogBlock;
import com.yearofice.worldgen.gen.YoiChunkGenerator;
import com.yearofice.worldgen.structure.PlacedMask;
import io.netty.channel.embedded.EmbeddedChannel;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

/**
 * Felling and firewood GameTests (Ready for development › M2; Trees, felling and firewood › GameTests; video 2, A1).
 * Jobs run synchronously through {@link WorkService#runFor} at the Work rate's deciseconds a tick, without asking the
 * shared clock for a rate, so tests in parallel never move the clock; chunks a test needs ticking are force-loaded by
 * reference count. Every test here finishes in well under a second of wall time apart from the falling tree's 2.5 s
 * landing (RECONCILE 185).
 */
@GameTestHolder(YearOfIce.MOD_ID)
public final class WoodGameTests {
    private WoodGameTests() {}

    /** The plot template's forest_duff layer is relative y = 1 (the structure sits one above the test origin); things stand at y = 2. */
    /** Search radius around the plot centre: the 17-block plot and no further, since tests run side by side 22 blocks apart. */
    static final double PLOT_R = 12;
    public static final String BATCH = "m2_wood";
    public static final String PLOT = YearOfIce.MOD_ID + ":gametest/wood_plot", EMPTY = YearOfIce.MOD_ID + ":gametest/empty3";

    @GameTestGenerator
    public static Collection<TestFunction> woodTests() {
        List<TestFunction> out = new ArrayList<>();
        out.add(test("yearofice:building/fell_spruce_leaves_stump", PLOT, 400, WoodGameTests::fellSpruce));
        out.add(test("yearofice:building/placed_logs_never_fall", PLOT, 200, WoodGameTests::placedLogsNeverFall));
        out.add(test("yearofice:building/sneak_cut_one_block", PLOT, 100, WoodGameTests::sneakCut));
        out.add(test("yearofice:building/buck_and_split", PLOT, 200, WoodGameTests::buckAndSplit));
        out.add(test("yearofice:wood/felling_mishap_rolls", EMPTY, 40, WoodGameTests::mishapRolls));
        out.add(test("yearofice:fire/seasoning_calendar", EMPTY, 20, WoodGameTests::seasoningCalendar));
        out.add(test("yearofice:trees/felled_never_regrow", EMPTY, 1200, WoodGameTests::felledNeverRegrow));
        out.add(test("yearofice:trees/save_budget_1000", EMPTY, 100, WoodGameTests::saveBudget));
        return com.yearofice.gametest.TestFilter.apply(out);
    }

    public static TestFunction test(String name, String template, int maxTicks, Consumer<GameTestHelper> body) {
        return new TestFunction(BATCH, name, template, maxTicks, 0, true, body);
    }

    // ---- helpers ------------------------------------------------------------------------------------------------

    public static ServerPlayer player(GameTestHelper h, String name, Vec3 rel) {
        ServerLevel level = h.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer p = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, p, cookie);
        p.getInventory().clearContent();
        moveTo(h, p, rel);
        return p;
    }

    static void moveTo(GameTestHelper h, ServerPlayer p, Vec3 rel) {
        Vec3 at = h.absoluteVec(rel);
        p.moveTo(at.x, at.y, at.z, 0, 0);
    }

    static ServerPlayer withHatchet(GameTestHelper h, String name, Vec3 rel) {
        ServerPlayer p = player(h, name, rel);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(WoodRegistry.HATCHET.get()));
        return p;
    }

    static WorkStartPayload at(BlockPos abs, Direction face) {
        return new WorkStartPayload(true, abs, -1, (byte) face.get3DDataValue(), abs.getX() + 0.5f, abs.getY() + 0.5f, abs.getZ() + 0.5f);
    }

    static WorkStartPayload at(Entity e) {
        return new WorkStartPayload(true, BlockPos.ZERO, e.getId(), (byte) 0, (float) e.getX(), (float) e.getY(), (float) e.getZ());
    }

    /** Grows a tree from the worldgen's own builder with its base at {@code relBase}; logs only replace air and leaves. */
    static void grow(GameTestHelper h, BlockPos relBase, TreeKind k, int height, int seed) {
        ServerLevel l = h.getLevel();
        BlockPos base = h.absolutePos(relBase);
        TreeShapes.build(new TreePlan.Tree(0, 0, WoodBlocks.coverSpecies(k), height, TreePlan.Kind.LIVE, -1, seed), (dx, dy, dz, part) -> {
            BlockPos p = base.offset(dx, dy, dz);
            BlockState here = l.getBlockState(p);
            if (part == TreeShapes.Part.LOG) {
                if (here.isAir() || here.getBlock() instanceof YoiLeavesBlock) l.setBlock(p, WoodBlocks.log(k).defaultBlockState(), Block.UPDATE_CLIENTS);
            } else if (part == TreeShapes.Part.LEAVES && here.isAir()) {
                l.setBlock(p, WoodBlocks.leaves(k).defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        });
    }

    static int count(GameTestHelper h, BlockPos relMin, BlockPos relMax, java.util.function.Predicate<BlockState> what) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(relMin), h.absolutePos(relMax))) if (what.test(h.getLevel().getBlockState(p))) n++;
        return n;
    }

    static <T extends Entity> List<T> around(GameTestHelper h, Class<T> type, double r) {
        Vec3 c = h.absoluteVec(new Vec3(8.5, 1, 8.5));
        return h.getLevel().getEntitiesOfClass(type, new AABB(c, c).inflate(r));
    }

    static int countItem(ServerPlayer p, GameTestHelper h, net.minecraft.world.item.Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) n += p.getInventory().getItem(i).getCount();
        for (ItemEntity e : around(h, ItemEntity.class, PLOT_R)) if (e.getItem().is(item)) n += e.getItem().getCount();
        return n;
    }

    /** A stack of {@code item} the player holds or dropped. */
    static ItemStack find(ServerPlayer p, GameTestHelper h, net.minecraft.world.item.Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (p.getInventory().getItem(i).is(item)) return p.getInventory().getItem(i);
        for (ItemEntity e : around(h, ItemEntity.class, PLOT_R)) if (e.getItem().is(item)) return e.getItem();
        return new ItemStack(item);
    }

    static void clearDrops(GameTestHelper h) {
        for (ItemEntity e : around(h, ItemEntity.class, PLOT_R)) e.discard();
        for (FelledLogEntity e : around(h, FelledLogEntity.class, PLOT_R)) e.discard();
        for (FallingTreeEntity e : around(h, FallingTreeEntity.class, PLOT_R)) e.discard();
    }

    /** Chunks forced by these tests, with how many running tests hold each; a chunk forced by anyone else is left alone. */
    static final Map<String, Integer> FORCED = new HashMap<>();

    static synchronized void force(ServerLevel level, ChunkPos c, boolean on) {
        String key = level.dimension().location() + "/" + c.toLong();
        Integer n = FORCED.get(key);
        if (on) {
            if (n != null) FORCED.put(key, n + 1);
            else if (!level.getForcedChunks().contains(c.toLong())) {
                level.setChunkForced(c.x, c.z, true);
                FORCED.put(key, 1);
            }
        } else if (n != null) {
            if (n > 1) FORCED.put(key, n - 1);
            else {
                FORCED.remove(key);
                level.setChunkForced(c.x, c.z, false);
            }
        }
    }

    static void forcePlot(GameTestHelper h, boolean on) {
        BlockPos a = h.absolutePos(new BlockPos(-16, 0, -16)), b = h.absolutePos(new BlockPos(32, 0, 32));
        for (int cx = Math.min(a.getX(), b.getX()) >> 4; cx <= Math.max(a.getX(), b.getX()) >> 4; cx++)
            for (int cz = Math.min(a.getZ(), b.getZ()) >> 4; cz <= Math.max(a.getZ(), b.getZ()) >> 4; cz++) force(h.getLevel(), new ChunkPos(cx, cz), on);
    }

    // ---- felling ------------------------------------------------------------------------------------------------

    /**
     * {@code building/fell_spruce_leaves_stump}: a 15 m white spruce (24 cm) is a 6-swing tree for the crate hatchet:
     * 2 notch swings from the north (the arrow locks toward the feller), 4 back-cut swings from the south, 24 game
     * minutes. The trunk and crown are gone, a stump stands at the base, the tree's mark is in {@code yearofice:trees},
     * and the falling tree lands as one felled log lying north, toward the notch, with 14 m of trunk.
     */
    static void fellSpruce(GameTestHelper h) {
        forcePlot(h, true);
        BlockPos base = new BlockPos(8, 2, 8);
        grow(h, base, TreeKind.WHITE_SPRUCE, 15, 12345);
        ServerPlayer p = withHatchet(h, "feller", new Vec3(8.5, 2, 4.5));
        BlockPos absBase = h.absolutePos(base);
        int leaves = count(h, new BlockPos(0, 1, 0), new BlockPos(16, 19, 16), s -> s.getBlock() instanceof YoiLeavesBlock);
        h.assertTrue(leaves > 20, "the test spruce has a crown (" + leaves + " leaves)");
        WorkJob j = WoodJobs.resolve(p, at(absBase.above(2), Direction.NORTH));
        h.assertTrue(j instanceof WoodJobs.Fell, "an axe on a natural trunk is a felling, got " + j);
        WoodJobs.Fell fell = (WoodJobs.Fell) j;
        h.assertTrue(fell.game().swings() == 6, "a 24 cm spruce takes the crate hatchet 6 swings, got " + fell.game().swings());
        long ds = WorkService.dsPerTick();
        int ticks = 0;
        while (!fell.game().notchDone() && ticks < 10_000) { WorkService.runFor(p, fell, 1); ticks++; }
        h.assertTrue(fell.game().notch() == 2 && fell.game().arrow() == 0, "notch 2 from the north: " + fell.game().notch() + " at " + fell.game().arrow());
        moveTo(h, p, new Vec3(8.5, 2, 12.5));
        WorkJob back = WoodJobs.resolve(p, at(absBase.above(2), Direction.SOUTH));
        h.assertTrue(back instanceof WoodJobs.Fell f2 && f2.game() == fell.game(), "walking round keeps the same tree's felling game");
        while (!back.finished() && ticks < 10_000) { WorkService.runFor(p, back, 1); ticks++; }
        double minutes = ticks * ds / 600.0;
        h.assertTrue(back.finished(), "the tree did not fall");
        h.assertTrue(Math.abs(minutes - 24) <= 1, "felling took " + minutes + " game minutes, want 24 +-1");
        h.assertTrue(fell.game().struck() == 6, "6 swings, struck " + fell.game().struck());
        int logs = count(h, new BlockPos(0, 1, 0), new BlockPos(16, 19, 16), s -> s.getBlock() instanceof YoiLogBlock);
        int left = count(h, new BlockPos(0, 1, 0), new BlockPos(16, 19, 16), s -> s.getBlock() instanceof YoiLeavesBlock);
        h.assertTrue(logs == 0 && left == 0, "trunk and crown cleared: " + logs + " logs, " + left + " leaves left");
        h.assertTrue(h.getLevel().getBlockState(absBase).getBlock() instanceof StumpBlock, "a stump at the base, found " + h.getLevel().getBlockState(absBase));
        h.assertTrue(TreeLedger.felled(h.getLevel(), absBase), "the tree's mark is in yearofice:trees");
        h.assertTrue(around(h, FallingTreeEntity.class, 4).size() == 1, "one falling tree turns about the stump");
        h.succeedWhen(() -> {
            List<FelledLogEntity> felled = around(h, FelledLogEntity.class, PLOT_R);
            h.assertTrue(around(h, FallingTreeEntity.class, 4).isEmpty() && felled.size() == 1, "waiting for it to land");
            FelledLogEntity log = felled.get(0);
            h.assertTrue(log.kind() == TreeKind.WHITE_SPRUCE && Math.abs(log.lengthM() - 14) < 1e-6 && !log.limbed(),
                    "one white spruce log of 14 m with its limbs on: " + log.kind() + " " + log.lengthM());
            h.assertTrue(Math.abs(log.bearing()) < 1e-3 && log.getBoundingBox().minZ < absBase.getZ() - 10,
                    "it fell north, toward the notch: bearing " + log.bearing() + ", box " + log.getBoundingBox());
            clearDrops(h);
            forcePlot(h, false);
        });
    }

    /**
     * {@code building/placed_logs_never_fall} (A1): in a 5 × 5 × 4 cabin of placed logs, cutting a window takes 3 game
     * minutes a block with the crate hatchet and removes exactly that one block each time, with no falling tree or
     * felled log; one window log is a plain log marked only in {@code placed_mask}, so the mask stops the search as
     * well as {@code placed=true}. A 15 m spruce felled against the wall clears only its own column: every wall log
     * stays.
     */
    static void placedLogsNeverFall(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        PlacedMask mask = PlacedMask.get(l);
        BlockPos min = new BlockPos(2, 2, 6), max = new BlockPos(6, 5, 10);
        List<BlockPos> wall = new ArrayList<>();
        for (BlockPos r : BlockPos.betweenClosed(min, max)) {
            boolean edge = r.getX() == 2 || r.getX() == 6 || r.getZ() == 6 || r.getZ() == 10;
            if (!edge) continue;
            BlockPos a = h.absolutePos(r);
            wall.add(a.immutable());
            l.setBlock(a, WorldBlocks.WHITE_SPRUCE_LOG.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            PlacedLogs.mark(l, a, l.getBlockState(a));
        }
        BlockPos win1 = h.absolutePos(new BlockPos(4, 3, 6)), win2 = h.absolutePos(new BlockPos(4, 4, 6));
        // win1 is a natural-state log that only the placed mask makes a piece.
        l.setBlock(win1, WorldBlocks.WHITE_SPRUCE_LOG.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        mask.mark(win1);
        try {
            ServerPlayer p = withHatchet(h, "builder", new Vec3(4.5, 2, 3.5));
            int before = wall.size();
            for (BlockPos w : List.of(win1, win2)) {
                h.assertTrue(FellingService.trunk(l, w) == null, "a cabin log is never a tree (" + w + ")");
                WorkJob j = WoodJobs.resolve(p, at(w, Direction.NORTH));
                h.assertTrue(j instanceof WoodJobs.CutBlock, "an axe on a placed log cuts one block, got " + j);
                double min2 = WorkService.runFor(p, j, 2000);
                h.assertTrue(Math.abs(min2 - 3) <= 0.1, "cut one block (placed log): 3 min, took " + min2);
                long standing = wall.stream().filter(b -> l.getBlockState(b).getBlock() instanceof YoiLogBlock).count();
                h.assertTrue(standing == before - 1, "exactly one block removed: " + (before - standing));
                before--;
                h.assertTrue(around(h, FallingTreeEntity.class, PLOT_R).isEmpty() && around(h, FelledLogEntity.class, PLOT_R).isEmpty(),
                        "no falling tree or felled log from a cabin");
            }
            h.assertTrue(countItem(p, h, WoodRegistry.ROUNDS.get(WoodKind.SPRUCE).get()) == 4, "2 rounds a cut block");
            // A natural spruce right against the north wall.
            BlockPos treeBase = new BlockPos(4, 2, 5);
            grow(h, treeBase, TreeKind.WHITE_SPRUCE, 15, 777);
            BlockPos absTree = h.absolutePos(treeBase);
            TrunkSearch.Trunk t = FellingService.trunk(l, absTree.above(3));
            h.assertTrue(t != null && t.height() == 15, "the spruce by the wall is its own 15-log column");
            FallingTreeEntity f = FellingService.fell(l, t, 0, p);
            f.discard();
            long walls = wall.stream().filter(b -> l.getBlockState(b).getBlock() instanceof YoiLogBlock).count();
            h.assertTrue(walls == wall.size() - 2, "every wall log stays after felling beside it: " + walls + " of " + (wall.size() - 2));
            h.assertTrue(l.getBlockState(absTree).getBlock() instanceof StumpBlock, "the spruce left its stump");
        } finally {
            mask.clear(h.absolutePos(new BlockPos(0, 0, 0)), h.absolutePos(new BlockPos(16, 19, 16)));
        }
        clearDrops(h);
        h.succeed();
    }

    /**
     * {@code building/sneak_cut_one_block} (A1): Sneak + use on a 6 m white spruce removes only its top block in 6 ±0.5
     * game minutes with the crate hatchet, gives 2 rounds, lowers the tree's height by 1 (its mark says 5) and never
     * starts a felling.
     */
    static void seasoningCalendar(GameTestHelper h) {
        var r = WoodConfig.rates();
        long jun1 = com.yearofice.sim.time.Calendar.dayOfYear(6, 1), may25 = com.yearofice.sim.time.Calendar.dayOfYear(5, 25);
        long shedJun = Seasoning.forecast(r, 50, jun1, 20, Seasoning.Place.WOODSHED, Seasoning.Form.SPLIT, 730);
        long rackJun = Seasoning.forecast(r, 50, jun1, 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        long rackMay = Seasoning.forecast(r, 50, may25, 20, Seasoning.Place.RACK, Seasoning.Form.SPLIT, 730);
        long round = Seasoning.forecast(r, 50, jun1, 20, Seasoning.Place.RACK, Seasoning.Form.ROUND, 730);
        h.assertTrue(shedJun > 0 && shedJun < rackJun, "woodshed beats the rack");
        h.assertTrue(rackJun < rackMay + 400 && rackMay <= rackJun, "an earlier cut is dry no later");
        h.assertTrue(round < 0 || round > rackJun, "a round is slower than a split");
        h.assertTrue(Seasoning.dry(r, 50, com.yearofice.sim.time.Calendar.dayOfYear(12, 1), com.yearofice.sim.time.Calendar.dayOfYear(12, 1) + 40, Seasoning.Place.RACK, Seasoning.Form.SPLIT) == 50,
                "no drying in December");
        h.succeed();
    }

    static void mishapRolls(GameTestHelper h) {
        var rng = new java.util.SplittableRandom(20261020L);
        int barber = 0, wedged = 0, hung = 0, safe = 0;
        double pb = WoodConfig.get("barber_chair_chance"), ph = WoodConfig.get("hang_up_chance");
        for (int i = 0; i < 1000; i++) {
            if (rng.nextDouble() < com.yearofice.sim.wood.Mishap.barberChance(20, true, false, false, pb)) barber++;
            if (rng.nextDouble() < com.yearofice.sim.wood.Mishap.barberChance(20, true, false, true, pb)) wedged++;
            if (rng.nextDouble() < com.yearofice.sim.wood.Mishap.hangUpChance(0.8, 12, 15, ph)) hung++;
            if (rng.nextDouble() < com.yearofice.sim.wood.Mishap.hangUpChance(3, 12, 15, ph)) safe++;
        }
        h.assertTrue(barber >= 260 && barber <= 340, "a 20 degree lean with the back cut first barber-chairs 30% +-4 (" + barber + "/1000)");
        h.assertTrue(wedged == 0 && safe == 0, "a felling wedge stops the barber chair and a clear line never hangs");
        h.assertTrue(hung >= 460 && hung <= 540, "a fall line within a block of a taller trunk hangs up 50% +-4 (" + hung + "/1000)");
        ServerPlayer p = player(h, "feller", new Vec3(1, 2, 1));
        for (var k : com.yearofice.sim.wood.Mishap.Kind.values())
            h.assertTrue(com.yearofice.body.injury.InjuryData.source(k.injurySource) != null, "injury source " + k.injurySource + " exists");
        h.assertTrue(Mishaps.apply(p, com.yearofice.sim.wood.Mishap.Kind.BARBER_CHAIR), "the kickback goes through the injury path");
        h.succeed();
    }

    static void sneakCut(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos base = new BlockPos(8, 2, 8);
        grow(h, base, TreeKind.WHITE_SPRUCE, 6, 99);
        BlockPos abs = h.absolutePos(base);
        ServerPlayer p = withHatchet(h, "sneaker", new Vec3(8.5, 2, 6.5));
        p.setShiftKeyDown(true);
        WorkJob j = WoodJobs.resolve(p, at(abs.above(), Direction.NORTH));
        h.assertTrue(j instanceof WoodJobs.CutTop, "Sneak on a trunk cuts its top block, got " + j);
        double m = WorkService.runFor(p, j, 2000);
        h.assertTrue(Math.abs(m - 6) <= 0.5, "cut the top block: 6 min, took " + m);
        TrunkSearch.Trunk t = FellingService.trunk(l, abs);
        h.assertTrue(t != null && t.height() == 5, "5 logs stand: " + (t == null ? "none" : t.height()));
        h.assertTrue(!(l.getBlockState(abs.above(5)).getBlock() instanceof YoiLogBlock), "the top block is gone");
        long mark = TreeLedger.markAt(l, abs);
        h.assertTrue(mark != 0 && TreeMarks.state(mark) == TreeMarks.State.TOPPED && TreeMarks.height(mark) == 5, "the mark says topped at 5");
        h.assertTrue(FellingService.gameIfAny(l, abs) == null && around(h, FallingTreeEntity.class, PLOT_R).isEmpty(), "no felling started");
        h.assertTrue(countItem(p, h, WoodRegistry.ROUNDS.get(WoodKind.SPRUCE).get()) == 2, "2 rounds");
        clearDrops(h);
        h.succeed();
    }

    // ---- bucking and firewood -----------------------------------------------------------------------------------

    /**
     * {@code building/buck_and_split}: a felled 9 m birch limbs in 10 game minutes (6 sticks as kindling), then bucks
     * into 16 rounds of 0.5 m at 0.75 min each (3 min an armload of 4), green at 50%; a round set on the chopping block
     * splits in 3 strikes off the crack and 1 on it (1 game minute a strike) into one split of the same wood and
     * moisture, which the fire ring burns as a quarter armload of birch. A round on a stump makes it a chopping block;
     * Sneak + use with a round sets a new one. A split stamped green on June 1 is seasoned under cover by August 21 (B4).
     */
    static void buckAndSplit(GameTestHelper h) {
        ServerLevel l = h.getLevel();
        BlockPos stump = h.absolutePos(new BlockPos(4, 2, 8));
        FelledLogEntity log = new FelledLogEntity(WoodRegistry.FELLED_LOG.get(), l);
        log.setup(stump, TreeKind.BIRCH, 9, 90f, 5, 8.0, 50, WoodItems.today());
        l.addFreshEntity(log);
        ServerPlayer p = withHatchet(h, "bucker", new Vec3(7.5, 2, 9.5));
        WorkJob limb = WoodJobs.resolve(p, at(log));
        h.assertTrue(limb instanceof WoodJobs.Limb, "a log with its limbs on limbs first, got " + limb);
        double m = WorkService.runFor(p, limb, 4000);
        h.assertTrue(Math.abs(m - 10) <= 0.1 && log.limbed(), "limb: 10 min with the crate hatchet, took " + m);
        h.assertTrue(countItem(p, h, FireRegistry.KINDLING.get()) == 6, "6 sticks as kindling");
        WorkJob buck = WoodJobs.resolve(p, at(log));
        h.assertTrue(buck instanceof WoodJobs.Buck, "a limbed log bucks, got " + buck);
        m = WorkService.runFor(p, buck, 8000);
        var roundItem = WoodRegistry.ROUNDS.get(WoodKind.BIRCH).get();
        int rounds = countItem(p, h, roundItem);
        h.assertTrue(rounds == 16 && log.isRemoved(), "an 8 m log bucks into 16 rounds: " + rounds);
        h.assertTrue(Math.abs(m - 12) <= 0.2, "16 rounds at 0.75 min: 12 min, took " + m);
        ItemStack sample = find(p, h, roundItem);
        h.assertTrue(WoodItems.moisturePct(sample) == 50, "rounds are green, 50%: " + WoodItems.moisturePct(sample));

        // Splitting.
        BlockPos blockPos = h.absolutePos(new BlockPos(8, 2, 8));
        l.setBlock(blockPos, WoodRegistry.CHOPPING_BLOCK.get().defaultBlockState(), Block.UPDATE_ALL);
        ChoppingBlockEntity be = (ChoppingBlockEntity) l.getBlockEntity(blockPos);
        moveTo(h, p, new Vec3(8.5, 2, 6.5));
        var splitItem = WoodRegistry.SPLITS.get(WoodKind.BIRCH).get();
        int[] strikes = {3, 1};
        double[][] aims = {{0.8, 0.5}, {0.5, 0.3}};
        for (int k = 0; k < 2; k++) {
            h.assertTrue(be.setRound(sample.copyWithCount(1), 0), "a round goes on the block");
            WorkJob j = WoodJobs.resolve(p, at(blockPos, Direction.UP));
            h.assertTrue(j instanceof WoodJobs.Split, "an axe on a round on the block splits it, got " + j);
            ((WoodJobs.Split) j).aim(aims[k][0], aims[k][1]);
            m = WorkService.runFor(p, j, 2000);
            h.assertTrue(Math.abs(m - strikes[k]) <= 0.05 && !be.hasRound(), (k == 0 ? "off the crack: 3 strikes" : "on the crack: 1 strike") + ", took " + m + " min");
        }
        h.assertTrue(countItem(p, h, splitItem) == 2, "two birch splits");
        ItemStack split = WoodItems.split(WoodKind.BIRCH, 1, 50, WoodItems.today());
        FuelData fuel = FireParts.fuel(split);
        h.assertTrue(fuel != null && fuel.wood().equals("birch") && Math.abs(fuel.armloadFraction() - 0.25) < 1e-9,
                "a split feeds the fire ring as a quarter armload of birch: " + fuel);
        h.assertTrue(FireParts.moisture(split) == 50, "the fire reads the split's moisture");
        h.assertTrue(FireParts.fuel(sample) == null, "a round is not split firewood");

        // B4 seasoning, under cover.
        long june1 = Calendar.dayOfYear(6, 1);
        WoodItems.stamp(split, 50, june1);
        double aug21 = WoodItems.moistureNow(split, Calendar.dayOfYear(8, 21));
        double rounds20 = WoodItems.moistureNow(WoodItems.round(WoodKind.BIRCH, 1, 50, june1), Calendar.dayOfYear(8, 21));
        h.assertTrue(aug21 < Seasoning.SEASONED_PCT && rounds20 > 30, "June 1 split seasoned by August 21 (" + aug21 + "%), a round not (" + rounds20 + "%)");
        WoodItems.settle(split, Calendar.dayOfYear(8, 21));
        h.assertTrue(FireParts.moisture(split) == Math.round(aug21) && FireParts.moisture(split) < 50,
                "settling updates the moisture the fire reads (whole percent of the exact " + aug21 + "): " + FireParts.moisture(split));

        // A round on a stump; Sneak + use sets a new chopping block.
        BlockPos s2 = h.absolutePos(new BlockPos(12, 2, 8));
        l.setBlock(s2, WorldBlocks.STUMP.get().defaultBlockState(), Block.UPDATE_ALL);
        ItemStack one = sample.copyWithCount(2);
        h.assertTrue(WoodJobs.roundOnStump(l, s2, one, p) && l.getBlockState(s2).getValue(ChoppingBlock.STUMP)
                && ((ChoppingBlockEntity) l.getBlockEntity(s2)).hasRound(), "any stump is a chopping block");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, one);
        p.setShiftKeyDown(true);
        BlockPos ground = h.absolutePos(new BlockPos(12, 1, 12));
        moveTo(h, p, new Vec3(11.5, 2, 10.5));
        WorkJob set = WoodJobs.resolve(p, at(ground, Direction.UP));
        h.assertTrue(set instanceof WoodJobs.SetBlock, "Sneak + use with a round sets a chopping block, got " + set);
        m = WorkService.runFor(p, set, 2000);
        h.assertTrue(Math.abs(m - 2) <= 0.05 && l.getBlockState(ground.above()).is(WoodRegistry.CHOPPING_BLOCK.get()), "set in 2 min, took " + m);
        clearDrops(h);
        h.succeed();
    }

    // ---- the trees attachment -----------------------------------------------------------------------------------

    /**
     * {@code trees/felled_never_regrow}: in the slice test dimension a planned live tree is felled; its mark is in the
     * chunk's {@code yearofice:trees}, and after a full chunk save and read ({@code ChunkSerializer} write and read, the
     * path a reload takes) the chunk still holds the stump and the mark, so the tree's TreePlan entry reads as felled
     * and never stands again. The test fells in a corner chunk no other test reads and leaves the stump (a felled tree
     * is the world's truth from then on).
     */
    static void felledNeverRegrow(GameTestHelper h) {
        ServerLevel l = h.getLevel().getServer().getLevel(M2WorldGameTests.SLICE);
        if (l == null) throw new GameTestAssertException("no yearofice:slice_test dimension");
        ValleyWorld w = ((YoiChunkGenerator) l.getChunkSource().getGenerator()).bind(l.getSeed());
        int[] s = w.spawn();
        TreeLedger.Planned found = null;
        LevelChunk chunk = null;
        int[][] quads = {{-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
        search:
        for (int d : new int[] {24, 20, 16, 12}) {
            for (int k = 0; k < 16; k++) {
                int cx = (s[0] >> 4) + quads[k / 4][0] * d + (k & 1), cz = (s[2] >> 4) + quads[k / 4][1] * d + ((k >> 1) & 1);
                LevelChunk c = l.getChunk(cx, cz);
                for (TreeLedger.Planned pl : TreeLedger.planned(l, c)) {
                    TrunkSearch.Trunk t = FellingService.trunk(l, pl.base());
                    if (pl.standing() && t != null && t.height() == pl.tree().height() && pl.tree().height() >= 5) {
                        found = pl;
                        chunk = c;
                        break search;
                    }
                }
            }
        }
        h.assertTrue(found != null, "no standing planned tree found near the slice's corner");
        BlockPos base = found.base();
        TrunkSearch.Trunk t = FellingService.trunk(l, base);
        FellingService.fell(l, t, 90, null).discard();
        h.assertTrue(TreeLedger.felled(l, base), "the mark is filed");
        h.assertTrue(!TreeLedger.plannedAt(l, base).standing(), "the plan entry reads as felled");
        CompoundTag saved = ChunkSerializer.write(l, chunk);
        ProtoChunk back = ChunkSerializer.read(l, l.getPoiManager(), new RegionStorageInfo("gametest", l.dimension(), "chunk"), chunk.getPos(), saved);
        TreeMarksData d = back.getExistingDataOrNull(WorldAttachments.TREES);
        h.assertTrue(d != null && d.has(base.getX() & 15, base.getZ() & 15, base.getY()), "the mark survives the chunk save and read");
        TreeLedger.Planned again = null;
        for (TreeLedger.Planned pl : TreeLedger.planned(l, back)) if (pl.base().equals(base)) again = pl;
        h.assertTrue(again != null && !again.standing(), "after the reload the tree's plan entry is still felled");
        h.assertTrue(back.getBlockState(base).getBlock() instanceof StumpBlock, "the reloaded chunk has the stump: " + back.getBlockState(base));
        h.assertTrue(!(back.getBlockState(base.above()).getBlock() instanceof YoiLogBlock), "and no trunk above it");
        h.succeed();
    }

    /**
     * {@code trees/save_budget_1000} (Ready for development › M2: under 20 KB of mod data for 1,000 felled trees): 1,000
     * marks over a woodlot of 8 × 8 chunks, measured as each chunk's attachment entry would be written in the chunk
     * save (named tag, uncompressed), are under 20,480 bytes and read back identical. Measured directly from the
     * serialized attachments rather than by felling 1,000 trees (RECONCILE 185); one real chunk's attachment save is
     * checked to use the same tag.
     */
    static void saveBudget(GameTestHelper h) {
        Random rng = new Random(1000);
        Map<Long, TreeMarksData> chunks = new HashMap<>();
        Set<String> used = new HashSet<>();
        int n = 0;
        while (n < 1000) {
            int cx = rng.nextInt(8), cz = rng.nextInt(8), lx = rng.nextInt(16), lz = rng.nextInt(16), y = 60 + rng.nextInt(40);
            if (!used.add(cx + "," + cz + "," + lx + "," + lz)) continue;
            chunks.computeIfAbsent(ChunkPos.asLong(cx, cz), k -> new TreeMarksData())
                    .put(TreeMarks.pack(lx, lz, y, TreeMarks.State.FELLED, 0, 10 + rng.nextInt(3000)));
            n++;
        }
        long bytes = 0;
        try {
            for (TreeMarksData d : chunks.values()) {
                CompoundTag entry = new CompoundTag();
                entry.put("yearofice:trees", d.saveTag());
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                NbtIo.write(entry, new DataOutputStream(bo));
                bytes += bo.size();
                TreeMarksData back = TreeMarksData.load(entry.get("yearofice:trees"));
                h.assertTrue(java.util.Arrays.equals(back.all(), d.all()), "a chunk's marks read back identical");
            }
        } catch (IOException e) {
            throw new GameTestAssertException(e.toString());
        }
        long b = bytes;
        YearOfIce.LOG.info("trees/save_budget_1000: {} bytes for 1000 felled trees in {} chunks", b, chunks.size());
        h.assertTrue(b < 20_480, "1,000 felled trees take " + b + " bytes of mod data, budget 20,480");
        // One real chunk writes the same tag under the attachment's id.
        LevelChunk c = h.getLevel().getChunkAt(h.absolutePos(BlockPos.ZERO));
        boolean had = c.hasData(WorldAttachments.TREES);
        TreeMarksData d = c.getData(WorldAttachments.TREES);
        long mark = TreeMarks.pack(1, 2, 64, TreeMarks.State.FELLED, 0, 5);
        long before = d.get(1, 2, 64);
        d.put(mark);
        CompoundTag all = c.writeAttachmentsToNBT(h.getLevel().registryAccess());
        Tag tag = all == null ? null : all.get("yearofice:trees");
        h.assertTrue(tag instanceof net.minecraft.nbt.LongArrayTag la && java.util.Arrays.stream(la.getAsLongArray()).anyMatch(v -> v == mark),
                "the chunk save holds the mark as a long array: " + tag);
        if (!had) c.removeData(WorldAttachments.TREES);
        else if (before != 0) d.put(before);
        else d.remove(1, 2, 64);
        h.succeed();
    }
}

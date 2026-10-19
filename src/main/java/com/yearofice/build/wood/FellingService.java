package com.yearofice.build.wood;

import com.yearofice.sim.wood.Bucking;
import com.yearofice.sim.wood.Felling;
import com.yearofice.sim.wood.FellingGame;
import com.yearofice.sim.wood.ToolStats;
import com.yearofice.sim.wood.TreeKind;
import com.yearofice.sim.wood.TreeMarks;
import com.yearofice.sim.wood.TrunkSearch;
import com.yearofice.world.logs.FallingTreeEntity;
import com.yearofice.worldgen.block.WorldBlocks;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * {@code build.wood.FellingService} (Trees, felling and firewood › Build; video 2, A1): finds a tree's trunk from
 * its blocks, keeps each tree's felling game while the feller walks round, and changes the world only when a felling
 * finishes: the trunk column and its crown are cleared, the base becomes a {@code stump}, the tree's mark goes into
 * {@code yearofice:trees}, and a {@code falling_tree} turns about the base and lands as one {@code felled_log}.
 *
 * <p>The search stops at every placed block, so a cabin's logs are never part of a tree. A placed log is cut out one
 * block at a time; Sneak on a standing trunk cuts its top block. Block writes use {@code UPDATE_CLIENTS |
 * UPDATE_KNOWN_SHAPE} (Layer 9), so nothing cascades.
 */
public final class FellingService {
    private FellingService() {}

    static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    /** Felling games under way, by the tree's base: release use, walk round, and the notch is still there. */
    private static final Map<GlobalPos, FellingGame> GAMES = new HashMap<>();

    /** The rooted natural trunk through {@code pos}, or null. */
    public static @Nullable TrunkSearch.Trunk trunk(ServerLevel level, BlockPos pos) {
        return TrunkSearch.find(WoodBlocks.probe(level), pos.getX(), pos.getY(), pos.getZ());
    }

    public static BlockPos base(TrunkSearch.Trunk t) { return new BlockPos(t.base()[0], t.base()[1], t.base()[2]); }

    public static TreeKind kind(TrunkSearch.Trunk t) { return TreeKind.byOrdinal(t.kind()); }

    /** The trunk's diameter in cm by its kind and height (1 block = 1 m). */
    public static double diameterCm(TrunkSearch.Trunk t) { return kind(t).diameterCm(t.height()); }

    /** The felling game of the tree at {@code base}, made when the first swing lands. */
    public static synchronized FellingGame game(ServerLevel level, BlockPos base, ToolStats tool, double dCm) {
        GlobalPos key = GlobalPos.of(level.dimension(), base.immutable());
        FellingGame g = GAMES.get(key);
        if (g == null) {
            g = new FellingGame(Felling.swings(tool.swings25(), dCm));
            GAMES.put(key, g);
        }
        return g;
    }

    public static synchronized @Nullable FellingGame gameIfAny(ServerLevel level, BlockPos base) {
        return GAMES.get(GlobalPos.of(level.dimension(), base.immutable()));
    }

    static synchronized void forget(ServerLevel level, BlockPos base) { GAMES.remove(GlobalPos.of(level.dimension(), base.immutable())); }

    /** Forgets every game (server stop). */
    public static synchronized void reset() { GAMES.clear(); }

    /**
     * Fells {@code t} toward compass bearing {@code bearing}: clears its column and crown, sets the stump, files the
     * mark and spawns the falling tree. Returns the falling tree.
     */
    public static FallingTreeEntity fell(ServerLevel level, TrunkSearch.Trunk t, double bearing, @Nullable ServerPlayer by) {
        TreeKind k = kind(t);
        BlockPos base = base(t);
        List<int[]> crown = TrunkSearch.crown(WoodBlocks.probe(level), t);
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int[] p : crown) level.setBlock(mp.set(p[0], p[1], p[2]), Blocks.AIR.defaultBlockState(), FLAGS);
        List<int[]> logs = t.logs();
        for (int i = logs.size() - 1; i >= 1; i--) level.setBlock(mp.set(logs.get(i)[0], logs.get(i)[1], logs.get(i)[2]), Blocks.AIR.defaultBlockState(), FLAGS);
        level.setBlock(base, WorldBlocks.STUMP.get().defaultBlockState(), FLAGS);
        TreeLedger.mark(level, base, TreeMarks.State.FELLED, 0, WoodItems.today());
        forget(level, base);
        double secs = Felling.fallSeconds(t.height(), WoodConfig.get("fall_min_s"), WoodConfig.get("fall_max_s"));
        TreeLedger.Planned plan = TreeLedger.plannedAt(level, base);
        FallingTreeEntity e = new FallingTreeEntity(WoodRegistry.FALLING_TREE.get(), level);
        e.setup(base, k, t.height(), (float) bearing, plan != null ? plan.tree().seed() : seedOf(base), (int) Math.round(secs * 20));
        level.addFreshEntity(e);
        return e;
    }

    /** A tree's shape seed from its base, the same one the worldgen draws its crown from where the plan has it. */
    public static int seedOf(BlockPos base) { return (int) (base.asLong() * 0x9E3779B97F4A7C15L >>> 32); }

    /** Cuts the top block of a standing trunk (Sneak): its crown layer goes with it; at the base it becomes a stump. */
    public static void cutTop(ServerLevel level, TrunkSearch.Trunk t, @Nullable ServerPlayer by) {
        TreeKind k = kind(t);
        BlockPos base = base(t);
        int[] top = t.top();
        if (t.height() > 1) {
            BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
            for (int[] p : TrunkSearch.crown(WoodBlocks.probe(level), t)) {
                if (p[1] >= top[1]) level.setBlock(mp.set(p[0], p[1], p[2]), Blocks.AIR.defaultBlockState(), FLAGS);
            }
            level.setBlock(new BlockPos(top[0], top[1], top[2]), Blocks.AIR.defaultBlockState(), FLAGS);
            TreeLedger.mark(level, base, TreeMarks.State.TOPPED, t.height() - 1, WoodItems.today());
        } else {
            for (int[] p : TrunkSearch.crown(WoodBlocks.probe(level), t)) level.setBlock(new BlockPos(p[0], p[1], p[2]), Blocks.AIR.defaultBlockState(), FLAGS);
            level.setBlock(base, WorldBlocks.STUMP.get().defaultBlockState(), FLAGS);
            TreeLedger.mark(level, base, TreeMarks.State.FELLED, 0, WoodItems.today());
        }
        forget(level, base);
        dropRounds(level, k, new BlockPos(top[0], top[1], top[2]), WoodConfig.get("green_moisture_pct"), by);
    }

    /** Cuts one placed (or unrooted) log block out: that block only, as 2 rounds; nothing else moves. */
    public static void cutBlock(ServerLevel level, BlockPos pos, @Nullable ServerPlayer by) {
        BlockState s = level.getBlockState(pos);
        TreeKind k = WoodBlocks.kindOf(s.getBlock());
        boolean placed = WoodBlocks.isStop(level, pos, s);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        com.yearofice.worldgen.structure.PlacedMask.get(level).unmark(pos);
        if (k != null) dropRounds(level, k, pos, WoodConfig.get(placed ? "placed_log_moisture_pct" : "green_moisture_pct"), by);
    }

    static void dropRounds(ServerLevel level, TreeKind k, BlockPos at, double moisture, @Nullable ServerPlayer by) {
        var rounds = WoodItems.round(k.wood, Bucking.ROUNDS_PER_BLOCK, moisture, WoodItems.today());
        if (by != null) WoodItems.give(by, rounds);
        else net.minecraft.world.level.block.Block.popResource(level, at, rounds);
        level.playSound(null, at, WoodRegistry.TREE_CHOP.get(), SoundSource.BLOCKS, 0.9f, 0.8f);
    }
}

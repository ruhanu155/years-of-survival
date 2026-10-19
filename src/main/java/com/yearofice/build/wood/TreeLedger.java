package com.yearofice.build.wood;

import com.yearofice.sim.wood.TreeMarks;
import com.yearofice.world.WorldHooks;
import com.yearofice.world.attach.TreeMarksData;
import com.yearofice.world.attach.WorldAttachments;
import com.yearofice.worldgen.TreePlan;
import com.yearofice.worldgen.TreePlacer;
import com.yearofice.worldgen.ValleyWorld;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

/**
 * The felled-tree ledger over the {@code yearofice:trees} chunk attachment: Building writes it (felling and the sneak
 * cut), anyone may read it. A tree's plan entry stands until a mark says otherwise, and a mark is saved with its
 * chunk, so a felled tree never stands again after a reload.
 */
public final class TreeLedger {
    private TreeLedger() {}

    /** Files a mark for the tree whose base is {@code base}. */
    public static void mark(ServerLevel level, BlockPos base, TreeMarks.State state, int height, long day) {
        ChunkAccess c = level.getChunk(base);
        c.getData(WorldAttachments.TREES).put(TreeMarks.pack(base.getX() & 15, base.getZ() & 15, base.getY(), state, height, day));
        c.setUnsaved(true);
    }

    /** The mark of the tree based at {@code base}, or 0 when it stands as planned. */
    public static long markAt(ServerLevel level, BlockPos base) {
        TreeMarksData d = level.getChunk(base).getExistingDataOrNull(WorldAttachments.TREES);
        return d == null ? 0 : d.get(base.getX() & 15, base.getZ() & 15, base.getY());
    }

    public static boolean felled(ServerLevel level, BlockPos base) {
        long m = markAt(level, base);
        return m != 0 && TreeMarks.state(m) == TreeMarks.State.FELLED;
    }

    /** The marks of one chunk (a debug and test view). */
    public static long[] marks(ChunkAccess c) {
        TreeMarksData d = c.getExistingDataOrNull(WorldAttachments.TREES);
        return d == null ? new long[0] : d.all();
    }

    /** A planned tree and what became of it. */
    public record Planned(TreePlan.Tree tree, BlockPos base, long mark) {
        public boolean standing() { return mark == 0 || TreeMarks.state(mark) != TreeMarks.State.FELLED; }

        /** Logs standing: the plan's height, or what a sneak cut left. */
        public int height() { return mark != 0 && TreeMarks.state(mark) == TreeMarks.State.TOPPED ? TreeMarks.height(mark) : tree.height(); }
    }

    /**
     * The live trees TreePlan puts in a chunk, each with its mark, for a level with our generator (empty otherwise).
     * {@code chunk} may be any loaded or freshly read chunk, so the reload test can ask a chunk read back from its save.
     */
    public static List<Planned> planned(ServerLevel level, ChunkAccess chunk) {
        ValleyWorld w = WorldHooks.world(level);
        List<Planned> out = new ArrayList<>();
        if (w == null) return out;
        ChunkPos cp = chunk.getPos();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        ValleyWorld.Columns cols = TreePlacer.columnsFor(w, x0, z0);
        TreeMarksData d = chunk.getExistingDataOrNull(WorldAttachments.TREES);
        for (TreePlan.Tree t : TreePlan.treesIn(w, x0, z0, x0 + 16, z0 + 16)) {
            if (t.kind() != TreePlan.Kind.LIVE) continue;
            int y = TreePlacer.baseY(cols, t.x(), t.z());
            if (y == ValleyWorld.NONE) continue;
            long m = d == null ? 0 : d.get(t.x() & 15, t.z() & 15, y);
            out.add(new Planned(t, new BlockPos(t.x(), y, t.z()), m));
        }
        return out;
    }

    /** The plan entry whose base is {@code base}, or null. */
    public static @Nullable Planned plannedAt(ServerLevel level, BlockPos base) {
        for (Planned p : planned(level, level.getChunk(base))) if (p.base().equals(base)) return p;
        return null;
    }
}

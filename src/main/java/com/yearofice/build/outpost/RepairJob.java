package com.yearofice.build.outpost;

import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Repairs an outpost one piece at a time (Maintenance loops under the tedium rule): each missing piece takes
 * {@value #DS_PER_PIECE} game deciseconds of work, replaced in layout order, floor and walls before the roof.
 * ponytail: a flat time per piece, no materials spent — upgrade: M8 lane A's piece costs and refunds.
 */
public final class RepairJob implements WorkJob {
    public static final long DS_PER_PIECE = 100;
    private final ServerLevel level;
    private final List<Outposts.Piece> todo;
    private long carry;
    private int done;
    private final int total;

    public RepairJob(ServerLevel level, List<Outposts.Piece> todo) {
        this.level = level;
        this.todo = new java.util.ArrayList<>(todo);
        this.todo.sort(java.util.Comparator.comparingInt(p -> p.role().equals("roof") ? 1 : 0));
        this.total = todo.size();
    }

    @Override public boolean valid(ServerPlayer p) { return p.serverLevel() == level; }

    @Override
    public void work(ServerPlayer p, long ds) {
        carry += ds;
        while (carry >= DS_PER_PIECE && done < total) {
            carry -= DS_PER_PIECE;
            Outposts.Piece pc = todo.get(done++);
            level.setBlock(pc.pos(), pc.state(), 3);
        }
    }

    @Override public boolean finished() { return done >= total; }
    @Override public float share() { return total == 0 ? 1f : (done + carry / (float) DS_PER_PIECE) / total; }
    @Override public float sharePerDs() { return total == 0 ? 0f : 1f / (DS_PER_PIECE * total); }
    @Override public WorkProgressPayload.Phase phase() { return WorkProgressPayload.Phase.CRAFT; }
    @Override public int done() { return done; }
    @Override public int of() { return total; }
}

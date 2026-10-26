package com.yearofice.build.outpost;

import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Repairs an outpost one piece at a time (Maintenance loops under the tedium rule): each missing piece takes
 * {@value #DS_PER_PIECE} game deciseconds of work, replaced in layout order, floor and walls before the roof.
 * Each piece spends its {@code piece.json} materials from the player's pack; with them short the job waits (and says
 * so). Creative players pay nothing.
 */
public final class RepairJob implements WorkJob {
    public static final long DS_PER_PIECE = 100;
    private final ServerLevel level;
    private final List<Outposts.Piece> todo;
    private long carry;
    private int done;
    private final int total;
    /** In play, an outpost's vanilla planks, logs and slabs cost one of themselves each (a GameTest's direct job leaves them free). */
    private final boolean priceVanilla;

    public RepairJob(ServerLevel level, List<Outposts.Piece> todo) { this(level, todo, false); }

    public RepairJob(ServerLevel level, List<Outposts.Piece> todo, boolean priceVanilla) {
        this.priceVanilla = priceVanilla;
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
            Outposts.Piece pc = todo.get(done);
            if (!pay(p, pc, false)) { carry = DS_PER_PIECE; return; }
            pay(p, pc, true);
            done++;
            level.setBlock(pc.pos(), pc.state(), 3);
            if (done >= total) {
                var site = OutpostSites.get(level).at(pc.pos());
                if (site != null && com.yearofice.api.story.AmbitionTracker.tallySet(p, "build.outposts_linked", site.getKey().asLong() + "") >= 3) {
                    com.yearofice.story.ambition.Ambitions.fire(p, "build.outposts_linked_3", com.yearofice.api.clock.ClockService.installed() ? com.yearofice.api.clock.ClockService.get().nowMinute() : 0);
                }
            }
        }
    }

    /** The materials a piece costs, from its data row (none for a piece without one). */
    public static java.util.List<com.yearofice.api.build.PieceData.Material> cost(Outposts.Piece pc) {
        var row = pc.state().getBlock().builtInRegistryHolder().getData(com.yearofice.api.build.PieceData.MAP);
        return row == null ? java.util.List.of() : row.materials();
    }

    /** What this job charges for a piece: the piece.json row, else (in play) one of the block's own item, else nothing. */
    java.util.List<com.yearofice.api.build.PieceData.Material> price(Outposts.Piece pc) {
        var c = cost(pc);
        if (!c.isEmpty() || !priceVanilla) return c;
        var item = pc.state().getBlock().asItem();
        if (item == net.minecraft.world.item.Items.AIR || pc.state().is(net.minecraft.world.level.block.Blocks.BARREL)) return c;
        return java.util.List.of(new com.yearofice.api.build.PieceData.Material(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item), 1));
    }

    private boolean pay(ServerPlayer p, Outposts.Piece pc, boolean take) {
        if (p.getAbilities().instabuild) return true;
        for (var m : price(pc)) {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(m.item());
            int have = 0;
            for (var s : p.getInventory().items) if (s.is(item)) have += s.getCount();
            if (have < m.count()) {
                if (!take && p.tickCount % 100 == 0) p.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Repair needs " + m.count() + " x " + item.getDescription().getString() + " in your pack."), true);
                return false;
            }
            if (take) {
                int left = m.count();
                for (var s : p.getInventory().items) {
                    if (left <= 0) break;
                    if (!s.is(item)) continue;
                    int t = Math.min(left, s.getCount());
                    s.shrink(t);
                    left -= t;
                }
            }
        }
        return true;
    }

    @Override public boolean finished() { return done >= total; }
    @Override public float share() { return total == 0 ? 1f : (done + carry / (float) DS_PER_PIECE) / total; }
    @Override public float sharePerDs() { return total == 0 ? 0f : 1f / (DS_PER_PIECE * total); }
    @Override public WorkProgressPayload.Phase phase() { return WorkProgressPayload.Phase.CRAFT; }
    @Override public int done() { return done; }
    @Override public int of() { return total; }
}

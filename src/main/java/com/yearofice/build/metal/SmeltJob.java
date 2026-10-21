package com.yearofice.build.metal;

import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** Pumping a bloomery for one smelt (6 game hours at Standard): holding use at the furnace while it holds ore and charcoal. */
public final class SmeltJob implements WorkJob {
    private final BlockPos pos;
    private final long needDs;
    private long ds;
    private boolean done;

    public SmeltJob(BlockPos pos) {
        this.pos = pos;
        this.needDs = Math.max(1, Math.round(MetalConfig.get("smelt_hours") * 36_000));
    }

    @Override
    public boolean valid(ServerPlayer p) {
        return p.serverLevel().getBlockEntity(pos) instanceof BloomeryBlockEntity be && be.canSmelt()
                && p.getEyePosition().distanceTo(pos.getCenter()) <= MetalConfig.get("work_reach_m") + 2;
    }

    @Override
    public void work(ServerPlayer p, long step) {
        if (done) return;
        ds += step;
        if (ds >= needDs && p.serverLevel().getBlockEntity(pos) instanceof BloomeryBlockEntity be) {
            be.finishSmelt();
            done = true;
        }
    }

    @Override public boolean finished() { return done; }
    @Override public float share() { return Math.min(1f, ds / (float) needDs); }
    @Override public float sharePerDs() { return 1f / needDs; }
    @Override public WorkProgressPayload.Phase phase() { return WorkProgressPayload.Phase.HEAT; }
    @Override public int done() { return 0; }
    @Override public int of() { return 0; }
}

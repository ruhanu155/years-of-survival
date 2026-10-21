package com.yearofice.build.metal;

import com.yearofice.api.practice.PracticeService;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.build.Smelt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Consolidating a bloom into bar iron (Tier 4 › Consolidate): 1 bloom and 3 kg of charcoal, 2 game hours (x0.7 at the
 * forge), in three phases of one hold: heat the bloom, hammer it, quench it. 2.0 kg of bar, 2.5 kg once Practiced.
 */
public final class ForgeJob implements WorkJob {
    /** Heat to this share of the job, then hammer to {@link #HAMMER_END}, then quench. */
    static final float HEAT_END = 0.35f, HAMMER_END = 0.85f;

    private final BlockPos pos;
    private final long needDs;
    private long ds;
    private boolean done;

    public ForgeJob(BlockPos pos, boolean atForge) {
        this.pos = pos;
        this.needDs = Math.max(1, Math.round(MetalConfig.get("consolidate_hours") * 36_000 * (atForge ? 0.7 : 1.0)));
    }

    static int charcoalIn(ServerPlayer p) {
        int n = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(MetalRegistry.CHARCOAL.get())) n += s.getCount();
        return n;
    }

    @Override
    public boolean valid(ServerPlayer p) {
        return p.getMainHandItem().is(MetalRegistry.IRON_BLOOM.get()) && charcoalIn(p) >= Smelt.CHARCOAL_PER_CONSOLIDATION
                && p.getEyePosition().distanceTo(pos.getCenter()) <= MetalConfig.get("work_reach_m") + 2;
    }

    @Override
    public void work(ServerPlayer p, long step) {
        if (done) return;
        ds += step;
        FurnaceDraft.post(p.serverLevel(), pos, true);
        if (ds < needDs) return;
        done = true;
        FurnaceDraft.post(p.serverLevel(), pos, false);
        p.getMainHandItem().shrink(1);
        int need = Smelt.CHARCOAL_PER_CONSOLIDATION;
        for (ItemStack s : p.getInventory().items) {
            if (need <= 0) break;
            if (!s.is(MetalRegistry.CHARCOAL.get())) continue;
            int t = Math.min(need, s.getCount());
            s.shrink(t);
            need -= t;
        }
        boolean practiced = PracticeService.installed() && PracticeService.get().practiced(p, Skill.SMITHING);
        ItemStack bars = new ItemStack(MetalRegistry.IRON_BAR.get(), Smelt.barUnits(practiced));
        if (!p.getInventory().add(bars)) p.drop(bars, false);
        if (PracticeService.installed()) PracticeService.get().add(p, Skill.SMITHING, 1);
    }

    @Override
    public void stopped(ServerPlayer p) { FurnaceDraft.post(p.serverLevel(), pos, false); }

    @Override public boolean finished() { return done; }
    @Override public float share() { return Math.min(1f, ds / (float) needDs); }
    @Override public float sharePerDs() { return 1f / needDs; }

    @Override
    public WorkProgressPayload.Phase phase() {
        float s = share();
        return s < HEAT_END ? WorkProgressPayload.Phase.HEAT : s < HAMMER_END ? WorkProgressPayload.Phase.HAMMER : WorkProgressPayload.Phase.QUENCH;
    }

    @Override public int done() { return 0; }
    @Override public int of() { return 0; }
}

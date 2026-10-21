package com.yearofice.build.metal;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.food.FoodConfig;
import com.yearofice.food.pan.PanJob;
import com.yearofice.food.pan.PanRegistry;
import com.yearofice.food.pan.PanTargets;
import com.yearofice.sim.food.BlackSand;
import com.yearofice.sim.time.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Shovelling gravel through a sluice box (video 2, A10): the pan job's hour of work, at the sluice multiplier
 * ({@link BlackSand#sluiceMult()}, 3) over the same placer or plain gravel, same open-water months, same seeded draw.
 */
public final class SluiceJob implements WorkJob {
    private static long loadCounter;
    private final BlockPos box;
    private final boolean placer;
    private long ds;
    private boolean done;
    private int monthOverride;
    private int units;

    public SluiceJob(BlockPos box, boolean placer) {
        this.box = box;
        this.placer = placer;
    }

    public void pinMonth(int m) { monthOverride = m; }

    /** Units (100 g) the last finished load gave. */
    public int units() { return units; }

    /** A sluice box over wet gravel, or null. */
    public static SluiceJob resolve(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        if (!level.getBlockState(pos).is(MetalRegistry.SLUICE_BOX.get())) return null;
        BlockPos bed = pos.below();
        if (!PanTargets.isGravel(level.getBlockState(bed)) || !(PanJob.wet(level, bed) || PanJob.wet(level, pos))) return null;
        return new SluiceJob(pos, PanJob.richGravel(level, bed));
    }

    @Override
    public boolean valid(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        return level.getBlockState(box).is(MetalRegistry.SLUICE_BOX.get()) && PanTargets.isGravel(level.getBlockState(box.below()))
                && p.getEyePosition().distanceTo(box.getCenter()) <= MetalConfig.get("work_reach_m") + 2;
    }

    @Override
    public void work(ServerPlayer p, long step) {
        if (done) return;
        ds += step;
        if (p.tickCount % 10 == 0) p.serverLevel().playSound(null, box, MetalRegistry.SLUICE_WASH.get(), SoundSource.PLAYERS, 0.5f, 1f);
        if (ds < 36_000) return;
        done = true;
        int month = monthOverride > 0 ? monthOverride : Calendar.month(ClockService.get().dayOfYear());
        double kg = FoodConfig.pan().kgPerHour(BlackSand.Tool.SLUICE, placer, month);
        units = PanJob.units(kg, p.serverLevel().getSeed(), box, loadCounter++);
        if (units > 0) {
            ItemStack out = new ItemStack(PanRegistry.BLACK_SAND.get(), units);
            if (!p.getInventory().add(out)) p.drop(out, false);
        }
        HudBus.get().post(p, HudLine.of(HudLineType.PICKUP, units > 0 ? "pan_black_sand" : "pan_nothing", "food.pan"));
    }

    @Override public boolean finished() { return done; }
    @Override public float share() { return Math.min(1f, ds / 36_000f); }
    @Override public float sharePerDs() { return 1f / 36_000f; }
    @Override public WorkProgressPayload.Phase phase() { return WorkProgressPayload.Phase.PAN; }
    @Override public int done() { return 0; }
    @Override public int of() { return 0; }
}

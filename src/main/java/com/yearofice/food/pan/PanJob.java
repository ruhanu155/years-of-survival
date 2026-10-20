package com.yearofice.food.pan;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.hud.HudBus;
import com.yearofice.api.hud.HudLine;
import com.yearofice.api.hud.HudLineType;
import com.yearofice.build.work.WorkJob;
import com.yearofice.build.work.WorkProgressPayload;
import com.yearofice.food.FoodConfig;
import com.yearofice.sim.decay.Thresholds;
import com.yearofice.sim.food.BlackSand;
import com.yearofice.sim.time.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The pan action of hold-to-work (video 2, A10): holding use with a gold pan or a birch pan on gravel under moving
 * water. One pan load is one game hour of swirling; it gives the tool's kg per game hour of black sand in 100 g units,
 * the fraction left to a seeded draw keyed by the place and the load (so the same load never rerolls). Open water
 * only, May to October. Placer gravel (or gravel inside a placer reach, {@link PlacerSites}) gives 0.3 kg an hour;
 * any other gravel 0.1; a birch pan works at half speed.
 */
public final class PanJob implements WorkJob {
    /** Game deciseconds in one game hour. */
    static final long HOUR_DS = 36_000;
    /** Black sand is counted in 100 g units. */
    public static final double UNIT_KG = 0.1;

    private final BlockPos pos;
    private final BlackSand.Tool tool;
    private final boolean placer;
    private long ds;
    private boolean done;
    private int loads;
    /** GameTests pin the month; 0 reads the calendar. */
    private int monthOverride;
    /** Counts every load of every job, so a repeat pan on the same gravel draws fresh. */
    private static long loadCounter;

    public PanJob(BlockPos pos, BlackSand.Tool tool, boolean placer) {
        this.pos = pos;
        this.tool = tool;
        this.placer = placer;
    }

    public void pinMonth(int m) { monthOverride = m; }

    public int loads() { return loads; }

    public static BlackSand.Tool toolOf(ItemStack s) { return s.is(PanRegistry.BIRCH_PAN.get()) ? BlackSand.Tool.BIRCH_PAN : BlackSand.Tool.GOLD_PAN; }

    /** Is this gravel placer gravel: the flagged block, or plain gravel inside a placer reach? */
    public static boolean richGravel(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (s.is(PanRegistry.PLACER_GRAVEL.get())) return true;
        return PlacerSites.reachAt(level, pos);
    }

    /** Gravel with water above or beside it: a creek bed, not a dry bar. */
    public static boolean wet(ServerLevel level, BlockPos pos) {
        if (level.getFluidState(pos.above()).is(FluidTags.WATER)) return true;
        for (Direction d : Direction.Plane.HORIZONTAL) if (level.getFluidState(pos.relative(d)).is(FluidTags.WATER)) return true;
        return false;
    }

    public static PanJob resolve(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.serverLevel();
        ItemStack held = p.getMainHandItem();
        if (!PanTargets.isPan(held) || !PanTargets.isGravel(level.getBlockState(pos)) || !wet(level, pos)) return null;
        return new PanJob(pos, toolOf(held), richGravel(level, pos));
    }

    private int month(ServerLevel level) {
        if (monthOverride > 0) return monthOverride;
        return Calendar.month(ClockService.get().dayOfYear());
    }

    @Override
    public boolean valid(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        return PanTargets.isPan(p.getMainHandItem()) && PanTargets.isGravel(level.getBlockState(pos))
                && p.getEyePosition().distanceTo(pos.getCenter()) <= 6;
    }

    @Override
    public void work(ServerPlayer p, long step) {
        if (done) return;
        ds += step;
        if (p.tickCount % 10 == 0) p.serverLevel().playSound(null, pos, PanRegistry.PAN_SWIRL.get(), SoundSource.PLAYERS, 0.5f, 1f);
        if (ds >= HOUR_DS) {
            ds = 0;
            loads++;
            int n = load(p);
            HudBus.get().post(p, HudLine.of(HudLineType.PICKUP, n > 0 ? "pan_black_sand" : "pan_nothing", "food.pan"));
            done = true;
        }
    }

    /** One finished pan load: the black sand in 100 g units goes into the player's hands. */
    private int load(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        BlackSand sand = FoodConfig.pan();
        double kg = sand.kgPerHour(tool, placer, month(level));
        int n = units(kg, level.getSeed(), pos, loadCounter++);
        if (n > 0) {
            ItemStack out = new ItemStack(PanRegistry.BLACK_SAND.get(), n);
            if (!p.getInventory().add(out)) p.drop(out, false);
        }
        return n;
    }

    /** Whole 100 g units of an hour's kg: the integer part, plus one more with the fraction's chance (a seeded draw). */
    public static int units(double kg, long seed, BlockPos pos, long load) {
        double u = kg / UNIT_KG;
        int n = (int) Math.floor(u + 1e-9);
        double frac = Math.max(0, u - n);
        if (frac > 0 && Thresholds.stream(seed, "pan_load", pos.getX(), pos.getY(), pos.getZ(), load).nextDouble() < frac) n++;
        return n;
    }

    @Override public boolean finished() { return done; }

    @Override public float share() { return Math.min(1f, ds / (float) HOUR_DS); }

    @Override public float sharePerDs() { return 1f / HOUR_DS; }

    @Override public WorkProgressPayload.Phase phase() { return WorkProgressPayload.Phase.PAN; }

    @Override public int done() { return 0; }

    @Override public int of() { return 0; }
}

package com.yearofice.decay;

import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.decay.DecayLadder;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.decay.Fresh;
import com.yearofice.sim.food.FoodValues;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The slice's Decay and freshness layer (Minecraft development › Layer 5): one clock, {@code air:slice}, at the
 * stand-in air temperature; stacks settle on read, nothing ticks. Lives come from the {@code yearofice:food} row's
 * {@code spoil_days_at_5c} by state (Items › Food values), turned into the reference life by
 * {@link FoodValues#refLifeDh}; the living-rot curve is {@code decay/meat.json}.
 */
public final class DecayServiceImpl implements DecayService {

    /** f_T at the stand-in air temperature. */
    static double rate(Level level) { return DecayCurves.meat().rate(DecayConfig.airC()); }

    @Override
    public double dAt(double gameMinute) { return gameMinute / 60.0 * DecayCurves.meat().rate(DecayConfig.airC()); }

    @Override
    public double dNow(Level level) { return dAt(DecayClock.minuteD(level)); }

    @Override
    public double airC(Level level) { return DecayConfig.airC(); }

    private static @Nullable FoodValues values(ItemStack stack) { return FoodMap.values(stack); }

    @Override
    public @Nullable Fresh stamp(Level level, ItemStack stack, FoodState state) {
        FoodValues v = values(stack);
        if (v == null || Double.isNaN(v.safeLifeDays(state))) return null;
        Fresh f = Fresh.stamp(state, clockAt(level), dNow(level), v.refLifeDh(state), 0, DecayConfig.bands());
        stack.set(YoiComponents.FRESH.get(), f);
        return f;
    }

    @Override
    public @Nullable Fresh settle(Level level, ItemStack stack) {
        Fresh f = stack.get(YoiComponents.FRESH.get());
        if (f == null) return null;
        Fresh s = f.settle(dNow(level));
        if (s != f && !level.isClientSide) stack.set(YoiComponents.FRESH.get(), s);
        return s;
    }

    @Override
    public double progress(Level level, ItemStack stack) {
        Fresh f = stack.get(YoiComponents.FRESH.get());
        return f == null ? 0.0 : f.progress(dNow(level));
    }

    @Override
    public int stage(Level level, ItemStack stack) { return DecayLadder.FOOD.stageOf(progress(level, stack)); }

    @Override
    public double daysUntil(Level level, ItemStack stack, double target) {
        Fresh f = stack.get(YoiComponents.FRESH.get());
        if (f == null) return Double.POSITIVE_INFINITY;
        double dh = f.decayHoursUntil(target, dNow(level));
        double r = rate(level);
        if (Double.isInfinite(dh) || r <= 0) return dh == 0 ? 0 : Double.POSITIVE_INFINITY;
        return dh / (24.0 * r);
    }

    @Override
    public void changeState(Level level, ItemStack stack, FoodState state) {
        Fresh f = stack.get(YoiComponents.FRESH.get());
        FoodValues v = values(stack);
        if (v == null || Double.isNaN(v.safeLifeDays(state))) return;
        if (f == null) {
            stamp(level, stack, state);
            return;
        }
        if (f.state() == state) return;
        double d = dNow(level);
        stack.set(YoiComponents.FRESH.get(), f.withState(state, v.refLifeDh(state), true, d, clockAt(level), d, DecayConfig.bands()));
    }

    @Override
    public void age(Level level, ItemStack stack, double days) {
        Fresh f = stack.get(YoiComponents.FRESH.get());
        if (f == null || Double.isInfinite(f.dueDh()) || days <= 0) return;
        double dh = days * 24.0 * rate(level);
        Fresh aged = new Fresh(f.state(), f.clock(), Fresh.quantise(f.dueDh() - dh, f.lifeDh(), DecayConfig.bands()),
                f.lifeDh(), f.traits(), f.known());
        stack.set(YoiComponents.FRESH.get(), aged.settle(dNow(level)));
    }
}

package com.yearofice.build.store;

import com.yearofice.api.decay.DecayService;
import com.yearofice.api.fire.FireHeatLookup;
import com.yearofice.build.multiblock.MultiblockData;
import com.yearofice.food.preserve.StoreBlockEntity;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.fire.FireHeat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code yearofice:smokehouse}: 60 kg of meat or fish, smoked cold by a fire pit 2-5 m away (Blueprint › The smoker).
 * The batch needs 48-72 game hours of a warm pit (set by the batch); hours the pit is out stall it. When done every
 * stack becomes {@code smoked}, keeping the decay progress it went in with, exactly as the smoke rack does.
 */
public class SmokehouseBlockEntity extends StoreBlockEntity {
    private double progressH;
    private long batch;
    private boolean done;
    private boolean pinPit;
    private boolean pit;
    private final List<Double> p0 = new ArrayList<>();

    public SmokehouseBlockEntity(BlockPos pos, BlockState state) { super(StoreRegistry.SMOKEHOUSE_ENTITY.get(), pos, state); }

    @Override
    public double capacityKg() { return MultiblockData.num("smokehouse", "capacity_kg"); }

    public double progressH() { return progressH; }
    public boolean done() { return done; }

    /** Hours the current batch needs: 48-72, from the batch number and position so a reload never changes it. */
    public double neededH() {
        double lo = MultiblockData.num("smokehouse", "needs_min_h"), hi = MultiblockData.num("smokehouse", "needs_max_h");
        return lo + (hi - lo) * new Random(worldPosition.asLong() * 31 + batch).nextDouble();
    }

    /** GameTests pin whether a pit burns; real play looks for a warm fire 2-5 m away. */
    public void pinPit(boolean burning) { pinPit = true; pit = burning; }

    public boolean pitBurning() {
        if (pinPit) return pit;
        if (level == null) return false;
        double lo = MultiblockData.num("smokehouse", "pit_min_m"), hi = MultiblockData.num("smokehouse", "pit_max_m");
        for (FireHeat h : FireHeatLookup.INSTANCE.fires(level)) {
            double d = h.distanceTo(worldPosition.getX() + 0.5, worldPosition.getY() + 0.2, worldPosition.getZ() + 0.5);
            if (h.warm() && d >= lo && d <= hi) return true;
        }
        return false;
    }

    @Override
    public int putOn(ItemStack held) {
        if (done || progressH > 0) return 0;
        int n = super.putOn(held);
        if (n > 0) {
            DecayService decay = DecayService.get();
            ItemStack s = items.get(items.size() - 1);
            if (decay.settle(level, s) == null) decay.stamp(level, s, FoodState.RAW);
            p0.add(decay.progress(level, s));
        }
        return n;
    }

    @Override
    public List<ItemStack> takeAll() {
        List<ItemStack> out = super.takeAll();
        done = false;
        progressH = 0;
        batch++;
        p0.clear();
        return out;
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level l) {
        super.setLevel(l);
        if (!l.isClientSide) SmokeClock.add(l, worldPosition);
    }

    @Override
    public void setRemoved() {
        if (level != null) SmokeClock.remove(level, worldPosition);
        super.setRemoved();
    }

    /** One game hour (or {@code hours} of them). */
    public void stepHours(int hours) {
        if (level == null || level.isClientSide || items.isEmpty() || done) return;
        for (int i = 0; i < hours && !done; i++) {
            if (pitBurning()) {
                progressH += 1;
                com.yearofice.ecosystem.pool.ScentSources.post((net.minecraft.server.level.ServerLevel) level, worldPosition, com.yearofice.ecosystem.pool.ScentSources.SMOKEHOUSE, 1); // M6 F
            }
            if (progressH >= neededH()) finish();
        }
        setChanged();
    }

    private void finish() {
        done = true;
        DecayService decay = DecayService.get();
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (decay.stamp(level, s, FoodState.SMOKED) == null) continue;
            double days = decay.daysUntil(level, s, p0.size() > i ? p0.get(i) : 0);
            if (days > 0 && !Double.isInfinite(days)) decay.age(level, s, days);
        }
        changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putDouble("progress_h", progressH);
        tag.putLong("batch", batch);
        tag.putBoolean("done", done);
        long[] bits = p0.stream().mapToLong(Double::doubleToLongBits).toArray();
        tag.putLongArray("p0", bits);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        progressH = tag.getDouble("progress_h");
        batch = tag.getLong("batch");
        done = tag.getBoolean("done");
        p0.clear();
        for (long b : tag.getLongArray("p0")) p0.add(Double.longBitsToDouble(b));
    }
}

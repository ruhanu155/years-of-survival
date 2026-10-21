package com.yearofice.food.preserve;

import com.yearofice.api.decay.DecayService;
import com.yearofice.body.intake.FoodMap;
import com.yearofice.food.Meals;
import com.yearofice.item.component.YoiComponents;
import com.yearofice.sim.food.Dishes;
import com.yearofice.sim.food.Nutrients;
import com.yearofice.sim.decay.FoodState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Shared bits of the two racks: one batch on the rack, stepped by game hours from {@link PreserveClock}. */
public abstract class RackBlockEntity extends StoreBlockEntity {
    protected boolean paused;
    protected boolean done;
    protected long batch;
    /** Decay progress each stack had when it went on, so a preserved life starts where the food already was. */
    protected final java.util.List<Double> p0 = new java.util.ArrayList<>();

    protected RackBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    /** GameTests stop the clock's hours and step hours themselves. */
    public void pause(boolean p) { paused = p; }

    public boolean done() { return done; }

    /** Flies laid eggs on this batch (above 15 C with no smoky fire within 3 m): it finishes rotten. */
    public boolean fouled() { return fouled; }

    /** Hours banked on the current batch. */
    public abstract double progressH();

    /** Hours the batch still needs (for {@code /yoi food rack}). */
    public abstract double hoursLeft();

    abstract void hour();

    abstract FoodState finishedState();

    void clockHours(int hours) {
        if (!paused) stepHours(hours);
    }

    public void stepHours(int hours) {
        if (level == null || level.isClientSide || items.isEmpty() || done) return;
        for (int i = 0; i < hours && !done; i++) hour();
        com.yearofice.ecosystem.pool.ScentSources.post((net.minecraft.server.level.ServerLevel) level, worldPosition, com.yearofice.ecosystem.pool.ScentSources.RACK, 1); // M6 F: meat on a rack smells
        setChanged();
    }

    /** The batch is ready: every stack takes the preserved state and keeps its progress on that life. */
    protected void finish() {
        done = true;
        DecayService decay = DecayService.get();
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            lose(s);
            if (decay.stamp(level, s, finishedState()) == null) continue;
            double days = decay.daysUntil(level, s, p0.size() > i ? p0.get(i) : 0);
            if (days > 0 && !Double.isInfinite(days)) decay.age(level, s, days);
            if (fouled) decay.age(level, s, 100_000);
        }
        changed();
    }

    /**
     * Dried and smoked strips weigh 40% of the fresh weight and keep every calorie (Blueprint › Preserving in detail:
     * Dried weight): the item's weight drops to 40% and it carries the nutrients of the fresh item as {@code nutrients}.
     */
    static void lose(ItemStack s) {
        if (s.has(YoiComponents.NUTRIENTS.get())) return;
        var row = FoodMap.row(s);
        if (row == null) return;
        double fresh = Meals.grams(s);
        s.set(YoiComponents.NUTRIENTS.get(), Nutrients.of(row.values("rack"), fresh, row.waterMlPer100g()));
        s.set(YoiComponents.WEIGHT_G.get(), (int) Math.max(1, Math.round(Dishes.driedGrams(fresh))));
    }

    /** True when the batch's strips were fouled (flies laid eggs): they finish rotten. */
    protected boolean fouled;

    @Override
    public int putOn(ItemStack held) {
        if (done || progressH() > 0) return 0;
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
    public java.util.List<ItemStack> takeAll() {
        java.util.List<ItemStack> out = super.takeAll();
        done = false;
        fouled = false;
        p0.clear();
        resetBatch();
        return out;
    }

    abstract void resetBatch();

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            PreserveClock.add(level, worldPosition);
            if (level instanceof net.minecraft.server.level.ServerLevel sl) RackWatch.watch(sl, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) {
            PreserveClock.remove(level, worldPosition);
            if (level instanceof net.minecraft.server.level.ServerLevel sl) RackWatch.unwatch(sl, worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null && !level.isClientSide) {
            PreserveClock.remove(level, worldPosition);
            if (level instanceof net.minecraft.server.level.ServerLevel sl) RackWatch.unwatch(sl, worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putBoolean("done", done);
        tag.putLong("batch", batch);
        tag.putBoolean("fouled", fouled);
        net.minecraft.nbt.ListTag l = new net.minecraft.nbt.ListTag();
        for (double d : p0) l.add(net.minecraft.nbt.DoubleTag.valueOf(d));
        tag.put("p0", l);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        done = tag.getBoolean("done");
        batch = tag.getLong("batch");
        fouled = tag.getBoolean("fouled");
        p0.clear();
        for (net.minecraft.nbt.Tag t : tag.getList("p0", net.minecraft.nbt.Tag.TAG_DOUBLE)) p0.add(((net.minecraft.nbt.DoubleTag) t).getAsDouble());
    }
}

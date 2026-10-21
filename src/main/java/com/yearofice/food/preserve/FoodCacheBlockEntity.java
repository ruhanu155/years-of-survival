package com.yearofice.food.preserve;

import com.yearofice.core.YearOfIce;
import com.yearofice.sim.food.MiceInStores;
import com.yearofice.sim.time.Calendar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code yearofice:food_cache}: a raised box for 200 kg of food. October to April mice take 2-3% of the units a week
 * unless three mouse deadfalls ring it within 3 blocks (Items › Item decay: MiceInStores). The week is settled on every
 * seventh game day and the roll is keyed by position and week, so opening the cache changes nothing.
 */
public class FoodCacheBlockEntity extends StoreBlockEntity {
    public static final double CAPACITY_KG = 200;
    public static final ResourceLocation MOUSE_DEADFALL = ResourceLocation.fromNamespaceAndPath(YearOfIce.MOD_ID, "mouse_deadfall");

    private int deadfallOverride = -1;
    private long lastWeek = Long.MIN_VALUE;
    private boolean droppings;

    public FoodCacheBlockEntity(BlockPos pos, BlockState state) { super(PreserveRegistry.CACHE_ENTITY.get(), pos, state); }

    @Override
    public double capacityKg() { return CAPACITY_KG; }

    /** GameTests (and {@code /yoi}) pin the ring count; -1 counts the real blocks. */
    public void pinDeadfalls(int n) { deadfallOverride = n; }

    /** Mice have been at the cache: droppings lie in it (the field note {@code mice_in_cache}; they cause food poisoning). */
    public boolean droppings() { return droppings; }

    /** Sweeping the cache out clears the droppings. */
    public void clean() { droppings = false; setChanged(); }

    public int deadfallsWithin3() {
        if (deadfallOverride >= 0) return deadfallOverride;
        if (level == null || !BuiltInRegistries.BLOCK.containsKey(MOUSE_DEADFALL)) return 0;
        var block = BuiltInRegistries.BLOCK.get(MOUSE_DEADFALL);
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(worldPosition.offset(-3, -3, -3), worldPosition.offset(3, 3, 3))) {
            if (level.getBlockState(p).is(block)) n++;
        }
        return n;
    }

    /**
     * What the cache keeps: each stack (food and dishes alike) spoils at the store's rate (StoreService: a raised box
     * in winter air, a cellar, a cold room) instead of the open air's, so the difference is added or taken back.
     */
    void spoilHours(int hours) {
        if (!(level instanceof ServerLevel sl) || items.isEmpty()) return;
        double rate = com.yearofice.api.build.StoreService.INSTANCE.rate(sl, worldPosition);
        for (ItemStack s : items) com.yearofice.api.decay.DecayService.get().shift(sl, s, hours, rate);
        setChanged();
    }

    void day(long absDay) {
        if (absDay % 7 != 0) return;
        settleWeek(absDay / 7, Calendar.month(Calendar.dayOfYearOfDay(absDay)));
    }

    /** A test pins the roll's stream (world seed and position), so a band test does not depend on where the cache stands. */
    private Long pinSeed;
    private net.minecraft.core.BlockPos pinPos;

    public void pinRoll(long seed, net.minecraft.core.BlockPos origin) {
        pinSeed = seed;
        pinPos = origin;
    }

    /** One week's mice, once per week number; returns the units lost. */
    public int settleWeek(long week, int month) {
        if (level == null || level.isClientSide || week == lastWeek || items.isEmpty()) return 0;
        lastWeek = week;
        long seed = pinSeed != null ? pinSeed : level instanceof ServerLevel sl ? sl.getSeed() : 0L;
        net.minecraft.core.BlockPos at = pinPos != null ? pinPos : worldPosition;
        int dead = deadfallsWithin3(), total = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack s = items.get(i);
            int lost = MiceInStores.unitsLost(seed, at.getX(), at.getY(), at.getZ(), week, i,
                    s.getCount(), MiceInStores.Store.CACHE, dead, month);
            if (lost > 0) {
                s.shrink(lost);
                total += lost;
            }
        }
        items.removeIf(ItemStack::isEmpty);
        if (total > 0) {
            droppings = true;
            changed();
        }
        return total;
    }

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
        tag.putLong("last_week", lastWeek);
        tag.putBoolean("droppings", droppings);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        lastWeek = tag.contains("last_week") ? tag.getLong("last_week") : Long.MIN_VALUE;
        droppings = tag.getBoolean("droppings");
    }
}

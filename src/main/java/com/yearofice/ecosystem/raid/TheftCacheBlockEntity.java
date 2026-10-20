package com.yearofice.ecosystem.raid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The stolen stack, the game minute it was cached, and whether it can still be recovered. */
public class TheftCacheBlockEntity extends BlockEntity {
    /** 70% of caches are recoverable within 2 game days; after that the thief has eaten it. */
    public static final long EATEN_AFTER_MIN = 2L * 24 * 60;

    private ItemStack stack = ItemStack.EMPTY;
    private long cachedAtMinute;
    private boolean recoverable = true;

    public TheftCacheBlockEntity(BlockPos pos, BlockState state) { super(RaidRegistry.THEFT_CACHE_ENTITY.get(), pos, state); }

    public void fill(ItemStack s, long minute, boolean recoverable) {
        stack = s.copy();
        cachedAtMinute = minute;
        this.recoverable = recoverable;
        setChanged();
    }

    public ItemStack stack() { return stack; }

    public long cachedAtMinute() { return cachedAtMinute; }

    public boolean recoverable() { return recoverable; }

    /** What the finder gets: the stack, or nothing when this cache was never recoverable. */
    public ItemStack take() {
        ItemStack out = recoverable ? stack : ItemStack.EMPTY;
        stack = ItemStack.EMPTY;
        setChanged();
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (!stack.isEmpty()) tag.put("stack", stack.save(reg));
        tag.putLong("cached_at_min", cachedAtMinute);
        tag.putBoolean("recoverable", recoverable);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        stack = tag.contains("stack") ? ItemStack.parse(reg, tag.getCompound("stack")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY;
        cachedAtMinute = tag.getLong("cached_at_min");
        recoverable = !tag.contains("recoverable") || tag.getBoolean("recoverable");
    }
}

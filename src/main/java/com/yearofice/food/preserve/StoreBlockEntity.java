package com.yearofice.food.preserve;

import com.yearofice.food.Meals;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** A station that holds food stacks by weight (racks, cache). Server decides; stacks save with the block. */
public abstract class StoreBlockEntity extends BlockEntity {
    protected final List<ItemStack> items = new ArrayList<>();

    protected StoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }

    public List<ItemStack> items() { return items; }

    public boolean empty() { return items.isEmpty(); }

    public abstract double capacityKg();

    public double heldKg() {
        double g = 0;
        for (ItemStack s : items) g += Meals.grams(s) * s.getCount();
        return g / 1000.0;
    }

    /** Moves as many items of {@code held} onto the station as fit; returns the number taken. */
    public int putOn(ItemStack held) {
        double g = Meals.grams(held);
        if (held.isEmpty() || !(g > 0)) return 0;
        int fit = (int) Math.floor((capacityKg() * 1000.0 - heldKg() * 1000.0 + 1e-6) / g);
        int n = Math.min(held.getCount(), fit);
        if (n <= 0) return 0;
        items.add(held.split(n));
        changed();
        return n;
    }

    public List<ItemStack> takeAll() {
        List<ItemStack> out = new ArrayList<>(items);
        items.clear();
        changed();
        return out;
    }

    public void dropAll() {
        if (level == null || level.isClientSide) return;
        for (ItemStack s : takeAll()) Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, s);
    }

    protected void changed() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        ListTag l = new ListTag();
        for (ItemStack s : items) l.add(s.save(regs));
        tag.put("items", l);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        items.clear();
        for (Tag t : tag.getList("items", Tag.TAG_COMPOUND)) ItemStack.parse(regs, t).ifPresent(items::add);
    }
}

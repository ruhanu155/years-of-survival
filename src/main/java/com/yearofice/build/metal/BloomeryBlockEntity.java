package com.yearofice.build.metal;

import com.yearofice.sim.build.Smelt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The bloomery's charge: ore in quarter-nodules, charcoal in kg, and the blooms waiting to be taken out ({@link Smelt}). */
public class BloomeryBlockEntity extends BlockEntity {
    private int quarters, charcoal, blooms;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) { super(MetalRegistry.BLOOMERY_ENTITY.get(), pos, state); }

    public int quarters() { return quarters; }
    public int charcoal() { return charcoal; }
    public int blooms() { return blooms; }

    /** Ore value of one item in quarters, 0 for a stack that is not ore. */
    public static int oreValue(ItemStack s) {
        if (s.is(MetalRegistry.IRON_NODULE.get())) return Smelt.NODULE;
        if (s.is(MetalRegistry.BOG_IRON.get())) return Smelt.BOG_IRON;
        if (s.is(MetalRegistry.SCRAP_IRON.get())) return Smelt.SCRAP;
        if (s.is(com.yearofice.food.pan.PanRegistry.BLACK_SAND.get())) return Smelt.SAND_UNIT;
        return 0;
    }

    /** Takes ore or charcoal from {@code s} into the furnace; returns how many items went in. */
    public int load(ItemStack s) {
        int n = s.getCount();
        int v = oreValue(s);
        if (v > 0) quarters += v * n;
        else if (s.is(MetalRegistry.CHARCOAL.get())) charcoal += n;
        else return 0;
        s.shrink(n);
        setChanged();
        return n;
    }

    public boolean canSmelt() { return Smelt.blooms(quarters, charcoal) >= 1; }

    /** One finished smelt: 5 nodules of ore and 20 kg of charcoal become a bloom. */
    public void finishSmelt() {
        if (!canSmelt()) return;
        quarters -= Smelt.QUARTERS_PER_BLOOM;
        charcoal -= Smelt.CHARCOAL_PER_BLOOM;
        blooms++;
        setChanged();
    }

    public int takeBlooms() {
        int n = blooms;
        blooms = 0;
        setChanged();
        return n;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("quarters", quarters);
        tag.putInt("charcoal_kg", charcoal);
        tag.putInt("blooms", blooms);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        quarters = tag.getInt("quarters");
        charcoal = tag.getInt("charcoal_kg");
        blooms = tag.getInt("blooms");
    }
}

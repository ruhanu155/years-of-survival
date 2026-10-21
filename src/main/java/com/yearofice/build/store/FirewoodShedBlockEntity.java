package com.yearofice.build.store;

import com.yearofice.build.wood.WoodConfig;
import com.yearofice.build.wood.WoodItems;
import com.yearofice.build.wood.WoodPieceItem;
import com.yearofice.sim.fire.Seasoning;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * {@code yearofice:firewood_shed}: split wood under cover. Pieces keep their seasoning stamp (moisture and day) and are
 * settled at the shed's rate (the existing {@link Seasoning} model, Place.WOODSHED) when they come out, so a shed is
 * closed-form and costs nothing while loaded.
 */
public class FirewoodShedBlockEntity extends BlockEntity {
    public static final int MAX_STACKS = 18;
    private final List<ItemStack> stacks = new ArrayList<>();

    public FirewoodShedBlockEntity(BlockPos pos, BlockState state) { super(StoreRegistry.SHED_ENTITY.get(), pos, state); }

    public List<ItemStack> stacks() { return stacks; }

    /** Settles the piece to {@code day} on the rack first (a held piece dries as under cover), then stores it. */
    public boolean put(ItemStack s, long day) {
        if (WoodItems.piece(s) == null || stacks.size() >= MAX_STACKS) return false;
        WoodItems.settle(s, day);
        stacks.add(s.copy());
        s.setCount(0);
        setChanged();
        return true;
    }

    /** Everything in the shed, settled at the woodshed rate to {@code day}. */
    public List<ItemStack> takeAll(long day) {
        List<ItemStack> out = new ArrayList<>();
        for (ItemStack s : stacks) {
            WoodItems.Stamp st = s.get(com.yearofice.build.wood.WoodRegistry.SEASONING.get());
            WoodPieceItem p = WoodItems.piece(s);
            if (st != null && p != null) {
                double m = Seasoning.dry(WoodConfig.rates(), st.moisture(), st.day(), day, Seasoning.Place.WOODSHED, p.form);
                WoodItems.stamp(s, m, day);
            }
            out.add(s);
        }
        stacks.clear();
        setChanged();
        return out;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        ListTag l = new ListTag();
        for (ItemStack s : stacks) l.add(s.save(regs));
        tag.put("stacks", l);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        stacks.clear();
        for (Tag t : tag.getList("stacks", Tag.TAG_COMPOUND)) ItemStack.parse(regs, t).ifPresent(stacks::add);
    }
}

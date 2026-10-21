package com.yearofice.ecosystem.livestock;

import com.yearofice.food.FoodItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Feed (kg), water (L) and eggs held by a trough, hay rack or nest box. */
public class FeedBoxBlockEntity extends BlockEntity {
    public static final double FEED_CAP_KG = 20, WATER_CAP_L = 40;
    public static final int EGG_CAP = 12, REACH = 3;

    private double feedKg, waterL;
    private int eggs;

    public FeedBoxBlockEntity(BlockPos pos, BlockState state) { super(LivestockRegistry.FEED_BOX_ENTITY.get(), pos, state); }

    public double feedKg() { return feedKg; }
    public double waterL() { return waterL; }
    public int eggs() { return eggs; }

    public boolean addFeed(double kg) {
        if (feedKg + kg > FEED_CAP_KG) return false;
        feedKg += kg;
        setChanged();
        return true;
    }

    public boolean addWater(double l) {
        if (waterL + l > WATER_CAP_L) return false;
        waterL += l;
        setChanged();
        return true;
    }

    public boolean addEgg() {
        if (eggs >= EGG_CAP) return false;
        eggs++;
        setChanged();
        return true;
    }

    public List<ItemStack> takeEggs() {
        if (eggs == 0) return List.of();
        ItemStack s = new ItemStack(FoodItems.HEN_EGG.get(), eggs);
        eggs = 0;
        setChanged();
        return List.of(s);
    }

    /** Moves up to {@code wantFeedKg} and {@code wantWaterL} from the nearest boxes within {@link #REACH} into the animal's own stores. */
    public static void draw(ServerLevel level, BlockPos at, LivestockCore c, double wantFeedKg, double wantWaterL) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-REACH, -1, -REACH), at.offset(REACH, 1, REACH))) {
            if (wantFeedKg <= 0 && wantWaterL <= 0) return;
            if (!(level.getBlockEntity(p) instanceof FeedBoxBlockEntity be)) continue;
            double f = Math.min(wantFeedKg, be.feedKg), w = Math.min(wantWaterL, be.waterL);
            if (f <= 0 && w <= 0) continue;
            be.feedKg -= f;
            be.waterL -= w;
            be.setChanged();
            c.feedKg += f;
            c.waterL += w;
            wantFeedKg -= f;
            wantWaterL -= w;
        }
    }

    /** A hen's egg goes to a nest box within reach; false when there is none (or all are full). */
    public static boolean layInBox(ServerLevel level, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-REACH, -1, -REACH), at.offset(REACH, 1, REACH))) {
            if (level.getBlockEntity(p) instanceof FeedBoxBlockEntity be && level.getBlockState(p).getBlock() instanceof FeedBoxBlock b
                    && b.kind == FeedBoxBlock.Kind.NEST_BOX && be.addEgg()) return true;
        }
        return false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putDouble("feed_kg", feedKg);
        tag.putDouble("water_l", waterL);
        tag.putInt("eggs", eggs);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        feedKg = tag.getDouble("feed_kg");
        waterL = tag.getDouble("water_l");
        eggs = tag.getInt("eggs");
    }
}

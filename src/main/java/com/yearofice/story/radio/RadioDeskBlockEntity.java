package com.yearofice.story.radio;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The desk's battery, 0–100 percent. A radio with under one sked of charge is dead air. */
public class RadioDeskBlockEntity extends BlockEntity {
    private double battery;

    public RadioDeskBlockEntity(BlockPos pos, BlockState state) { super(RadioRegistry.RADIO_DESK_ENTITY.get(), pos, state); }

    public double battery() { return battery; }

    public void charge(double pct) {
        battery = Math.max(0, Math.min(100, battery + pct));
        setChanged();
    }

    public boolean drain(double pct) {
        if (battery < pct) return false;
        charge(-pct);
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) RadioEvents.LIVE.add(this);
    }

    @Override
    public void setRemoved() {
        RadioEvents.LIVE.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putDouble("battery_pct", battery);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        battery = tag.getDouble("battery_pct");
    }
}

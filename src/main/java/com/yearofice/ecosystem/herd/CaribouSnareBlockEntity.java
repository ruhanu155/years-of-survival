package com.yearofice.ecosystem.herd;

import com.yearofice.ecosystem.companion.CompanionRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The snare in the mouth holds one animal at a time: the id of the caribou it has, and how many passes it has seen. */
public class CaribouSnareBlockEntity extends BlockEntity {
    private UUID held;
    private int passes, takenThisSeason;

    public CaribouSnareBlockEntity(BlockPos pos, BlockState state) { super(CompanionRegistry.CARIBOU_SNARE_ENTITY.get(), pos, state); }

    public UUID held() { return held; }

    public boolean holding() { return held != null; }

    public int passes() { return passes; }

    public int taken() { return takenThisSeason; }

    public void hold(UUID id) {
        held = id;
        setChanged();
    }

    public void release() {
        if (held != null) takenThisSeason++;
        held = null;
        setChanged();
    }

    void notePass() {
        passes++;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.saveAdditional(tag, r);
        if (held != null) tag.putUUID("held", held);
        tag.putInt("passes", passes);
        tag.putInt("taken", takenThisSeason);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) {
        super.loadAdditional(tag, r);
        held = tag.hasUUID("held") ? tag.getUUID("held") : null;
        passes = tag.getInt("passes");
        takenThisSeason = tag.getInt("taken");
    }
}

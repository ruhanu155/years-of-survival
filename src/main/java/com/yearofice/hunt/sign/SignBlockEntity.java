package com.yearofice.hunt.sign;

import com.yearofice.sim.hunt.Sign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The sign's record: species, the game minute it was made, the way it was heading, the gait and the ground. */
public class SignBlockEntity extends BlockEntity {
    String species = "";
    double madeMin;
    float hx, hz;
    int gait;
    Sign.Substrate substrate = Sign.Substrate.SNOW;

    public SignBlockEntity(BlockPos pos, BlockState state) { super(SignRegistry.SIGN.get(), pos, state); }

    /** Registered here, not in onLoad: a block set by a test gets setLevel but no load (LESSONS). */
    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof net.minecraft.server.level.ServerLevel sl) SignService.track(sl, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof net.minecraft.server.level.ServerLevel sl) SignService.untrack(sl, worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.saveAdditional(t, r);
        t.putString("species", species);
        t.putDouble("made_min", madeMin);
        t.putFloat("hx", hx);
        t.putFloat("hz", hz);
        t.putInt("gait", gait);
        t.putString("substrate", substrate.name());
    }

    @Override
    protected void loadAdditional(CompoundTag t, HolderLookup.Provider r) {
        super.loadAdditional(t, r);
        species = t.getString("species");
        madeMin = t.getDouble("made_min");
        hx = t.getFloat("hx");
        hz = t.getFloat("hz");
        gait = t.getInt("gait");
        try {
            substrate = Sign.Substrate.valueOf(t.getString("substrate"));
        } catch (IllegalArgumentException e) {
            substrate = Sign.Substrate.SNOW;
        }
    }
}

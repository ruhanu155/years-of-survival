package com.yearofice.crop;

import com.yearofice.sim.crop.CropModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The numbers of one crop block: sowing day, seeds sown and up, growth units, frost and dry-day marks. */
public final class CropBlockEntity extends BlockEntity {
    int sownDay = -1;
    int seeds = 1;
    int up = 0;
    double units = 0;
    double frostMult = 1.0;
    int dryDays = 0;
    boolean dead = false;
    boolean frosted = false;
    double soilCAtSowing = 0;

    public CropBlockEntity(BlockPos pos, BlockState state) { super(CropRegistry.CROP_BE.get(), pos, state); }

    public int seeds() { return seeds; }

    public int up() { return up; }

    public double units() { return units; }

    public double frostMult() { return frostMult; }

    public int sownDay() { return sownDay; }

    public boolean dead() { return dead; }

    public double stand() { return seeds <= 0 ? 0 : (double) up / seeds; }

    public String blockId() { return BuiltInRegistries.BLOCK.getKey(getBlockState().getBlock()).toString(); }

    public CropSpec spec() { return CropData.byBlock(blockId()); }

    /** Sets a fresh sowing. */
    public void sow(int sownDay, int seeds, int up, double soilC) {
        this.sownDay = sownDay;
        this.seeds = seeds;
        this.up = up;
        this.units = 0;
        this.frostMult = 1.0;
        this.dryDays = 0;
        this.dead = false;
        this.frosted = false;
        this.soilCAtSowing = soilC;
        restage();
    }

    /** The stage from units, up and death. */
    public int stage() {
        CropSpec s = spec();
        if (dead) return 5;
        if (s == null || up <= 0) return 0;
        return CropModel.stage(units, s.sproutUnits, s.ripeUnits);
    }

    public void addUnits(double u) {
        if (dead || up <= 0) return;
        units += u;
        restage();
    }

    public void kill() { dead = true; restage(); }

    public void markFrosted(boolean f) { frosted = f; restage(); }

    public boolean frosted() { return frosted; }

    public void multFrost(double m) { frostMult *= m; setChanged(); }

    public void setDry(int d) { dryDays = d; setChanged(); }

    public int dryDays() { return dryDays; }

    void restage() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState s = getBlockState();
            int st = stage();
            if (s.getBlock() instanceof CropBlock && (s.getValue(CropBlock.STAGE) != st || s.getValue(CropBlock.FROSTED) != frosted)) {
                level.setBlock(worldPosition, s.setValue(CropBlock.STAGE, st).setValue(CropBlock.FROSTED, frosted), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("sown_day", sownDay);
        tag.putInt("seeds", seeds);
        tag.putInt("up", up);
        tag.putDouble("units", units);
        tag.putDouble("frost_mult", frostMult);
        tag.putInt("dry_days", dryDays);
        tag.putBoolean("dead", dead);
        tag.putBoolean("frosted", frosted);
        tag.putDouble("soil_c_at_sowing", soilCAtSowing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        sownDay = tag.getInt("sown_day");
        seeds = tag.contains("seeds") ? tag.getInt("seeds") : 1;
        up = tag.getInt("up");
        units = tag.getDouble("units");
        frostMult = tag.contains("frost_mult") ? tag.getDouble("frost_mult") : 1.0;
        dryDays = tag.getInt("dry_days");
        dead = tag.getBoolean("dead");
        frosted = tag.getBoolean("frosted");
        soilCAtSowing = tag.getDouble("soil_c_at_sowing");
    }
}

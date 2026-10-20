package com.yearofice.food.preserve;

import com.yearofice.api.env.EnvService;
import com.yearofice.food.FoodConfig;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.env.Precip;
import com.yearofice.sim.food.Racks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** {@code yearofice:drying_rack}: 15 kg of 1 cm strips; 2 dry days make them {@code dried} (Racks.Drying). */
public class DryingRackBlockEntity extends RackBlockEntity {
    private Racks.Drying drying = Racks.Drying.start();

    public DryingRackBlockEntity(BlockPos pos, BlockState state) { super(PreserveRegistry.DRYING_ENTITY.get(), pos, state); }

    @Override
    public double capacityKg() { return FoodConfig.racks().dryCapacityKg(); }

    @Override
    public double progressH() { return drying.progressH(); }

    @Override
    public double hoursLeft() { return Math.max(0, FoodConfig.racks().dryHours() - drying.progressH()); }

    public int rainRun() { return drying.rainRun(); }

    /** Rain reaches the strips: liquid precipitation with open sky above (a roof keeps the rack dry). */
    private Boolean testRain;

    /** GameTests pin whether the next hours are rainy; null reads the weather. */
    public void pinRain(Boolean rain) { testRain = rain; }

    boolean uncoveredRain() {
        if (testRain != null) return testRain;
        if (!(level instanceof ServerLevel sl)) return false;
        Precip p = EnvService.get().precip(sl, worldPosition);
        return p != null && p.liquid() && level.canSeeSky(worldPosition.above());
    }

    @Override
    void hour() {
        drying = drying.hour(uncoveredRain(), FoodConfig.racks());
        if (drying.done(FoodConfig.racks())) finish();
    }

    @Override
    FoodState finishedState() { return FoodState.DRIED; }

    @Override
    void resetBatch() { drying = Racks.Drying.start(); }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putDouble("progress_h", drying.progressH());
        tag.putInt("rain_run", drying.rainRun());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        drying = new Racks.Drying(tag.getDouble("progress_h"), tag.getInt("rain_run"));
    }
}

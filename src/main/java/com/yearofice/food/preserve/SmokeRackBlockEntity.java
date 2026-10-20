package com.yearofice.food.preserve;

import com.yearofice.api.env.EnvService;
import com.yearofice.food.FoodConfig;
import com.yearofice.food.cooking.RingHeat;
import com.yearofice.sim.decay.FoodState;
import com.yearofice.sim.food.Doneness;
import com.yearofice.sim.food.Racks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** {@code yearofice:smoke_rack}: 10 kg over a fire ring; hot smoke for 8-12 game hours makes the batch {@code smoked}. */
public class SmokeRackBlockEntity extends RackBlockEntity {
    private Racks.Smoking smoking;
    private Doneness.Fire testFire;
    private double testWind = Double.NaN;

    public SmokeRackBlockEntity(BlockPos pos, BlockState state) { super(PreserveRegistry.SMOKE_ENTITY.get(), pos, state); }

    private Racks.Smoking smoking() {
        if (smoking == null) smoking = Racks.Smoking.start(seed(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), batch, FoodConfig.racks());
        return smoking;
    }

    private long seed() { return level instanceof ServerLevel sl ? sl.getSeed() : 0L; }

    @Override
    public double capacityKg() { return FoodConfig.racks().smokeCapacityKg(); }

    @Override
    public double progressH() { return smoking == null ? 0 : smoking.progressH(); }

    @Override
    public double hoursLeft() { return Math.max(0, smoking().neededH() - smoking().progressH()); }

    /** GameTests pin the fire and the wind the rack reads. */
    public void pinConditions(Doneness.Fire fire, double windMs) { testFire = fire; testWind = windMs; }

    /** Hot smoke needs a burning fire (Low to Hot) or coals under the rack; embers and banked fires stall the batch. */
    public static boolean hot(Doneness.Fire f) {
        return f == Doneness.Fire.LOW || f == Doneness.Fire.STEADY || f == Doneness.Fire.HOT || f == Doneness.Fire.COALS;
    }

    public Doneness.Fire fire() {
        if (testFire != null) return testFire;
        return level == null ? Doneness.Fire.OUT : RingHeat.at(level, worldPosition.below());
    }

    public double windMs() {
        if (!Double.isNaN(testWind)) return testWind;
        return level instanceof ServerLevel sl ? EnvService.get().wind(sl, worldPosition).ms() : 0;
    }

    @Override
    void hour() {
        smoking = smoking().hour(hot(fire()), windMs(), FoodConfig.racks());
        if (smoking.done()) finish();
    }

    @Override
    FoodState finishedState() { return FoodState.SMOKED; }

    @Override
    void resetBatch() {
        smoking = null;
        batch++;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.saveAdditional(tag, regs);
        tag.putDouble("progress_h", progressH());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider regs) {
        super.loadAdditional(tag, regs);
        smoking = null;
        double p = tag.getDouble("progress_h");
        if (p > 0) {
            Racks.Smoking s = smoking();
            smoking = new Racks.Smoking(p, s.neededH());
        }
    }
}

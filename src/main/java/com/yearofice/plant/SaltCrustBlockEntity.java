package com.yearofice.plant;

import com.yearofice.api.clock.ClockService;
import com.yearofice.api.env.EnvService;
import com.yearofice.sim.plant.LickModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The grams of crust on a lick block, its cap (seep 1 kg, clay bank 0.5 kg) and the day it was last brought up to date (closed-form catch-up). */
public final class SaltCrustBlockEntity extends BlockEntity {
    private double grams = LickModel.SEEP_CAP_G;
    private int capG = LickModel.SEEP_CAP_G;
    private int lastDay = -1;
    private int sinceRain = 99;

    public SaltCrustBlockEntity(BlockPos pos, BlockState state) { super(LickRegistry.SALT_CRUST_BE.get(), pos, state); }

    public double grams() { return grams; }

    public int capG() { return capG; }

    /** Test and worldgen hook: set the cap and the grams on hand, and mark today as the last update. */
    public void init(int capG, double grams, int today) {
        this.capG = capG;
        this.grams = Math.min(capG, grams);
        this.lastDay = today;
        this.sinceRain = 99;
        sync();
    }

    /** Brings the crust up to {@code today} day by day (a lick left unloaded matches one that was loaded). */
    public void catchUp(ServerLevel level, int today) {
        if (lastDay < 0) { lastDay = today; return; }
        double kg = ForageConfig.tuning().lickRegrowKgPerWeek();
        for (int d = lastDay + 1; d <= today; d++) {
            boolean rainy = rainOn(level, d);
            grams = LickModel.dayRegrow(grams, capG, Math.floorMod(d, 365), rainy, sinceRain, kg);
            sinceRain = rainy ? 0 : Math.min(99, sinceRain + 1);
        }
        lastDay = Math.max(lastDay, today);
        sync();
    }

    /** Test hook: one explicit day, with the rain flag given. */
    public void stepDay(int doy, boolean rainy) {
        grams = LickModel.dayRegrow(grams, capG, doy, rainy, sinceRain, ForageConfig.tuning().lickRegrowKgPerWeek());
        sinceRain = rainy ? 0 : Math.min(99, sinceRain + 1);
        sync();
    }

    private static boolean rainOn(ServerLevel level, long absDay) {
        try {
            var day = EnvService.get().day(level, absDay);
            return day.precipMm() >= 1.0 && day.meanC() > 0.0;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Takes up to {@code g} grams; returns what came off. */
    public double take(double g) {
        double got = Math.min(g, grams);
        grams -= got;
        sync();
        return got;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState s = getBlockState();
            int want = LickModel.crustState(grams);
            if (s.getBlock() instanceof SaltCrustBlock && s.getValue(SaltCrustBlock.CRUST) != want) {
                level.setBlock(worldPosition, s.setValue(SaltCrustBlock.CRUST, want), 3);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putDouble("grams", grams);
        tag.putInt("cap_g", capG);
        tag.putInt("last_day", lastDay);
        tag.putInt("since_rain", sinceRain);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        grams = tag.contains("grams") ? tag.getDouble("grams") : grams;
        capG = tag.contains("cap_g") ? tag.getInt("cap_g") : capG;
        lastDay = tag.contains("last_day") ? tag.getInt("last_day") : lastDay;
        sinceRain = tag.contains("since_rain") ? tag.getInt("since_rain") : sinceRain;
    }
}

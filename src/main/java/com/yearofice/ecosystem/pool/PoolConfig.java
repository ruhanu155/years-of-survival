package com.yearofice.ecosystem.pool;

import com.yearofice.ecosystem.PresetConfig;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** {@code [ecosystem]} keys of the valley's off-screen population (RECONCILE 306), with Gentle, Standard and Brutal values. */
public final class PoolConfig {
    private PoolConfig() {}

    /**
     * @param reachScale      multiplies every scent source's reach
     * @param interestScale   multiplies the hourly chance that an animal in reach starts an approach
     * @param gutPileBaseM    reach of a fresh gut pile or fish-cleaning spot in a 3 m/s wind (calm air halves it)
     * @param carcassBaseM    reach of a carcass
     * @param rackBaseM       reach of a rack with meat or fish on it
     * @param smokehouseBaseM reach of a smokehouse with a fire burning
     */
    public record PoolTuning(double reachScale, double interestScale, double gutPileBaseM, double carcassBaseM,
                             double rackBaseM, double smokehouseBaseM) {}

    public static final PresetConfig<PoolTuning> POOL = new PresetConfig<>(PoolTuning.class, p -> switch (p) {
        case GENTLE -> new PoolTuning(0.7, 0.6, 1500, 2000, 800, 1200);
        case STANDARD -> new PoolTuning(1.0, 1.0, 1500, 2000, 800, 1200);
        case BRUTAL -> new PoolTuning(1.3, 1.5, 1500, 2000, 800, 1200);
    }, Map.of("reachScale", "Multiplies how far (and so how many valley animals) a scent source reaches along the wind.",
            "interestScale", "Multiplies the hourly chance that an off-screen bear, wolf pack or wolverine in reach starts walking to the scent.",
            "gutPileBaseM", "Reach in metres of a fresh gut pile or fish-cleaning spot (times wind and rain factors).",
            "carcassBaseM", "Reach in metres of a carcass.",
            "rackBaseM", "Reach in metres of a rack with meat or fish.",
            "smokehouseBaseM", "Reach in metres of a smokehouse with a fire."));

    public static void define(ModConfigSpec.Builder b) {
        b.comment("The valley's bears: off-screen animals pulled by scent (RECONCILE 306).").push("ecosystem");
        POOL.define(b);
        b.pop();
    }

    public static PoolTuning tuning() { return POOL.tuning(); }

    public static void invalidate() { POOL.invalidate(); }
}

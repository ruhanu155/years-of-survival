package com.yearofice.item.inventory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Practice skill {@code hauling} (video 2, A11): a rep for every {@code hauling_rep_ticks} spent walking with
 * 15 kg or more. Reps live in the player's persistent data under {@code yearofice_hauling} until the shared
 * {@code yearofice:practice} attachment exists; the band edges read them through {@link LoadCalculator#hauling}.
 */
public final class PackSkill {
    private PackSkill() {}

    private static final String KEY = "yearofice_hauling";

    public static double reps(Player p) { return p.getPersistentData().getDouble(KEY); }

    public static void setReps(Player p, double reps) { p.getPersistentData().putDouble(KEY, Math.max(0, reps)); }

    /** Called each player tick; counts walking time under a real load and awards a rep per interval. */
    public static void tick(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        double moving = p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.0004 ? 1 : 0;
        if (moving == 0 || LoadCalculator.kgOf(p) < PackConfig.get("band_light_kg")) return;
        int t = root.getInt(KEY + "_t") + 1;
        if (t >= PackConfig.get("hauling_rep_ticks")) {
            t = 0;
            setReps(p, Math.min(reps(p) + 1, PackConfig.get("hauling_reps")));
        }
        root.putInt(KEY + "_t", t);
    }
}

package com.yearofice.item.inventory;

import com.yearofice.api.practice.PracticeService;
import com.yearofice.sim.build.Skill;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The Practice skill {@code hauling} (video 2, A11): a rep for every {@code hauling_rep_ticks} spent walking with
 * 15 kg or more. Reps live in the shared {@code yearofice:practice} attachment (Skill.HAULING); the old
 * persistent-data stand-in under {@code yearofice_hauling} is moved over once. The band edges read them through {@link LoadCalculator#hauling}.
 */
public final class PackSkill {
    private PackSkill() {}

    private static final String KEY = "yearofice_hauling";

    public static double reps(Player p) {
        migrate(p);
        return PracticeService.installed() ? PracticeService.get().reps(p, Skill.HAULING) : 0;
    }

    public static void setReps(Player p, double reps) {
        if (PracticeService.installed()) PracticeService.get().set(p, Skill.HAULING, Math.max(0, reps));
    }

    /** Moves the stand-in's reps into the attachment, once. */
    private static void migrate(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY) || !PracticeService.installed()) return;
        double old = root.getDouble(KEY);
        root.remove(KEY);
        if (old > PracticeService.get().reps(p, Skill.HAULING)) PracticeService.get().set(p, Skill.HAULING, old);
    }

    /** Called each player tick; counts walking time under a real load and awards a rep per interval. */
    public static void tick(ServerPlayer p) {
        CompoundTag root = p.getPersistentData();
        double moving = p.onGround() && p.getDeltaMovement().horizontalDistanceSqr() > 0.0004 ? 1 : 0;
        if (moving == 0 || LoadCalculator.kgOf(p) < PackConfig.get("band_light_kg")) return;
        int t = root.getInt(KEY + "_t") + 1;
        if (t >= PackConfig.get("hauling_rep_ticks")) {
            t = 0;
            if (PracticeService.installed() && reps(p) < PackConfig.get("hauling_reps"))
                PracticeService.get().add(p, Skill.HAULING, 1);
        }
        root.putInt(KEY + "_t", t);
    }
}

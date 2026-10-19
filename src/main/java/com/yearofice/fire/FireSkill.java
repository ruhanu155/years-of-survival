package com.yearofice.fire;

import com.yearofice.api.body.BodyService;
import com.yearofice.core.rules.YoiConfig;
import com.yearofice.sim.fire.FirePractice;
import com.yearofice.sim.fire.FireTuning;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The player's Fire skill counter and energy, as the bow drill reads them. Reps are a stand-in: they live in the
 * player's persistent data under {@code yearofice_fire} until the studio's {@code yearofice:practice} attachment
 * exists (M3). Energy is read from {@code api.body.BodyService} (0–10 bolts). Nothing here writes body state.
 */
public final class FireSkill {
    private FireSkill() {}

    private static final String KEY = "yearofice_fire";

    private static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(KEY)) root.put(KEY, new CompoundTag());
        return root.getCompound(KEY);
    }

    public static double reps(Player p) { return tag(p).getDouble("reps"); }

    public static void setReps(Player p, double reps) { tag(p).putDouble("reps", Math.max(0, reps)); }

    /** Adds reps under the daily cap (N ÷ 5, at least 2); returns what was gained. */
    public static double addReps(Player p, double add, FireTuning t) {
        CompoundTag tag = tag(p);
        long day = gameDay(p);
        if (tag.getLong("day") != day) {
            tag.putLong("day", day);
            tag.putDouble("today", 0);
        }
        double g = FirePractice.gain(tag.getDouble("today"), add, t.drillSkillReps());
        tag.putDouble("today", tag.getDouble("today") + g);
        tag.putDouble("reps", tag.getDouble("reps") + g);
        return g;
    }

    /** The game day from level time at the configured day length. */
    static long gameDay(Player p) {
        long ticksPerDay = (long) YoiConfig.DAY_LENGTH_MIN.get() * 60 * 20;
        return p.level().getGameTime() / ticksPerDay;
    }

    /** Energy in bolts (0–10), read from {@link BodyService} (RECONCILE 164). Full energy off the server. */
    public static double energyBolts(Player p) {
        return p instanceof ServerPlayer sp ? BodyService.get().energy(sp) : 10.0;
    }
}

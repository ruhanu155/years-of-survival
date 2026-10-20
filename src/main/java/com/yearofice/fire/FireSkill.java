package com.yearofice.fire;

import com.yearofice.api.body.BodyService;
import com.yearofice.api.practice.PracticeService;
import com.yearofice.sim.build.Skill;
import com.yearofice.sim.fire.FireTuning;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The player's Fire skill counter and energy, as the bow drill reads them. Reps live in the {@code yearofice:practice}
 * attachment (Crafting and building in depth › Practice), reached through {@code api.practice.PracticeService}; this
 * class only names the Fire skill for the bow drill. Energy is read from {@code api.body.BodyService} (0–10 bolts).
 * Nothing here writes body state.
 */
public final class FireSkill {
    private FireSkill() {}

    public static double reps(Player p) { return PracticeService.get().reps(p, Skill.FIRE); }

    public static void setReps(Player p, double reps) { PracticeService.get().set(p, Skill.FIRE, reps); }

    /** Adds reps under the daily cap (N ÷ 5, at least 2); returns what was gained. */
    public static double addReps(Player p, double add, FireTuning t) {
        return PracticeService.get().add(p, Skill.FIRE, add, t.drillSkillReps());
    }

    /** Energy in bolts (0–10), read from {@link BodyService} (RECONCILE 164). Full energy off the server. */
    public static double energyBolts(Player p) {
        return p instanceof ServerPlayer sp ? BodyService.get().energy(sp) : 10.0;
    }
}

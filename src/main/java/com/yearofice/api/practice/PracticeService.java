package com.yearofice.api.practice;

import com.yearofice.sim.build.Practice;
import com.yearofice.sim.build.Skill;
import net.minecraft.world.entity.player.Player;

/**
 * The player's practice reps (Crafting and building in depth › Practice; the {@code yearofice:practice} attachment,
 * written only by {@code build.craft}). Every system that trains a skill reads and adds reps here; none keeps its
 * own counter. A finished job is {@link Practice#JOB_REP}, a failed attempt {@link Practice#FAIL_REP}; a skill gains
 * at most N ÷ 5 reps a game day (at least 2), and practice never decays.
 */
public interface PracticeService {

    /** Reps in {@code skill}, 0 for a new player. */
    double reps(Player player, Skill skill);

    /** Adds {@code reps} under the daily cap; returns what was actually gained. */
    double add(Player player, Skill skill, double reps);

    /** As {@link #add(Player, Skill, double)} with the skill's N overridden (a tuning dial of the caller's). */
    double add(Player player, Skill skill, double reps, int n);

    /** Sets the count outright (debug commands and tests only). */
    void set(Player player, Skill skill, double reps);

    /** The preview word: Clumsy, Handy or Practiced. */
    default Practice.Word word(Player player, Skill skill) { return Practice.word(reps(player, skill), skill.n); }

    default boolean practiced(Player player, Skill skill) { return Practice.practiced(reps(player, skill), skill.n); }

    static PracticeService get() {
        PracticeService s = Holder.service;
        if (s == null) throw new IllegalStateException("PracticeService not installed yet");
        return s;
    }

    static boolean installed() { return Holder.service != null; }

    static void install(PracticeService service) {
        if (Holder.service != null) throw new IllegalStateException("PracticeService already installed");
        Holder.service = service;
    }

    final class Holder {
        private static volatile PracticeService service;

        private Holder() {}
    }
}

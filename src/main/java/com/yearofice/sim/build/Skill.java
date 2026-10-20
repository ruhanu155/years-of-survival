package com.yearofice.sim.build;

import java.util.Locale;
import java.util.Optional;

/**
 * The practice skills and their N, the reps that make a skill Practiced (Crafting and building in depth › Practice,
 * the skills table). The id is the data name in recipes and in the {@code yearofice:practice} attachment.
 */
public enum Skill {
    FIRE(10), CORDAGE(40), KNAPPING(25), WOODCRAFT(30), FELLING(30), SPLITTING(60), FISH_CLEANING(30), BUTCHERING(40),
    HIDE_WORK(15), SEWING(8), TRAPPING(30), NETTING(8), LOG_BUILDING(80), SMOKING(10), SMITHING(20), FISHING(50),
    HAULING(300);

    /** Reps to Practiced. */
    public final int n;

    Skill(int n) { this.n = n; }

    public String id() { return name().toLowerCase(Locale.ROOT); }

    public static Optional<Skill> byId(String id) {
        if (id == null) return Optional.empty();
        for (Skill s : values()) if (s.id().equals(id)) return Optional.of(s);
        return Optional.empty();
    }
}

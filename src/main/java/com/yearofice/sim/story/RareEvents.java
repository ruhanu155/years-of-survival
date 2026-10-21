package com.yearofice.sim.story;

import java.util.Random;

/** Rare events (Blueprint › Open world in depth): each rolls once on May 1 against its yearly chance. */
public final class RareEvents {
    private RareEvents() {}

    /** One event row, as read from {@code event/<id>.json}. */
    public record Def(String id, double chancePerYear, int fromYear) {}

    /** True when the event lands in {@code year} (1-based); years before {@code fromYear} never roll. */
    public static boolean rolls(Def d, int year, long worldSeed) {
        if (year < d.fromYear()) return false;
        long h = worldSeed * 0x9E3779B97F4A7C15L + d.id().hashCode() * 31L + year * 0xBF58476D1CE4E5B9L;
        return new Random(h).nextDouble() < d.chancePerYear();
    }

    /** Rare-event ids with a lore clip the player can learn from the sky. */
    public static boolean skyLore(String sign) { return sign.equals("aurora") || sign.equals("sun_dog"); }
}

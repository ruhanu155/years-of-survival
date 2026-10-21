package com.yearofice.sim.ecosystem;

import com.yearofice.sim.time.Calendar;
import java.util.Map;

/**
 * Young by species (Land animals in depth › Mothers and young): when they are born, the plan (follower, hider or
 * denner) and how the game judges an orphan. Pure Java. M5 debt: leverets, porcupettes; M6: calves, fawns, pups,
 * kits and cubs.
 */
public final class Young {
    private Young() {}

    public enum Plan { FOLLOWER, HIDER, DENNER }

    /** Born from/to as month-day pairs; a litter size range; weeks the young hides (hiders) or follows. */
    public record Spec(Plan plan, int fromMonth, int fromDay, int toMonth, int toDay, int litterMin, int litterMax, int weeks) {
        public boolean bornOn(int doy) { return Calendar.within(doy, Calendar.dayOfYear(fromMonth, fromDay), Calendar.dayOfYear(toMonth, toDay) + weeks * 7); }
    }

    public static final Map<String, Spec> TABLE = Map.ofEntries(
            Map.entry("moose", new Spec(Plan.FOLLOWER, 5, 20, 6, 10, 1, 2, 12)),
            Map.entry("caribou", new Spec(Plan.FOLLOWER, 5, 25, 6, 5, 1, 1, 16)),
            Map.entry("mule_deer", new Spec(Plan.HIDER, 6, 5, 6, 25, 1, 2, 8)),
            Map.entry("wolf", new Spec(Plan.DENNER, 5, 1, 5, 1, 4, 6, 12)),
            Map.entry("black_bear", new Spec(Plan.FOLLOWER, 5, 1, 5, 20, 1, 3, 40)),
            Map.entry("grizzly", new Spec(Plan.FOLLOWER, 5, 1, 5, 20, 1, 3, 100)),
            Map.entry("lynx", new Spec(Plan.DENNER, 6, 1, 6, 1, 0, 4, 12)),
            Map.entry("red_fox", new Spec(Plan.DENNER, 4, 25, 4, 25, 4, 8, 20)),
            Map.entry("coyote", new Spec(Plan.DENNER, 4, 25, 4, 25, 4, 8, 20)),
            Map.entry("wolverine", new Spec(Plan.DENNER, 3, 15, 3, 15, 2, 2, 24)),
            Map.entry("porcupine", new Spec(Plan.HIDER, 5, 20, 5, 20, 1, 1, 17)),
            Map.entry("snowshoe_hare", new Spec(Plan.HIDER, 6, 1, 6, 1, 4, 4, 4)));

    /** Hare litters: June 1, July 10 and August 20. */
    public static final int[][] HARE_LITTERS = {{6, 1}, {7, 10}, {8, 20}};

    public static boolean hasYoung(String species, int doy) {
        Spec s = TABLE.get(species);
        if (s == null) return false;
        if (species.equals("snowshoe_hare")) {
            for (int[] l : HARE_LITTERS) if (Calendar.within(doy, Calendar.dayOfYear(l[0], l[1]), Calendar.dayOfYear(l[0], l[1]) + 28)) return true;
            return false;
        }
        return s.bornOn(doy);
    }

    /**
     * Hiders are not orphans: a hider counts as orphaned only after two missed visits (two dusks for hares, one
     * day for a moose calf or a deer fawn, two dusks for a porcupette). Followers and denners lose their young
     * when the mother is gone.
     */
    public static boolean orphaned(String species, int missedVisits) {
        Spec s = TABLE.get(species);
        if (s == null || s.plan() != Plan.HIDER) return false;
        int allowed = species.equals("mule_deer") ? 1 : 2;
        return missedVisits >= allowed;
    }

    /** A lone hider is not an orphan while its mother is within this distance (a deer doe within 300 m). */
    public static double motherWithinM(String species) { return species.equals("mule_deer") ? 300 : 100; }

    /** Share of leverets a player finds that are orphaned (15%). */
    public static final double FOUND_ORPHAN_SHARE = 0.15;
}
